package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 资产分类数据访问接口
 * 关联表: asset_category(资产分类表)
 * 主要操作: 分类的增删改查、树形结构查询
 */
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    /** 根据状态查询分类列表 */
    List<AssetCategory> findByStatus(int status);

    /** 根据状态统计分类数量 */
    long countByStatus(int status);

    /** 根据分类编码查询分类 */
    Optional<AssetCategory> findByCategoryCode(String categoryCode);

    /** 判断分类编码是否存在 */
    boolean existsByCategoryCode(String categoryCode);

    /** 根据父分类ID查询子分类列表(树形结构) */
    List<AssetCategory> findByParentId(Long parentId);
}