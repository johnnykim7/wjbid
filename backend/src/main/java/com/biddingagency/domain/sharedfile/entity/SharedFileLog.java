package com.biddingagency.domain.sharedfile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** 고치거나 지우는 메서드를 두지 않는다(BIZ-115). */
@Entity
@Table(name = "shared_file_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SharedFileLog {

    public enum Action {
        UPLOAD, DOWNLOAD, WRONG_PASSWORD, LOCKED, DELETE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shared_file_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID sharedFileId;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 20)
    private Action action;

    @Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "member_email", nullable = false, updatable = false)
    private String memberEmail;

    @Column(name = "client_ip", updatable = false, length = 45)
    private String clientIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
