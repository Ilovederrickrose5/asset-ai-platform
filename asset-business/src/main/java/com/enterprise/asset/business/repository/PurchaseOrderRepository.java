package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 采购订单数据访问接口
 * 关联表: purchase_order(采购订单表)
 * 主要操作: 采购订单的增删改查、状态查询、权限过滤查询
 * 
 * 注意：包含User实体查询的方法需要改为Feign调用auth服务
 */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /** 根据创建人ID查询订单 */
    List<PurchaseOrder> findByCreatorId(Long creatorId);

    /** 根据部门ID查询订单列表 */
    List<PurchaseOrder> findByDepartmentId(Long departmentId);

    /** 根据部门ID分页查询订单 */
    Page<PurchaseOrder> findByDepartmentId(Long departmentId, Pageable pageable);

    /** 根据状态查询订单列表 */
    List<PurchaseOrder> findByStatus(String status);

    /** 根据状态分页查询订单 */
    Page<PurchaseOrder> findByStatus(String status, Pageable pageable);

    /** 根据采购申请ID查询订单 */
    List<PurchaseOrder> findByPurchaseRequestId(Long purchaseRequestId);

    /** 根据供应商ID查询订单 */
    List<PurchaseOrder> findBySupplierId(Long supplierId);

    /** 根据部门ID和状态查询订单 */
    List<PurchaseOrder> findByDepartmentIdAndStatus(Long departmentId, String status);

    /** 根据部门ID和状态分页查询订单 */
    Page<PurchaseOrder> findByDepartmentIdAndStatus(Long departmentId, String status, Pageable pageable);

    /** 领导查询待处理订单(仅限本部门) */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = :status AND po.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId)")
    List<PurchaseOrder> findOrdersForLeader(@Param("userId") Long userId, @Param("status") String status);

    /** 管理员查询待处理订单(所有部门) */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = :status AND (po.departmentId IS NULL OR po.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId))")
    List<PurchaseOrder> findOrdersForAdmin(@Param("userId") Long userId, @Param("status") String status);
}