package com.enterprise.asset.ai.exception;

/**
 * 认证失败 / Token 无效（HTTP 401）。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
