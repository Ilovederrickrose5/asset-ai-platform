package com.enterprise.asset.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_log")
@Data
public class SysLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    private String username;
    
    private String operation;
    
    private String method;
    
    private String params;
    
    private String ip;
    
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    private String status;
    
    private String error;
    
    @Column(name = "log_type", columnDefinition = "varchar(20) default 'SYSTEM'")
    private String logType;
}