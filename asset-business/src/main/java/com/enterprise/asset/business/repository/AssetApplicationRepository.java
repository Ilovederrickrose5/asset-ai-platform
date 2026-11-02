package com.enterprise.asset.business.repository;

import com.enterprise.asset.business.entity.AssetApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 资产业务申请数据访问接口
 * 关联表: asset_application(资产业务申请表)
 * 主要操作: 申请的增删改查、状态统计、分页查询、类型筛选
 */
// 继承JpaRepository，提供CRUD操作
public interface AssetApplicationRepository extends JpaRepository<AssetApplication, Long> {

       /** 根据申请人ID查询申请列表 */
       List<AssetApplication> findByApplicantId(Long applicantId);

       /** 根据状态查询申请列表 */
       List<AssetApplication> findByStatus(String status);

       /** 根据状态查询并按申请日期倒序 */
       List<AssetApplication> findByStatusOrderByApplicationDateDesc(String status);

       /** 根据状态统计申请数量 */
       long countByStatus(String status);

       /** 根据部门ID查询申请列表 */
       List<AssetApplication> findByDepartmentId(Long departmentId);

       /** 根据状态和部门ID查询申请 */
       List<AssetApplication> findByStatusAndDepartmentId(String status, Long departmentId);

       /** 分页查询所有申请 */
       Page<AssetApplication> findAll(Pageable pageable);

       /** 按申请人ID分页查询 */
       Page<AssetApplication> findByApplicantId(Long applicantId, Pageable pageable);

       /** 按部门ID分页查询 */
       Page<AssetApplication> findByDepartmentId(Long departmentId, Pageable pageable);

       /** 按类型和状态分页查询所有申请 */
       @Query("SELECT a FROM AssetApplication a WHERE " +
                     "(:type IS NULL OR :type = '' OR a.applicationType = :type) AND " +
                     "(:status IS NULL OR :status = '' OR a.status = :status)")
       Page<AssetApplication> findByTypeAndStatus(@Param("type") String type, @Param("status") String status,
                     Pageable pageable);

       /** 按申请人ID、类型和状态分页查询 */
       @Query("SELECT a FROM AssetApplication a WHERE a.applicantId = :applicantId AND " +
                     "(:type IS NULL OR :type = '' OR a.applicationType = :type) AND " +
                     "(:status IS NULL OR :status = '' OR a.status = :status)")
       Page<AssetApplication> findByApplicantIdAndTypeAndStatus(
                     @Param("applicantId") Long applicantId,
                     @Param("type") String type,
                     @Param("status") String status,
                     Pageable pageable);

       /** 按部门ID、类型和状态分页查询 */
       @Query("SELECT a FROM AssetApplication a WHERE a.departmentId = :departmentId AND " +
                     "(:type IS NULL OR :type = '' OR a.applicationType = :type) AND " +
                     "(:status IS NULL OR :status = '' OR a.status = :status)")
       Page<AssetApplication> findByDepartmentIdAndTypeAndStatus(
                     @Param("departmentId") Long departmentId,
                     @Param("type") String type,
                     @Param("status") String status,
                     Pageable pageable);

       /** 按申请人ID查询并按更新时间倒序 */
       @Query("SELECT a FROM AssetApplication a WHERE a.applicantId = :applicantId ORDER BY a.updateTime DESC")
       List<AssetApplication> findByApplicantIdOrderByUpdateTimeDesc(@Param("applicantId") Long applicantId);
}