package com.enterprise.asset.auth.repository;

import com.enterprise.asset.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 权限数据访问接口
 * 关联表: permission(权限表)
 * 主要操作: 权限的增删改查、权限编码查询
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /** 根据权限编码查询权限 */
    Optional<Permission> findByCode(String code);

    /** 判断权限编码是否存在 */
    boolean existsByCode(String code);

    /** 根据父权限ID查询子权限 */
    List<Permission> findByParentId(Long parentId);

    /** 查询所有启用的权限 */
    List<Permission> findByStatus(Integer status);
}