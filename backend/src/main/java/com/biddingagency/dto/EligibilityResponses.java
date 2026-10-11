package com.biddingagency.dto;

import com.biddingagency.domain.eligibility.entity.EligibilityAnswerValue;
import com.biddingagency.domain.member.entity.DocumentCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CR-137 적격성 진단 응답 (T3-2 E).
 */
public final class EligibilityResponses {

    private EligibilityResponses() {
    }

    public record NoticeRow(UUID noticeId, String solicitationNumber, String koreanTitle,
                            LocalDateTime responseDeadline, boolean open,
                            int itemCount, int answeredCount) {
    }

    public record NoticeHead(UUID noticeId, String solicitationNumber, String koreanTitle,
                             String organizationName, LocalDateTime responseDeadline, boolean open) {
    }

    public record VaultDocument(UUID id, String name, LocalDateTime uploadedAt) {
    }

    public record Item(int itemNo, String title, String description, boolean mandatory, boolean gate,
                       String sourceRef, String evidenceBy, EligibilityAnswerValue answer, boolean stale,
                       LocalDateTime answeredAt, DocumentCategory vaultCategory, String vaultCategoryName,
                       List<VaultDocument> vaultDocuments) {
    }

    public record Summary(int gateCount, int have, int lack, int unsure, int unanswered, int allCount) {
    }

    public record Detail(NoticeHead notice, List<Item> items, Summary summary) {
    }

    public record Saved(Item item, Summary summary) {
    }
}
