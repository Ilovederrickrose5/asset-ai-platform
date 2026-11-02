package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.enums.PurchaseRequestStatus;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.entity.PurchaseRequest;
import com.enterprise.asset.business.service.PurchaseRequestService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 采购需求申请控制器 - 处理采购申请的提交与审批 */
@RestController
@RequestMapping("/api/purchase-requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    /**
     * GET /api/purchase-requests - 分页获取采购申请列表
     * 
     * @param page   页码
     * @param size   每页数量
     * @param status 状态筛选（可选）
     * @return 采购申请分页数据
     */
    @GetMapping
    public ResponseEntity<Result<Page<PurchaseRequest>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseRequest> requests = purchaseRequestService.getAllRequests(page, size, status);
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * GET /api/purchase-requests/{id} - 根据ID获取采购申请
     * 
     * @param id 申请ID
     * @return 采购申请详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<PurchaseRequest>> getRequestById(@PathVariable Long id) {
        PurchaseRequest request = purchaseRequestService.getRequestById(id);
        if (request == null) {
            return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
        }
        return ResponseEntity.ok(Result.success(request));
    }

    /**
     * GET /api/purchase-requests/applicant/{applicantId} - 获取申请人的申请列表
     * 
     * @param applicantId 申请人ID
     * @param page        页码
     * @param size        每页数量
     * @param status      状态筛选（可选）
     * @return 采购申请分页数据
     */
    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Result<Page<PurchaseRequest>>> getRequestsByApplicantId(
            @PathVariable Long applicantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseRequest> requests = purchaseRequestService.getRequestsByApplicantId(applicantId, page, size,
                status);
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * GET /api/purchase-requests/department/{departmentId} - 获取部门申请列表
     * 
     * @param departmentId 部门ID
     * @param page         页码
     * @param size         每页数量
     * @param status       状态筛选（可选）
     * @return 采购申请分页数据
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<Page<PurchaseRequest>>> getRequestsByDepartmentId(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseRequest> requests = purchaseRequestService.getRequestsByDepartmentId(departmentId, page, size,
                status);
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * GET /api/purchase-requests/status/{status} - 按状态获取申请列表
     * 
     * @param status 状态
     * @return 采购申请列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getRequestsByStatus(@PathVariable String status) {
        List<PurchaseRequest> requests = purchaseRequestService.getRequestsByStatus(status);
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * GET /api/purchase-requests/applicant/{applicantId}/status/{status} -
     * 获取申请人指定状态的申请
     * 
     * @param applicantId 申请人ID
     * @param status      状态
     * @return 采购申请列表
     */
    @GetMapping("/applicant/{applicantId}/status/{status}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getRequestsByApplicantIdAndStatus(
            @PathVariable Long applicantId,
            @PathVariable String status) {
        List<PurchaseRequest> requests = purchaseRequestService.getRequestsByApplicantIdAndStatus(applicantId, status);
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * GET /api/purchase-requests/pending - 获取待审批申请
     * 
     * @return 待审批申请列表
     */
    @GetMapping("/pending")
    public ResponseEntity<Result<List<PurchaseRequest>>> getPendingRequests() {
        List<PurchaseRequest> requests = purchaseRequestService
                .getRequestsByStatus(PurchaseRequestStatus.PENDING.getCode());
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * GET /api/purchase-requests/pending/approver/{approverId} - 获取审批人待处理的申请
     * 
     * @param approverId 审批人ID
     * @return 待处理申请列表
     */
    @GetMapping("/pending/approver/{approverId}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getPendingRequestsForApprover(@PathVariable Long approverId) {
        List<PurchaseRequest> requests = purchaseRequestService.getPendingRequestsForApprover(approverId);
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * GET /api/purchase-requests/approved/approver/{approverId} - 获取审批人已批准的申请
     * 
     * @param approverId 审批人ID
     * @return 已批准申请列表
     */
    @GetMapping("/approved/approver/{approverId}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getApprovedRequestsByApprover(@PathVariable Long approverId) {
        List<PurchaseRequest> requests = purchaseRequestService.getApprovedRequestsByApproverId(approverId);
        return ResponseEntity.ok(Result.success(requests));
    }

    /**
     * POST /api/purchase-requests - 创建采购申请
     * 
     * @param request 采购申请实体
     * @return 创建后的申请
     */
    @PostMapping
    public ResponseEntity<Result<PurchaseRequest>> createRequest(@RequestBody PurchaseRequest request) {
        try {
            PurchaseRequest createdRequest = purchaseRequestService.createRequest(request);
            return ResponseEntity.ok(Result.success(createdRequest));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "创建采购需求申请失败：" + e.getMessage()));
        }
    }

    /**
     * PUT /api/purchase-requests/{id} - 更新采购申请
     * 
     * @param id      申请ID
     * @param request 更新数据
     * @return 更新后的申请
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<PurchaseRequest>> updateRequest(
            @PathVariable Long id,
            @RequestBody PurchaseRequest request) {
        try {
            PurchaseRequest updatedRequest = purchaseRequestService.updateRequest(id, request);
            if (updatedRequest == null) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success(updatedRequest));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "更新采购需求申请失败：" + e.getMessage()));
        }
    }

    /**
     * DELETE /api/purchase-requests/{id} - 删除采购申请
     * 
     * @param id 申请ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteRequest(@PathVariable Long id) {
        try {
            boolean deleted = purchaseRequestService.deleteRequest(id);
            if (!deleted) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success("删除成功"));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "删除采购需求申请失败：" + e.getMessage()));
        }
    }

    /**
     * POST /api/purchase-requests/{id}/approve - 批准采购申请
     * 
     * @param id              申请ID
     * @param approvalRequest 审批信息
     * @return 审批后的申请
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Result<PurchaseRequest>> approveRequest(
            @PathVariable Long id,
            @RequestBody ApprovalRequest approvalRequest) {
        try {
            PurchaseRequest request = purchaseRequestService.approveRequest(
                    id,
                    approvalRequest.getApproverId(),
                    approvalRequest.getApproverName(),
                    approvalRequest.getApprovalRemark());
            if (request == null) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success(request));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "审批失败：" + e.getMessage()));
        }
    }

    /**
     * POST /api/purchase-requests/{id}/reject - 驳回采购申请
     * 
     * @param id              申请ID
     * @param approvalRequest 审批信息
     * @return 驳回后的申请
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Result<PurchaseRequest>> rejectRequest(
            @PathVariable Long id,
            @RequestBody ApprovalRequest approvalRequest) {
        try {
            PurchaseRequest request = purchaseRequestService.rejectRequest(
                    id,
                    approvalRequest.getApproverId(),
                    approvalRequest.getApproverName(),
                    approvalRequest.getApprovalRemark());
            if (request == null) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success(request));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "审批失败：" + e.getMessage()));
        }
    }

    /** 审批请求体 */
    static class ApprovalRequest {
        private Long approverId;
        private String approverName;
        private String approvalRemark;

        public Long getApproverId() {
            return approverId;
        }

        public void setApproverId(Long approverId) {
            this.approverId = approverId;
        }

        public String getApproverName() {
            return approverName;
        }

        public void setApproverName(String approverName) {
            this.approverName = approverName;
        }

        public String getApprovalRemark() {
            return approvalRemark;
        }

        public void setApprovalRemark(String approvalRemark) {
            this.approvalRemark = approvalRemark;
        }
    }
}