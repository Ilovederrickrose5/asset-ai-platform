package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.entity.PurchaseOrder;
import com.enterprise.asset.business.service.PurchaseOrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 采购订单控制器 - 处理采购订单的创建与状态管理 */
@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    /**
     * GET /api/purchase-orders - 分页获取采购订单列表
     * 
     * @param page   页码（从1开始）
     * @param size   每页数量
     * @param status 状态筛选（可选）
     * @return 订单列表及总数
     */
    @GetMapping
    public ResponseEntity<Result<Map<String, Object>>> getAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseOrder> orderPage = purchaseOrderService.getOrdersByPage(page - 1, size, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", orderPage.getContent());
        result.put("total", orderPage.getTotalElements());
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * GET /api/purchase-orders/{id} - 根据ID获取采购订单
     * 
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<PurchaseOrder>> getOrderById(@PathVariable Long id) {
        PurchaseOrder order = purchaseOrderService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
        }
        return ResponseEntity.ok(Result.success(order));
    }

    /**
     * GET /api/purchase-orders/creator/{creatorId} - 获取创建人的订单列表
     * 
     * @param creatorId 创建人ID
     * @return 订单列表
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByCreatorId(@PathVariable Long creatorId) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByCreatorId(creatorId);
        return ResponseEntity.ok(Result.success(orders));
    }

    /**
     * GET /api/purchase-orders/department/{departmentId} - 获取部门订单列表
     * 
     * @param departmentId 部门ID
     * @return 订单列表
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByDepartmentId(@PathVariable Long departmentId) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByDepartmentId(departmentId);
        return ResponseEntity.ok(Result.success(orders));
    }

    /**
     * GET /api/purchase-orders/status/{status} - 按状态获取订单列表
     * 
     * @param status 订单状态
     * @return 订单列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByStatus(@PathVariable String status) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByStatus(status);
        return ResponseEntity.ok(Result.success(orders));
    }

    /**
     * GET /api/purchase-orders/request/{purchaseRequestId} - 获取关联采购需求的订单
     * 
     * @param purchaseRequestId 采购需求ID
     * @return 订单列表
     */
    @GetMapping("/request/{purchaseRequestId}")
    public ResponseEntity<Result<List<PurchaseOrder>>> getOrdersByPurchaseRequestId(
            @PathVariable Long purchaseRequestId) {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByPurchaseRequestId(purchaseRequestId);
        return ResponseEntity.ok(Result.success(orders));
    }

    /**
     * POST /api/purchase-orders - 创建采购订单
     * 
     * @param order 采购订单实体
     * @return 创建后的订单
     */
    @PostMapping
    public ResponseEntity<Result<PurchaseOrder>> createOrder(@RequestBody PurchaseOrder order) {
        try {
            PurchaseOrder createdOrder = purchaseOrderService.createOrder(order);
            return ResponseEntity.ok(Result.success(createdOrder));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "创建采购订单失败：" + e.getMessage()));
        }
    }

    /**
     * PUT /api/purchase-orders/{id} - 更新采购订单
     * 
     * @param id    订单ID
     * @param order 更新数据
     * @return 更新后的订单
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<PurchaseOrder>> updateOrder(
            @PathVariable Long id,
            @RequestBody PurchaseOrder order) {
        try {
            PurchaseOrder updatedOrder = purchaseOrderService.updateOrder(id, order);
            if (updatedOrder == null) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success(updatedOrder));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "更新采购订单失败：" + e.getMessage()));
        }
    }

    /**
     * DELETE /api/purchase-orders/{id} - 删除采购订单
     * 
     * @param id 订单ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteOrder(@PathVariable Long id) {
        try {
            boolean deleted = purchaseOrderService.deleteOrder(id);
            if (!deleted) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success("删除成功"));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "删除采购订单失败：" + e.getMessage()));
        }
    }

    /**
     * PUT /api/purchase-orders/{id}/status - 更新订单状态
     * 
     * @param id           订单ID
     * @param statusUpdate 状态更新请求体
     * @return 更新后的订单
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Result<PurchaseOrder>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest statusUpdate) {
        try {
            PurchaseOrder order = purchaseOrderService.updateOrderStatus(id, statusUpdate.getStatus());
            if (order == null) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success(order));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "更新订单状态失败：" + e.getMessage()));
        }
    }

    /**
     * PUT /api/purchase-orders/{id}/complete - 完成订单（到货入库）
     * 
     * @param id 订单ID
     * @return 完成后的订单
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Result<PurchaseOrder>> completeOrder(@PathVariable Long id) {
        try {
            PurchaseOrder order = purchaseOrderService.completeOrder(id);
            if (order == null) {
                return ResponseEntity.ok(Result.error(404, "采购订单不存在"));
            }
            return ResponseEntity.ok(Result.success(order));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "完成订单失败：" + e.getMessage()));
        }
    }

    /** 状态更新请求体 */
    static class StatusUpdateRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}