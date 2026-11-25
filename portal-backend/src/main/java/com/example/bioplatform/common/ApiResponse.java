package com.example.bioplatform.common;

import java.time.Instant;

/** 前后端统一响应结构，确保成功和异常响应具有相同外形。 */
public record ApiResponse<T>(String code, String message, T data, Instant timestamp) {
    /** 构造成功响应并记录服务器响应时间。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "成功", data, Instant.now());
    }

    /** 构造不携带业务数据的错误响应。 */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}
