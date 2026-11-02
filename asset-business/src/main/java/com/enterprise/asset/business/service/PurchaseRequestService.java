/** 采购申请服务接口 - 处理采购申请流程管理 */
package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.PurchaseRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PurchaseRequestService {
    
    /** 获取所有采购申请 */
    List<PurchaseRequest> getAllRequests();
    
    /** 根据ID获取采购申请 */
    PurchaseRequest getRequestById(Long id);
    
    /** 根据申请人ID获取采购申请 */
    List<PurchaseRequest> getRequestsByApplicantId(Long applicantId);
    
    /** 根据部门ID获取采购申请 */
    List<PurchaseRequest> getRequestsByDepartmentId(Long departmentId);
    
    /** 根据状态获取采购申请 */
    List<PurchaseRequest> getRequestsByStatus(String status);
    
    /** 根据申请人ID和状态获取采购申请 */
    List<PurchaseRequest> getRequestsByApplicantIdAndStatus(Long applicantId, String status);
    
    /** 获取审批人待审批的申请 */
    List<PurchaseRequest> getPendingRequestsForApprover(Long userId);
    
    /** 创建采购申请 */
    PurchaseRequest createRequest(PurchaseRequest request);
    
    /** 更新采购申请 */
    PurchaseRequest updateRequest(Long id, PurchaseRequest request);
    
    /** 删除采购申请 */
    boolean deleteRequest(Long id);
    
    /** 审批采购申请 */
    PurchaseRequest approveRequest(Long id, Long approverId, String approverName, String approvalRemark);
    
    /** 驳回采购申请 */
    PurchaseRequest rejectRequest(Long id, Long approverId, String approverName, String approvalRemark);
    
    /** 分页查询采购申请 */
    Page<PurchaseRequest> getAllRequests(int page, int size, String status);
    
    /** 分页按申请人ID查询采购申请 */
    Page<PurchaseRequest> getRequestsByApplicantId(Long applicantId, int page, int size, String status);
    
    /** 分页按部门ID查询采购申请 */
    Page<PurchaseRequest> getRequestsByDepartmentId(Long departmentId, int page, int size, String status);
    
    /** 获取审批人审批过的历史记录 */
    List<PurchaseRequest> getApprovedRequestsByApproverId(Long approverId);
}