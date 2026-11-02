package com.enterprise.asset.business.client.fallback;

import com.enterprise.asset.business.client.AuthFeignClient;
import com.enterprise.asset.common.dto.DepartmentDTO;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.dto.ValidateTokenRequest;
import com.enterprise.asset.common.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * AuthFeignClient 降级工厂
 * - 触发时机: 调用 asset-auth 超时/异常/被 Sentinel 熔断
 * - 用 FallbackFactory 而非 fallback 属性: 能拿到 cause 异常, 便于日志排查
 * - 设计原则: 返回 Result.error(...) 让上层 Controller 按统一格式处理, 不抛异常
 */
@Slf4j
@Component
public class AuthFeignClientFallbackFactory implements FallbackFactory<AuthFeignClient> {

    @Override
    public AuthFeignClient create(Throwable cause) {
        log.warn("调用 asset-auth 服务失败, 触发降级. 原因: {}", cause.getMessage());

        return new AuthFeignClient() {
            @Override
            public Result<UserDTO> validateToken(ValidateTokenRequest request) {
                return Result.error(503, "认证服务暂不可用, Token 校验降级");
            }

            @Override
            public Result<UserDTO> getUserById(Long id) {
                return Result.error(503, "认证服务暂不可用, 用户查询降级");
            }

            @Override
            public Result<UserDTO> getUserByUsername(String username) {
                return Result.error(503, "认证服务暂不可用, 用户查询降级");
            }

            @Override
            public Result<UserDTO> getCurrentUser() {
                return Result.error(503, "认证服务暂不可用, 当前用户查询降级");
            }

            @Override
            public Result<Long> getUserCount() {
                // 统计类降级返回 0, 避免阻塞报表渲染
                return Result.success(0L);
            }

            @Override
            public Result<List<UserDTO>> getAllUsers() {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<UserDTO>> getUsersByDepartment(Long deptId) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Long> countUsersByDepartment(Long deptId) {
                return Result.success(0L);
            }

            @Override
            public Result<DepartmentDTO> getDepartmentById(Long id) {
                return Result.error(503, "认证服务暂不可用, 部门查询降级");
            }

            @Override
            public Result<Long> getDepartmentUserCount(Long deptId) {
                return Result.success(0L);
            }

            @Override
            public Result<List<DepartmentDTO>> getAllDepartments() {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Long> getDepartmentCount() {
                return Result.success(0L);
            }
        };
    }
}
