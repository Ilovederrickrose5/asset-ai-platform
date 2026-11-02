/** 采购订单服务接口 - 处理采购订单CRUD与状态管理 */
package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.PurchaseOrder;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PurchaseOrderService {
    
    /** 获取所有采购订单 */
    List<PurchaseOrder> getAllOrders();
    
    /** 分页查询采购订单 */
    Page<PurchaseOrder> getOrdersByPage(int page, int size, String status);
    
    /** 根据ID获取采购订单 */
    PurchaseOrder getOrderById(Long id);
    
    /** 根据创建人ID获取采购订单 */
    List<PurchaseOrder> getOrdersByCreatorId(Long creatorId);
    
    /** 根据部门ID获取采购订单 */
    List<PurchaseOrder> getOrdersByDepartmentId(Long departmentId);
    
    /** 根据状态获取采购订单 */
    List<PurchaseOrder> getOrdersByStatus(String status);
    
    /** 根据采购需求ID获取关联订单 */
    List<PurchaseOrder> getOrdersByPurchaseRequestId(Long purchaseRequestId);
    
    /** 创建采购订单 */
    PurchaseOrder createOrder(PurchaseOrder order);
    
    /** 更新采购订单 */
    PurchaseOrder updateOrder(Long id, PurchaseOrder order);
    
    /** 删除采购订单 */
    boolean deleteOrder(Long id);
    
    /** 更新订单状态 */
    PurchaseOrder updateOrderStatus(Long id, String status);
    
    /** 完成订单（到货入库） */
    PurchaseOrder completeOrder(Long id);
}