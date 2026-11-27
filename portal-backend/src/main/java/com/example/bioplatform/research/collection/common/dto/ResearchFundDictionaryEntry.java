package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/**
 * 基金编号到官方英文全称的本地字典记录。
 */
@Data
public class ResearchFundDictionaryEntry {
    /**
     * grantId：资助项目编号，用于查询本地基金名称字典。
     */
    private String grantId;
    /**
     * fullName：本地字典中的规范英文全称。
     */
    private String fullName;
}
