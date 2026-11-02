package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 部门数据访问接口
 * 关联表: department(部门表)
 * 主要操作: 部门的增删改查、部门负责人查询
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /** 根据状态统计部门数量 */
    long countByStatus(Integer status);

    /** 根据部门编码查询部门 */
    Optional<Department> findByDeptCode(String deptCode);

    /** 根据部门名称查询部门 */
    Optional<Department> findByDeptName(String deptName);

    /** 根据负责人ID查询部门列表 */
    List<Department> findByLeader(Long leader);
}