package com.example.bioplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/** Web 层公共配置，目前仅开放受控来源对 API 的跨域访问。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors-origins}")
    private String[] origins;

    /**
     * 为 API 路径注册开发环境跨域规则。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 采集管理接口使用POST；仍不开放PUT/DELETE等当前项目未使用的方法。
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET", "POST", "OPTIONS").maxAge(3600);
    }
}
