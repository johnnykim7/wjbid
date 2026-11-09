package com.biddingagency.dto;

import com.biddingagency.domain.sharedfile.entity.SharedFile;
import com.biddingagency.domain.sharedfile.entity.SharedFileLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SharedFileResponses {

    private SharedFileResponses() {
    }

    public record Rules(long maxUploadMb, List<String> blockedExtensions) {
    }

    public record Item(UUID id, String fileName, long sizeBytes, String uploadedByEmail, LocalDateTime createdAt,
                       LocalDateTime lockedUntil, boolean mine) {

        public static Item of(SharedFile file, UUID viewerId, LocalDateTime now) {
            return new Item(file.getId(), file.getFileName(), file.getSizeBytes(), file.getUploadedByEmail(),
                    file.getCreatedAt(), file.isLocked(now) ? file.getLockedUntil() : null,
                    file.getUploadedById().equals(viewerId));
        }
    }

    public record ListView(Rules rules, List<Item> files, int page, int totalPages, long totalElements) {
    }

    public record Uploaded(UUID id, String fileName, long sizeBytes, LocalDateTime createdAt, String mailedTo) {
    }

    public record LogItem(Long id, LocalDateTime createdAt, String memberEmail, String fileName, String action,
                          String clientIp) {

        public static LogItem of(SharedFileLog log) {
            return new LogItem(log.getId(), log.getCreatedAt(), log.getMemberEmail(), log.getFileName(),
                    log.getAction().name(), log.getClientIp());
        }
    }

    public record LogPage(List<LogItem> logs, int page, int totalPages, long totalElements) {
    }
}
