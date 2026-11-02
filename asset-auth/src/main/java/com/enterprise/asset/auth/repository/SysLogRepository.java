package com.enterprise.asset.auth.repository;

import com.enterprise.asset.auth.entity.SysLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 系统日志数据访问接口
 * 关联表: sys_log(系统日志表)
 * 主要操作: 日志的增删改查、最近日志查询
 */
public interface SysLogRepository extends JpaRepository<SysLog, Long> {
    
    /** 查询最近的日志记录 */
    @Query("SELECT s FROM SysLog s ORDER BY s.createTime DESC LIMIT ?1")
    List<SysLog> findRecentLogs(int limit);
}