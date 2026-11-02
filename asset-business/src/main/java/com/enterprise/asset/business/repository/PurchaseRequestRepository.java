package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.PurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 采购申请数据访问接口
 * 关联表: purchase_request(采购申请表)
 * 主要操作: 采购申请的增删改查、审批查询、分页查询
 * 
 * 注意：包含User实体查询的方法需要改为Feign调用auth服务
 */
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    /** 根据申请人ID查询申请列表 */
    List<PurchaseRequest> findByApplicantId(Long applicantId);
    
    /** 根据部门ID查询申请列表 */
    List<PurchaseRequest> findByDepartmentId(Long departmentId);
    
    /** 根据状态查询申请列表 */
    List<PurchaseRequest> findByStatus(String status);
    
    /** 根据申请人ID和状态查询申请 */
    List<PurchaseRequest> findByApplicantIdAndStatus(Long applicantId, String status);

    /** 根据部门ID和状态查询申请 */
    List<PurchaseRequest> findByDepartmentIdAndStatus(Long departmentId, String status);

    /** 领导查询待审批的申请(仅限本部门) */
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.status = :status AND pr.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId)")
    List<PurchaseRequest> findPendingRequestsForLeader(@Param("userId") Long userId, @Param("status") String status);

    /** 管理员查询待审批的申请(所有部门) */
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.status = :status AND (pr.departmentId IS NULL OR pr.departmentId IN (SELECT u.deptId FROM User u WHERE u.id = :userId))")
    List<PurchaseRequest> findPendingRequestsForAdmin(@Param("userId") Long userId, @Param("status") String status);
    
    /** 分页查询所有申请 */
    Page<PurchaseRequest> findAll(Pageable pageable);
    
    /** 按申请人ID分页查询 */
    Page<PurchaseRequest> findByApplicantId(Long applicantId, Pageable pageable);
    
    /** 按部门ID分页查询 */
    Page<PurchaseRequest> findByDepartmentId(Long departmentId, Pageable pageable);
    
    /** 按状态分页查询 */
    @Query("SELECT pr FROM PurchaseRequest pr WHERE (:status IS NULL OR :status = '' OR pr.status = :status)")
    Page<PurchaseRequest> findByStatus(@Param("status") String status, Pageable pageable);
    
    /** 按申请人ID和状态分页查询 */
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.applicantId = :applicantId AND (:status IS NULL OR :status = '' OR pr.status = :status)")
    Page<PurchaseRequest> findByApplicantIdAndStatus(@Param("applicantId") Long applicantId, @Param("status") String status, Pageable pageable);
    
    /** 根据审批人ID查询审批记录 */
    List<PurchaseRequest> findByApproverIdOrderByApprovalDateDesc(Long approverId);
    
    /** 按部门ID和状态分页查询 */
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.departmentId = :departmentId AND (:status IS NULL OR :status = '' OR pr.status = :status)")
    Page<PurchaseRequest> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId, @Param("status") String status, Pageable pageable);
    
    /** 按申请人ID查询并按更新时间倒序 */
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.applicantId = :applicantId ORDER BY pr.updateTime DESC")
    List<PurchaseRequest> findByApplicantIdOrderByUpdateTimeDesc(@Param("applicantId") Long applicantId);
}