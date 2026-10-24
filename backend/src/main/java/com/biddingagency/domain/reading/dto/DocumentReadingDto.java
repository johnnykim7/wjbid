package com.biddingagency.domain.reading.dto;

import com.biddingagency.domain.reading.entity.ReadAs;
import com.biddingagency.domain.reading.entity.ReadingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 고객 서류 읽기 관리자 응답 (CR-169, T3-2) */
public final class DocumentReadingDto {

    private DocumentReadingDto() {
    }

    /** 공고별 서류(BID_DOCUMENT)와 공통 서류함(VAULT) */
    public enum Origin {
        BID_DOCUMENT,
        VAULT
    }

    public record Fulfill(String requirementTitle, String reqId) {
    }

    public record Overview(boolean readingStarted, boolean inProgress, List<Item> items) {
    }

    /** readingId 와 status 는 아직 읽기를 걸지 않은 후보면 비어 있다 */
    public record Item(
            UUID readingId,
            String fileName,
            Origin origin,
            UUID documentId,
            String category,
            List<Fulfill> fulfills,
            ReadingStatus status,
            ReadAs readAs,
            int factCount,
            boolean visionUsed,
            String failureReason,
            LocalDateTime readAt
    ) {
    }

    public record Detail(
            UUID readingId,
            String fileName,
            ReadingStatus status,
            ReadAs readAs,
            boolean visionUsed,
            String failureReason,
            LocalDateTime readAt,
            List<Map<String, Object>> facts
    ) {
    }

    /** 제안서 Section 관리 절 상세의 근거 서류 꼬리표 */
    public record SourceDocument(UUID readingId, String fileName, ReadingStatus status, int factCount) {
    }
}
