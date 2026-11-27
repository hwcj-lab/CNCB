package com.example.bioplatform.research.collection.gsa.service.impl;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 流式读取article.publications；每个批次完成入库后再推进断点，不加载整个JSON树。
 */
public final class GsaStatisticsReader {
    /**
     * 限制本工具或内部状态对象的实例化入口，具体创建方式由所属类控制。
     */
    private GsaStatisticsReader() {
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
     * 只在本地快照重读时跳过已提交索引；一轮导入只遍历文件一次。
     */
    public static boolean read(Path file, long after, int size, ObjectMapper json, BatchConsumer sink) throws Exception {
        try (InputStream in = Files.newInputStream(file); JsonParser p = json.getFactory().createParser(in)) {
            if (p.nextToken() != JsonToken.START_OBJECT)
                throw new IOException("GSA统计响应不是JSON对象，可能被安全验证拦截");
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String name = p.currentName();
                p.nextToken();
                if (!"article".equals(name)) {
                    p.skipChildren();
                    continue;
                }
                if (p.currentToken() != JsonToken.START_OBJECT) throw new IOException("article不是对象");
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String field = p.currentName();
                    p.nextToken();
                    if (!"publications".equals(field)) {
                        p.skipChildren();
                        continue;
                    }
                    if (p.currentToken() != JsonToken.START_ARRAY)
                        throw new IOException("article.publications不是数组");
                    long index = 0;
                    List<JsonNode> batch = new ArrayList<>(size);
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        if (p.currentToken() != JsonToken.START_OBJECT) throw new IOException("支持文章条目不是对象");
                        if (index++ < after) {
                            p.skipChildren();
                            continue;
                        }
                        batch.add(json.readTree(p));
                        if (batch.size() >= size) {
                            if (!sink.accept(List.copyOf(batch), index)) return false;
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty() && !sink.accept(List.copyOf(batch), index)) return false;
                    if (index < after) throw new IOException("本地快照长度小于持久化游标，请恢复原缓存文件");
                    return true;
                }
                throw new IOException("GSA统计缺少article.publications，禁止将结构变化当作采集完成");
            }
            throw new IOException("GSA统计缺少article对象");
        }
    }
}
