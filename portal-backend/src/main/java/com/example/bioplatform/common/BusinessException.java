package com.example.bioplatform.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 可由业务层主动抛出的异常，携带 HTTP 状态与稳定错误码。 */
@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
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
