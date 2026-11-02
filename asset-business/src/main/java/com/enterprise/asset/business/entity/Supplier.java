package com.enterprise.asset.business.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "supplier")
@Data
public class Supplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    private String phone;
    
    private String address;
    
    private Integer status;
    
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}