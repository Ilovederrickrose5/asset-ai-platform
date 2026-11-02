package com.enterprise.asset.auth.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.auth.entity.Role;
import com.enterprise.asset.auth.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 角色控制器 - 处理角色CRUD操作 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * GET /api/roles - 获取所有角色列表
     * 
     * @return 角色列表
     */
    @GetMapping
    public ResponseEntity<Result<List<Role>>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(Result.success(roles));
    }

    /**
     * GET /api/roles/{id} - 根据ID获取角色
     * 
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<Role>> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return ResponseEntity.ok(Result.error(404, "角色不存在"));
        }
        return ResponseEntity.ok(Result.success(role));
    }

    /**
     * POST /api/roles - 创建新角色
     * 
     * @param role 角色实体
     * @return 创建后的角色
     */
    @PostMapping
    public ResponseEntity<Result<Role>> createRole(@RequestBody Role role) {
        Role createdRole = roleService.createRole(role);
        return ResponseEntity.ok(Result.success(createdRole));
    }

    /**
     * PUT /api/roles/{id} - 更新角色信息
     * 
     * @param id   角色ID
     * @param role 更新数据
     * @return 更新后的角色
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<Role>> updateRole(
            @PathVariable Long id,
            @RequestBody Role role) {
        Role updatedRole = roleService.updateRole(id, role);
        if (updatedRole == null) {
            return ResponseEntity.ok(Result.error(404, "角色不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedRole));
    }

    /**
     * DELETE /api/roles/{id} - 删除角色
     * 
     * @param id 角色ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteRole(@PathVariable Long id) {
        boolean deleted = roleService.deleteRole(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "角色不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }
}