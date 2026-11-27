package com.example.bioplatform.research.collection.common.service.relation;

import java.util.Map;

/**
 * 每个原库自己的业务唯一值解释器。
 */
public interface RelationIdentifierResolver {
    /**
     * 来源唯一值的类型与内容组合，用于原库写入、查询和关联边定位。
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param value 待校验、转换或合并的值
     */
    record Identifier(String type, String value) {
    }

    /**
     * 判断本实现是否支持指定数据库来源；工厂据此选择专用实现，匹配成功不代表已经执行写入。
     * @param database 本次处理的数据库来源编码
     * @return true表示满足上述条件，false表示不满足
     */
    boolean supports(String database);

    /**
     * 返回工厂选择优先级；当前工厂按升序排列，数值越小越先匹配。
     * @return 选择优先级，数值越小越先匹配
     */
    int priority();

    /**
     * 把原始标识解析成规范的类型与值，或建立当前上下文的身份对应关系。
     * @param database 本次处理的数据库来源编码
     * @param identifiers 请求提供的唯一值名称与内容映射
     * @return 按上述规则计算或读取的Identifier结果
     */
    Identifier resolve(String database, Map<String, String> identifiers);
}
