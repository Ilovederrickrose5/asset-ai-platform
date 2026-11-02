package com.enterprise.asset.business.controller;

import com.enterprise.asset.business.entity.Supplier;
import com.enterprise.asset.business.service.SupplierService;
import com.enterprise.asset.common.util.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 供应商控制器 - 处理供应商CRUD操作 */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * GET /api/suppliers - 获取所有供应商列表
     * 
     * @return 供应商列表
     */
    @GetMapping
    public ResponseEntity<Result<List<Supplier>>> getAllSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(Result.success(suppliers));
    }

    /**
     * GET /api/suppliers/{id} - 根据ID获取供应商
     * 
     * @param id 供应商ID
     * @return 供应商详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<Supplier>> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return supplier != null ? ResponseEntity.ok(Result.success(supplier))
                : ResponseEntity.ok(Result.error(404, "供应商不存在"));
    }

    /**
     * POST /api/suppliers - 创建新供应商
     * 
     * @param supplier 供应商实体
     * @return 创建后的供应商
     */
    @PostMapping
    public ResponseEntity<Result<Supplier>> createSupplier(@RequestBody Supplier supplier) {
        Supplier createdSupplier = supplierService.createSupplier(supplier);
        return ResponseEntity.ok(Result.success(createdSupplier));
    }

    /**
     * PUT /api/suppliers/{id} - 更新供应商信息
     * 
     * @param id       供应商ID
     * @param supplier 更新数据
     * @return 更新后的供应商
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<Supplier>> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        Supplier updatedSupplier = supplierService.updateSupplier(id, supplier);
        return updatedSupplier != null ? ResponseEntity.ok(Result.success(updatedSupplier))
                : ResponseEntity.ok(Result.error(404, "供应商不存在"));
    }

    /**
     * DELETE /api/suppliers/{id} - 删除供应商
     * 
     * @param id 供应商ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(Result.success("删除成功"));
    }
}