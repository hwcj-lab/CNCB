package com.example.bioplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = {"com.example.bioplatform.catalog.mapper", "com.example.bioplatform.research"}, annotationClass = org.apache.ibatis.annotations.Mapper.class)
/** 生物数据资源推荐与科研产出分析平台后端启动入口。 */
public class BioPlatformApplication {
    /**
     * 启动 Spring Boot 应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(BioPlatformApplication.class, args);
    }
}
