package com.enterprise.asset.auth.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.auth.entity.User;
import com.enterprise.asset.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 用户控制器 - 处理用户CRUD及角色管理 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users - 获取所有用户列表
     * 
     * @return 用户列表
     */
    @GetMapping
    public ResponseEntity<Result<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(Result.success(users));
    }

    /**
     * GET /api/users/active - 获取活跃用户列表
     * 
     * @return 活跃用户列表
     */
    @GetMapping("/active")
    public ResponseEntity<Result<List<User>>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        return ResponseEntity.ok(Result.success(users));
    }

    /**
     * GET /api/users/{id} - 根据ID获取用户详情
     * 
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * GET /api/users/username/{username} - 根据用户名获取用户
     * 
     * @param username 用户名
     * @return 用户详情
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<Result<User>> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * GET /api/users/department/{departmentId} - 获取部门用户列表
     * 
     * @param departmentId 部门ID
     * @return 用户列表
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<List<User>>> getUsersByDepartment(@PathVariable Long departmentId) {
        List<User> users = userService.getUsersByDepartment(departmentId);
        return ResponseEntity.ok(Result.success(users));
    }

    /**
     * POST /api/users - 创建新用户
     * 
     * @param user 用户实体（用户名、密码、姓名、部门等）
     * @return 创建后的用户
     */
    @PostMapping
    public ResponseEntity<Result<User>> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(Result.success(createdUser));
    }

    /**
     * PUT /api/users/{id} - 更新用户信息
     * 
     * @param id   用户ID
     * @param user 更新的用户数据
     * @return 更新后的用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<User>> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        if (updatedUser == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedUser));
    }

    /**
     * DELETE /api/users/{id} - 删除用户
     * 
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /**
     * PUT /api/users/{userId}/roles - 更新用户角色
     * 
     * @param userId    用户ID
     * @param roleCodes 角色编码列表（取第一个）
     * @return 更新后的用户
     */
    @PutMapping("/{userId}/roles")
    public ResponseEntity<Result<User>> updateRole(
            @PathVariable Long userId,
            @RequestBody List<String> roleCodes) {
        String roleCode = roleCodes != null && !roleCodes.isEmpty() ? roleCodes.get(0) : null;
        User user = userService.updateRole(userId, roleCode);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * GET /api/users/count - 获取活跃用户数量
     * 
     * @return 用户数量
     */
    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getActiveUserCount() {
        long count = userService.getActiveUserCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/users/count/department/{departmentId} - 获取部门用户数量
     * 
     * @param departmentId 部门ID
     * @return 用户数量
     */
    @GetMapping("/count/department/{departmentId}")
    public ResponseEntity<Result<Long>> getUserCountByDepartment(@PathVariable Long departmentId) {
        long count = userService.getUserCountByDepartment(departmentId);
        return ResponseEntity.ok(Result.success(count));
    }
}