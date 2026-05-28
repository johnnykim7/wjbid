package com.biddingagency.domain.rfp.dto;

import com.biddingagency.domain.rfp.entity.PatternGuide;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/** 공고유형별 패턴 가이드 DTO (CR-013 재설계) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatternGuideDto(
        String id,
        String industryType,
        String status,
        String source,
        Map<String, Object> guideJson,
        String guideMarkdown,
        Integer sampleCount,
        String extractedAt,
        String errorMessage
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static PatternGuideDto from(PatternGuide g) {
        return new PatternGuideDto(
                g.getId().toString(),
                g.getIndustryType().name(),
                g.getStatus() != null ? g.getStatus().name() : null,
                g.getSource() != null ? g.getSource().name() : null,
                g.getGuideJson(),
                g.getGuideMarkdown(),
                g.getSampleCount(),
                g.getExtractedAt() != null ? g.getExtractedAt().format(FMT) : null,
                g.getErrorMessage()
        );
    }
}
