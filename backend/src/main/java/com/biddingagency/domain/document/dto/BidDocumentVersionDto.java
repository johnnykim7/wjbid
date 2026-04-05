package com.biddingagency.domain.document.dto;

import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BidDocumentVersionDto(
        String id,
        Integer versionNo,
        String versionLabel,
        Map<String, Object> contentJson,
        String editedBy,
        String editedAt,
        String changeSummary,
        Integer wordCount
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static BidDocumentVersionDto from(BidDocumentVersion v) {
        return new BidDocumentVersionDto(
                v.getId() != null ? v.getId().toString() : null,
                v.getVersionNo(),
                v.getVersionLabel(),
                v.getContentJson(),
                v.getEditedBy() != null ? v.getEditedBy().toString() : null,
                v.getEditedAt() != null ? v.getEditedAt().format(FMT) : null,
                v.getChangeSummary(),
                v.getWordCount()
        );
    }

    public static BidDocumentVersionDto summary(BidDocumentVersion v) {
        return new BidDocumentVersionDto(
                v.getId() != null ? v.getId().toString() : null,
                v.getVersionNo(),
                v.getVersionLabel(),
                null,
                v.getEditedBy() != null ? v.getEditedBy().toString() : null,
                v.getEditedAt() != null ? v.getEditedAt().format(FMT) : null,
                v.getChangeSummary(),
                v.getWordCount()
        );
    }
}
