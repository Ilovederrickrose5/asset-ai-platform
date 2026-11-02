package com.enterprise.asset.common.dto;

import lombok.Data;

/**
 * Token验证响应DTO
 */
@Data
public class ValidateTokenResponse {
    private Boolean valid;
    private String message;
    private UserDTO user;  // 如果token有效，返回用户信息
}