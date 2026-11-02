package com.enterprise.asset.auth.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.auth.entity.Department;
import com.enterprise.asset.auth.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 部门控制器 - 处理部门CRUD操作 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * GET /api/departments - 获取所有部门列表
     * 
     * @return 部门列表
     */
    @GetMapping
    public ResponseEntity<Result<List<Department>>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(Result.success(departments));
    }

    /**
     * GET /api/departments/active - 获取活跃部门列表
     * 
     * @return 活跃部门列表
     */
    @GetMapping("/active")
    public ResponseEntity<Result<List<Department>>> getActiveDepartments() {
        List<Department> departments = departmentService.getActiveDepartments();
        return ResponseEntity.ok(Result.success(departments));
    }

    /**
     * GET /api/departments/{id} - 根据ID获取部门
     * 
     * @param id 部门ID
     * @return 部门详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<Department>> getDepartmentById(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        if (department == null) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success(department));
    }

    /**
     * GET /api/departments/code/{deptCode} - 根据编码获取部门
     * 
     * @param deptCode 部门编码
     * @return 部门详情
     */
    @GetMapping("/code/{deptCode}")
    public ResponseEntity<Result<Department>> getDepartmentByCode(@PathVariable String deptCode) {
        Department department = departmentService.getDepartmentByCode(deptCode);
        if (department == null) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success(department));
    }

    /**
     * POST /api/departments - 创建新部门
     * 
     * @param department 部门实体（自动生成编码）
     * @return 创建后的部门
     */
    @PostMapping
    public ResponseEntity<Result<Department>> createDepartment(@RequestBody Department department) {
        if (department.getDeptCode() == null || department.getDeptCode().isEmpty()) {
            department.setDeptCode(departmentService.generateDeptCode());
        }
        Department createdDepartment = departmentService.createDepartment(department);
        return ResponseEntity.ok(Result.success(createdDepartment));
    }

    /**
     * GET /api/departments/generate-code - 生成部门编码
     * 
     * @return 部门编码
     */
    @GetMapping("/generate-code")
    public ResponseEntity<Result<String>> generateDeptCode() {
        String deptCode = departmentService.generateDeptCode();
        return ResponseEntity.ok(Result.success(deptCode));
    }

    /**
     * PUT /api/departments/{id} - 更新部门信息
     * 
     * @param id         部门ID
     * @param department 更新数据
     * @return 更新后的部门
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<Department>> updateDepartment(
            @PathVariable Long id,
            @RequestBody Department department) {
        Department updatedDepartment = departmentService.updateDepartment(id, department);
        if (updatedDepartment == null) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedDepartment));
    }

    /**
     * DELETE /api/departments/{id} - 删除部门
     * 
     * @param id 门ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteDepartment(@PathVariable Long id) {
        boolean deleted = departmentService.deleteDepartment(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /**
     * GET /api/departments/count - 获取活跃部门数量
     * 
     * @return 部门数量
     */
    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getActiveDepartmentCount() {
        long count = departmentService.getActiveDepartmentCount();
        return ResponseEntity.ok(Result.success(count));
    }
}