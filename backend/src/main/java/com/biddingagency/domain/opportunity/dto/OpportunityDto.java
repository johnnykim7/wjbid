package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Opportunity DTO for API responses
 *
 * Maps Opportunity entity fields to frontend-compatible field names
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpportunityDto(
        String id,
        String solicitationNumber,
        String title,
        String agencyName,          // mapped from organizationName
        String naicsCode,           // extracted from rawJson
        String setAside,            // extracted from rawJson
        String responseDeadline,    // formatted date string
        String postedDate,          // formatted date string
        String placeOfPerformance,  // extracted from rawJson
        String description,         // extracted from rawJson
        String status,              // "active" or "closed"
        String type,
        String uiLink,
        List<String> resourceLinks, // attachment URLs from SAM.gov
        AnalysisResultDto analysis  // AI 사전 분석 결과 (상세 조회 시만)
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 원본 기준 (near-deadline/recent 등 — 노출 단위가 원본일 때). id=opportunityId. */
    public static OpportunityDto from(Opportunity opp) {
        Map<String, Object> raw = opp.getRawJson();
        return new OpportunityDto(
                opp.getId() != null ? opp.getId().toString() : null,
                opp.getSolicitationNumber(),
                opp.getTitle(),
                opp.getOrganizationName(),
                getString(raw, "naicsCode"),
                getString(raw, "setAside"),
                formatDate(opp.getResponseDeadline()),
                formatDate(opp.getPostedDate()),
                getString(raw, "placeOfPerformance"),
                getString(raw, "description"),
                Boolean.TRUE.equals(opp.getActive()) ? "active" : "closed",
                opp.getTypeKo() != null && !opp.getTypeKo().isBlank() ? opp.getTypeKo() : opp.getType(),
                opp.getUiLink(),
                getStringList(raw, "resourceLinks"),
                null
        );
    }

    /**
     * CR-016: 고객 노출 단위 = 공고문(Notice). id=noticeId, title=한글화 제목(없으면 원문),
     * analysis=Notice 한글화 결과, 나머지 메타는 원본(Opportunity)에서.
     */
    public static OpportunityDto fromNotice(Notice notice) {
        Opportunity opp = notice.getOpportunity();
        Map<String, Object> raw = opp.getRawJson();
        String displayTitle = notice.getKoreanTitle() != null && !notice.getKoreanTitle().isBlank()
                ? notice.getKoreanTitle() : opp.getTitle();
        // 고객 노출은 번역본 우선: typeKo가 있으면 그걸, 없으면 영문 type fallback
        String displayType = opp.getTypeKo() != null && !opp.getTypeKo().isBlank()
                ? opp.getTypeKo() : opp.getType();

        return new OpportunityDto(
                notice.getId().toString(),
                opp.getSolicitationNumber(),
                displayTitle,
                opp.getOrganizationName(),
                getString(raw, "naicsCode"),
                getString(raw, "setAside"),
                formatDate(opp.getResponseDeadline()),
                formatDate(opp.getPostedDate()),
                getString(raw, "placeOfPerformance"),
                getString(raw, "description"),
                Boolean.TRUE.equals(opp.getActive()) ? "active" : "closed",
                displayType,
                opp.getUiLink(),
                getStringList(raw, "resourceLinks"),
                AnalysisResultDto.fromNotice(notice)
        );
    }

    private static String formatDate(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DATE_FORMATTER);
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val == null) return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : s;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof List<?> list && !list.isEmpty()) {
            return (List<String>) list;
        }
        return null;
    }
}
