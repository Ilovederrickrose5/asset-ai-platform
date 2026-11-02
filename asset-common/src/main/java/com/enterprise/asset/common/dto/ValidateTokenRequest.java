package com.enterprise.asset.common.dto;

import lombok.Data;

/**
 * Token验证请求DTO
 */
@Data
public class ValidateTokenRequest {
    private String token;
}