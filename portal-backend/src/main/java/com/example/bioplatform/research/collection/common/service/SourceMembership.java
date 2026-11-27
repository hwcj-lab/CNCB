package com.example.bioplatform.research.collection.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * 通过明确cncbBanks路由判断本库归属；发现关联边本身不能证明目标文献属于当前来源。
 */
@Component
public class SourceMembership {
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;

    /**
     * 构造SourceMembership，注入本组件所需依赖并建立初始状态。
     * @param json JSON解析与序列化组件
     */
    public SourceMembership(ObjectMapper json) {
        this.json = json;
    }

    /**
     * 根据明确的cncbBanks路由判断目标是否属于当前来源，单有一条关联边不能证明本库归属。
     * @param code 当前来源、策略或业务响应使用的编码
     * @param payload 接口原始响应内容
     * @param routes 来源路由持久层或已加载的路由配置
     * @return true表示满足上述条件，false表示不满足
     */
    public boolean belongs(String code, String payload, Map<String, String> routes) {
        if (payload == null || payload.isBlank()) return false;
        try {
            var banks = json.readTree(payload);
            if (banks == null || !banks.isArray()) return false;
            for (var bank : banks) {
                var target = routes.get(bank.path("db").asText("").trim().toLowerCase(Locale.ROOT));
                if (code.equalsIgnoreCase(target)) return true;
            }
            return false;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid cncbBanks JSON", ex);
        }
    }
}
