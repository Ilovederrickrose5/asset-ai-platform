package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.business.entity.Supplier;
import com.enterprise.asset.business.repository.SupplierRepository;
import com.enterprise.asset.business.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** 供应商服务实现 - 处理供应商CRUD操作 */
@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public Supplier getSupplierById(Long id) {
        Optional<Supplier> supplier = supplierRepository.findById(id);
        return supplier.orElse(null);
    }

    @Override
    public Supplier createSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    /**
     * 更新供应商信息
     * 业务流程：查询供应商→更新字段（名称、联系人、电话、地址、状态）→保存
     */
    @Override
    public Supplier updateSupplier(Long id, Supplier supplier) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);
        if (existingSupplier.isPresent()) {
            Supplier updatedSupplier = existingSupplier.get();
            updatedSupplier.setSupplierName(supplier.getSupplierName());
            updatedSupplier.setContactPerson(supplier.getContactPerson());
            updatedSupplier.setPhone(supplier.getPhone());
            updatedSupplier.setAddress(supplier.getAddress());
            updatedSupplier.setStatus(supplier.getStatus());
            return supplierRepository.save(updatedSupplier);
        }
        return null;
    }

    @Override
    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }
}