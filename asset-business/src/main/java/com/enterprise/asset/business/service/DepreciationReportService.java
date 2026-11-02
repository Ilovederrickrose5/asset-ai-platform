/** 折旧报表服务接口 - 处理折旧报表生成与导出 */
package com.enterprise.asset.business.service;

import com.enterprise.asset.common.dto.AssetNetValueDTO;
import com.enterprise.asset.common.dto.DepreciationReportDTO;
import com.enterprise.asset.common.dto.DepreciationSummaryDTO;

import java.time.LocalDate;
import java.util.List;

public interface DepreciationReportService {
    
    /** 生成折旧明细报表 */
    List<DepreciationReportDTO> generateDepreciationDetailReport(LocalDate startDate, LocalDate endDate);
    
    /** 按分类生成折旧明细报表 */
    List<DepreciationReportDTO> generateDepreciationDetailReportByCategory(Long categoryId, LocalDate startDate, LocalDate endDate);
    
    /** 按部门生成折旧明细报表 */
    List<DepreciationReportDTO> generateDepreciationDetailReportByDepartment(Long departmentId, LocalDate startDate, LocalDate endDate);
    
    /** 生成折旧汇总报表 */
    List<DepreciationSummaryDTO> generateDepreciationSummaryReport(LocalDate startDate, LocalDate endDate);
    
    /** 按分类生成折旧汇总报表 */
    List<DepreciationSummaryDTO> generateDepreciationSummaryReportByCategory(LocalDate startDate, LocalDate endDate);
    
    /** 按部门生成折旧汇总报表 */
    List<DepreciationSummaryDTO> generateDepreciationSummaryReportByDepartment(LocalDate startDate, LocalDate endDate);
    
    /** 生成资产净值报表 */
    List<AssetNetValueDTO> generateAssetNetValueReport(LocalDate asOfDate);
    
    /** 按分类生成资产净值报表 */
    List<AssetNetValueDTO> generateAssetNetValueReportByCategory(Long categoryId, LocalDate asOfDate);
    
    /** 按部门生成资产净值报表 */
    List<AssetNetValueDTO> generateAssetNetValueReportByDepartment(Long departmentId, LocalDate asOfDate);
    
    /** 导出报表为Excel */
    byte[] exportToExcel(List<DepreciationReportDTO> data, String reportType) throws Exception;
    
    /** 导出报表为PDF */
    byte[] exportToPDF(List<DepreciationReportDTO> data, String reportType) throws Exception;
}