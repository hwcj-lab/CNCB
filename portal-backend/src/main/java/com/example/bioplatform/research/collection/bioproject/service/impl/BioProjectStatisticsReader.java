package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目列表以磁盘快照流式读取；每个项目全部论文落库后才提交项目游标。
 */
public final class BioProjectStatisticsReader {
    /**
     * 按项目编号加载项目详情的回调，与列表流式遍历解耦。
     */
    @FunctionalInterface
    public interface ProjectLoader {
        /**
         * 执行约定的数据加载回调，具体读取来源由注入实现决定。
         * @param row 本次处理的一条记录
         * @return 按上述规则计算或读取的JsonNode结果
         */
        JsonNode load(JsonNode row) throws Exception;
    }

    /**
     * 消费一批来源记录的回调；调用成功后读取器才推进持久化进度。
     */
    @FunctionalInterface
    public interface BatchConsumer {
        /**
         * 消费已解析的一批记录；只有调用成功后，读取方才可继续推进对应批次。
         * @param rows 本批待处理记录
         * @param nextIndex 下一条待处理列表记录的索引
         * @return true表示满足上述条件，false表示不满足
         */
        boolean accept(List<JsonNode> rows, long nextIndex) throws Exception;
    }

    /**
     * 流式读取项目列表，并对项目全部出版记录分批回调；项目完成后才推进相应项目索引。
     * @param file 已经保存到本地的官方列表快照文件路径
     * @param after 上次已提交的扫描ID
     * @param size 本次读取的最大条数或目标容量
     * @param json JSON解析与序列化组件
     * @param loader 实际读取资源数据的回调
     * @param sink 接收当前批次的回调或集合
     * @return true表示满足上述条件，false表示不满足
     */
    public static boolean read(Path file, long after, int size, ObjectMapper json, ProjectLoader loader, BatchConsumer sink) throws Exception {
        try (var in = Files.newInputStream(file); var parser = json.getFactory().createParser(in)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) throw new IOException("BioProject列表不是JSON数组");
            long index = 0;
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() != JsonToken.START_OBJECT)
                    throw new IOException("BioProject列表条目不是对象");
                if (index++ < after) {
                    parser.skipChildren();
                    continue;
                }
                JsonNode row = json.readTree(parser);
                JsonNode project = loader.load(row);
                List<JsonNode> batch = new ArrayList<>(size);
                for (JsonNode paper : project.path("listPublication")) {
                    var enriched = paper.deepCopy();
                    if (!enriched.isObject()) continue;
                    ((com.fasterxml.jackson.databind.node.ObjectNode) enriched).put("proAccession", BioProjectPublicationParser.projectCode(row));
                    ((com.fasterxml.jackson.databind.node.ObjectNode) enriched).set("projectGrants", project.path("listGrants"));
                    ((com.fasterxml.jackson.databind.node.ObjectNode) enriched).set("projectOrganization", project.path("submitter").path("organization"));
                    batch.add(enriched);
                    // 项目最后一个批次之前不推进项目游标；异常后重做本项目，UPSERT幂等。
                    if (batch.size() == size) {
                        if (!sink.accept(List.copyOf(batch), index - 1)) return false;
                        batch.clear();
                    }
                }
                if (!sink.accept(List.copyOf(batch), index)) return false;
            }
            if (index < after) throw new IOException("BioProject固定列表短于已提交游标");
            return true;
        }
    }
}
