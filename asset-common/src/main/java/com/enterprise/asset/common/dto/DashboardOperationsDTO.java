package com.enterprise.asset.common.dto;

import java.util.List;

public class DashboardOperationsDTO {
    
    private List<RecentOperationDTO> pendingTasks;
    private List<RecentOperationDTO> recentActivities;

    public DashboardOperationsDTO() {
    }

    public DashboardOperationsDTO(List<RecentOperationDTO> pendingTasks, List<RecentOperationDTO> recentActivities) {
        this.pendingTasks = pendingTasks;
        this.recentActivities = recentActivities;
    }

    public List<RecentOperationDTO> getPendingTasks() {
        return pendingTasks;
    }

    public void setPendingTasks(List<RecentOperationDTO> pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public List<RecentOperationDTO> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<RecentOperationDTO> recentActivities) {
        this.recentActivities = recentActivities;
    }
}