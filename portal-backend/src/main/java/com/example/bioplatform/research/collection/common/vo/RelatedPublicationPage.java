package com.example.bioplatform.research.collection.common.vo;


import java.util.List;

/** 分页关联文献结果。 */
public record RelatedPublicationPage(int page, int size, long total, List<RelatedPublicationView> items) {}
