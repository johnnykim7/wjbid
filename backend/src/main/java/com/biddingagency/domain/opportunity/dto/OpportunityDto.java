package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        OffsetDateTime responseDeadlineAt, // 마감 시각(서울). 날짜만 있는 값은 브라우저가 UTC 자정으로 읽는다
        String postedDate,          // formatted date string
        String placeOfPerformance,  // extracted from rawJson
        String description,         // extracted from rawJson
        String status,              // "active" or "closed"
        String type,
        String uiLink,
        List<String> resourceLinks, // attachment URLs from SAM.gov
        AnalysisResultDto analysis, // AI 사전 분석 결과 (상세 조회 시만)
        String originalTitle,       // 원문 제목 (원본 공고 상세만, CR-152)
        String publicNoticeId,      // 같은 공고의 공개 공고문 id (원본 공고 상세만, CR-152)
        String samUrl               // SAM.gov 원문 공개 주소. uiLink 는 SAM 로그인이 필요한 작업공간 주소다 (상세만, CR-152)
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 공고 시각은 서울 벽시계로 저장돼 있다(DB 접속 serverTimezone=Asia/Seoul) */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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
                toKst(opp.getResponseDeadline()),
                formatDate(opp.getPostedDate()),
                getString(raw, "placeOfPerformance"),
                getString(raw, "description"),
                Boolean.TRUE.equals(opp.getActive()) ? "active" : "closed",
                opp.getTypeKo() != null && !opp.getTypeKo().isBlank() ? opp.getTypeKo() : opp.getType(),
                opp.getUiLink(),
                getStringList(raw, "resourceLinks"),
                null,
                null,
                null,
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
                toKst(opp.getResponseDeadline()),
                formatDate(opp.getPostedDate()),
                getString(raw, "placeOfPerformance"),
                getString(raw, "description"),
                Boolean.TRUE.equals(opp.getActive()) ? "active" : "closed",
                displayType,
                opp.getUiLink(),
                getStringList(raw, "resourceLinks"),
                AnalysisResultDto.fromNotice(notice),
                null,
                null,
                null
        );
    }

    /**
     * CR-152: 공개 공고문이 없는 모은 공고의 상세. id=opportunityId, title=한글 제목(없으면 원문),
     * description=모아 둔 원문 본문. rawJson 의 description 은 본문이 아니라 SAM 본문 조회 주소라 쓰지 않는다.
     */
    public static OpportunityDto fromOrigin(Opportunity opp, UUID publicNoticeId, String samUrl) {
        Map<String, Object> raw = opp.getRawJson();
        String displayTitle = opp.getTitleKo() != null && !opp.getTitleKo().isBlank()
                ? opp.getTitleKo() : opp.getTitle();
        String displayType = opp.getTypeKo() != null && !opp.getTypeKo().isBlank()
                ? opp.getTypeKo() : opp.getType();
        return new OpportunityDto(
                opp.getId().toString(),
                opp.getSolicitationNumber(),
                displayTitle,
                opp.getOrganizationName(),
                getString(raw, "naicsCode"),
                getString(raw, "setAside"),
                formatDate(opp.getResponseDeadline()),
                toKst(opp.getResponseDeadline()),
                formatDate(opp.getPostedDate()),
                getString(raw, "placeOfPerformance"),
                opp.getDescriptionBody() != null && !opp.getDescriptionBody().isBlank() ? opp.getDescriptionBody() : null,
                Boolean.TRUE.equals(opp.getActive()) ? "active" : "closed",
                displayType,
                opp.getUiLink(),
                getStringList(raw, "resourceLinks"),
                null,
                opp.getTitle(),
                publicNoticeId != null ? publicNoticeId.toString() : null,
                samUrl
        );
    }

    public OpportunityDto withSamUrl(String url) {
        return new OpportunityDto(id, solicitationNumber, title, agencyName, naicsCode, setAside, responseDeadline,
                responseDeadlineAt, postedDate, placeOfPerformance, description, status, type, uiLink, resourceLinks,
                analysis, originalTitle, publicNoticeId, url);
    }

    private static OffsetDateTime toKst(LocalDateTime dt) {
        return dt == null ? null : dt.atZone(KST).toOffsetDateTime();
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
