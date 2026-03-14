package com.ev.charging.exception;

import com.ev.charging.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于表示应用业务逻辑中的错误，所有已知的业务错误应该抛出此异常
 * GlobalExceptionHandler 会捕获此异常并返回相应的状态码和错误消息
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 使用错误消息构造异常，默认状态码为500
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 使用状态码和错误消息构造异常
     *
     * @param code    状态码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用ResultCode枚举构造异常（推荐使用）
     * 便于维护错误码，避免魔法数字
     *
     * @param resultCode 结果码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用错误消息和根原因构造异常，默认状态码为500
     *
     * @param message 错误消息
     * @param cause   根原因异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    /**
     * 使用状态码、错误消息和根原因构造异常
     *
     * @param code    状态码
     * @param message 错误消息
     * @param cause   根原因异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 使用ResultCode枚举和根原因构造异常
     *
     * @param resultCode 结果码枚举
     * @param cause      根原因异常
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
    }
}
