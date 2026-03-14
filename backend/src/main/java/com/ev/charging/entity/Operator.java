package com.ev.charging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 运营商实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operator")
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 运营商名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 运营商编码（唯一）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 联系人
     */
    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    /**
     * 联系电话
     */
    @Column(name = "contact_phone", length = 11)
    private String contactPhone;

    /**
     * 邮箱
     */
    @Column(length = 100)
    private String email;

    /**
     * 运营商状态：1-正常，0-禁用
     */
    @Column(nullable = false)
    @Builder.Default
    private Byte status = 1;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
