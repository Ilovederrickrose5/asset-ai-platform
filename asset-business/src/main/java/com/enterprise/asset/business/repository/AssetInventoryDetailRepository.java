package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.AssetInventoryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 资产盘点明细数据访问接口
 * 关联表: asset_inventory_detail(资产盘点明细表)
 * 主要操作: 盘点明细的增删改查、按盘点计划查询
 */
public interface AssetInventoryDetailRepository extends JpaRepository<AssetInventoryDetail, Long> {

    /** 根据盘点计划ID查询明细列表 */
    List<AssetInventoryDetail> findByInventoryId(Long inventoryId);

    /** 根据资产ID查询盘点记录 */
    List<AssetInventoryDetail> findByAssetId(Long assetId);

    /** 根据状态查询盘点明细 */
    List<AssetInventoryDetail> findByStatus(String status);

    /** 根据盘点计划ID删除明细 */
    void deleteByInventoryId(Long inventoryId);
}