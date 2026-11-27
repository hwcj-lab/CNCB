package com.example.bioplatform.research.preparation.model;

import java.util.List;

/**
 * 整理入口的来源选择与批次参数；空来源列表表示全部启用来源。
 * @param sourceCodes 参与本次处理的来源编码集合或拼接值
 * @param batchSize 每批读取或写入的记录数量上限
 */
public record PreparationRequest(List<String> sourceCodes, Integer batchSize) {
}
