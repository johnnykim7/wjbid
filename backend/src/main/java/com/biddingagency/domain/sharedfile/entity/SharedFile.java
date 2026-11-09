package com.biddingagency.domain.sharedfile.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shared_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SharedFile extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "key_salt", nullable = false, columnDefinition = "BINARY(16)")
    private byte[] keySalt;

    @Column(name = "password_check", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] passwordCheck;

    @Column(name = "uploaded_by_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID uploadedById;

    @Column(name = "uploaded_by_email", nullable = false)
    private String uploadedByEmail;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** 한도에 이르면 잠그고 횟수를 비운다. 잠금이 풀린 뒤에는 다시 한도만큼 시도할 수 있다. */
    public boolean recordWrongPassword(int maxAttempts, LocalDateTime lockUntil) {
        failedAttempts++;
        if (failedAttempts < maxAttempts) {
            return false;
        }
        failedAttempts = 0;
        lockedUntil = lockUntil;
        return true;
    }

    public void clearFailures() {
        failedAttempts = 0;
        lockedUntil = null;
    }
}
