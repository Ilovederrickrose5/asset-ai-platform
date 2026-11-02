/** 资产盘点服务接口 - 处理盘点计划管理与执行 */
package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.AssetInventory;
import com.enterprise.asset.business.entity.AssetInventoryDetail;

import java.util.List;

public interface AssetInventoryService {
    /** 创建盘点计划 */
    AssetInventory createPlan(AssetInventory plan);

    /** 根据ID获取盘点计划 */
    AssetInventory getPlanById(Long id);

    /** 获取所有盘点计划 */
    List<AssetInventory> getAllPlans();

    /** 根据状态获取盘点计划 */
    List<AssetInventory> getPlansByStatus(String status);

    /** 根据创建人ID获取盘点计划 */
    List<AssetInventory> getPlansByCreatorId(Long creatorId);

    /** 获取用户创建或参与的盘点计划 */
    List<AssetInventory> getPlansByCreatorOrAssignee(Long userId);

    /** 根据用户和部门获取盘点计划 */
    List<AssetInventory> getPlansByUserAndDept(Long userId, String deptName);

    /** 更新盘点计划 */
    AssetInventory updatePlan(Long id, AssetInventory plan);

    /** 更新盘点计划状态 */
    AssetInventory updatePlanStatus(Long id, String status);

    /** 删除盘点计划 */
    boolean deletePlan(Long id);

    /** 分配盘点任务 */
    AssetInventory assignTask(Long id, Long assigneeId, String assigneeName, String inventoryArea);

    /** 开始盘点任务 */
    AssetInventory startTask(Long id);
    
    /** 完成盘点任务 */
    AssetInventory completeTask(Long id);

    /** 添加盘点明细 */
    AssetInventoryDetail addInventoryDetail(Long inventoryId, AssetInventoryDetail detail);

    /** 获取盘点明细列表 */
    List<AssetInventoryDetail> getInventoryDetails(Long inventoryId);

    /** 更新盘点明细 */
    AssetInventoryDetail updateInventoryDetail(Long detailId, AssetInventoryDetail detail);

    /** 删除盘点明细 */
    boolean deleteInventoryDetail(Long detailId);
}