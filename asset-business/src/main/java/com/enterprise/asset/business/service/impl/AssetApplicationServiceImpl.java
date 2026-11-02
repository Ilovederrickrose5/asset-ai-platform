package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.common.enums.ApplicationStatus;
import com.enterprise.asset.common.enums.ApplicationType;
import com.enterprise.asset.common.enums.AssetStatus;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.AssetApplication;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.exception.BusinessException;
import com.enterprise.asset.business.repository.AssetApplicationRepository;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import com.enterprise.asset.business.service.AssetApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 资产业务申请服务实现 - 处理业务逻辑 */
@Service
public class AssetApplicationServiceImpl implements AssetApplicationService {

    private final AssetApplicationRepository assetApplicationRepository;
    private final AssetRepository assetRepository;
    private final SysLogRepository sysLogRepository;

    public AssetApplicationServiceImpl(AssetApplicationRepository assetApplicationRepository,
            AssetRepository assetRepository, SysLogRepository sysLogRepository) {
        this.assetApplicationRepository = assetApplicationRepository;
        this.assetRepository = assetRepository;
        this.sysLogRepository = sysLogRepository;
    }

    // === 查询方法 ===

    @Override
    public List<AssetApplication> getAllApplications() {
        return assetApplicationRepository.findAll();
    }

    @Override
    public List<AssetApplication> getAllApplicationsByDepartment(Long departmentId) {
        return assetApplicationRepository.findByDepartmentId(departmentId);
    }

    @Override
    public AssetApplication getApplicationById(Long id) {
        return assetApplicationRepository.findById(id).orElse(null);
    }

    @Override
    public List<AssetApplication> getApplicationsByApplicantId(Long applicantId) {
        return assetApplicationRepository.findByApplicantId(applicantId);
    }

    @Override
    public List<AssetApplication> getApplicationsByStatus(String status) {
        return assetApplicationRepository.findByStatus(status);
    }

    @Override
    public List<AssetApplication> getApplicationsByStatusAndDepartment(String status, Long departmentId) {
        return assetApplicationRepository.findByStatusAndDepartmentId(status, departmentId);
    }

    // === 核心业务流程 ===

    /**
     * 创建申请
     * Controller传入: AssetApplication对象(包含assetId、applicantId、applicationType等)
     * 业务逻辑: 设置申请时间→校验资产状态→原子占用资产→填充资产信息→保存申请→添加操作日志
     * 返回: 保存后的申请记录
     */
    @Override
    @Transactional
    public AssetApplication createApplication(AssetApplication application) {
        application.setApplicationDate(LocalDateTime.now());
        if (application.getStatus() == null || application.getStatus().isEmpty()) {
            application.setStatus(ApplicationStatus.PENDING.getCode());
        }

        // === 闸3: 校验资产状态 - 已报废/已处置的资产不能再申请任何业务 ===
        Asset asset = assetRepository.findById(application.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在,无法创建申请"));

        AssetStatus assetStatus = AssetStatus.fromCode(asset.getStatus());
        if (assetStatus != null && assetStatus.isScrapped()) {
            throw new BusinessException("资产已报废,无法创建申请");
        }

        // 填充资产名称和编号
        if (application.getAssetName() == null || application.getAssetName().isEmpty()) {
            application.setAssetName(asset.getAssetName());
        }
        if (application.getAssetNo() == null || application.getAssetNo().isEmpty()) {
            application.setAssetNo(asset.getAssetNo());
        }

        // 先保存申请拿到ID,再尝试占用资产
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        // === 闸1: 原子占用资产,防并发重复申请 ===
        // 第一阶段: 尝试无主占用(资产当前未被任何申请占用)
        int rows = assetRepository.lockAsset(asset.getId(), savedApplication.getId());
        if (rows == 0) {
            // 第二阶段: 占用失败,检查是否为"二级审批接力"场景
            // 仅当旧占用申请已处于非活跃状态(已审批/已驳回/已完成等)时,才允许接力占用
            Asset occupiedAsset = assetRepository.findById(asset.getId()).orElse(null);
            Long currentAppId = occupiedAsset != null ? occupiedAsset.getCurrentApplicationId() : null;
            if (currentAppId != null) {
                AssetApplication currentApp = assetApplicationRepository.findById(currentAppId).orElse(null);
                if (currentApp != null && isInactiveApplicationStatus(currentApp.getStatus())) {
                    // 用条件UPDATE原子接力,防止接力瞬间又有新申请钻空子
                    rows = assetRepository.transferLock(asset.getId(), currentAppId, savedApplication.getId());
                }
            }
        }
        if (rows == 0) {
            // 抛异常触发事务回滚,撤回刚保存的申请记录
            throw new BusinessException("该资产已有进行中的申请,请勿重复提交");
        }

        // 添加操作日志到SysLog表
        SysLog log = new SysLog();
        log.setUserId(application.getApplicantId());
        log.setUsername(application.getApplicantName());
        log.setOperation("申请" + getTypeNameCN(application.getApplicationType()) + ": " + asset.getAssetName());
        log.setLogType("ASSET");
        log.setStatus("success");
        sysLogRepository.save(log);

        return savedApplication;
    }

    @Override
    @Transactional
    public AssetApplication updateApplication(Long id, AssetApplication application) {
        AssetApplication existingApplication = assetApplicationRepository.findById(id).orElse(null);
        if (existingApplication == null) {
            return null;
        }
        existingApplication.setApplicationReason(application.getApplicationReason());
        existingApplication.setDepartmentId(application.getDepartmentId());
        existingApplication.setDepartmentName(application.getDepartmentName());
        return assetApplicationRepository.save(existingApplication);
    }

    @Override
    @Transactional
    public boolean deleteApplication(Long id) {
        if (assetApplicationRepository.existsById(id)) {
            assetApplicationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 批准申请
     * Controller传入:
     * id(申请ID)、approverId(审批人ID)、approverName(审批人姓名)、approvalRemark(审批备注)
     * 业务逻辑: 状态机校验→更新申请状态→根据申请类型更新资产状态→释放占用→添加审批日志
     * 返回: 批准后的申请记录
     */
    @Override
    @Transactional
    public AssetApplication approveApplication(Long id, Long approverId, String approverName, String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }

        ApplicationType type = ApplicationType.fromCode(application.getApplicationType());

        // === 闸3: 状态机校验 - 防止重复审批/跨状态审批 ===
        // 已 finalize 的申请(approved/rejected)禁止再审批,这是并发的关键防线
        ApplicationStatus currentStatus = ApplicationStatus.fromCode(application.getStatus());
        if (currentStatus != null && currentStatus.isFinalized()) {
            throw new BusinessException("申请已结束审批流程,请勿重复操作");
        }
        if (!isApprovableStatus(type, currentStatus)) {
            throw new BusinessException("当前申请状态[" + application.getStatus() + "]不允许审批");
        }

        // 更新申请状态为已批准
        application.setStatus(ApplicationStatus.APPROVED.getCode());
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        // 根据申请类型更新资产状态
        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            // === 闸3: 资产状态校验 - 已报废的资产不能再被领用/转移/维修 ===
            if (type != ApplicationType.DISPOSAL) {
                AssetStatus assetStatus = AssetStatus.fromCode(asset.getStatus());
                if (assetStatus != null && assetStatus.isScrapped()) {
                    throw new BusinessException("资产已报废,无法执行此审批");
                }
            }

            // 根据applicationType 区分业务逻辑
            if (type == ApplicationType.RECEIVE) {
                // 领用: 资产分配给申请人
                asset.setStatus(AssetStatus.USING.getCode());
                asset.setUseStatus(AssetStatus.USING.getCode());
                asset.setUserId(application.getApplicantId());
                asset.setDeptId(application.getDepartmentId());
            } else if (type == ApplicationType.TRANSFER) {
                // 转移: 资产分配给接收方
                asset.setStatus(AssetStatus.USING.getCode());
                asset.setUseStatus(AssetStatus.USING.getCode());
                asset.setUserId(application.getTransfereeId());
                asset.setDeptId(application.getTransfereeDeptId());
            } else if (type == ApplicationType.DISPOSAL) {
                // 报废: 资产状态改为报废
                asset.setStatus(AssetStatus.SCRAPPED.getCode());
                asset.setUseStatus(AssetStatus.SCRAPPED.getCode());
                if (application.getEstimatedValue() != null) {
                    asset.setPurchasePrice(application.getEstimatedValue());
                }
            } else if (type == ApplicationType.MAINTENANCE) {
                // 维修: 批准后资产状态不变,等待点击开始维修
            }
            // === 闸2: 保存资产状态时,@Version自动校验,并发冲突会抛 ObjectOptimisticLockingFailureException
            // ===
            // 由 GlobalExceptionHandler 捕获并返回"资产状态已被其他人变更,请刷新后重试"
            if (type != ApplicationType.MAINTENANCE) {
                assetRepository.save(asset);
                // 审批通过后释放占用(维修除外,维修期间需继续占用资产防并发申请)
                assetRepository.unlockAsset(asset.getId(), application.getId());
            }

            // 添加审批日志
            SysLog log = new SysLog();
            log.setUserId(approverId);
            log.setUsername(approverName);
            log.setOperation("批准" + getTypeNameCN(application.getApplicationType()) + ": " + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);

            // 转移申请额外添加双方日志
            if (type == ApplicationType.TRANSFER) {
                addTransferLogs(application, asset);
            }
        }

        return savedApplication;
    }

    @Override
    @Transactional
    public AssetApplication approveApplicationForLeader(Long id, Long approverId, String approverName,
            String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }
        // 状态改为等待领导审批(二级审批流程)
        application.setStatus(ApplicationStatus.PENDING_LEADER.getCode());
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);
        return assetApplicationRepository.save(application);
    }

    /**
     * 拒绝申请
     * Controller传入: id、approverId、approverName、approvalRemark
     * 业务逻辑: 更新申请状态为rejected→添加拒绝日志
     */
    @Override
    @Transactional
    public AssetApplication rejectApplication(Long id, Long approverId, String approverName, String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }

        // === 闸3: 状态机校验 - 已 finalize 的申请不能再拒绝 ===
        ApplicationStatus currentStatus = ApplicationStatus.fromCode(application.getStatus());
        if (currentStatus != null && currentStatus.isFinalized()) {
            throw new BusinessException("申请已结束审批流程,无法再次拒绝");
        }

        application.setStatus(ApplicationStatus.REJECTED.getCode());
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            // 拒绝后释放占用,允许其他人重新对该资产提交申请
            assetRepository.unlockAsset(asset.getId(), application.getId());

            SysLog log = new SysLog();
            log.setUserId(approverId);
            log.setUsername(approverName);
            log.setOperation("拒绝" + getTypeNameCN(application.getApplicationType()) + ": " + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    @Override
    @Transactional
    public AssetApplication updateStatus(Long id, String status) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }
        application.setStatus(status);
        return assetApplicationRepository.save(application);
    }

    /**
     * 开始维修
     * Controller传入: id(申请ID)、userId(操作人ID)、userName(操作人姓名)
     * 业务逻辑: 验证状态→更新申请为维修中→更新资产状态为maintenance→添加日志
     */
    @Override
    @Transactional
    public AssetApplication startMaintenance(Long id, Long userId, String userName) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null || !ApplicationStatus.APPROVED.getCode().equals(application.getStatus())) {
            return null;
        }

        application.setStatus(ApplicationStatus.IN_PROGRESS.getCode());
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            asset.setStatus(AssetStatus.MAINTENANCE.getCode());
            asset.setUseStatus(AssetStatus.MAINTENANCE.getCode());
            assetRepository.save(asset);

            SysLog log = new SysLog();
            log.setUserId(userId);
            log.setUsername(userName);
            log.setOperation("开始维修: " + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    /**
     * 完成维修
     * Controller传入: id、userId、userName
     * 业务逻辑: 验证状态→更新申请为已完成→更新资产状态为using→添加日志
     */
    @Override
    @Transactional
    public AssetApplication completeMaintenance(Long id, Long userId, String userName) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null || !ApplicationStatus.IN_PROGRESS.getCode().equals(application.getStatus())) {
            return null;
        }

        application.setStatus(ApplicationStatus.COMPLETED.getCode());
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            asset.setStatus(AssetStatus.USING.getCode());
            asset.setUseStatus(AssetStatus.USING.getCode());
            assetRepository.save(asset);
            // 维修完成释放占用,允许其他人重新申请该资产
            assetRepository.unlockAsset(asset.getId(), application.getId());

            SysLog log = new SysLog();
            log.setUserId(userId);
            log.setUsername(userName);
            log.setOperation("完成维修: " + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    // === 分页查询 ===

    @Override
    public Page<AssetApplication> getAllApplications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AssetApplication.APPLICATION_DATE));
        return assetApplicationRepository.findAll(pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByApplicantId(Long applicantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AssetApplication.APPLICATION_DATE));
        return assetApplicationRepository.findByApplicantId(applicantId, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByDepartmentId(Long departmentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AssetApplication.APPLICATION_DATE));
        return assetApplicationRepository.findByDepartmentId(departmentId, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByTypeAndStatus(String type, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AssetApplication.APPLICATION_DATE));
        return assetApplicationRepository.findByTypeAndStatus(type, status, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByApplicantIdAndTypeAndStatus(Long applicantId, String type,
            String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AssetApplication.APPLICATION_DATE));
        return assetApplicationRepository.findByApplicantIdAndTypeAndStatus(applicantId, type, status, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByDepartmentIdAndTypeAndStatus(Long departmentId, String type,
            String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AssetApplication.APPLICATION_DATE));
        return assetApplicationRepository.findByDepartmentIdAndTypeAndStatus(departmentId, type, status, pageable);
    }

    // === 工具方法 ===

    /** 申请类型英文转中文 */
    private String getTypeNameCN(String applicationType) {
        ApplicationType type = ApplicationType.fromCode(applicationType);
        if (type != null) {
            return "资产" + type.getName();
        }
        return applicationType;
    }

    /**
     * 判断申请是否可被审批(状态机校验)
     * - DISPOSAL(报废): 允许从 PENDING_LEADER(领导审批) 或 LEADER_APPROVED(资产管理员终审) 转入
     * APPROVED
     * - 其他类型: 仅允许从 PENDING 转入 APPROVED
     * 兼容 controller 现有调用方式
     */
    private boolean isApprovableStatus(ApplicationType type, ApplicationStatus currentStatus) {
        if (currentStatus == null) {
            return false;
        }
        if (type == ApplicationType.DISPOSAL) {
            return currentStatus == ApplicationStatus.PENDING_LEADER
                    || currentStatus == ApplicationStatus.LEADER_APPROVED;
        }
        return currentStatus == ApplicationStatus.PENDING;
    }

    /**
     * 判断申请状态是否"非活跃"(已离开审批流程)
     * 用于 createApplication 的接力占用判定: 仅当旧占用申请已非活跃时,新申请才能接管占用
     * 包含: APPROVED/REJECTED/COMPLETED/LEADER_APPROVED + controller 自定义的
     * final_approval_created
     */
    private boolean isInactiveApplicationStatus(String statusCode) {
        if (statusCode == null) {
            return false;
        }
        ApplicationStatus status = ApplicationStatus.fromCode(statusCode);
        if (status != null && (status.isFinalized() || status == ApplicationStatus.COMPLETED
                || status == ApplicationStatus.LEADER_APPROVED)) {
            return true;
        }
        // controller createFinalApproval 流程会用到这个中间态
        return "final_approval_created".equalsIgnoreCase(statusCode);
    }

    /** 资产转移添加双方日志 */
    private void addTransferLogs(AssetApplication application, Asset asset) {
        SysLog transferorLog = new SysLog();
        transferorLog.setUserId(application.getApplicantId());
        transferorLog.setUsername(application.getApplicantName());
        String transfereeName = application.getTransfereeName() != null ? application.getTransfereeName() : "未知用户";
        transferorLog.setOperation("资产转移成功:已将" + asset.getAssetName() + "转移给" + transfereeName);
        transferorLog.setLogType("ASSET");
        transferorLog.setStatus("success");
        sysLogRepository.save(transferorLog);

        if (application.getTransfereeId() != null && application.getTransfereeName() != null) {
            SysLog transfereeLog = new SysLog();
            transfereeLog.setUserId(application.getTransfereeId());
            transfereeLog.setUsername(application.getTransfereeName());
            transfereeLog.setOperation("收到资产:" + application.getApplicantName() + "转移给您的" + asset.getAssetName());
            transfereeLog.setLogType("ASSET");
            transfereeLog.setStatus("success");
            sysLogRepository.save(transfereeLog);
        }
    }
}