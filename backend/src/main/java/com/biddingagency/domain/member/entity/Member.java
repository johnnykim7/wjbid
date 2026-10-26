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

    /** 인증번호나 SNS 가 이메일을 확인한 시각. CR-146 전에 가입한 회원은 비어 있다 (BIZ-062) */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /** SNS 로만 가입한 회원은 비어 있어 비밀번호로 로그인되지 않는다 (CR-146, BIZ-064) */
    @Column(name = "password_hash")
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

    /** SAM.gov Unique Entity ID — 가입 후 프로필에서 입력 (CR-124) */
    @Column(name = "uei", length = 20)
    private String uei;

    /** CAGE (Commercial and Government Entity) 코드 — 가입 후 프로필에서 입력 (CR-124) */
    @Column(name = "cage_code", length = 10)
    private String cageCode;

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

    public void updateBusinessRegistrationNumber(String businessRegistrationNumber) {
        this.businessRegistrationNumber = businessRegistrationNumber;
    }

    /** SAM.gov 식별자 갱신 (프로필 화면, CR-124) */
    public void updateSamIdentifiers(String uei, String cageCode) {
        this.uei = uei;
        this.cageCode = cageCode;
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
