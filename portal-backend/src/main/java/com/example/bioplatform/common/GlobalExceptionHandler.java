package com.example.bioplatform.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
/** 将控制器异常转换为统一响应，并避免向客户端泄露堆栈或连接信息。 */
public class GlobalExceptionHandler {
    /**
     * 处理服务层明确声明的业务错误。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 统一处理 Bean Validation、类型转换和参数绑定错误。
     */
    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class, BindException.class, IllegalArgumentException.class, org.springframework.web.bind.ServletRequestBindingException.class})
    public ResponseEntity<ApiResponse<Void>> invalid(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", "参数格式或范围无效，请检查年份、类型和ID"));
    }

    /**
     * 处理访问不存在的静态资源或接口。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(Exception e) {
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "接口不存在"));
    }

    /**
     * 处理请求方式不匹配。
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> method(Exception e) {
        return ResponseEntity.status(405).body(ApiResponse.error("METHOD_NOT_ALLOWED", "该接口不支持此请求方式"));
    }

    /**
     * 兜底记录未知异常，客户端仅获得通用提示。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
        log.error("请求处理失败", e);
        return ResponseEntity.status(500).body(ApiResponse.error("INTERNAL_ERROR", "服务暂不可用，请检查后端日志和数据库连接"));
    }
}
