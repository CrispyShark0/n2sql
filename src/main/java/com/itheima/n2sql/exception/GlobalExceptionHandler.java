package com.itheima.n2sql.exception;

import com.itheima.n2sql.model.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 — 所有 Controller 中抛出的异常都会被这里捕获
 *
 * 工作原理：
 *   1. Controller 方法执行时抛出异常
 *   2. Spring 发现这个类上有 @RestControllerAdvice 注解
 *   3. 根据异常类型匹配对应的 @ExceptionHandler 方法
 *   4. 执行该方法，把返回值作为 HTTP 响应返回给前端
 *
 * 这样做的好处：
 *   - Controller 里不需要 try-catch，代码更干净
 *   - 所有错误返回格式统一，前端好处理
 *   - 异常信息会被记录到日志，方便排查
 */
@Slf4j  // Lombok注解：自动生成一个 log 对象，用于打印日志
@RestControllerAdvice  // 告诉 Spring：我是全局异常处理器
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     * 当代码中 throw new BizException("xxx") 时，就会进入这个方法
     */
    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e) {
        // 记录警告日志（业务异常通常是预期内的，用 warn 级别）
        log.warn("业务异常: {}", e.getMessage());
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     * 当接口参数加了 @NotBlank 等校验注解，校验不通过时会抛出这个异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 从校验结果中取出第一条错误信息
        String errorMsg = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("参数校验异常: {}", errorMsg);
        return ApiResult.error(400, errorMsg);
    }

    /**
     * 处理所有未被上面方法捕获的异常（兜底）
     * 这是最后一道防线，确保任何意外错误都不会返回原始异常堆栈给前端
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        // 未知异常用 error 级别记录，并打印完整堆栈信息
        log.error("系统异常: ", e);
        return ApiResult.error(500, "系统内部错误，请联系管理员");
    }
}
