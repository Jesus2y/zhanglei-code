package com.logistics.calculator.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用程序中抛出的各类异常，返回标准化的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理运行时异常
     * 
     * @param e 运行时异常对象
     * @return 错误响应（HTTP 400，包含错误消息）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 处理参数验证异常
     * 当请求参数不符合验证规则时触发
     * 
     * @param e 方法参数验证异常对象
     * @return 错误响应（HTTP 400，包含各字段的验证错误详情）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "参数验证失败");
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        response.put("errors", errors);
        
        return ResponseEntity.badRequest().body(response);
    }
}
