package com.example.bioplatform.common;

import java.time.Instant;

/**
 * 前后端统一响应结构，确保成功和异常响应具有相同外形。
 * @param code 统一响应业务码，成功与异常均使用此字段
 * @param message 响应说明、当前阶段说明或错误提示
 * @param data 响应或处理结果载荷，具体结构由声明类型决定
 * @param timestamp 统一响应的生成时间戳，便于客户端定位请求时间
 */
public record ApiResponse<T>(String code, String message, T data, Instant timestamp) {
    /**
     * 构造成功响应并记录服务器响应时间。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "成功", data, Instant.now());
    }

    /**
     * 构造不携带业务数据的错误响应。
     */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}
