package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.entity.DepreciationRecord;
import com.enterprise.asset.business.service.DepreciationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 折旧计算控制器 - 处理资产折旧计算与记录管理
 * 职责：接收前端请求 → 转发给业务层 → 返回响应
 */
@RestController
@RequestMapping("/api/depreciation")
public class DepreciationController {

    private final DepreciationService depreciationService;

    public DepreciationController(DepreciationService depreciationService) {
        this.depreciationService = depreciationService;
    }

    /**
     * POST /api/depreciation/calculate/{assetId} - 计算单个资产折旧
     * 流程：接收前端参数 → 调用Service → 返回结果
     * 参数：assetId(路径), startDate, endDate, depreciationMethod(可选),
     * actualWorkUnits(工作量法用)
     */
    @PostMapping("/calculate/{assetId}")
    public ResponseEntity<Result<DepreciationRecord>> calculateAssetDepreciation(
            @PathVariable Long assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String depreciationMethod,
            @RequestParam(required = false) Integer actualWorkUnits) {
        try {
            DepreciationRecord record = depreciationService.calculateAssetDepreciation(assetId, startDate, endDate,
                    depreciationMethod, actualWorkUnits);
            return ResponseEntity.ok(Result.success(record));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * POST /api/depreciation/calculate/batch - 批量计算资产折旧
     * 
     * @param assetIds           资产ID列表
     * @param depreciationMonth  折旧月份
     * @param depreciationMethod 折旧方法（可选）
     * @return 折旧记录列表
     */
    @PostMapping("/calculate/batch")
    public ResponseEntity<Result<List<DepreciationRecord>>> calculateBatchDepreciation(
            @RequestBody List<Long> assetIds,
            @RequestParam String depreciationMonth,
            @RequestParam(required = false) String depreciationMethod) {
        try {
            List<DepreciationRecord> records = depreciationService.calculateBatchDepreciation(assetIds,
                    depreciationMonth, depreciationMethod);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * POST /api/depreciation/calculate/all - 计算所有资产折旧
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 折旧记录列表
     */
    @PostMapping("/calculate/all")
    public ResponseEntity<Result<List<DepreciationRecord>>> calculateAllAssetsDepreciation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationRecord> records = depreciationService.calculateAllAssetsDepreciation(startDate, endDate);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/records/{recordId} - 获取折旧记录详情
     * 
     * @param recordId 记录ID
     * @return 折旧记录
     */
    @GetMapping("/records/{recordId}")
    public ResponseEntity<Result<DepreciationRecord>> getDepreciationRecordById(@PathVariable Long recordId) {
        try {
            DepreciationRecord record = depreciationService.getDepreciationRecordById(recordId);
            return ResponseEntity.ok(Result.success(record));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/records/asset/{assetId} - 获取资产的所有折旧记录
     * 
     * @param assetId 资产ID
     * @return 折旧记录列表
     */
    @GetMapping("/records/asset/{assetId}")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByAssetId(
            @PathVariable Long assetId) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByAssetId(assetId);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/records/asset/{assetId}/latest - 获取资产最新折旧记录
     * 
     * @param assetId 资产ID
     * @return 最新折旧记录
     */
    @GetMapping("/records/asset/{assetId}/latest")
    public ResponseEntity<Result<DepreciationRecord>> getLatestDepreciationRecord(@PathVariable Long assetId) {
        try {
            DepreciationRecord record = depreciationService.getLatestDepreciationRecord(assetId);
            return ResponseEntity.ok(Result.success(record));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/records/date-range - 按日期范围查询折旧记录
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 折旧记录列表
     */
    @GetMapping("/records/date-range")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByDateRange(startDate,
                    endDate);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/records/category/{categoryId} - 按类别查询折旧记录
     * 
     * @param categoryId 类别ID
     * @return 折旧记录列表
     */
    @GetMapping("/records/category/{categoryId}")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByCategory(
            @PathVariable Long categoryId) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByCategory(categoryId);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/records/department/{departmentId} - 按部门查询折旧记录
     * 
     * @param departmentId 部门ID
     * @return 折旧记录列表
     */
    @GetMapping("/records/department/{departmentId}")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByDepartment(
            @PathVariable Long departmentId) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByDepartment(departmentId);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * DELETE /api/depreciation/records/{recordId}/rollback - 回滚折旧记录
     * 
     * @param recordId 记录ID
     * @return 回滚结果
     */
    @DeleteMapping("/records/{recordId}/rollback")
    public ResponseEntity<Result<String>> rollbackDepreciationRecord(@PathVariable Long recordId) {
        try {
            depreciationService.rollbackDepreciationRecord(recordId);
            return ResponseEntity.ok(Result.success("回滚成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
}