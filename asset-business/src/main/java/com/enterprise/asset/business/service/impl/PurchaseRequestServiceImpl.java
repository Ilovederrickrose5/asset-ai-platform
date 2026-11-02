package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.enums.PurchaseRequestStatus;
import com.enterprise.asset.common.enums.UserRole;
import com.enterprise.asset.business.entity.Department;
import com.enterprise.asset.business.entity.PurchaseRequest;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.entity.User;
import com.enterprise.asset.business.repository.PurchaseRequestRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import com.enterprise.asset.business.repository.UserRepository;
import com.enterprise.asset.business.repository.DepartmentRepository;
import com.enterprise.asset.business.service.PurchaseRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 采购申请服务实现 - 处理采购申请流程管理（含权限校验、审批流程、日志记录） */
@Service
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseRequestServiceImpl.class);

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SysLogRepository sysLogRepository;

    public PurchaseRequestServiceImpl(PurchaseRequestRepository purchaseRequestRepository,
            UserRepository userRepository, DepartmentRepository departmentRepository,
            SysLogRepository sysLogRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.sysLogRepository = sysLogRepository;
    }

    @Override
    public List<PurchaseRequest> getAllRequests() {
        return purchaseRequestRepository.findAll();
    }

    @Override
    public PurchaseRequest getRequestById(Long id) {
        return purchaseRequestRepository.findById(id).orElse(null);
    }

    @Override
    public List<PurchaseRequest> getRequestsByApplicantId(Long applicantId) {
        return purchaseRequestRepository.findByApplicantId(applicantId);
    }

    @Override
    public List<PurchaseRequest> getRequestsByDepartmentId(Long departmentId) {
        return purchaseRequestRepository.findByDepartmentId(departmentId);
    }

    @Override
    public List<PurchaseRequest> getRequestsByStatus(String status) {
        return purchaseRequestRepository.findByStatus(status);
    }

    @Override
    public List<PurchaseRequest> getRequestsByApplicantIdAndStatus(Long applicantId, String status) {
        return purchaseRequestRepository.findByApplicantIdAndStatus(applicantId, status);
    }

    @Override
    public List<PurchaseRequest> getPendingRequestsForApprover(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }

        UserRole userRole = UserRole.fromCode(user.getRole());

        if (userRole != null && userRole.isAdmin()) {
            return purchaseRequestRepository.findPendingRequestsForAdmin(userId,
                    PurchaseRequestStatus.PENDING.getCode());
        } else if (userRole != null && (userRole.isLeader() || userRole.isManager())) {
            // 如果用户有部门ID，查询本部门的待审批申请
            if (user.getDeptId() != null) {
                return purchaseRequestRepository.findByDepartmentIdAndStatus(user.getDeptId(),
                        PurchaseRequestStatus.PENDING.getCode());
            } else {
                // 如果用户没有部门ID，返回所有待审批申请（兜底处理）
                return purchaseRequestRepository.findByStatus(PurchaseRequestStatus.PENDING.getCode());
            }
        } else {
            return List.of();
        }
    }

    @Override
    public List<PurchaseRequest> getApprovedRequestsByApproverId(Long approverId) {
        return purchaseRequestRepository.findByApproverIdOrderByApprovalDateDesc(approverId);
    }

    private boolean hasAnyRole(UserDTO user, UserRole... roles) {
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

    @Override
    public PurchaseRequest createRequest(PurchaseRequest request) {
        logger.info("========== 开始创建采购需求申请 ==========");

        // 1. 用户认证检查
        logger.info("[步骤1/3] 检查用户认证状态");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        logger.info("用户认证状态: {}", isAuthenticated ? "已登录" : "未登录");

        if (!isAuthenticated) {
            logger.error("用户未登录，无法创建采购申请");
            throw new SecurityException("用户未登录");
        }

        UserDTO userDTO = (authentication.getPrincipal() instanceof UserDTO) ? (UserDTO) authentication.getPrincipal()
                : null;
        logger.info("当前用户名: {}", userDTO != null ? userDTO.getUsername() : "null");
        logger.info("用户信息: id={}, name={}, role={}, deptId={}",
                userDTO != null ? userDTO.getId() : "null",
                userDTO != null ? userDTO.getRealName() : "null",
                userDTO != null ? userDTO.getRoleCodes() : "null",
                userDTO != null ? userDTO.getDeptId() : "null");

        if (userDTO == null) {
            logger.error("用户不存在: {}", userDTO != null ? userDTO.getUsername() : "null");
            throw new SecurityException("用户不存在");
        }

        // 2. 权限检查
        logger.info("[步骤2/3] 检查创建权限");
        boolean isManager = hasAnyRole(userDTO, UserRole.MANAGER, UserRole.ASSET_MANAGER, UserRole.DEPT_MANAGER);
        boolean isAdmin = hasAnyRole(userDTO, UserRole.ADMIN);

        logger.info("角色检查: isManager={}, isAdmin={}", isManager, isAdmin);

        if (!isManager && !isAdmin) {
            logger.error("权限验证失败: 只有部门资产管理员或管理员才能提交采购申请");
            throw new SecurityException("只有部门资产管理员或管理员才能提交采购申请");
        }

        // 3. 设置申请信息并保存
        logger.info("[步骤3/3] 设置申请信息并保存");
        logger.info("申请物品: {}", request.getItemName());
        logger.info("申请数量: {}", request.getQuantity());
        logger.info("规格型号: {}", request.getSpecification());
        logger.info("预估单价: {}", request.getEstimatedUnitPrice());
        logger.info("采购理由: {}", request.getPurchaseReason());

        request.setApplicantId(userDTO.getId());
        request.setApplicantName(userDTO.getRealName());
        request.setDepartmentId(userDTO.getDeptId());
        request.setDepartmentName(getDepartmentName(userDTO.getDeptId()));
        request.setApplicationDate(LocalDateTime.now());
        request.setStatus(PurchaseRequestStatus.PENDING.getCode());

        if (request.getEstimatedUnitPrice() != null && request.getQuantity() != null) {
            BigDecimal totalAmount = request.getEstimatedUnitPrice().multiply(new BigDecimal(request.getQuantity()));
            request.setTotalAmount(totalAmount);
            logger.info("计算总金额: {} * {} = {}", request.getEstimatedUnitPrice(), request.getQuantity(), totalAmount);
        }

        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);
        logger.info("采购需求申请创建成功: id={}, itemName={}", savedRequest.getId(), savedRequest.getItemName());
        logger.info("========== 采购需求申请创建完成 ==========");

        // 记录操作日志
        try {
            SysLog log = new SysLog();
            log.setUserId(userDTO.getId());
            log.setUsername(userDTO.getUsername());
            log.setOperation("提交采购需求申请：" + request.getItemName());
            log.setLogType("SYSTEM");
            log.setStatus("success");
            sysLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("添加采购需求申请创建日志失败: {}", e.getMessage());
        }

        return savedRequest;
    }

    @Override
    public PurchaseRequest updateRequest(Long id, PurchaseRequest request) {
        PurchaseRequest existingRequest = purchaseRequestRepository.findById(id).orElse(null);
        if (existingRequest == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未登录");
        }

        UserDTO userDTO = (authentication.getPrincipal() instanceof UserDTO) ? (UserDTO) authentication.getPrincipal()
                : null;
        if (userDTO == null) {
            throw new SecurityException("用户不存在");
        }

        boolean isAdmin = hasAnyRole(userDTO, UserRole.ADMIN);

        if (!isAdmin && !existingRequest.getApplicantId().equals(userDTO.getId())) {
            throw new SecurityException("无权修改此采购申请");
        }

        if (!PurchaseRequestStatus.PENDING.getCode().equals(existingRequest.getStatus())) {
            throw new SecurityException("只能修改待审批的采购申请");
        }

        existingRequest.setItemName(request.getItemName());
        existingRequest.setSpecification(request.getSpecification());
        existingRequest.setQuantity(request.getQuantity());
        existingRequest.setUnit(request.getUnit());
        existingRequest.setEstimatedUnitPrice(request.getEstimatedUnitPrice());
        existingRequest.setPurchaseReason(request.getPurchaseReason());

        if (request.getEstimatedUnitPrice() != null && request.getQuantity() != null) {
            existingRequest
                    .setTotalAmount(request.getEstimatedUnitPrice().multiply(new BigDecimal(request.getQuantity())));
        }

        return purchaseRequestRepository.save(existingRequest);
    }

    @Override
    public boolean deleteRequest(Long id) {
        PurchaseRequest request = purchaseRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未登录");
        }

        UserDTO userDTO = (authentication.getPrincipal() instanceof UserDTO) ? (UserDTO) authentication.getPrincipal()
                : null;
        if (userDTO == null) {
            throw new SecurityException("用户不存在");
        }

        boolean isAdmin = hasAnyRole(userDTO, UserRole.ADMIN);

        if (!isAdmin && !request.getApplicantId().equals(userDTO.getId())) {
            throw new SecurityException("无权删除此采购申请");
        }

        if (!PurchaseRequestStatus.PENDING.getCode().equals(request.getStatus())) {
            throw new SecurityException("只能删除待审批的采购申请");
        }

        purchaseRequestRepository.deleteById(id);
        return true;
    }

    @Override
    public PurchaseRequest approveRequest(Long id, Long approverId, String approverName, String approvalRemark) {
        logger.info("========== 开始批准采购需求申请 ==========");
        logger.info("申请ID: {}", id);

        // 1. 查询申请
        logger.info("[步骤1/4] 查询采购需求申请");
        PurchaseRequest request = purchaseRequestRepository.findById(id).orElse(null);
        if (request == null) {
            logger.error("采购需求申请不存在: id={}", id);
            return null;
        }
        logger.info("申请信息: id={}, itemName={}, applicantName={}, departmentId={}, status={}",
                request.getId(), request.getItemName(), request.getApplicantName(),
                request.getDepartmentId(), request.getStatus());

        // 2. 用户认证检查
        logger.info("[步骤2/4] 检查用户认证状态");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("用户未登录，无法审批");
            throw new SecurityException("用户未登录");
        }

        UserDTO userDTO = (authentication.getPrincipal() instanceof UserDTO) ? (UserDTO) authentication.getPrincipal() : null;
        if (userDTO == null) {
            logger.error("用户不存在: {}", userDTO != null ? userDTO.getUsername() : "null");
            throw new SecurityException("用户不存在");
        }
        logger.info("当前用户: id={}, name={}, role={}, deptId={}",
                userDTO.getId(), userDTO.getRealName(), userDTO.getRoleCodes(), userDTO.getDeptId());

        // 3. 权限检查
        logger.info("[步骤3/4] 检查审批权限");
        boolean isLeader = hasAnyRole(userDTO, UserRole.LEADER);
        boolean isAdmin = hasAnyRole(userDTO, UserRole.ADMIN);

        logger.info("角色检查: isLeader={}, isAdmin={}", isLeader, isAdmin);

        if (!isAdmin && !isLeader) {
            logger.error("权限验证失败: 只有部门领导或管理员才能审批采购申请");
            throw new SecurityException("只有部门领导或管理员才能审批采购申请");
        }

        // 部门领导只能审批本部门的采购申请
        if (isLeader && !isAdmin) {
            if (userDTO.getDeptId() == null) {
                logger.error("权限验证失败: 领导用户没有部门信息");
                throw new SecurityException("领导用户没有部门信息");
            }
            if (request.getDepartmentId() == null || !request.getDepartmentId().equals(userDTO.getDeptId())) {
                logger.error("权限验证失败: 部门领导只能审批本部门的采购申请, userDeptId={}, requestDeptId={}",
                        userDTO.getDeptId(), request.getDepartmentId());
                throw new SecurityException("部门领导只能审批本部门的采购申请");
            }
        }

        // 4. 状态检查和批准操作
        logger.info("[步骤4/4] 检查状态并执行批准");
        logger.info("当前申请状态: {}", request.getStatus());
        if (!PurchaseRequestStatus.PENDING.getCode().equals(request.getStatus())) {
            logger.error("状态验证失败: 只能审批待审批的采购申请");
            throw new SecurityException("只能审批待审批的采购申请");
        }

        logger.info("执行批准操作, 审批备注: {}", approvalRemark);
        request.setStatus(PurchaseRequestStatus.APPROVED.getCode());
        request.setApproverId(userDTO.getId());
        request.setApproverName(userDTO.getRealName());
        request.setApprovalDate(LocalDateTime.now());
        request.setApprovalRemark(approvalRemark);

        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);
        logger.info("采购需求申请批准成功: id={}, status={}", savedRequest.getId(), savedRequest.getStatus());
        logger.info("========== 采购需求申请批准完成 ==========");

        // 记录操作日志
        try {
            SysLog log = new SysLog();
            log.setUserId(userDTO.getId());
            log.setUsername(userDTO.getUsername());
            log.setOperation("批准采购需求申请：" + request.getItemName());
            log.setLogType("SYSTEM");
            log.setStatus("success");
            sysLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("添加采购需求申请批准日志失败: {}", e.getMessage());
        }

        return savedRequest;
    }

    public PurchaseRequest rejectRequest(Long id, Long approverId, String approverName, String approvalRemark) {
        PurchaseRequest request = purchaseRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未登录");
        }

        UserDTO userDTO = (authentication.getPrincipal() instanceof UserDTO) ? (UserDTO) authentication.getPrincipal() : null;
        if (userDTO == null) {
            throw new SecurityException("用户不存在");
        }

        boolean isLeader = hasAnyRole(userDTO, UserRole.LEADER);
        boolean isAdmin = hasAnyRole(userDTO, UserRole.ADMIN);

        if (!isAdmin && !isLeader) {
            throw new SecurityException("只有部门领导或管理员才能审批采购申请");
        }

        // 部门领导只能驳回本部门的采购申请
        if (isLeader && !isAdmin) {
            if (userDTO.getDeptId() == null) {
                throw new SecurityException("领导用户没有部门信息");
            }
            if (request.getDepartmentId() == null || !request.getDepartmentId().equals(userDTO.getDeptId())) {
                throw new SecurityException("部门领导只能审批本部门的采购申请");
            }
        }

        if (!PurchaseRequestStatus.PENDING.getCode().equals(request.getStatus())) {
            throw new SecurityException("只能审批待审批的采购申请");
        }

        request.setStatus(PurchaseRequestStatus.REJECTED.getCode());
        request.setApproverId(userDTO.getId());
        request.setApproverName(userDTO.getRealName());
        request.setApprovalDate(LocalDateTime.now());
        request.setApprovalRemark(approvalRemark);

        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);

        // 记录操作日志
        try {
            SysLog log = new SysLog();
            log.setUserId(userDTO.getId());
            log.setUsername(userDTO.getUsername());
            log.setOperation("拒绝采购需求申请：" + request.getItemName());
            log.setLogType("SYSTEM");
            log.setStatus("success");
            sysLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("添加采购需求申请拒绝日志失败: {}", e.getMessage());
        }

        return savedRequest;
    }

    private String getDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return "公共资产";
        }
        Department department = departmentRepository.findById(departmentId).orElse(null);
        if (department != null && department.getDeptName() != null) {
            return department.getDeptName();
        }
        return "部门" + departmentId;
    }

    // 分页查询实现
    @Override
    public Page<PurchaseRequest> getAllRequests(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PurchaseRequest.APPLICATION_DATE));
        Page<PurchaseRequest> result = purchaseRequestRepository.findByStatus(status, pageable);
        updateDepartmentNames(result.getContent());
        return result;
    }

    @Override
    public Page<PurchaseRequest> getRequestsByApplicantId(Long applicantId, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PurchaseRequest.APPLICATION_DATE));
        Page<PurchaseRequest> result = purchaseRequestRepository.findByApplicantIdAndStatus(applicantId, status,
                pageable);
        updateDepartmentNames(result.getContent());
        return result;
    }

    @Override
    public Page<PurchaseRequest> getRequestsByDepartmentId(Long departmentId, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PurchaseRequest.APPLICATION_DATE));
        Page<PurchaseRequest> result = purchaseRequestRepository.findByDepartmentIdAndStatus(departmentId, status,
                pageable);
        updateDepartmentNames(result.getContent());
        return result;
    }

    /**
     * 更新采购申请列表中的部门名称为实际部门名称
     */
    private void updateDepartmentNames(List<PurchaseRequest> requests) {
        for (PurchaseRequest request : requests) {
            if (request.getDepartmentId() != null) {
                String realDepartmentName = getDepartmentName(request.getDepartmentId());
                if (realDepartmentName != null && !realDepartmentName.startsWith("部门")) {
                    request.setDepartmentName(realDepartmentName);
                }
            }
        }
    }
}