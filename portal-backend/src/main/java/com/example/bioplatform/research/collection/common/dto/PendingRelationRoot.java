package com.example.bioplatform.research.collection.common.dto;


/** 来源表中等待关联发现的根文献唯一值。
 * @param identifierType 唯一值类别，例如PMID、DOI或SOURCE_KEY
 * @param identifierValue 与identifierType配套的原始业务唯一值
 */
public record PendingRelationRoot(String identifierType, String identifierValue) {}
