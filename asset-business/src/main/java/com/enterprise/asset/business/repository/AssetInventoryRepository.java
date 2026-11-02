package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.AssetInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 资产盘点数据访问接口
 * 关联表: asset_inventory(资产盘点表)
 * 主要操作: 盘点计划的增删改查、权限过滤查询、任务分配查询
 */
public interface AssetInventoryRepository extends JpaRepository<AssetInventory, Long> {

    /** 根据状态查询盘点计划 */
    List<AssetInventory> findByStatus(String status);

    /** 根据创建人ID查询盘点计划 */
    List<AssetInventory> findByCreatorId(Long creatorId);

    /** 根据盘点编号前缀查询 */
    List<AssetInventory> findByInventoryNoStartingWith(String prefix);

    /** 根据盘点范围查询 */
    List<AssetInventory> findByInventoryScope(String inventoryScope);

    /** 查询我创建的或分配给我的任务 */
    List<AssetInventory> findByCreatorIdOrAssigneeId(Long creatorId, Long assigneeId);

    /** 查询我创建的、分配给我的、或我部门的任务 */
    List<AssetInventory> findByCreatorIdOrAssigneeIdOrInventoryScope(Long creatorId, Long assigneeId,
            String inventoryScope);

    /** 查询我创建的、分配给我的、或本部门用户创建且盘点范围是本部门的任务 */
    @Query("SELECT ai FROM AssetInventory ai WHERE " +
           "ai.creatorId = :userId OR " +
           "ai.assigneeId = :userId OR " +
           "(ai.inventoryScope = :deptName AND ai.deptId = :deptId)")
    List<AssetInventory> findMyTasksOrDeptTasks(@Param("userId") Long userId,
                                                 @Param("deptName") String deptName,
                                                 @Param("deptId") Long deptId);
    
    /** 按被分配人ID查询并按创建时间倒序 */
    @Query("SELECT ai FROM AssetInventory ai WHERE ai.assigneeId = :assigneeId ORDER BY ai.createTime DESC")
    List<AssetInventory> findByAssigneeIdOrderByCreateTimeDesc(@Param("assigneeId") Long assigneeId);
}