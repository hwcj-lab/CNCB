package com.example.bioplatform.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 可由业务层主动抛出的异常，携带 HTTP 状态与稳定错误码。 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * status：当前对象的业务状态，由对应任务或目录状态机定义。
     */
    private final HttpStatus status;
    /**
     * code：当前来源、策略或业务响应使用的编码。
     */
    private final String code;

    /**
     * 创建一个可安全返回给客户端的业务异常。
     */
    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
