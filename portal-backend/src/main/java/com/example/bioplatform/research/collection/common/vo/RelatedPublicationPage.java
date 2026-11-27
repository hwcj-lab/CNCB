package com.example.bioplatform.research.collection.common.vo;


import java.util.List;

/**
 * 分页关联文献结果。
 * @param page 分页页码或单页结果
 * @param size 本次读取的最大条数或目标容量
 * @param total 当前范围内的总量
 * @param items 当前分页返回的文献或关联结果列表
 */
public record RelatedPublicationPage(int page, int size, long total, List<RelatedPublicationView> items) {
}
