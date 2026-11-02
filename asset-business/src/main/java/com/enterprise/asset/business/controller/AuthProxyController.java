package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.dto.DepartmentDTO;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.client.AuthFeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证服务代理控制器
 * 【本次修改点】前端请求/api/departments、/api/users时，通过Feign调用auth服务获取数据
 * 解决business服务缺少用户/部门接口的问题
 */
@RestController
@RequestMapping("/api")
public class AuthProxyController {

    private final AuthFeignClient authFeignClient;

    public AuthProxyController(AuthFeignClient authFeignClient) {
        this.authFeignClient = authFeignClient;
    }

    /**
     * GET /api/departments - 获取所有部门列表（转发到auth服务）
     */
    @GetMapping("/departments")
    public ResponseEntity<Result<List<DepartmentDTO>>> getAllDepartments() {
        Result<List<DepartmentDTO>> result = authFeignClient.getAllDepartments();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/users - 获取所有用户列表（转发到auth服务）
     */
    @GetMapping("/users")
    public ResponseEntity<Result<List<UserDTO>>> getAllUsers() {
        Result<List<UserDTO>> result = authFeignClient.getAllUsers();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/departments/{id} - 根据ID获取部门（转发到auth服务）
     */
    @GetMapping("/departments/{id}")
    public ResponseEntity<Result<DepartmentDTO>> getDepartmentById(@PathVariable Long id) {
        Result<DepartmentDTO> result = authFeignClient.getDepartmentById(id);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/users/{id} - 根据ID获取用户（转发到auth服务）
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Result<UserDTO>> getUserById(@PathVariable Long id) {
        Result<UserDTO> result = authFeignClient.getUserById(id);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/users/username/{username} - 根据用户名获取用户（转发到auth服务）
     */
    @GetMapping("/users/username/{username}")
    public ResponseEntity<Result<UserDTO>> getUserByUsername(@PathVariable String username) {
        Result<UserDTO> result = authFeignClient.getUserByUsername(username);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/users/department/{deptId} - 根据部门ID获取用户列表（转发到auth服务）
     */
    @GetMapping("/users/department/{deptId}")
    public ResponseEntity<Result<List<UserDTO>>> getUsersByDepartment(@PathVariable Long deptId) {
        Result<List<UserDTO>> result = authFeignClient.getUsersByDepartment(deptId);
        return ResponseEntity.ok(result);
    }
}