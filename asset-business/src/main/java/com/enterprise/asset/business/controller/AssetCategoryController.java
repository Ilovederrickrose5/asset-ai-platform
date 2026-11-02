package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.entity.AssetCategory;
import com.enterprise.asset.business.service.AssetCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 资产分类控制器 - 处理资产分类CRUD操作 */
@RestController
@RequestMapping("/api/asset-categories")
public class AssetCategoryController {

    private final AssetCategoryService assetCategoryService;

    public AssetCategoryController(AssetCategoryService assetCategoryService) {
        this.assetCategoryService = assetCategoryService;
    }

    /**
     * GET /api/asset-categories - 获取所有分类列表
     * 
     * @return 分类列表
     */
    @GetMapping
    public ResponseEntity<Result<List<AssetCategory>>> getAllCategories() {
        List<AssetCategory> categories = assetCategoryService.getAllCategories();
        return ResponseEntity.ok(Result.success(categories));
    }

    /**
     * GET /api/asset-categories/active - 获取活跃分类列表
     * 
     * @return 活跃分类列表
     */
    @GetMapping("/active")
    public ResponseEntity<Result<List<AssetCategory>>> getActiveCategories() {
        List<AssetCategory> categories = assetCategoryService.getActiveCategories();
        return ResponseEntity.ok(Result.success(categories));
    }

    /**
     * GET /api/asset-categories/{id} - 根据ID获取分类
     * 
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<AssetCategory>> getCategoryById(@PathVariable Long id) {
        AssetCategory category = assetCategoryService.getCategoryById(id);
        if (category == null) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success(category));
    }

    /**
     * GET /api/asset-categories/code/{categoryCode} - 根据编码获取分类
     * 
     * @param categoryCode 分类编码
     * @return 分类详情
     */
    @GetMapping("/code/{categoryCode}")
    public ResponseEntity<Result<AssetCategory>> getCategoryByCode(@PathVariable String categoryCode) {
        AssetCategory category = assetCategoryService.getCategoryByCode(categoryCode);
        if (category == null) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success(category));
    }

    /**
     * POST /api/asset-categories - 创建新分类
     * 
     * @param category 分类实体
     * @return 创建后的分类
     */
    @PostMapping
    public ResponseEntity<Result<AssetCategory>> createCategory(@RequestBody AssetCategory category) {
        AssetCategory createdCategory = assetCategoryService.createCategory(category);
        return ResponseEntity.ok(Result.success(createdCategory));
    }

    /**
     * PUT /api/asset-categories/{id} - 更新分类信息
     * 
     * @param id       分类ID
     * @param category 更新数据
     * @return 更新后的分类
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<AssetCategory>> updateCategory(
            @PathVariable Long id,
            @RequestBody AssetCategory category) {
        AssetCategory updatedCategory = assetCategoryService.updateCategory(id, category);
        if (updatedCategory == null) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedCategory));
    }

    /**
     * DELETE /api/asset-categories/{id} - 删除分类
     * 
     * @param id 分类ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteCategory(@PathVariable Long id) {
        boolean deleted = assetCategoryService.deleteCategory(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /**
     * GET /api/asset-categories/count - 获取活跃分类数量
     * 
     * @return 分类数量
     */
    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getActiveCategoryCount() {
        long count = assetCategoryService.getActiveCategoryCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/asset-categories/generate-code - 生成分类编码
     * 
     * @param parentId 父分类ID（可选）
     * @return 分类编码
     */
    @GetMapping("/generate-code")
    public ResponseEntity<Result<String>> generateCategoryCode(@RequestParam("parentId") Long parentId) {
        String categoryCode = assetCategoryService.generateCategoryCode(parentId);
        return ResponseEntity.ok(Result.success(categoryCode));
    }
}