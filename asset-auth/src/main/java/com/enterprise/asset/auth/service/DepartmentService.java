package com.enterprise.asset.auth.service;

import com.enterprise.asset.auth.entity.Department;
import com.enterprise.asset.auth.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 部门服务 - 处理部门CRUD操作与编码生成 */
@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /** 获取所有部门列表 */
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    /** 获取活跃部门列表（status=1） */
    public List<Department> getActiveDepartments() {
        return departmentRepository.findAll().stream()
                .filter(dept -> dept.getStatus() == 1)
                .toList();
    }

    /** 根据ID获取部门 */
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id).orElse(null);
    }

    /** 根据部门编码获取部门 */
    public Department getDepartmentByCode(String deptCode) {
        return departmentRepository.findByDeptCode(deptCode).orElse(null);
    }

    /** 创建部门 */
    @Transactional
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    /**
     * 更新部门信息
     * 事务处理：@Transactional保证数据一致性
     */
    @Transactional
    public Department updateDepartment(Long id, Department department) {
        Department existingDepartment = departmentRepository.findById(id).orElse(null);
        if (existingDepartment == null) {
            return null;
        }

        existingDepartment.setDeptName(department.getDeptName());
        existingDepartment.setDeptCode(department.getDeptCode());
        existingDepartment.setParentId(department.getParentId());
        existingDepartment.setLeader(department.getLeader());
        existingDepartment.setDescription(department.getDescription());
        existingDepartment.setStatus(department.getStatus());

        return departmentRepository.save(existingDepartment);
    }

    /** 根据领导ID查询部门 */
    public List<Department> getDepartmentsByLeader(Long leaderId) {
        return departmentRepository.findByLeader(leaderId);
    }

    /**
     * 删除部门
     * 事务处理：@Transactional保证数据一致性
     */
    @Transactional
    public boolean deleteDepartment(Long id) {
        if (departmentRepository.existsById(id)) {
            departmentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /** 获取活跃部门数量 */
    public long getActiveDepartmentCount() {
        return departmentRepository.countByStatus(1);
    }

    /**
     * 生成部门编码
     * 规则：DEPT + 3位数字，从001开始递增
     */
    public String generateDeptCode() {
        List<Department> departments = departmentRepository.findAll();

        int maxCode = 0;
        for (Department dept : departments) {
            String deptCode = dept.getDeptCode();
            if (deptCode != null && deptCode.startsWith("DEPT")) {
                try {
                    String numPart = deptCode.substring(4);
                    int code = Integer.parseInt(numPart);
                    if (code > maxCode) {
                        maxCode = code;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        return "DEPT" + String.format("%03d", maxCode + 1);
    }

    /** 检查部门编码是否存在 */
    public boolean isDeptCodeExists(String deptCode) {
        return departmentRepository.findByDeptCode(deptCode).isPresent();
    }
}