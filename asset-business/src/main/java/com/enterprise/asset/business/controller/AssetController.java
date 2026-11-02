package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.service.AssetService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<Asset>>> getAllAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean onlyMine) {
        Page<Asset> assets = assetService.getAssetsWithPagination(
                page, size, status, sortBy, sortDir, keyword, onlyMine);
        return ResponseEntity.ok(Result.success(assets));
    }

    /** 不分页的全量资产列表（按当前用户权限范围），用于下拉选项和统计 */
    @GetMapping("/all")
    public ResponseEntity<Result<List<Asset>>> getAssetList() {
        return ResponseEntity.ok(Result.success(assetService.getAllAssets()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Asset>> getAssetById(@PathVariable Long id) {
        Asset asset = assetService.getAssetById(id);
        if (asset == null) {
            return ResponseEntity.ok(Result.error(404, "资产不存在"));
        }
        return ResponseEntity.ok(Result.success(asset));
    }

    @PostMapping
    public ResponseEntity<Result<Asset>> createAsset(@RequestBody Asset asset) {
        Asset createdAsset = assetService.createAsset(asset);
        return ResponseEntity.ok(Result.success(createdAsset));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Asset>> updateAsset(
            @PathVariable Long id,
            @RequestBody Asset asset) {
        Asset updatedAsset = assetService.updateAsset(id, asset);
        if (updatedAsset == null) {
            return ResponseEntity.ok(Result.error(404, "资产不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedAsset));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteAsset(@PathVariable Long id) {
        boolean deleted = assetService.deleteAsset(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "资产不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Result<Asset>> updateAssetStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest statusUpdate) {
        try {
            Asset asset = assetService.updateAssetStatus(id, statusUpdate.getStatus());
            if (asset == null) {
                return ResponseEntity.ok(Result.error(404, "资产不存在"));
            }
            return ResponseEntity.ok(Result.success(asset));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "操作失败，请重试"));
        }
    }

    static class StatusUpdateRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @PutMapping("/{id}/use-status")
    public ResponseEntity<Result<Asset>> updateAssetUseStatus(
            @PathVariable Long id,
            @RequestBody UseStatusUpdateRequest useStatusUpdate) {
        try {
            Asset asset = assetService.updateAssetUseStatus(id, useStatusUpdate.getUseStatus());
            if (asset == null) {
                return ResponseEntity.ok(Result.error(404, "资产不存在"));
            }
            return ResponseEntity.ok(Result.success(asset));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        }
    }

    static class UseStatusUpdateRequest {
        private String useStatus;

        public String getUseStatus() {
            return useStatus;
        }

        public void setUseStatus(String useStatus) {
            this.useStatus = useStatus;
        }
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByDepartment(@PathVariable Long departmentId) {
        List<Asset> assets = assetService.getAssetsByDepartment(departmentId);
        return ResponseEntity.ok(Result.success(assets));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByUser(@PathVariable Long userId) {
        List<Asset> assets = assetService.getAssetsByUser(userId);
        return ResponseEntity.ok(Result.success(assets));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByStatus(@PathVariable String status) {
        List<Asset> assets = assetService.getAssetsByStatus(status);
        return ResponseEntity.ok(Result.success(assets));
    }

    @GetMapping("/use-status/{useStatus}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByUseStatus(@PathVariable String useStatus) {
        List<Asset> assets = assetService.getAssetsByUseStatus(useStatus);
        return ResponseEntity.ok(Result.success(assets));
    }

    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getAssetCount() {
        long count = assetService.getAssetCount();
        return ResponseEntity.ok(Result.success(count));
    }

    @GetMapping("/count/department/{departmentId}")
    public ResponseEntity<Result<Long>> getAssetCountByDepartment(@PathVariable Long departmentId) {
        long count = assetService.getAssetCountByDepartment(departmentId);
        return ResponseEntity.ok(Result.success(count));
    }

    @GetMapping("/count/user/{userId}")
    public ResponseEntity<Result<Long>> getAssetCountByUser(@PathVariable Long userId) {
        long count = assetService.getAssetCountByUser(userId);
        return ResponseEntity.ok(Result.success(count));
    }

    @GetMapping("/locations")
    public ResponseEntity<Result<List<String>>> getAllLocations() {
        List<String> locations = assetService.getAllLocations();
        return ResponseEntity.ok(Result.success(locations));
    }

    @GetMapping("/available")
    public ResponseEntity<Result<List<Asset>>> getAvailableAssets() {
        List<Asset> assets = assetService.getAvailableAssets();
        return ResponseEntity.ok(Result.success(assets));
    }
}