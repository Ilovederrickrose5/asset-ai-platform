package com.enterprise.asset.business.service;

import com.enterprise.asset.common.enums.ApplicationStatus;
import com.enterprise.asset.common.enums.ApplicationType;
import com.enterprise.asset.common.enums.InventoryStatus;
import com.enterprise.asset.common.enums.LogType;
import com.enterprise.asset.common.enums.PurchaseRequestStatus;
import com.enterprise.asset.common.enums.UserRole;
import com.enterprise.asset.common.dto.DashboardOperationsDTO;
import com.enterprise.asset.common.dto.RecentOperationDTO;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.client.AuthFeignClient;
import com.enterprise.asset.business.entity.AssetApplication;
import com.enterprise.asset.business.entity.AssetInventory;
import com.enterprise.asset.business.entity.PurchaseRequest;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.repository.AssetApplicationRepository;
import com.enterprise.asset.business.repository.AssetInventoryRepository;
import com.enterprise.asset.business.repository.PurchaseRequestRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 最近操作服务 - 处理仪表盘待办任务和操作记录查询
 * 
 * Feign改造说明：
 * - 删除了UserRepository本地DAO依赖
 * - 通过AuthFeignClient远程调用asset-auth服务获取用户信息
 * - 方法参数从User实体改为UserDTO，实现跨服务数据传输
 */
@Service
public class RecentOperationService {

    private final SysLogRepository sysLogRepository;
    private final AuthFeignClient authFeignClient;
    private final AssetApplicationRepository assetApplicationRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final AssetInventoryRepository assetInventoryRepository;

    public RecentOperationService(SysLogRepository sysLogRepository, 
            AuthFeignClient authFeignClient,
            AssetApplicationRepository assetApplicationRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            AssetInventoryRepository assetInventoryRepository) {
        this.sysLogRepository = sysLogRepository;
        this.authFeignClient = authFeignClient;
        this.assetApplicationRepository = assetApplicationRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.assetInventoryRepository = assetInventoryRepository;
    }

    /**
     * 获取仪表盘操作数据（待办任务+最近动态）
     * 权限规则：admin/leader查看待审批任务，普通用户查看自己的申请结果
     * 
     * Feign改造：参数从User实体改为UserDTO
     */
    public DashboardOperationsDTO getDashboardOperations(int limit, UserDTO user) {
        List<RecentOperationDTO> pendingTasks = new ArrayList<>();

        String roleCode = user.getRoleCodes() != null && !user.getRoleCodes().isEmpty() 
                ? user.getRoleCodes().get(0) 
                : "user";
        UserRole userRole = UserRole.fromCode(roleCode);
        boolean isAdmin = userRole != null && userRole.isAdmin();
        boolean isLeader = userRole != null && userRole.isLeader();
        boolean isManager = userRole != null && userRole.isManager();
        boolean isUser = !isAdmin && !isLeader && !isManager;
        Long userDeptId = user.getDeptId();

        if (isAdmin || isLeader) {
            List<PurchaseRequest> pendingPurchases = isAdmin
                    ? purchaseRequestRepository.findByStatus(PurchaseRequestStatus.PENDING.getCode())
                    : purchaseRequestRepository.findByDepartmentIdAndStatus(userDeptId,
                            PurchaseRequestStatus.PENDING.getCode());
            for (PurchaseRequest req : pendingPurchases) {
                RecentOperationDTO dto = new RecentOperationDTO();
                dto.setTime(req.getApplicationDate());
                dto.setTimeText(formatTime(req.getApplicationDate()));
                String applicantName = req.getApplicantName() != null ? req.getApplicantName() : "未知用户";
                dto.setContent("[采购] " + applicantName + "提交了采购需求申请：" + req.getItemName());
                dto.setType("purchase");
                dto.setOperator(applicantName);
                dto.setPending(true);
                pendingTasks.add(dto);
            }

            List<AssetApplication> pendingApplications = isAdmin
                    ? assetApplicationRepository.findByStatus(ApplicationStatus.PENDING.getCode())
                    : assetApplicationRepository.findByStatusAndDepartmentId(ApplicationStatus.PENDING.getCode(),
                            userDeptId);
            for (AssetApplication app : pendingApplications) {
                RecentOperationDTO dto = new RecentOperationDTO();
                dto.setTime(app.getApplicationDate());
                dto.setTimeText(formatTime(app.getApplicationDate()));
                String applicantName = app.getApplicantName() != null ? app.getApplicantName() : "未知用户";
                String appType = getApplicationTypeTextShort(app.getApplicationType());
                String assetName = app.getAssetName() != null ? app.getAssetName() : app.getItemName();
                String assetNo = app.getAssetNo() != null ? " (" + app.getAssetNo() + ")" : "";
                dto.setContent("[资产] " + applicantName + " 申请" + appType + "："
                        + (assetName != null ? assetName : "未知物品") + assetNo);
                dto.setType("application");
                dto.setOperator(applicantName);
                dto.setPending(true);
                pendingTasks.add(dto);
            }

            List<AssetInventory> pendingInventories = assetInventoryRepository
                    .findByStatus(InventoryStatus.PENDING.getCode());
            for (AssetInventory inv : pendingInventories) {
                if (inv.getAssigneeId() != null && inv.getAssigneeId().equals(user.getId())) {
                    RecentOperationDTO dto = new RecentOperationDTO();
                    dto.setTime(inv.getCreateTime());
                    dto.setTimeText(formatTime(inv.getCreateTime()));
                    String planName = inv.getInventoryName() != null ? inv.getInventoryName() : inv.getPlanName();
                    dto.setContent("[盘点] 盘点任务：" + planName);
                    dto.setType("inventory");
                    dto.setOperator(inv.getCreatorName());
                    dto.setPending(true);
                    pendingTasks.add(dto);
                }
            }

            pendingTasks.sort(Comparator.comparing(RecentOperationDTO::getTime).reversed());
            if (pendingTasks.size() > limit) {
                pendingTasks = pendingTasks.subList(0, limit);
            }
        }

        List<RecentOperationDTO> allActivities = new ArrayList<>();

        List<SysLog> sysLogs = sysLogRepository.findRecentLogs(limit * 2);
        for (SysLog log : sysLogs) {
            if (checkPermission(log.getUserId(), user, isAdmin, isLeader, isManager, userDeptId)) {
                RecentOperationDTO dto = new RecentOperationDTO();
                dto.setTime(log.getCreateTime());
                dto.setTimeText(formatTime(log.getCreateTime()));
                String logType = getLogTypeLabel(log.getLogType());
                String content = formatLogContent(log.getOperation(), log.getParams(), log.getLogType());
                dto.setContent(logType + " " + content);
                dto.setType(log.getLogType().toLowerCase());
                dto.setOperator(log.getUsername());
                dto.setPending(false);
                allActivities.add(dto);
            }
        }

        if (isUser || isManager) {
            List<AssetApplication> userApps = assetApplicationRepository
                    .findByApplicantIdOrderByUpdateTimeDesc(user.getId());
            for (AssetApplication app : userApps) {
                if (!ApplicationStatus.PENDING.getCode().equals(app.getStatus())) {
                    RecentOperationDTO dto = new RecentOperationDTO();
                    dto.setTime(app.getUpdateTime() != null ? app.getUpdateTime() : app.getApplicationDate());
                    dto.setTimeText(
                            formatTime(app.getUpdateTime() != null ? app.getUpdateTime() : app.getApplicationDate()));
                    String appType = getApplicationTypeText(app.getApplicationType());
                    String assetName = app.getAssetName() != null ? app.getAssetName() : app.getItemName();
                    String statusText = getStatusText(app.getStatus());
                    ApplicationStatus appStatus = ApplicationStatus.fromCode(app.getStatus());
                    String statusIcon = appStatus == ApplicationStatus.APPROVED ? "✓" : "✗";
                    dto.setContent("[资产] " + statusIcon + " 您的" + appType + "已" + statusText + "："
                            + (assetName != null ? assetName : "未知物品"));
                    dto.setType("application");
                    dto.setOperator(user.getRealName());
                    dto.setPending(false);
                    allActivities.add(dto);
                }
            }

            if (isManager) {
                List<PurchaseRequest> userPurchases = purchaseRequestRepository
                        .findByApplicantIdOrderByUpdateTimeDesc(user.getId());
                for (PurchaseRequest req : userPurchases) {
                    if (!PurchaseRequestStatus.PENDING.getCode().equals(req.getStatus())) {
                        RecentOperationDTO dto = new RecentOperationDTO();
                        dto.setTime(req.getUpdateTime() != null ? req.getUpdateTime() : req.getApplicationDate());
                        dto.setTimeText(formatTime(
                                req.getUpdateTime() != null ? req.getUpdateTime() : req.getApplicationDate()));
                        String statusText = getPurchaseStatusText(req.getStatus());
                        PurchaseRequestStatus reqStatus = PurchaseRequestStatus.fromCode(req.getStatus());
                        String statusIcon = reqStatus == PurchaseRequestStatus.APPROVED ? "✓" : "✗";
                        dto.setContent("[采购] " + statusIcon + " 您的采购需求申请已" + statusText + "：" + req.getItemName());
                        dto.setType("purchase");
                        dto.setOperator(user.getRealName());
                        dto.setPending(false);
                        allActivities.add(dto);
                    }
                }
            }
        }

        if (isLeader || isAdmin) {
            List<PurchaseRequest> approvedPurchases = purchaseRequestRepository
                    .findByApproverIdOrderByApprovalDateDesc(user.getId());
            for (PurchaseRequest req : approvedPurchases) {
                RecentOperationDTO dto = new RecentOperationDTO();
                dto.setTime(req.getApprovalDate());
                dto.setTimeText(formatTime(req.getApprovalDate()));
                PurchaseRequestStatus reqStatus = PurchaseRequestStatus.fromCode(req.getStatus());
                String statusIcon = reqStatus == PurchaseRequestStatus.APPROVED ? "✓" : "✗";
                String action = reqStatus == PurchaseRequestStatus.APPROVED ? "批准" : "拒绝";
                dto.setContent("[采购] " + statusIcon + " 您" + action + "了采购需求申请：" + req.getItemName());
                dto.setType("purchase");
                dto.setOperator(user.getRealName());
                dto.setPending(false);
                allActivities.add(dto);
            }
        }

        allActivities.sort(Comparator.comparing(RecentOperationDTO::getTime).reversed());
        if (allActivities.size() > limit) {
            allActivities = allActivities.subList(0, limit);
        }

        return new DashboardOperationsDTO(pendingTasks, allActivities);
    }

    /**
     * 保留原有方法用于兼容性，参数从User实体改为UserDTO
     */
    public List<RecentOperationDTO> getRecentOperations(int limit, UserDTO user) {
        DashboardOperationsDTO dto = getDashboardOperations(limit, user);
        List<RecentOperationDTO> combined = new ArrayList<>();
        combined.addAll(dto.getPendingTasks());
        combined.addAll(dto.getRecentActivities());
        combined.sort(Comparator.comparing(RecentOperationDTO::getTime).reversed());
        if (combined.size() > limit) {
            return combined.subList(0, limit);
        }
        return combined;
    }

    /**
     * 权限检查方法
     * Feign改造：使用AuthFeignClient远程调用获取用户部门信息
     */
    private boolean checkPermission(Long logUserId, UserDTO user,
            boolean isAdmin, boolean isLeader, boolean isManager, Long deptId) {
        if (isAdmin)
            return true;
        if (logUserId == null)
            return false;
        if (logUserId.equals(user.getId()))
            return true;

        if ((isLeader || isManager) && deptId != null) {
            // Feign远程调用：获取日志用户的部门ID
            Result<UserDTO> logUserResult = authFeignClient.getUserById(logUserId);
            UserDTO logUser = logUserResult.getData();
            return logUser != null && deptId.equals(logUser.getDeptId());
        }

        return false;
    }

    private String formatTime(LocalDateTime time) {
        if (time == null)
            return "";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(time, now);

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时前";
        } else if (seconds < 604800) {
            return (seconds / 86400) + "天前";
        } else {
            return time.toLocalDate().toString();
        }
    }

    private String getApplicationTypeText(String type) {
        if (type == null)
            return "资产申请";
        ApplicationType appType = ApplicationType.fromCode(type);
        return switch (appType) {
            case RECEIVE -> "申请领用资产";
            case TRANSFER -> "申请转移资产";
            case MAINTENANCE -> "申请维修资产";
            case DISPOSAL -> "申请报废资产";
            default -> "资产申请";
        };
    }

    private String getApplicationTypeTextShort(String type) {
        if (type == null)
            return "资产";
        ApplicationType appType = ApplicationType.fromCode(type);
        return switch (appType) {
            case RECEIVE -> "领用";
            case TRANSFER -> "转移";
            case MAINTENANCE -> "维修";
            case DISPOSAL -> "报废";
            default -> "资产";
        };
    }

    private String getStatusText(String status) {
        if (status == null)
            return "未知状态";
        ApplicationStatus appStatus = ApplicationStatus.fromCode(status);
        return appStatus.getName();
    }

    private String getPurchaseStatusText(String status) {
        if (status == null)
            return "未知状态";
        PurchaseRequestStatus reqStatus = PurchaseRequestStatus.fromCode(status);
        return reqStatus.getName();
    }

    private String getLogTypeLabel(String logType) {
        if (logType == null) {
            return "[系统]";
        }
        LogType type = LogType.fromCode(logType);
        return "[" + type.getName() + "]";
    }

    private String formatLogContent(String operation, String params, String logType) {
        if (operation == null) {
            return params != null ? params : "操作记录";
        }

        LogType type = LogType.fromCode(logType);

        if (type == LogType.INVENTORY) {
            return formatInventoryContent(operation, params);
        }

        if (type == LogType.ASSET) {
            return formatAssetContent(operation, params);
        }

        if (operation.startsWith("批准")) {
            return "✓ " + operation + (params != null ? "：" + params : "");
        } else if (operation.startsWith("拒绝")) {
            return "✗ " + operation + (params != null ? "：" + params : "");
        } else if (operation.startsWith("提交")) {
            return "→ " + operation + (params != null ? "：" + params : "");
        }

        return operation + (params != null && !params.isEmpty() ? "：" + params : "");
    }

    private String formatInventoryContent(String operation, String params) {
        if ("创建盘点计划".equals(operation)) {
            if (params != null && params.startsWith("创建了盘点计划: ")) {
                String planName = params.substring(10);
                return "创建了盘点计划「" + planName + "」";
            }
            return "创建了盘点计划";
        } else if ("分配盘点任务".equals(operation)) {
            if (params != null && params.contains("分配给")) {
                String result = params.replace("将盘点计划'", "").replace("'分配给: ", " → ");
                String[] parts = result.split(" → ");
                if (parts.length >= 2) {
                    return "分配盘点任务「" + parts[0] + "」给 " + parts[1];
                } else {
                    return "分配了盘点任务";
                }
            }
            return "分配了盘点任务";
        } else if ("收到盘点任务".equals(operation)) {
            if (params != null && params.contains("分配给您盘点任务: ")) {
                String[] parts = params.split(" 分配给您盘点任务: ");
                if (parts.length == 2) {
                    return parts[0] + " 分配给您盘点任务「" + parts[1] + "」";
                }
            }
            return "收到盘点任务";
        } else if ("开始盘点".equals(operation)) {
            if (params != null && params.startsWith("开始执行盘点任务: ") && params.length() > 12) {
                String planName = params.substring(12);
                return "开始执行盘点「" + planName + "」";
            }
            return "开始执行盘点任务";
        } else if ("完成盘点".equals(operation)) {
            if (params != null && params.startsWith("完成盘点任务: ") && params.length() > 10) {
                String planInfo = params.substring(10);
                return "完成盘点「" + planInfo + "」";
            }
            return "完成盘点任务";
        } else if ("删除盘点计划".equals(operation)) {
            if (params != null && params.startsWith("删除了盘点计划: ") && params.length() > 10) {
                String planName = params.substring(10);
                return "删除了盘点计划「" + planName + "」";
            }
            return "删除了盘点计划";
        }
        return operation + (params != null ? "：" + params : "");
    }

    private String formatAssetContent(String operation, String params) {
        if ("新增资产".equals(operation)) {
            return "新增资产" + (params != null ? "：" + params : "");
        } else if ("修改资产".equals(operation)) {
            return "修改资产信息" + (params != null ? "：" + params : "");
        } else if ("删除资产".equals(operation)) {
            return "删除资产" + (params != null ? "：" + params : "");
        } else if ("转移资产".equals(operation)) {
            return "转移资产" + (params != null ? "：" + params : "");
        } else if ("报废资产".equals(operation)) {
            return "报废资产" + (params != null ? "：" + params : "");
        }
        return operation + (params != null ? "：" + params : "");
    }
}