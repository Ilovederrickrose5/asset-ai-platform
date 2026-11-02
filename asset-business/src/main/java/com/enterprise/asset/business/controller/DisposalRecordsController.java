package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.enums.ApplicationType;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.business.entity.AssetApplication;
import com.enterprise.asset.business.service.AssetApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** 报废记录控制器 - 处理资产报废申请记录查询 */
@RestController
@RequestMapping("/api/disposal-records")
public class DisposalRecordsController {

    private final AssetApplicationService applicationService;

    public DisposalRecordsController(AssetApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * GET /api/disposal-records - 获取报废记录列表（支持分页和部门筛选）
     * 权限：admin查看全部，leader/manager查看本部门，普通用户查看自己的记录
     * 
     * @param departmentId 部门ID（可选）
     * @param page         页码，默认1
     * @param pageSize     每页大小，默认10
     * @return 分页的报废记录列表
     */
    @GetMapping
    public Result<PageResult<AssetApplication>> getDisposalRecords(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error(401, "用户未认证");
        }

        UserDTO userDTO = (authentication.getPrincipal() instanceof UserDTO) ? (UserDTO) authentication.getPrincipal()
                : null;
        if (userDTO == null) {
            return Result.error(404, "用户不存在");
        }

        List<String> roles = userDTO.getRoleCodes();
        boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"));
        boolean isLeader = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("LEADER"));
        boolean isManager = roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("MANAGER"));

        List<AssetApplication> allApplications = applicationService.getAllApplications();
        List<AssetApplication> disposalApplications = allApplications.stream()
                .filter(app -> ApplicationType.DISPOSAL.getCode().equals(app.getApplicationType()))
                .collect(Collectors.toList());

        List<AssetApplication> filteredApplications = disposalApplications.stream()
                .filter(application -> {
                    if (isAdmin) {
                        return true;
                    }

                    if ((isLeader || isManager)
                            && userDTO.getDeptId() != null) {
                        return application.getDepartmentId() != null
                                && application.getDepartmentId().equals(userDTO.getDeptId());
                    }

                    return application.getApplicantId() != null && application.getApplicantId().equals(userDTO.getId());
                })
                .collect(Collectors.toList());

        if (departmentId != null) {
            filteredApplications = filteredApplications.stream()
                    .filter(application -> application.getDepartmentId() != null
                            && application.getDepartmentId().equals(departmentId))
                    .collect(Collectors.toList());
        }

        int total = filteredApplications.size();

        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }

        int start = (page - 1) * pageSize;
        if (start >= total) {
            start = total;
        }

        List<AssetApplication> paginatedApplications = filteredApplications.stream()
                .skip(start)
                .limit(pageSize)
                .collect(Collectors.toList());

        PageResult<AssetApplication> result = new PageResult<>();
        result.setRecords(paginatedApplications);
        result.setTotal(total);
        result.setSize(pageSize);
        result.setCurrent(page);

        return Result.success(result);
    }

    /** 分页结果类 */
    public static class PageResult<T> {
        private List<T> records;
        private long total;
        private int size;
        private int current;

        // getters and setters
        public List<T> getRecords() {
            return records;
        }

        public void setRecords(List<T> records) {
            this.records = records;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }
    }
}