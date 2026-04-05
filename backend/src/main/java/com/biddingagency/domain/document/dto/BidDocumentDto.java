package com.biddingagency.domain.document.dto;

import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BidDocumentDto(
        String id,
        String bidRequestId,
        String documentType,
        String status,
        Integer currentVersionNo,
        String createdAt,
        String updatedAt,
        BidDocumentVersionDto latestVersion
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static BidDocumentDto from(BidDocument doc) {
        return new BidDocumentDto(
                doc.getId() != null ? doc.getId().toString() : null,
                doc.getBidRequest() != null && doc.getBidRequest().getId() != null
                        ? doc.getBidRequest().getId().toString() : null,
                doc.getDocumentType() != null ? doc.getDocumentType().name() : null,
                doc.getStatus() != null ? doc.getStatus().name() : null,
                doc.getCurrentVersionNo(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().format(FMT) : null,
                doc.getUpdatedAt() != null ? doc.getUpdatedAt().format(FMT) : null,
                null
        );
    }

    public static BidDocumentDto withVersion(BidDocument doc, BidDocumentVersion version) {
        return new BidDocumentDto(
                doc.getId() != null ? doc.getId().toString() : null,
                doc.getBidRequest() != null && doc.getBidRequest().getId() != null
                        ? doc.getBidRequest().getId().toString() : null,
                doc.getDocumentType() != null ? doc.getDocumentType().name() : null,
                doc.getStatus() != null ? doc.getStatus().name() : null,
                doc.getCurrentVersionNo(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().format(FMT) : null,
                doc.getUpdatedAt() != null ? doc.getUpdatedAt().format(FMT) : null,
                version != null ? BidDocumentVersionDto.from(version) : null
        );
    }
}
