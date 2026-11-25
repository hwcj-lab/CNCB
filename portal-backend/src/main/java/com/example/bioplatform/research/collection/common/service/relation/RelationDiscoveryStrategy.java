package com.example.bioplatform.research.collection.common.service.relation;

/**
 * 新增发现路径实现本接口；不得在策略里写文献表、创建队列或自行递归。
 */
public interface RelationDiscoveryStrategy {
    String code();

    int priority();

    boolean supports(RelationDiscoveryInput input);

    RelationDiscoveryResult discover(RelationDiscoveryInput input, BeforeRequest control) throws Exception;

    @FunctionalInterface
    interface BeforeRequest {
        void run() throws InterruptedException;
    }
}
