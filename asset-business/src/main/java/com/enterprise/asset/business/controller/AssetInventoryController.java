package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.entity.AssetInventory;
import com.enterprise.asset.business.entity.AssetInventoryDetail;
import com.enterprise.asset.business.service.AssetInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 资产盘点控制器 - 处理盘点计划的创建与执行 */
@RestController
@RequestMapping("/api/asset-inventory")
public class AssetInventoryController {

    private final AssetInventoryService assetInventoryService;

    public AssetInventoryController(AssetInventoryService assetInventoryService) {
        this.assetInventoryService = assetInventoryService;
    }

    /**
     * GET /api/asset-inventory - 获取所有盘点计划
     * 
     * @return 盘点计划列表
     */
    @GetMapping
    public ResponseEntity<Result<List<AssetInventory>>> getAllPlans() {
        List<AssetInventory> plans = assetInventoryService.getAllPlans();
        return ResponseEntity.ok(Result.success(plans));
    }

    /**
     * GET /api/asset-inventory/{id} - 根据ID获取盘点计划
     * 
     * @param id 计划ID
     * @return 盘点计划详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<AssetInventory>> getPlanById(@PathVariable Long id) {
        AssetInventory plan = assetInventoryService.getPlanById(id);
        if (plan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(plan));
    }

    /**
     * GET /api/asset-inventory/user/{userId} - 获取用户相关的盘点计划（创建或分配的）
     * 
     * @param userId 用户ID
     * @return 盘点计划列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Result<List<AssetInventory>>> getPlansByCreatorOrAssignee(@PathVariable Long userId) {
        List<AssetInventory> plans = assetInventoryService.getPlansByCreatorOrAssignee(userId);
        return ResponseEntity.ok(Result.success(plans));
    }

    /**
     * GET /api/asset-inventory/status/{status} - 按状态获取盘点计划
     * 
     * @param status 状态
     * @return 盘点计划列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<AssetInventory>>> getPlansByStatus(@PathVariable String status) {
        List<AssetInventory> plans = assetInventoryService.getPlansByStatus(status);
        return ResponseEntity.ok(Result.success(plans));
    }

    /**
     * GET /api/asset-inventory/creator/{creatorId} - 获取创建人的盘点计划
     * 
     * @param creatorId 创建人ID
     * @return 盘点计划列表
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<Result<List<AssetInventory>>> getPlansByCreatorId(@PathVariable Long creatorId) {
        List<AssetInventory> plans = assetInventoryService.getPlansByCreatorId(creatorId);
        return ResponseEntity.ok(Result.success(plans));
    }

    /**
     * POST /api/asset-inventory - 创建盘点计划
     * 
     * @param plan 盘点计划实体
     * @return 创建后的计划
     */
    @PostMapping
    public ResponseEntity<Result<AssetInventory>> createPlan(@RequestBody AssetInventory plan) {
        AssetInventory createdPlan = assetInventoryService.createPlan(plan);
        return ResponseEntity.ok(Result.success(createdPlan));
    }

    /**
     * PUT /api/asset-inventory/{id} - 更新盘点计划
     * 
     * @param id   计划ID
     * @param plan 更新数据
     * @return 更新后的计划
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<AssetInventory>> updatePlan(@PathVariable Long id, @RequestBody AssetInventory plan) {
        AssetInventory updatedPlan = assetInventoryService.updatePlan(id, plan);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    /**
     * PUT /api/asset-inventory/{id}/status - 更新盘点计划状态
     * 
     * @param id     计划ID
     * @param status 新状态
     * @return 更新后的计划
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Result<AssetInventory>> updatePlanStatus(@PathVariable Long id, @RequestParam String status) {
        AssetInventory updatedPlan = assetInventoryService.updatePlanStatus(id, status);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    /**
     * DELETE /api/asset-inventory/{id} - 删除盘点计划
     * 
     * @param id 计划ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deletePlan(@PathVariable Long id) {
        boolean deleted = assetInventoryService.deletePlan(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /**
     * POST /api/asset-inventory/{id}/assign - 分配盘点任务
     * 
     * @param id      计划ID
     * @param request 分配信息（assigneeId, assigneeName, inventoryArea）
     * @return 更新后的计划
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<Result<AssetInventory>> assignTask(@PathVariable Long id,
            @RequestBody AssignTaskRequest request) {
        AssetInventory updatedPlan = assetInventoryService.assignTask(id, request.getAssigneeId(),
                request.getAssigneeName(), request.getInventoryArea());
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    /**
     * POST /api/asset-inventory/{id}/start - 开始盘点任务
     * 
     * @param id 计划ID
     * @return 更新后的计划
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Result<AssetInventory>> startTask(@PathVariable Long id) {
        AssetInventory updatedPlan = assetInventoryService.startTask(id);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    /**
     * POST /api/asset-inventory/{id}/complete - 宏成盘点任务
     * 
     * @param id 计划ID
     * @return 更新后的计划
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Result<AssetInventory>> completeTask(@PathVariable Long id) {
        AssetInventory updatedPlan = assetInventoryService.completeTask(id);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    /**
     * GET /api/asset-inventory/{id}/details - 获取盘点明细列表
     * 
     * @param id 计划ID
     * @return 盘点明细列表
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<Result<List<AssetInventoryDetail>>> getInventoryDetails(@PathVariable Long id) {
        List<AssetInventoryDetail> details = assetInventoryService.getInventoryDetails(id);
        return ResponseEntity.ok(Result.success(details));
    }

    /**
     * POST /api/asset-inventory/{id}/details - 添加盘点明细
     * 
     * @param id     计划ID
     * @param detail 盘点明细
     * @return 添加后的明细
     */
    @PostMapping("/{id}/details")
    public ResponseEntity<Result<AssetInventoryDetail>> addInventoryDetail(@PathVariable Long id,
            @RequestBody AssetInventoryDetail detail) {
        AssetInventoryDetail addedDetail = assetInventoryService.addInventoryDetail(id, detail);
        return ResponseEntity.ok(Result.success(addedDetail));
    }

    /**
     * PUT /api/asset-inventory/details/{detailId} - 更新盘点明细
     * 
     * @param detailId 明细ID
     * @param detail   更新数据
     * @return 更新后的明细
     */
    @PutMapping("/details/{detailId}")
    public ResponseEntity<Result<AssetInventoryDetail>> updateInventoryDetail(@PathVariable Long detailId,
            @RequestBody AssetInventoryDetail detail) {
        AssetInventoryDetail updatedDetail = assetInventoryService.updateInventoryDetail(detailId, detail);
        if (updatedDetail == null) {
            return ResponseEntity.ok(Result.error(404, "盘点明细不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedDetail));
    }

    /**
     * DELETE /api/asset-inventory/details/{detailId} - 删除盘点明细
     * 
     * @param detailId 明细ID
     * @return 删除结果
     */
    @DeleteMapping("/details/{detailId}")
    public ResponseEntity<Result<String>> deleteInventoryDetail(@PathVariable Long detailId) {
        boolean deleted = assetInventoryService.deleteInventoryDetail(detailId);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "盘点明细不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /** 分配任务请求体 */
    static class AssignTaskRequest {
        private Long assigneeId;
        private String assigneeName;
        private String inventoryArea;

        public Long getAssigneeId() {
            return assigneeId;
        }

        public void setAssigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
        }

        public String getAssigneeName() {
            return assigneeName;
        }

        public void setAssigneeName(String assigneeName) {
            this.assigneeName = assigneeName;
        }

        public String getInventoryArea() {
            return inventoryArea;
        }

        public void setInventoryArea(String inventoryArea) {
            this.inventoryArea = inventoryArea;
        }
    }
}