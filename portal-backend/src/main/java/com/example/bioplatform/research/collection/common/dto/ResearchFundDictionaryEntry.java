package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 基金编号到官方英文全称的本地字典记录。 */
@Data
public class ResearchFundDictionaryEntry {
    private String grantId;
    private String fullName;
}
