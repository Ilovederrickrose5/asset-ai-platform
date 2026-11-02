package com.enterprise.asset.business.client;

import com.enterprise.asset.business.client.fallback.AuthFeignClientFallbackFactory;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.dto.DepartmentDTO;
import com.enterprise.asset.common.dto.ValidateTokenRequest;
import com.enterprise.asset.common.util.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Auth服务Feign客户端
 * 用于业务服务调用认证服务的用户、部门等接口
 *
 * fallbackFactory: Sentinel 触发熔断/调用异常时进入降级工厂
 * 触发条件需 application.yml 配置 feign.sentinel.enabled=true
 */
@FeignClient(name = "asset-auth", path = "/api/auth", fallbackFactory = AuthFeignClientFallbackFactory.class)
public interface AuthFeignClient {

    @PostMapping("/validate-token")
    Result<UserDTO> validateToken(@RequestBody ValidateTokenRequest request);

    @GetMapping("/users/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/username/{username}")
    Result<UserDTO> getUserByUsername(@PathVariable("username") String username);

    @GetMapping("/users/current")
    Result<UserDTO> getCurrentUser();

    @GetMapping("/users/count")
    Result<Long> getUserCount();

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    Result<List<UserDTO>> getAllUsers();

    /**
     * 根据部门ID获取用户列表
     */
    @GetMapping("/users/department/{deptId}")
    Result<List<UserDTO>> getUsersByDepartment(@PathVariable("deptId") Long deptId);

    /**
     * 统计指定部门的用户数量
     */
    @GetMapping("/users/count/dept/{deptId}")
    Result<Long> countUsersByDepartment(@PathVariable("deptId") Long deptId);

    @GetMapping("/departments/{id}")
    Result<DepartmentDTO> getDepartmentById(@PathVariable("id") Long id);

    @GetMapping("/departments/stats")
    Result<Long> getDepartmentUserCount(@RequestParam("deptId") Long deptId);

    /**
     * 获取所有部门列表
     */
    @GetMapping("/departments")
    Result<List<DepartmentDTO>> getAllDepartments();

    /**
     * 统计启用状态的部门数量
     */
    @GetMapping("/departments/count")
    Result<Long> getDepartmentCount();
}