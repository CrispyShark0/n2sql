package com.itheima.n2sql.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 *
 * 当业务逻辑出现问题时（比如数据源不存在、SQL执行失败），
 * 就抛出这个异常，它会被 GlobalExceptionHandler 自动捕获处理。
 *
 * 使用示例：
 *   throw new BizException("数据源不存在");
 *   throw new BizException(404, "数据源不存在");
 */
@Getter  // Lombok注解：自动生成 getter 方法
public class BizException extends RuntimeException {

    /** 错误状态码，默认500 */
    private final int code;

    /**
     * 只传错误消息（状态码默认500）
     * 用法：throw new BizException("数据源连接失败");
     */
    public BizException(String message) {
        super(message);         // 调用父类构造方法，设置异常消息
        this.code = 500;
    }

    /**
     * 传状态码 + 错误消息
     * 用法：throw new BizException(404, "数据源不存在");
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 传错误消息 + 原始异常（用于异常链，方便排查根本原因）
     * 用法：catch(SQLException e) { throw new BizException("SQL执行失败", e); }
     */
    public BizException(String message, Throwable cause) {
        super(message, cause);  // cause 就是原始异常，日志里能看到完整的错误链
        this.code = 500;
    }
}
