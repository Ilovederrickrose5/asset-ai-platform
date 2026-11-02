package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.enums.ApplicationStatus;
import com.enterprise.asset.common.enums.ApplicationType;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.common.enums.UserRole;
import com.enterprise.asset.business.entity.AssetApplication;
import com.enterprise.asset.business.service.AssetApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** 资产业务申请控制器 - 处理领用/转移/维修/报废 */
@RestController
@RequestMapping("/api/asset-applications")
public class AssetApplicationController {

    private final AssetApplicationService assetApplicationService;

    public AssetApplicationController(AssetApplicationService assetApplicationService) {
        this.assetApplicationService = assetApplicationService;
    }

    // === 查询接口 ===
    @GetMapping
    public ResponseEntity<Result<Page<AssetApplication>>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }

        Page<AssetApplication> applications;
        if (isAdmin(user)) {
            applications = assetApplicationService.getApplicationsByTypeAndStatus(type, status, page, size);
        } else if (isApprover(user)) {
            Long deptId = user.getDeptId();
            applications = deptId == null
                    ? Page.empty()
                    : assetApplicationService.getApplicationsByDepartmentIdAndTypeAndStatus(deptId, type, status,
                            page, size);
        } else {
            applications = assetApplicationService.getApplicationsByApplicantIdAndTypeAndStatus(
                    user.getId(), type, status, page, size);
        }
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<AssetApplication>> getApplicationById(@PathVariable Long id) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        AssetApplication application = assetApplicationService.getApplicationById(id);
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        boolean visible = isAdmin(user)
                || user.getId().equals(application.getApplicantId())
                || (isApprover(user) && application.getDepartmentId() != null
                        && application.getDepartmentId().equals(user.getDeptId()));
        if (!visible) {
            return ResponseEntity.ok(Result.error(403, "无权查看该申请"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Result<Page<AssetApplication>>> getApplicationsByApplicantId(
            @PathVariable Long applicantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        if (!isAdmin(user) && !user.getId().equals(applicantId)) {
            return ResponseEntity.ok(Result.error(403, "只能查看自己的申请"));
        }
        return ResponseEntity.ok(Result.success(assetApplicationService
                .getApplicationsByApplicantIdAndTypeAndStatus(applicantId, type, status, page, size)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsByStatus(@PathVariable String status) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        return ResponseEntity.ok(Result.success(applicationsByStatusForUser(user, status)));
    }

    @GetMapping("/status/{status}/department/{departmentId}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsByStatusAndDepartment(
            @PathVariable String status, @PathVariable Long departmentId) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        if (!isAdmin(user) && !departmentId.equals(user.getDeptId())) {
            return ResponseEntity.ok(Result.error(403, "只能查看本部门的申请"));
        }
        return ResponseEntity.ok(Result.success(
                assetApplicationService.getApplicationsByStatusAndDepartment(status, departmentId)));
    }

    @GetMapping("/status/pending_leader")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsPendingLeader() {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        if (!isLeaderOrAdmin(user)) {
            return ResponseEntity.ok(Result.error(403, "只有领导可以查看待终审申请"));
        }
        return ResponseEntity.ok(Result.success(
                applicationsByStatusForUser(user, ApplicationStatus.PENDING_LEADER.getCode())));
    }

    @GetMapping("/status/pending_leader/department/{departmentId}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsPendingLeaderByDepartment(
            @PathVariable Long departmentId) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        if (!isAdmin(user) && !departmentId.equals(user.getDeptId())) {
            return ResponseEntity.ok(Result.error(403, "只能查看本部门的申请"));
        }
        return ResponseEntity.ok(Result.success(assetApplicationService
                .getApplicationsByStatusAndDepartment(ApplicationStatus.PENDING_LEADER.getCode(), departmentId)));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<Page<AssetApplication>>> getApplicationsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "用户未登录"));
        }
        if (!isAdmin(user) && !departmentId.equals(user.getDeptId())) {
            return ResponseEntity.ok(Result.error(403, "只能查看本部门的申请"));
        }
        return ResponseEntity.ok(Result.success(assetApplicationService
                .getApplicationsByDepartmentIdAndTypeAndStatus(departmentId, type, status, page, size)));
    }

    /** 按当前用户的数据范围查询指定状态的申请 */
    private List<AssetApplication> applicationsByStatusForUser(UserDTO user, String status) {
        if (isAdmin(user)) {
            return assetApplicationService.getApplicationsByStatus(status);
        }
        if (isApprover(user)) {
            Long deptId = user.getDeptId();
            return deptId == null
                    ? List.of()
                    : assetApplicationService.getApplicationsByStatusAndDepartment(status, deptId);
        }
        return assetApplicationService.getApplicationsByStatus(status).stream()
                .filter(application -> user.getId().equals(application.getApplicantId()))
                .toList();
    }

    // === 核心业务流程 ===

    /** 创建申请 - applicationType: RECEIVE/TRANSFER/MAINTENANCE/DISPOSAL */
    @PostMapping
    public ResponseEntity<Result<AssetApplication>> createApplication(@RequestBody AssetApplication application) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(Result.error(401, "未登录"));
        }
        // 申请人信息以令牌为准，忽略请求体中的身份字段，防止伪造
        application.setApplicantId(currentUser.getId());
        application.setApplicantName(displayName(currentUser));
        application.setDepartmentId(currentUser.getDeptId());
        application.setStatus(ApplicationStatus.PENDING.getCode());

        AssetApplication createdApplication = assetApplicationService.createApplication(application);
        return ResponseEntity.ok(Result.success(createdApplication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<AssetApplication>> updateApplication(
            @PathVariable Long id, @RequestBody AssetApplication application) {
        UserDTO currentUser = getCurrentUser();
        AssetApplication existing = assetApplicationService.getApplicationById(id);
        if (existing == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        if (!isAdmin(currentUser) && !currentUser.getId().equals(existing.getApplicantId())) {
            return ResponseEntity.ok(Result.error(403, "只能修改自己提交的申请"));
        }

        AssetApplication updatedApplication = assetApplicationService.updateApplication(id, application);
        return ResponseEntity.ok(Result.success(updatedApplication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteApplication(@PathVariable Long id) {
        UserDTO currentUser = getCurrentUser();
        AssetApplication existing = assetApplicationService.getApplicationById(id);
        if (existing == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        boolean owner = currentUser != null && currentUser.getId().equals(existing.getApplicantId());
        if (!isAdmin(currentUser) && !owner) {
            return ResponseEntity.ok(Result.error(403, "只能删除自己提交的申请"));
        }
        if (!isAdmin(currentUser) && !ApplicationStatus.PENDING.getCode().equals(existing.getStatus())) {
            return ResponseEntity.ok(Result.error(400, "已进入审批流程的申请不能删除"));
        }
        boolean deleted = assetApplicationService.deleteApplication(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /**
     * 批准申请
     * 报废二级审批：资产管理员 pending → pending_leader，领导 pending_leader → approved（资产置为报废）
     * 其他申请：拥有审批职责的角色 pending → approved
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Result<AssetApplication>> approveApplication(
            @PathVariable Long id, @RequestBody(required = false) ApprovalRequest approvalRequest) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(Result.error(401, "未登录"));
        }

        AssetApplication application = assetApplicationService.getApplicationById(id);
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }

        Long approverId = currentUser.getId();
        String approverName = displayName(currentUser);
        String approvalRemark = approvalRequest != null ? approvalRequest.getApprovalRemark() : null;
        String status = application.getStatus();
        boolean disposal = ApplicationType.DISPOSAL.getCode().equals(application.getApplicationType());

        if (disposal) {
            // 一级：资产管理员初审
            if (ApplicationStatus.PENDING.getCode().equals(status)) {
                if (!isManagerOrAdmin(currentUser)) {
                    return ResponseEntity.ok(Result.error(403, "资产管理员初审通过后才能提交领导审批"));
                }
                return ResponseEntity.ok(Result.success(assetApplicationService.approveApplicationForLeader(
                        id, approverId, approverName, approvalRemark)));
            }
            // 二级：领导终审
            if (ApplicationStatus.PENDING_LEADER.getCode().equals(status)
                    || ApplicationStatus.LEADER_APPROVED.getCode().equals(status)) {
                if (!isLeaderOrAdmin(currentUser)) {
                    return ResponseEntity.ok(Result.error(403, "仅领导可执行报废终审"));
                }
                AssetApplication approved = assetApplicationService.approveApplication(
                        id, approverId, approverName, approvalRemark);
                if (application.getOriginalApplicationId() != null) {
                    assetApplicationService.updateStatus(application.getOriginalApplicationId(),
                            ApplicationStatus.LEADER_APPROVED.getCode());
                }
                return ResponseEntity.ok(Result.success(approved));
            }
            return ResponseEntity.ok(Result.error(400, "当前状态不允许报废审批"));
        }

        if (!isApprover(currentUser)) {
            return ResponseEntity.ok(Result.error(403, "没有审批权限"));
        }
        return ResponseEntity.ok(Result.success(assetApplicationService.approveApplication(
                id, approverId, approverName, approvalRemark)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Result<AssetApplication>> rejectApplication(
            @PathVariable Long id, @RequestBody(required = false) ApprovalRequest approvalRequest) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(Result.error(401, "未登录"));
        }
        if (!isApprover(currentUser)) {
            return ResponseEntity.ok(Result.error(403, "没有审批权限"));
        }

        String approvalRemark = approvalRequest != null ? approvalRequest.getApprovalRemark() : null;
        AssetApplication application = assetApplicationService.rejectApplication(
                id, currentUser.getId(), displayName(currentUser), approvalRemark);
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    @PostMapping("/create-final-approval")
    public ResponseEntity<Result<AssetApplication>> createFinalApproval(
            @RequestBody AssetApplication finalApprovalRequest) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(Result.error(401, "未登录"));
        }
        if (!isManagerOrAdmin(currentUser)) {
            return ResponseEntity.ok(Result.error(403, "没有创建终审申请的权限"));
        }

        Long originalApplicationId = finalApprovalRequest.getOriginalApplicationId();
        finalApprovalRequest.setApplicantId(currentUser.getId());
        finalApprovalRequest.setApplicantName(displayName(currentUser));
        finalApprovalRequest.setDepartmentId(currentUser.getDeptId());
        finalApprovalRequest.setStatus(ApplicationStatus.PENDING_LEADER.getCode());
        finalApprovalRequest.setApplicationDate(LocalDateTime.now());

        AssetApplication finalApproval = assetApplicationService.createApplication(finalApprovalRequest);
        if (originalApplicationId != null) {
            assetApplicationService.updateStatus(originalApplicationId, "final_approval_created");
        }
        return ResponseEntity.ok(Result.success(finalApproval));
    }

    /** 开始维修 - 更新资产状态为维修中 */
    @PutMapping("/{id}/start-maintenance")
    public ResponseEntity<Result<AssetApplication>> startMaintenance(@PathVariable Long id) {
        UserDTO currentUser = getCurrentUser();
        if (!isManagerOrAdmin(currentUser)) {
            return ResponseEntity.ok(Result.error(403, "没有维修操作权限"));
        }
        AssetApplication application = assetApplicationService.startMaintenance(id,
                currentUser.getId(), displayName(currentUser));
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在或状态不允许"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    /** 完成维修 - 更新资产状态为使用中 */
    @PostMapping("/{id}/complete-maintenance")
    public ResponseEntity<Result<AssetApplication>> completeMaintenance(@PathVariable Long id) {
        UserDTO currentUser = getCurrentUser();
        if (!isManagerOrAdmin(currentUser)) {
            return ResponseEntity.ok(Result.error(403, "没有维修操作权限"));
        }
        AssetApplication application = assetApplicationService.completeMaintenance(id,
                currentUser.getId(), displayName(currentUser));
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在或状态不允许"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    // === 当前用户与角色判断 ===

    private UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDTO userDTO) {
            return userDTO;
        }
        return null;
    }

    private String displayName(UserDTO user) {
        if (user == null) {
            return null;
        }
        return user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
    }

    private boolean isAdmin(UserDTO user) {
        return hasRole(user, UserRole.ADMIN);
    }

    private boolean isManagerOrAdmin(UserDTO user) {
        return isAdmin(user) || hasRole(user, UserRole.MANAGER, UserRole.ASSET_MANAGER, UserRole.DEPT_MANAGER);
    }

    private boolean isLeaderOrAdmin(UserDTO user) {
        return isAdmin(user) || hasRole(user, UserRole.LEADER);
    }

    private boolean isApprover(UserDTO user) {
        return isManagerOrAdmin(user) || isLeaderOrAdmin(user);
    }

    private boolean hasRole(UserDTO user, UserRole... roles) {
        if (user == null || user.getRoleCodes() == null) {
            return false;
        }
        for (String code : user.getRoleCodes()) {
            UserRole parsed = UserRole.fromCode(code);
            if (parsed == null) {
                continue;
            }
            for (UserRole target : roles) {
                if (parsed == target) {
                    return true;
                }
            }
        }
        return false;
    }

    // === 请求参数类 ===
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
