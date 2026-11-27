package com.example.bioplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;

/** Swagger/OpenAPI 元数据配置。 */
@Configuration
public class OpenApiConfig {
    /**
     * 声明平台接口文档的标题、版本及数据真实性说明。
     */
    @Bean
    OpenAPI api() {
        return new OpenAPI().info(new Info().title("生物数据资源与科研产出平台 API").version("1.0.0").description("目录来源于research；REAL为有限调研样例，DEMO为独立合成数据。待核字段不当作已确认事实。"));
    }
}
