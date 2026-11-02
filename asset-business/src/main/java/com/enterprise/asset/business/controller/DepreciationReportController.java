package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.common.dto.AssetNetValueDTO;
import com.enterprise.asset.common.dto.DepreciationReportDTO;
import com.enterprise.asset.common.dto.DepreciationSummaryDTO;
import com.enterprise.asset.business.service.DepreciationReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 折旧报表控制器 - 处理折旧报表生成与导出 */
@RestController
@RequestMapping("/api/depreciation/reports")
public class DepreciationReportController {

    private final DepreciationReportService depreciationReportService;

    public DepreciationReportController(DepreciationReportService depreciationReportService) {
        this.depreciationReportService = depreciationReportService;
    }

    /**
     * GET /api/depreciation/reports/detail - 生成折旧明细报表
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 折旧明细列表
     */
    @GetMapping("/detail")
    public ResponseEntity<Result<List<DepreciationReportDTO>>> generateDepreciationDetailReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationReportDTO> report = depreciationReportService.generateDepreciationDetailReport(startDate,
                    endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/detail/category/{categoryId} - 按资产类别生成折旧明细报表
     * 
     * @param categoryId 资产类别ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 折旧明细列表
     */
    @GetMapping("/detail/category/{categoryId}")
    public ResponseEntity<Result<List<DepreciationReportDTO>>> generateDepreciationDetailReportByCategory(
            @PathVariable Long categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationReportDTO> report = depreciationReportService
                    .generateDepreciationDetailReportByCategory(categoryId, startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/detail/department/{departmentId} - 按部门生成折旧明细报表
     * 
     * @param departmentId 部门ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 折旧明细列表
     */
    @GetMapping("/detail/department/{departmentId}")
    public ResponseEntity<Result<List<DepreciationReportDTO>>> generateDepreciationDetailReportByDepartment(
            @PathVariable Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationReportDTO> report = depreciationReportService
                    .generateDepreciationDetailReportByDepartment(departmentId, startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/summary - 生成折旧汇总报表
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 折旧汇总列表
     */
    @GetMapping("/summary")
    public ResponseEntity<Result<List<DepreciationSummaryDTO>>> generateDepreciationSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationSummaryDTO> report = depreciationReportService.generateDepreciationSummaryReport(startDate,
                    endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/summary/category - 按类别生成折旧汇总报表
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 折旧汇总列表
     */
    @GetMapping("/summary/category")
    public ResponseEntity<Result<List<DepreciationSummaryDTO>>> generateDepreciationSummaryReportByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationSummaryDTO> report = depreciationReportService
                    .generateDepreciationSummaryReportByCategory(startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/summary/department - 按部门生成折旧汇总报表
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 折旧汇总列表
     */
    @GetMapping("/summary/department")
    public ResponseEntity<Result<List<DepreciationSummaryDTO>>> generateDepreciationSummaryReportByDepartment(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationSummaryDTO> report = depreciationReportService
                    .generateDepreciationSummaryReportByDepartment(startDate, endDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/net-value - 生成资产净值报表
     * 
     * @param asOfDate 截至日期
     * @return 资产净值列表
     */
    @GetMapping("/net-value")
    public ResponseEntity<Result<List<AssetNetValueDTO>>> generateAssetNetValueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        try {
            List<AssetNetValueDTO> report = depreciationReportService.generateAssetNetValueReport(asOfDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/net-value/category/{categoryId} - 按类别生成资产净值报表
     * 
     * @param categoryId 类别ID
     * @param asOfDate   截至日期
     * @return 资产净值列表
     */
    @GetMapping("/net-value/category/{categoryId}")
    public ResponseEntity<Result<List<AssetNetValueDTO>>> generateAssetNetValueReportByCategory(
            @PathVariable Long categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        try {
            List<AssetNetValueDTO> report = depreciationReportService.generateAssetNetValueReportByCategory(categoryId,
                    asOfDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/net-value/department/{departmentId} -
     * 按部门生成资产净值报表
     * 
     * @param departmentId 部门ID
     * @param asOfDate     截至日期
     * @return 资产净值列表
     */
    @GetMapping("/net-value/department/{departmentId}")
    public ResponseEntity<Result<List<AssetNetValueDTO>>> generateAssetNetValueReportByDepartment(
            @PathVariable Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        try {
            List<AssetNetValueDTO> report = depreciationReportService
                    .generateAssetNetValueReportByDepartment(departmentId, asOfDate);
            return ResponseEntity.ok(Result.success(report));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    /**
     * GET /api/depreciation/reports/export/excel - 导出折旧报表为Excel
     * 
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param reportType 报表类型
     * @return Excel文件字节流
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reportType) {
        try {
            List<DepreciationReportDTO> data = depreciationReportService.generateDepreciationDetailReport(startDate,
                    endDate);
            byte[] excelBytes = depreciationReportService.exportToExcel(data, reportType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", reportType + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()).toString().getBytes());
        }
    }

    /**
     * GET /api/depreciation/reports/export/pdf - 导出折旧报表为PDF
     * 
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param reportType 报表类型
     * @return PDF文件字节流
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reportType) {
        try {
            List<DepreciationReportDTO> data = depreciationReportService.generateDepreciationDetailReport(startDate,
                    endDate);
            byte[] pdfBytes = depreciationReportService.exportToPDF(data, reportType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", reportType + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()).toString().getBytes());
        }
    }
}