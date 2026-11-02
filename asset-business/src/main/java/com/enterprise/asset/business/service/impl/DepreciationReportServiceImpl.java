package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.common.dto.AssetNetValueDTO;
import com.enterprise.asset.common.dto.DepreciationReportDTO;
import com.enterprise.asset.common.dto.DepreciationSummaryDTO;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.AssetCategory;
import com.enterprise.asset.business.entity.Department;
import com.enterprise.asset.business.entity.DepreciationRecord;
import com.enterprise.asset.business.repository.AssetCategoryRepository;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.DepartmentRepository;
import com.enterprise.asset.business.repository.DepreciationRecordRepository;
import com.enterprise.asset.business.service.DepreciationReportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import com.itextpdf.kernel.colors.ColorConstants;
// import com.itextpdf.kernel.pdf.PdfDocument;
// import com.itextpdf.kernel.pdf.PdfWriter;
// import com.itextpdf.layout.Document;
// import com.itextpdf.layout.element.Cell;
// import com.itextpdf.layout.element.Paragraph;
// import com.itextpdf.layout.element.Table;
// import com.itextpdf.layout.property.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 折旧报表服务实现 - 处理折旧报表生成与导出（支持按分类/部门统计、Excel/PDF导出） */
@Service
public class DepreciationReportServiceImpl implements DepreciationReportService {

    private static final Logger logger = LoggerFactory.getLogger(DepreciationReportServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int DECIMAL_PLACES = 2;

    private final DepreciationRecordRepository depreciationRecordRepository;
    private final AssetRepository assetRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final DepartmentRepository departmentRepository;

    public DepreciationReportServiceImpl(DepreciationRecordRepository depreciationRecordRepository,
            AssetRepository assetRepository, AssetCategoryRepository assetCategoryRepository,
            DepartmentRepository departmentRepository) {
        this.depreciationRecordRepository = depreciationRecordRepository;
        this.assetRepository = assetRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepreciationReportDTO> generateDepreciationDetailReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<DepreciationRecord> records = depreciationRecordRepository.findByCreateTimeBetween(startDateTime,
                endDateTime);
        return convertToDepreciationReportDTOs(records);
    }

    @Override
    public List<DepreciationReportDTO> generateDepreciationDetailReportByCategory(Long categoryId, LocalDate startDate,
            LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<DepreciationRecord> records = depreciationRecordRepository.findByCreateTimeBetween(startDateTime,
                endDateTime);
        return records.stream()
                .filter(record -> record.getCategoryId() != null && record.getCategoryId().equals(categoryId))
                .map(this::convertToDepreciationReportDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DepreciationReportDTO> generateDepreciationDetailReportByDepartment(Long departmentId,
            LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<DepreciationRecord> records = depreciationRecordRepository.findByCreateTimeBetween(startDateTime,
                endDateTime);
        return records.stream()
                .filter(record -> record.getDepartmentId() != null && record.getDepartmentId().equals(departmentId))
                .map(this::convertToDepreciationReportDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DepreciationSummaryDTO> generateDepreciationSummaryReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<DepreciationRecord> records = depreciationRecordRepository.findByCreateTimeBetween(startDateTime,
                endDateTime);
        Map<Long, List<DepreciationRecord>> groupedByAsset = records.stream()
                .collect(Collectors.groupingBy(DepreciationRecord::getAssetId));

        List<DepreciationSummaryDTO> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<DepreciationRecord>> entry : groupedByAsset.entrySet()) {
            List<DepreciationRecord> assetRecords = entry.getValue();
            DepreciationRecord latestRecord = assetRecords.stream()
                    .max(Comparator.comparing(DepreciationRecord::getCreateTime))
                    .orElse(assetRecords.get(0));

            DepreciationSummaryDTO summary = new DepreciationSummaryDTO();
            summary.setCategoryId(latestRecord.getCategoryId());
            summary.setDepartmentId(latestRecord.getDepartmentId());
            summary.setAssetCount(1);
            summary.setTotalOriginalValue(latestRecord.getOriginalValue());
            summary.setTotalNetValue(latestRecord.getCurrentNetValue());
            summary.setTotalDepreciation(latestRecord.getDepreciationAmount());
            summary.setTotalAccumulatedDepreciation(latestRecord.getAccumulatedDepreciation());

            BigDecimal depreciationRate = latestRecord.getAccumulatedDepreciation()
                    .divide(latestRecord.getOriginalValue(), DECIMAL_PLACES, ROUNDING_MODE)
                    .multiply(new BigDecimal(100));
            summary.setDepreciationRate(depreciationRate);

            summaries.add(summary);
        }

        return summaries;
    }

    @Override
    public List<DepreciationSummaryDTO> generateDepreciationSummaryReportByCategory(LocalDate startDate,
            LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<DepreciationRecord> records = depreciationRecordRepository.findByCreateTimeBetween(startDateTime,
                endDateTime);
        Map<Long, List<DepreciationRecord>> groupedByCategory = records.stream()
                .filter(record -> record.getCategoryId() != null)
                .collect(Collectors.groupingBy(DepreciationRecord::getCategoryId));

        List<DepreciationSummaryDTO> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<DepreciationRecord>> entry : groupedByCategory.entrySet()) {
            Long categoryId = entry.getKey();
            List<DepreciationRecord> categoryRecords = entry.getValue();

            AssetCategory category = assetCategoryRepository.findById(categoryId).orElse(null);
            String categoryName = category != null ? category.getCategoryName() : "未知分类";

            DepreciationSummaryDTO summary = new DepreciationSummaryDTO();
            summary.setCategoryId(categoryId);
            summary.setCategoryName(categoryName);
            summary.setAssetCount(
                    (int) categoryRecords.stream().map(DepreciationRecord::getAssetId).distinct().count());
            summary.setTotalOriginalValue(categoryRecords.stream()
                    .map(DepreciationRecord::getOriginalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalNetValue(categoryRecords.stream()
                    .map(DepreciationRecord::getCurrentNetValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalDepreciation(categoryRecords.stream()
                    .map(DepreciationRecord::getDepreciationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalAccumulatedDepreciation(categoryRecords.stream()
                    .map(DepreciationRecord::getAccumulatedDepreciation)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            BigDecimal totalOriginal = summary.getTotalOriginalValue();
            if (totalOriginal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal depreciationRate = summary.getTotalAccumulatedDepreciation()
                        .divide(totalOriginal, DECIMAL_PLACES, ROUNDING_MODE)
                        .multiply(new BigDecimal(100));
                summary.setDepreciationRate(depreciationRate);
            } else {
                summary.setDepreciationRate(BigDecimal.ZERO);
            }

            summaries.add(summary);
        }

        return summaries;
    }

    @Override
    public List<DepreciationSummaryDTO> generateDepreciationSummaryReportByDepartment(LocalDate startDate,
            LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<DepreciationRecord> records = depreciationRecordRepository.findByCreateTimeBetween(startDateTime,
                endDateTime);
        Map<Long, List<DepreciationRecord>> groupedByDepartment = records.stream()
                .filter(record -> record.getDepartmentId() != null)
                .collect(Collectors.groupingBy(DepreciationRecord::getDepartmentId));

        List<DepreciationSummaryDTO> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<DepreciationRecord>> entry : groupedByDepartment.entrySet()) {
            Long departmentId = entry.getKey();
            List<DepreciationRecord> departmentRecords = entry.getValue();

            Department department = departmentRepository.findById(departmentId).orElse(null);
            String departmentName = department != null ? department.getDeptName() : "未知部门";

            DepreciationSummaryDTO summary = new DepreciationSummaryDTO();
            summary.setDepartmentId(departmentId);
            summary.setDepartmentName(departmentName);
            summary.setAssetCount(
                    (int) departmentRecords.stream().map(DepreciationRecord::getAssetId).distinct().count());
            summary.setTotalOriginalValue(departmentRecords.stream()
                    .map(DepreciationRecord::getOriginalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalNetValue(departmentRecords.stream()
                    .map(DepreciationRecord::getCurrentNetValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalDepreciation(departmentRecords.stream()
                    .map(DepreciationRecord::getDepreciationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalAccumulatedDepreciation(departmentRecords.stream()
                    .map(DepreciationRecord::getAccumulatedDepreciation)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            BigDecimal totalOriginal = summary.getTotalOriginalValue();
            if (totalOriginal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal depreciationRate = summary.getTotalAccumulatedDepreciation()
                        .divide(totalOriginal, DECIMAL_PLACES, ROUNDING_MODE)
                        .multiply(new BigDecimal(100));
                summary.setDepreciationRate(depreciationRate);
            } else {
                summary.setDepreciationRate(BigDecimal.ZERO);
            }

            summaries.add(summary);
        }

        return summaries;
    }

    @Override
    public List<AssetNetValueDTO> generateAssetNetValueReport(LocalDate asOfDate) {
        List<Asset> allAssets = assetRepository.findAll();
        List<AssetNetValueDTO> netValueDTOs = new ArrayList<>();

        if (allAssets.isEmpty()) {
            return netValueDTOs;
        }

        // 收集所有资产ID
        List<Long> assetIds = allAssets.stream().map(Asset::getId).collect(Collectors.toList());

        // 批量查询所有资产的最新折旧记录
        Map<Long, DepreciationRecord> latestRecordsMap = new HashMap<>();
        if (!assetIds.isEmpty()) {
            List<DepreciationRecord> latestRecords = depreciationRecordRepository.findLatestByAssetIds(assetIds);
            for (DepreciationRecord record : latestRecords) {
                latestRecordsMap.put(record.getAssetId(), record);
            }
        }

        // 收集所有分类ID和部门ID
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> departmentIds = new HashSet<>();
        for (Asset asset : allAssets) {
            if (asset.getCategoryId() != null) {
                categoryIds.add(asset.getCategoryId());
            }
            if (asset.getDeptId() != null) {
                departmentIds.add(asset.getDeptId());
            }
        }

        // 批量查询分类信息
        Map<Long, AssetCategory> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<AssetCategory> categories = assetCategoryRepository.findAllById(categoryIds);
            for (AssetCategory category : categories) {
                categoryMap.put(category.getId(), category);
            }
        }

        // 批量查询部门信息
        Map<Long, Department> departmentMap = new HashMap<>();
        if (!departmentIds.isEmpty()) {
            List<Department> departments = departmentRepository.findAllById(departmentIds);
            for (Department department : departments) {
                departmentMap.put(department.getId(), department);
            }
        }

        for (Asset asset : allAssets) {
            AssetNetValueDTO dto = new AssetNetValueDTO();
            dto.setAssetId(asset.getId());
            dto.setAssetNo(asset.getAssetNo());
            dto.setAssetName(asset.getAssetName());
            dto.setStatus(asset.getStatus());
            dto.setOriginalValue(asset.getOriginalValue() != null ? asset.getOriginalValue() : BigDecimal.ZERO);
            dto.setPurchaseDate(asset.getPurchaseDate());
            dto.setUsefulLife(asset.getUsefulLife());

            DepreciationRecord latestRecord = latestRecordsMap.get(asset.getId());
            if (latestRecord != null) {
                dto.setNetValue(latestRecord.getCurrentNetValue());
                dto.setAccumulatedDepreciation(latestRecord.getAccumulatedDepreciation());
                dto.setUsedMonths(latestRecord.getUsedMonths());
            } else {
                dto.setNetValue(asset.getOriginalValue() != null ? asset.getOriginalValue() : BigDecimal.ZERO);
                dto.setAccumulatedDepreciation(BigDecimal.ZERO);
                dto.setUsedMonths(0);
            }

            if (asset.getCategoryId() != null) {
                AssetCategory category = categoryMap.get(asset.getCategoryId());
                dto.setCategoryName(category != null ? category.getCategoryName() : "未知分类");
            }

            if (asset.getDeptId() != null) {
                Department department = departmentMap.get(asset.getDeptId());
                dto.setDepartmentName(department != null ? department.getDeptName() : "未知部门");
            }

            BigDecimal originalValue = dto.getOriginalValue();
            if (originalValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal depreciationRate = dto.getAccumulatedDepreciation()
                        .divide(originalValue, DECIMAL_PLACES, ROUNDING_MODE)
                        .multiply(new BigDecimal(100));
                dto.setDepreciationRate(depreciationRate);
            } else {
                dto.setDepreciationRate(BigDecimal.ZERO);
            }

            netValueDTOs.add(dto);
        }

        return netValueDTOs;
    }

    @Override
    public List<AssetNetValueDTO> generateAssetNetValueReportByCategory(Long categoryId, LocalDate asOfDate) {
        AssetCategory category = assetCategoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return new ArrayList<>();
        }

        String categoryName = category.getCategoryName();
        List<AssetNetValueDTO> allNetValues = generateAssetNetValueReport(asOfDate);
        return allNetValues.stream()
                .filter(dto -> dto.getCategoryName() != null && dto.getCategoryName().equals(categoryName))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetNetValueDTO> generateAssetNetValueReportByDepartment(Long departmentId, LocalDate asOfDate) {
        Department department = departmentRepository.findById(departmentId).orElse(null);
        if (department == null) {
            return new ArrayList<>();
        }

        String departmentName = department.getDeptName();
        List<AssetNetValueDTO> allNetValues = generateAssetNetValueReport(asOfDate);
        return allNetValues.stream()
                .filter(dto -> dto.getDepartmentName() != null && dto.getDepartmentName().equals(departmentName))
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportToExcel(List<DepreciationReportDTO> data, String reportType) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(reportType);

            Row headerRow = sheet.createRow(0);
            String[] headers = { "资产编号", "资产名称", "分类", "部门", "折旧方法", "原值", "净值", "本期折旧", "累计折旧", "使用年限", "已用月数", "折旧期间",
                    "计算日期", "状态" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderCellStyle(workbook));
            }

            int rowNum = 1;
            for (DepreciationReportDTO dto : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getAssetNo());
                row.createCell(1).setCellValue(dto.getAssetName());
                row.createCell(2).setCellValue(dto.getCategoryName());
                row.createCell(3).setCellValue(dto.getDepartmentName());
                row.createCell(4).setCellValue(dto.getDepreciationMethod());
                row.createCell(5).setCellValue(dto.getOriginalValue().doubleValue());
                row.createCell(6).setCellValue(dto.getCurrentNetValue().doubleValue());
                row.createCell(7).setCellValue(dto.getDepreciationAmount().doubleValue());
                row.createCell(8).setCellValue(dto.getAccumulatedDepreciation().doubleValue());
                row.createCell(9).setCellValue(dto.getUsefulLife());
                row.createCell(10).setCellValue(dto.getUsedMonths());
                row.createCell(11).setCellValue(dto.getDepreciationPeriod());
                row.createCell(12).setCellValue(dto.getCalculationDate().format(DATE_FORMATTER));
                row.createCell(13).setCellValue(dto.getStatus());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @Override
    public byte[] exportToPDF(List<DepreciationReportDTO> data, String reportType) throws Exception {
        logger.info("导出PDF报表: {} 数据量: {}", reportType, data.size());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        StringBuilder content = new StringBuilder();
        content.append("<html><head><meta charset='UTF-8'><title>").append(reportType).append("</title></head><body>");
        content.append("<h1>").append(reportType).append("</h1>");
        content.append("<table border='1' cellpadding='5' cellspacing='0'>");
        content.append(
                "<tr><th>资产编号</th><th>资产名称</th><th>分类</th><th>部门</th><th>折旧方法</th><th>原值</th><th>净值</th><th>本期折旧</th><th>累计折旧</th><th>使用年限</th><th>已用月数</th><th>折旧期间</th><th>计算日期</th><th>状态</th></tr>");

        for (DepreciationReportDTO dto : data) {
            content.append("<tr>");
            content.append("<td>").append(dto.getAssetNo()).append("</td>");
            content.append("<td>").append(dto.getAssetName()).append("</td>");
            content.append("<td>").append(dto.getCategoryName() != null ? dto.getCategoryName() : "").append("</td>");
            content.append("<td>").append(dto.getDepartmentName() != null ? dto.getDepartmentName() : "")
                    .append("</td>");
            content.append("<td>").append(dto.getDepreciationMethod()).append("</td>");
            content.append("<td>").append(dto.getOriginalValue().toString()).append("</td>");
            content.append("<td>").append(dto.getCurrentNetValue().toString()).append("</td>");
            content.append("<td>").append(dto.getDepreciationAmount().toString()).append("</td>");
            content.append("<td>").append(dto.getAccumulatedDepreciation().toString()).append("</td>");
            content.append("<td>").append(dto.getUsefulLife() != null ? dto.getUsefulLife().toString() : "")
                    .append("</td>");
            content.append("<td>").append(dto.getUsedMonths() != null ? dto.getUsedMonths().toString() : "")
                    .append("</td>");
            content.append("<td>").append(dto.getDepreciationPeriod()).append("</td>");
            content.append("<td>").append(dto.getCalculationDate().format(DATE_FORMATTER)).append("</td>");
            content.append("<td>").append(dto.getStatus()).append("</td>");
            content.append("</tr>");
        }

        content.append("</table></body></html>");

        outputStream.write(content.toString().getBytes("UTF-8"));
        return outputStream.toByteArray();
    }

    private List<DepreciationReportDTO> convertToDepreciationReportDTOs(List<DepreciationRecord> records) {
        return records.stream()
                .map(this::convertToDepreciationReportDTO)
                .collect(Collectors.toList());
    }

    private DepreciationReportDTO convertToDepreciationReportDTO(DepreciationRecord record) {
        DepreciationReportDTO dto = new DepreciationReportDTO();
        dto.setAssetId(record.getAssetId());
        dto.setAssetNo(record.getAssetNo());
        dto.setAssetName(record.getAssetName());
        dto.setDepreciationMethod(record.getDepreciationMethod());
        dto.setOriginalValue(record.getOriginalValue());
        dto.setCurrentNetValue(record.getCurrentNetValue());
        dto.setDepreciationAmount(record.getDepreciationAmount());
        dto.setAccumulatedDepreciation(record.getAccumulatedDepreciation());
        dto.setUsefulLife(record.getUsefulLife());
        dto.setUsedMonths(record.getUsedMonths());
        dto.setDepreciationPeriod(record.getDepreciationPeriod());
        dto.setCalculationDate(record.getCreateTime() != null ? record.getCreateTime().toLocalDate() : LocalDate.now());
        dto.setStatus(record.getStatus());

        if (record.getCategoryId() != null) {
            AssetCategory category = assetCategoryRepository.findById(record.getCategoryId()).orElse(null);
            dto.setCategoryName(category != null ? category.getCategoryName() : "未知分类");
        }

        if (record.getDepartmentId() != null) {
            Department department = departmentRepository.findById(record.getDepartmentId()).orElse(null);
            dto.setDepartmentName(department != null ? department.getDeptName() : "未知部门");
        }

        return dto;
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}