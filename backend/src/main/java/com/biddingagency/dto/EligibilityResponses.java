package com.biddingagency.dto;

import com.biddingagency.domain.eligibility.entity.EligibilityAnswerValue;
import com.biddingagency.domain.eligibility.entity.EligibilityBlockerProblem;
import com.biddingagency.domain.eligibility.entity.EligibilityVerdict;
import com.biddingagency.domain.member.entity.DocumentCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CR-137 적격성 진단 응답 (T3-2 E). CR-140 에서 판정과 붙인 서류를 더함.
 */
public final class EligibilityResponses {

    private EligibilityResponses() {
    }

    public record NoticeRow(UUID noticeId, String solicitationNumber, String koreanTitle,
                            LocalDateTime responseDeadline, boolean open,
                            int itemCount, int answeredCount, int gateCount,
                            EligibilityVerdict verdict, List<Blocker> blockers) {
    }

    public record NoticeHead(UUID noticeId, String solicitationNumber, String koreanTitle,
                             String organizationName, LocalDateTime responseDeadline, boolean open) {
    }

    public record VaultDocument(UUID id, String name, LocalDateTime uploadedAt, LocalDate expiresOn, boolean expired) {
    }

    public record Item(int itemNo, String title, String description, boolean mandatory, boolean gate,
                       String sourceRef, String evidenceBy, EligibilityAnswerValue answer, boolean stale,
                       LocalDateTime answeredAt, DocumentCategory vaultCategory, String vaultCategoryName,
                       List<VaultDocument> vaultDocuments, AttachedDocument attachedDocument) {
    }

    public record AttachedDocument(UUID id, String name, LocalDate expiresOn, boolean expired) {
    }

    /** 판정을 정한 자격. documentName, expiresOn 은 EXPIRED 일 때만 채운다 */
    public record Blocker(int itemNo, String title, String sourceRef, EligibilityBlockerProblem problem,
                          String documentName, LocalDate expiresOn) {
    }

    public record Summary(int gateCount, int have, int lack, int unsure, int unanswered, int allCount) {
    }

    public record Detail(NoticeHead notice, List<Item> items, Summary summary,
                         EligibilityVerdict verdict, List<Blocker> blockers) {
    }

    public record Saved(Item item, Summary summary, EligibilityVerdict verdict, List<Blocker> blockers) {
    }
}
