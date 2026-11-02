/** 供应商服务接口 - 处理供应商CRUD操作 */
package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.Supplier;
import java.util.List;

public interface SupplierService {
    /** 获取所有供应商列表 */
    List<Supplier> getAllSuppliers();
    /** 根据ID获取供应商 */
    Supplier getSupplierById(Long id);
    /** 创建供应商 */
    Supplier createSupplier(Supplier supplier);
    /** 更新供应商信息 */
    Supplier updateSupplier(Long id, Supplier supplier);
    /** 删除供应商 */
    void deleteSupplier(Long id);
}