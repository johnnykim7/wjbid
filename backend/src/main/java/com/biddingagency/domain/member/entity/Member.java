package com.biddingagency.domain.member.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Member entity (회원)
 * 역할: ADMIN (관리자) / CUSTOMER (고객)
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "business_registration_number", length = 50)
    private String businessRegistrationNumber;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Business methods

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateProfile(String companyName, String contactPerson, String phone, String address) {
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.address = address;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Role enum — ADMIN + CUSTOMER only
     */
    public enum Role {
        ADMIN,      // 관리자 (입찰 대행 운영)
        CUSTOMER    // 고객 (입찰 의뢰자)
    }
}
