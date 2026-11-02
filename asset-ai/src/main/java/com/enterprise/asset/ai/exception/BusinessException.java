package com.enterprise.asset.ai.exception;

/**
 * 可安全返回给前端的业务异常（HTTP 400）。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
