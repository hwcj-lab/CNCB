package com.example.bioplatform.research.collection.common.service.relation;

import java.io.IOException;

/**
 * Both metadata services returned valid responses without the requested PMID.
 */
public class PublicationNotFoundException extends IOException {
    /**
     * 构造PublicationNotFoundException，注入本组件所需依赖并建立初始状态。
     * @param pmid 已有PubMed标识；为空时不通过标题猜补
     */
    public PublicationNotFoundException(String pmid) {
        super("OpenLB及PubMed均未返回PMID " + pmid);
    }
}
