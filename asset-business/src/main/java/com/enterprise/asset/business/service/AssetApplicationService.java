package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.AssetApplication;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AssetApplicationService {

    /** 获取所有申请 */
    List<AssetApplication> getAllApplications();

    /** 按部门获取申请列表 */
    List<AssetApplication> getAllApplicationsByDepartment(Long departmentId);

    /** 根据ID获取申请 */
    AssetApplication getApplicationById(Long id);

    /** 创建申请 - applicationType: RECEIVE/TRANSFER/MAINTENANCE/DISPOSAL */
    AssetApplication createApplication(AssetApplication application);

    /** 更新申请 */
    AssetApplication updateApplication(Long id, AssetApplication application);

    /** 删除申请 */
    boolean deleteApplication(Long id);

    /** 按申请人ID获取申请 */
    List<AssetApplication> getApplicationsByApplicantId(Long applicantId);

    /** 按状态获取申请 */
    List<AssetApplication> getApplicationsByStatus(String status);

    /** 按状态和部门获取申请 */
    List<AssetApplication> getApplicationsByStatusAndDepartment(String status, Long departmentId);

    /** 审批申请(管理员/经理) */
    AssetApplication approveApplication(Long id, Long approverId, String approverName, String approvalRemark);

    /** 领导审批申请 */
    AssetApplication approveApplicationForLeader(Long id, Long approverId, String approverName, String approvalRemark);

    /** 驳回申请 */
    AssetApplication rejectApplication(Long id, Long approverId, String approverName, String approvalRemark);

    /** 更新申请状态 */
    AssetApplication updateStatus(Long id, String status);

    /** 开始维修流程 */
    AssetApplication startMaintenance(Long id, Long userId, String userName);

    /** 完成维修 */
    AssetApplication completeMaintenance(Long id, Long userId, String userName);

    /** 分页查询所有申请 */
    Page<AssetApplication> getAllApplications(int page, int size);

    /** 分页按申请人ID查询 */
    Page<AssetApplication> getApplicationsByApplicantId(Long applicantId, int page, int size);

    /** 分页按部门ID查询 */
    Page<AssetApplication> getApplicationsByDepartmentId(Long departmentId, int page, int size);

    /** 分页按类型和状态查询 */
    Page<AssetApplication> getApplicationsByTypeAndStatus(String type, String status, int page, int size);

    /** 分页按申请人ID、类型和状态查询 */
    Page<AssetApplication> getApplicationsByApplicantIdAndTypeAndStatus(Long applicantId, String type, String status,
            int page, int size);

    /** 分页按部门ID、类型和状态查询 */
    Page<AssetApplication> getApplicationsByDepartmentIdAndTypeAndStatus(Long departmentId, String type, String status,
            int page, int size);
}