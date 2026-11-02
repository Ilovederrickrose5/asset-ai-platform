package com.enterprise.asset.business.exception;

/**
 * 业务异常: 用于状态机校验失败、资产占用冲突、并发审批冲突等业务层错误
 * 由全局异常处理器捕获后返回友好提示给前端
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 409;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
