package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 供应商数据访问接口
 * 关联表: supplier(供应商表)
 * 主要操作: 供应商的增删改查
 */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}