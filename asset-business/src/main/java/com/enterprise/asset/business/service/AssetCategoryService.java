/** 资产分类服务接口 - 处理资产分类CRUD操作与编码生成 */
package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.AssetCategory;

import java.util.List;

public interface AssetCategoryService {
    
    /** 获取所有分类列表 */
    List<AssetCategory> getAllCategories();
    
    /** 获取活跃分类列表 */
    List<AssetCategory> getActiveCategories();
    
    /** 根据ID获取分类 */
    AssetCategory getCategoryById(Long id);
    
    /** 根据编码获取分类 */
    AssetCategory getCategoryByCode(String categoryCode);
    
    /** 创建分类 */
    AssetCategory createCategory(AssetCategory category);
    
    /** 更新分类信息 */
    AssetCategory updateCategory(Long id, AssetCategory category);
    
    /** 删除分类 */
    boolean deleteCategory(Long id);
    
    /** 获取活跃分类数量 */
    long getActiveCategoryCount();
    
    /** 生成分类编码（支持父子分类） */
    String generateCategoryCode(Long parentId);
}