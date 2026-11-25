package com.example.bioplatform.research.collection.common.service;


import java.util.List;

/**
 * 按cncbBanks中的数据库类型解析官方资源内的关联文献PMID；可通过新增Spring组件扩展。
 */
public interface RelatedResourceResolver {
    /**
     * 支持的cncbBanks.db名称，不区分大小写。
     */
    List<String> supportedDatabaseNames();

    /**
     * 根据资源编号和官方链接读取该资源明确列出的PubMed ID。
     */
    RelatedResourceData findRelatedPublications(String accession, String officialUrl) throws Exception;
}
