package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_category")
@Data
public class AssetCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "category_name", nullable = false)
    private String categoryName;
    
    @Column(name = "category_code", unique = true, nullable = false)
    private String categoryCode;
    
    @Column(name = "parent_id")
    private Long parentId;
    
    private String description;
    
    private Integer status = 1;
    
    private Integer usefulLife;
    
    private String depreciationMethod;
    
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}