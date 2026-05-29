package com.biddingagency.domain.compliance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 요구사항 슬롯 (CR-010)
 *
 * 공고 분석으로 도출된 요구사항 1건 + 매핑된 고객 업로드 서류 요약.
 */
@Getter
@Builder
public class RequiredDocumentSlot {

    private UUID requirementItemId;
    private String title;
    private String description;
    private Boolean isBlocker;
    private String category;
    private String status;            // FULFILLED | PENDING | MISSING
    private String fulfillmentType;   // CLIENT_DOCUMENT | DOCUMENT_SECTION | ATTACHMENT | null
    private MappedDocument mappedClientDocument;

    @Getter
    @Builder
    public static class MappedDocument {
        private UUID id;
        private String fileName;
        private Long fileSize;
        private LocalDateTime uploadedAt;
    }
}
