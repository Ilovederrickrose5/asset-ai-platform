package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 资产数据访问接口
 * 关联表: asset(资产表)
 * 主要操作: 资产的增删改查、统计查询
 */
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

        /** 查询所有资产总数(包含所有状态) */
        @Query(value = "SELECT COUNT(*) FROM asset", nativeQuery = true)
        long countAllAssets();

        /** 根据部门ID查询资产总数 */
        @Query(value = "SELECT COUNT(*) FROM asset WHERE dept_id = ?1", nativeQuery = true)
        long countAllAssetsByDepartment(Long departmentId);

        /** 根据用户ID查询资产总数 */
        @Query(value = "SELECT COUNT(*) FROM asset WHERE user_id = ?1", nativeQuery = true)
        long countAllAssetsByUser(Long userId);

        /** 根据部门ID查询资产列表 */
        List<Asset> findByDeptId(Long deptId);

        /** 根据用户ID查询资产列表 */
        List<Asset> findByUserId(Long userId);

        /** 查询所有未分配使用人的资产 */
        List<Asset> findByUserIdIsNull();

        /** 查询指定部门未分配使用人的资产 */
        List<Asset> findByDeptIdAndUserIdIsNull(Long deptId);

        /** 分页查询所有资产 */
        Page<Asset> findAll(Pageable pageable);

        /** 根据部门ID分页查询资产 */
        Page<Asset> findByDeptId(Long deptId, Pageable pageable);

        /** 根据用户ID分页查询资产 */
        Page<Asset> findByUserId(Long userId, Pageable pageable);

        /** 根据状态分页查询资产 */
        Page<Asset> findByStatus(String status, Pageable pageable);

        /** 根据部门ID和状态分页查询资产 */
        Page<Asset> findByDeptIdAndStatus(Long deptId, String status, Pageable pageable);

        /** 根据用户ID和状态分页查询资产 */
        Page<Asset> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

        /**
         * 原子占用资产: 仅当 current_application_id 为空时,设置为新申请ID
         * 利用数据库行锁 + 条件更新,保证并发下只有一个申请能占用成功
         * 
         * @return 更新行数: 1=占用成功, 0=已被其他申请占用
         */
        @Modifying
        @Query("UPDATE Asset a SET a.currentApplicationId = :applicationId " +
                        "WHERE a.id = :assetId AND a.currentApplicationId IS NULL")
        int lockAsset(@Param("assetId") Long assetId, @Param("applicationId") Long applicationId);

        /**
         * 释放资产占用: 仅当 current_application_id 等于指定申请ID时才清除
         * 加条件防止误释放别人的占用
         */
        @Modifying
        @Query("UPDATE Asset a SET a.currentApplicationId = NULL " +
                        "WHERE a.id = :assetId AND a.currentApplicationId = :applicationId")
        int unlockAsset(@Param("assetId") Long assetId, @Param("applicationId") Long applicationId);

        /**
         * 接力占用资产: 仅当 current_application_id 等于旧申请ID时,替换为新申请ID
         * 用于二级审批接力场景(原申请已结束,新申请接管资产占用)
         */
        @Modifying
        @Query("UPDATE Asset a SET a.currentApplicationId = :newApplicationId " +
                        "WHERE a.id = :assetId AND a.currentApplicationId = :oldApplicationId")
        int transferLock(@Param("assetId") Long assetId,
                        @Param("oldApplicationId") Long oldApplicationId,
                        @Param("newApplicationId") Long newApplicationId);
}