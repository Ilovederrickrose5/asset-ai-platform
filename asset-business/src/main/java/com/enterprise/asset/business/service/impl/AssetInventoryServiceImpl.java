package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.enums.InventoryStatus;
import com.enterprise.asset.common.enums.LogType;
import com.enterprise.asset.common.enums.UserRole;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.AssetInventory;
import com.enterprise.asset.business.entity.AssetInventoryDetail;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.repository.AssetInventoryDetailRepository;
import com.enterprise.asset.business.repository.AssetInventoryRepository;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import com.enterprise.asset.business.service.AssetInventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 资产盘点服务实现 - 处理盘点计划管理与执行(含权限校验、任务分配、结果统计) */
@Service
public class AssetInventoryServiceImpl implements AssetInventoryService {

    private final AssetInventoryRepository assetInventoryRepository;
    private final AssetInventoryDetailRepository assetInventoryDetailRepository;
    private final AssetRepository assetRepository;
    private final SysLogRepository sysLogRepository;

    public AssetInventoryServiceImpl(AssetInventoryRepository assetInventoryRepository,
            AssetInventoryDetailRepository assetInventoryDetailRepository, AssetRepository assetRepository,
            SysLogRepository sysLogRepository) {
        this.assetInventoryRepository = assetInventoryRepository;
        this.assetInventoryDetailRepository = assetInventoryDetailRepository;
        this.assetRepository = assetRepository;
        this.sysLogRepository = sysLogRepository;
    }

    @Override
    @Transactional
    public AssetInventory createPlan(AssetInventory plan) {
        UserDTO currentUser = getCurrentUser();
        if (!canCreatePlan()) {
            throw new RuntimeException("您没有创建盘点计划的权限");
        }

        if (!isAdmin(currentUser)) {
            if ("全部资产".equals(plan.getInventoryScope())) {
                throw new RuntimeException("您不能创建全部资产的盘点计划");
            }
            plan.setDeptId(currentUser.getDeptId());
        }
        plan.setCreatorId(currentUser.getId());
        plan.setCreatorName(displayName(currentUser));

        String inventoryNo = generateInventoryNo();
        plan.setInventoryNo(inventoryNo);

        if (plan.getPlanName() == null || plan.getPlanName().isEmpty()) {
            plan.setPlanName(plan.getInventoryName());
        }

        plan.setStatus(InventoryStatus.PENDING.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        generateInventoryDetails(savedPlan);

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getCreatorId(), savedPlan.getCreatorName(), "创建盘点计划",
                "创建了盘点计划: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    private void generateInventoryDetails(AssetInventory plan) {
        List<Asset> assets = new ArrayList<>();
        String scope = plan.getInventoryScope();

        if (plan.getDeptId() != null) {
            assets = assetRepository.findByDeptId(plan.getDeptId());
        } else if ("全部资产".equals(scope)) {
            assets = assetRepository.findAll();
        }

        for (Asset asset : assets) {
            AssetInventoryDetail detail = new AssetInventoryDetail();
            detail.setInventoryId(plan.getId());
            detail.setAssetId(asset.getId());
            detail.setAssetNo(asset.getAssetNo());
            detail.setAssetName(asset.getAssetName());
            detail.setSystemQuantity(1);
            detail.setActualQuantity(0);
            detail.setDifferenceQuantity(0);
            detail.setStatus(InventoryStatus.PENDING.getCode());
            detail.setCreateTime(LocalDateTime.now());
            if (plan.getCreatorId() != null) {
                detail.setInventoryPersonId(plan.getCreatorId());
                detail.setInventoryPersonName(plan.getCreatorName());
            }
            assetInventoryDetailRepository.save(detail);
        }
    }

    private String generateInventoryNo() {
        LocalDateTime now = LocalDateTime.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String prefix = "INV" + year + month;

        List<AssetInventory> thisMonthPlans = assetInventoryRepository
                .findByInventoryNoStartingWith(prefix);
        int maxSeq = 0;
        for (AssetInventory plan : thisMonthPlans) {
            String no = plan.getInventoryNo();
            if (no != null && no.length() >= 13 && no.startsWith(prefix)) {
                String seqStr = no.substring(no.length() - 5);
                try {
                    int seq = Integer.parseInt(seqStr);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        int nextSeq = maxSeq + 1;
        return prefix + String.format("%05d", nextSeq);
    }

    @Override
    public AssetInventory getPlanById(Long id) {
        return assetInventoryRepository.findById(id).orElse(null);
    }

    @Override
    public List<AssetInventory> getAllPlans() {
        return assetInventoryRepository.findAll();
    }

    @Override
    public List<AssetInventory> getPlansByCreatorOrAssignee(Long userId) {
        return assetInventoryRepository.findByCreatorIdOrAssigneeId(userId, userId);
    }

    @Override
    public List<AssetInventory> getPlansByUserAndDept(Long userId, String deptName) {
        return assetInventoryRepository.findByCreatorIdOrAssigneeIdOrInventoryScope(userId, userId, deptName);
    }

    public List<AssetInventory> getPlansByStatus(String status) {
        return assetInventoryRepository.findByStatus(status);
    }

    @Override
    public List<AssetInventory> getPlansByCreatorId(Long creatorId) {
        return assetInventoryRepository.findByCreatorId(creatorId);
    }

    @Override
    @Transactional
    public AssetInventory updatePlan(Long id, AssetInventory plan) {
        AssetInventory existingPlan = assetInventoryRepository.findById(id).orElse(null);
        if (existingPlan == null) {
            return null;
        }
        existingPlan.setPlanName(plan.getPlanName());
        existingPlan.setInventoryScope(plan.getInventoryScope());
        existingPlan.setExpectedCompletionTime(plan.getExpectedCompletionTime());
        existingPlan.setRemark(plan.getRemark());
        return assetInventoryRepository.save(existingPlan);
    }

    @Override
    @Transactional
    public AssetInventory updatePlanStatus(Long id, String status) {
        AssetInventory existingPlan = assetInventoryRepository.findById(id).orElse(null);
        if (existingPlan == null) {
            return null;
        }
        existingPlan.setStatus(status);
        if (InventoryStatus.COMPLETED.getCode().equals(status)) {
            existingPlan.setActualCompletionTime(LocalDateTime.now());
        }
        return assetInventoryRepository.save(existingPlan);
    }

    @Override
    @Transactional
    public boolean deletePlan(Long id) {
        Optional<AssetInventory> planOpt = assetInventoryRepository.findById(id);
        if (planOpt.isPresent()) {
            AssetInventory plan = planOpt.get();

            String username = getCurrentUsername();
            Long userId = getCurrentUserId();

            String planName = plan.getInventoryName() != null ? plan.getInventoryName() : plan.getPlanName();
            saveInventoryLog(userId, username, "删除盘点计划",
                    "删除了盘点计划: " + planName, LogType.INVENTORY.getCode());

            assetInventoryDetailRepository.deleteByInventoryId(id);
            assetInventoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public AssetInventory assignTask(Long id, Long assigneeId, String assigneeName, String inventoryArea) {
        AssetInventory plan = assetInventoryRepository.findById(id).orElse(null);
        if (plan == null) {
            return null;
        }

        if (!canAssignTask(plan, assigneeId)) {
            throw new RuntimeException("您没有分配该盘点任务的权限");
        }

        plan.setAssigneeId(assigneeId);
        plan.setAssigneeName(assigneeName);
        plan.setInventoryArea(inventoryArea);
        plan.setStatus(InventoryStatus.IN_PROGRESS.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        String currentUsername = getCurrentUsername();
        Long currentUserId = getCurrentUserId();

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();

        saveInventoryLog(currentUserId, currentUsername, "分配盘点任务",
                "将盘点计划'" + planName + "'分配给: " + assigneeName, LogType.INVENTORY.getCode());

        saveInventoryLog(assigneeId, assigneeName, "收到盘点任务",
                currentUsername + " 分配给您盘点任务: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    private void saveInventoryLog(Long userId, String username, String operation, String params, String logType) {
        try {
            SysLog log = new SysLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setOperation(operation);
            log.setParams(params);
            log.setLogType(logType);
            log.setStatus("success");
            sysLogRepository.save(log);
        } catch (Exception e) {
        }
    }

    private boolean canAssignTask(AssetInventory plan, Long assigneeId) {
        return isManagerOrAdmin(getCurrentUser());
    }

    @Override
    @Transactional
    public AssetInventory startTask(Long id) {
        AssetInventory plan = assetInventoryRepository.findById(id).orElse(null);
        if (plan == null) {
            return null;
        }

        if (!hasInventoryPermission(id)) {
            throw new RuntimeException("您没有执行该盘点任务的权限");
        }

        plan.setStartTime(LocalDateTime.now());
        plan.setStatus(InventoryStatus.IN_PROGRESS.getCode());
        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getAssigneeId(), savedPlan.getAssigneeName(), "开始盘点",
                "开始执行盘点任务: " + planName, LogType.INVENTORY.getCode());

        return savedPlan;
    }

    @Override
    @Transactional
    public AssetInventory completeTask(Long id) {
        AssetInventory plan = assetInventoryRepository.findById(id).orElse(null);
        if (plan == null) {
            return null;
        }

        if (!hasInventoryPermission(id)) {
            throw new RuntimeException("您没有完成该盘点任务的权限");
        }

        plan.setCompleteTime(LocalDateTime.now());
        plan.setStatus(InventoryStatus.COMPLETED.getCode());
        plan.setActualCompletionTime(LocalDateTime.now());

        calculateInventoryResult(plan);

        AssetInventory savedPlan = assetInventoryRepository.save(plan);

        String planName = savedPlan.getInventoryName() != null ? savedPlan.getInventoryName() : savedPlan.getPlanName();
        saveInventoryLog(savedPlan.getAssigneeId(), savedPlan.getAssigneeName(), "完成盘点",
                "完成盘点任务: " + planName + " (盘盈: " + savedPlan.getSurplusCount() + ", 盘亏: " + savedPlan.getShortageCount()
                        + ")",
                LogType.INVENTORY.getCode());

        return savedPlan;
    }

    private void calculateInventoryResult(AssetInventory plan) {
        List<AssetInventoryDetail> details = assetInventoryDetailRepository.findByInventoryId(plan.getId());

        int totalCount = details.size();
        int actualCount = 0;
        int surplusCount = 0;
        int shortageCount = 0;
        for (AssetInventoryDetail detail : details) {
            int diff = detail.getActualQuantity() - detail.getSystemQuantity();
            detail.setDifferenceQuantity(diff);

            if (detail.getActualQuantity() > detail.getSystemQuantity()) {
                detail.setStatus(InventoryStatus.SURPLUS.getCode());
                surplusCount++;
                actualCount += detail.getActualQuantity();
            } else if (detail.getActualQuantity() < detail.getSystemQuantity()) {
                detail.setStatus(InventoryStatus.SHORTAGE.getCode());
                shortageCount++;
                actualCount += detail.getActualQuantity();
            } else if (detail.getActualQuantity() > 0) {
                detail.setStatus(InventoryStatus.NORMAL.getCode());
                actualCount += detail.getActualQuantity();
            } else {
                detail.setStatus(InventoryStatus.PENDING.getCode());
            }

            assetInventoryDetailRepository.save(detail);
        }

        plan.setTotalCount(totalCount);
        plan.setActualCount(actualCount);
        plan.setSurplusCount(surplusCount);
        plan.setShortageCount(shortageCount);
    }

    @Override
    @Transactional
    public AssetInventoryDetail addInventoryDetail(Long inventoryId, AssetInventoryDetail detail) {
        detail.setInventoryId(inventoryId);
        return assetInventoryDetailRepository.save(detail);
    }

    @Override
    public List<AssetInventoryDetail> getInventoryDetails(Long inventoryId) {
        List<AssetInventoryDetail> details = assetInventoryDetailRepository.findByInventoryId(inventoryId);
        for (AssetInventoryDetail detail : details) {
            if (detail.getAssetName() == null || detail.getAssetName().isEmpty()) {
                Asset asset = assetRepository.findById(detail.getAssetId()).orElse(null);
                if (asset != null) {
                    detail.setAssetName(asset.getAssetName());
                }
            }
        }
        return details;
    }

    @Override
    @Transactional
    public AssetInventoryDetail updateInventoryDetail(Long detailId, AssetInventoryDetail detail) {
        AssetInventoryDetail existingDetail = assetInventoryDetailRepository.findById(detailId).orElse(null);
        if (existingDetail == null) {
            return null;
        }

        if (!hasInventoryPermission(existingDetail.getInventoryId())) {
            return null;
        }

        existingDetail.setActualQuantity(detail.getActualQuantity());
        existingDetail.setRemark(detail.getRemark());
        existingDetail.setStatus(detail.getStatus());
        return assetInventoryDetailRepository.save(existingDetail);
    }

    private boolean hasInventoryPermission(Long inventoryId) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        AssetInventory plan = assetInventoryRepository.findById(inventoryId).orElse(null);
        if (plan == null) {
            return false;
        }
        if (isAdmin(currentUser)) {
            return true;
        }
        if (isManagerOrAdmin(currentUser)) {
            return plan.getDeptId() == null || currentUser.getDeptId() == null
                    || plan.getDeptId().equals(currentUser.getDeptId());
        }
        return currentUser.getId().equals(plan.getAssigneeId());
    }

    @Override
    @Transactional
    public boolean deleteInventoryDetail(Long detailId) {
        if (assetInventoryDetailRepository.existsById(detailId)) {
            assetInventoryDetailRepository.deleteById(detailId);
            return true;
        }
        return false;
    }

    private boolean canCreatePlan() {
        UserDTO currentUser = getCurrentUser();
        return isManagerOrAdmin(currentUser) || hasRole(currentUser, UserRole.LEADER);
    }

    private UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof UserDTO userDTO ? userDTO : null;
    }

    private boolean isAdmin(UserDTO user) {
        return hasRole(user, UserRole.ADMIN);
    }

    private boolean isManagerOrAdmin(UserDTO user) {
        return isAdmin(user) || hasRole(user, UserRole.MANAGER, UserRole.ASSET_MANAGER, UserRole.DEPT_MANAGER);
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

    private String displayName(UserDTO user) {
        if (user == null) {
            return "system";
        }
        return user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
    }

    private String getCurrentUsername() {
        return displayName(getCurrentUser());
    }

    private Long getCurrentUserId() {
        UserDTO currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }
}