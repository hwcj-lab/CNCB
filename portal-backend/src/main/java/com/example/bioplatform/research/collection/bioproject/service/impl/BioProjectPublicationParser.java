package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.example.bioplatform.research.collection.bioproject.dto.BioProjectPublicationSeed;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaPublicationParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 原始项目机构保留在原始论文载荷内；只从论文元数据提取作者署名。
 */
public final class BioProjectPublicationParser {
    /**
     * 从项目或列表记录提取明确的BioProject编号。
     * @param row 本次处理的一条记录
     * @return 按上述规则计算或读取的String结果
     */
    public static String projectCode(JsonNode row) {
        String code = com.example.bioplatform.research.common.PublicationValues.text(row, "accession");
        if (!code.matches("PRJ(?:CA|NA|EB|DA)[A-Z0-9]+"))
            throw new IllegalArgumentException("BioProject项目编号不合法：" + code);
        return code;
    }

    /**
     * 将本类约定的原始输入转换成规范结构；不以字段缺失为由猜造PMID或作者归属。
     * @param row 本次处理的一条记录
     * @param json JSON解析与序列化组件
     * @return 按上述规则计算或读取的BioProjectPublicationSeed结果
     */
    public static BioProjectPublicationSeed parse(JsonNode row, ObjectMapper json) {
        var converted = row.deepCopy();
        var object = (com.fasterxml.jackson.databind.node.ObjectNode) converted;
        object.put("journal", com.example.bioplatform.research.common.PublicationValues.text(row, "journalTitle"));
        object.put("pDate", com.example.bioplatform.research.common.PublicationValues.text(row, "year"));
        var original = GsaPublicationParser.parse(converted, json);
        var seed = new BioProjectPublicationSeed();
        org.springframework.beans.BeanUtils.copyProperties(original, seed);
        seed.setRawPayload(row.toString());
        seed.setSourceHash(sha(row.toString()));
        seed.setGrantsJson(row.path("projectGrants").isArray() ? row.path("projectGrants").toString() : "[]");
        if (seed.getPmid() == null && seed.getDoi() == null)
            seed.setSourceUrl("https://ngdc.cncb.ac.cn/bioproject/browse/" + seed.getBioprojectAccession());
        return seed;
    }

    /**
     * 为业务字符串生成稳定摘要，便于索引和比较；摘要不能替代原始业务含义。
     */
    public static String sha(String value) {
        return com.example.bioplatform.research.common.PublicationValues.sha(value);
    }
}
