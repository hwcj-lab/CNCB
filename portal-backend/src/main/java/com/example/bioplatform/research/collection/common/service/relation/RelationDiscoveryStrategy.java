package com.example.bioplatform.research.collection.common.service.relation;

/**
 * 新增发现路径实现本接口；不得在策略里写文献表、创建队列或自行递归。
 */
public interface RelationDiscoveryStrategy {
    /**
     * 返回当前发现策略的稳定编码，用于选择路径和记录发现来源。
     * @return 按上述规则计算或读取的String结果
     */
    String code();

    /**
     * 返回工厂选择优先级；当前工厂按升序排列，数值越小越先匹配。
     * @return 选择优先级，数值越小越先匹配
     */
    int priority();

    /**
     * 判断本实现是否支持指定数据库来源；工厂据此选择专用实现，匹配成功不代表已经执行写入。
     * @param input 本次处理的统一输入
     * @return true表示满足上述条件，false表示不满足
     */
    boolean supports(RelationDiscoveryInput input);

    /**
     * 按本策略从已有资源元数据发现关联目标，返回目标及证据；不自行展开后续递归。
     * @param input 本次处理的统一输入
     * @param control 每次请求前执行的限速或停止检查回调
     * @return 按上述规则计算或读取的RelationDiscoveryResult结果
     */
    RelationDiscoveryResult discover(RelationDiscoveryInput input, BeforeRequest control) throws Exception;

    /**
     * 关联策略每次请求前的限速和停止检查扩展点。
     */
    @FunctionalInterface
    interface BeforeRequest {
        /**
         * 每次上游请求前执行限速或停止检查；抛出异常时调用方不得继续发送该请求。
         */
        void run() throws InterruptedException;
    }
}
