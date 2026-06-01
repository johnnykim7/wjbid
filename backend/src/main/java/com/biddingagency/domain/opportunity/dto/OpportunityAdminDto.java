package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 원본 공고(Opportunity) DTO — CR-016.
 * 원본은 선별 풀. 노출/분석은 공고문(Notice)으로 이동 → noticeCount(딸린 공고문 수)만 표시.
 * CR-021 2차: SAM.gov 원본 메타 필드 (setAside/naics/classificationCode/placeOfPerformance/description/pointOfContact/resourceLinks) 노출.
 */
@Data
@Builder
public class OpportunityAdminDto {
    private UUID id;
    private String noticeId;
    private String solicitationNumber;
    private String title;
    private String type;
    private String organizationName;
    private LocalDateTime postedDate;
    private LocalDateTime responseDeadline;
    private Boolean active;
    private String uiLink;
    private long attachmentCount;
    /** CR-019: 외부서 가져와야 할 첨부 수 (MANUAL_FETCH_REQUIRED). >0이면 "가져와야 함" 표식 */
    private long manualFetchRequiredCount;
    /** 이 원본에서 생성된 공고문 수 (0이면 아직 미선별) */
    private int noticeCount;

    // ── CR-021 2차: SAM.gov 원본 메타 (raw_json 기반) ──
    /** 입찰 분류 (Set Aside) — SDVOSB/8(a)/HUBZone 등. 미설정 시 null */
    private String setAside;
    /** NAICS 코드 (산업 분류) */
    private String naicsCode;
    /** Classification Code (PSC) */
    private String classificationCode;
    /** 수행 장소 — SAM 원본 placeOfPerformance(문자열 또는 객체) */
    private Object placeOfPerformance;
    /** Description — SAM API 외부 링크 URL 또는 본문(긴 텍스트일 수 있음) */
    private String description;
    /** 담당자 (Points of Contact) — [{type, email, phone, fullName, fax}] 형태 */
    private List<Map<String, Object>> pointOfContact;
    /** Resource Links — 외부 자료 링크 */
    private List<String> resourceLinks;

    // ── CR-022 (재구현): 본문 한글 번역 ──
    /** 본문 한글 번역 결과. null = 미번역 또는 실패. 화면은 영문 description fallback */
    private String descriptionSummaryKo;
    /** 제목 한글 번역 결과. null = 미번역. 화면은 영문 title fallback */
    private String titleKo;
    /** 공고 유형 한글 라벨. null = 미번역. 화면은 영문 type fallback */
    private String typeKo;
    /** 마지막 번역 성공 시각. null = 한 번도 성공 안 함 */
    private LocalDateTime translatedAt;

    @SuppressWarnings("unchecked")
    public static OpportunityAdminDto from(Opportunity opp, long attachmentCount,
                                           long manualFetchRequiredCount, int noticeCount) {
        Map<String, Object> raw = opp.getRawJson();
        String setAside = null;
        String naics = null;
        String psc = null;
        Object pop = null;
        String desc = null;
        List<Map<String, Object>> pocs = null;
        List<String> resourceLinks = null;
        if (raw != null) {
            setAside = strOrNull(raw.get("setAside"));
            naics = strOrNull(raw.get("naicsCode"));
            psc = strOrNull(raw.get("classificationCode"));
            pop = raw.get("placeOfPerformance");
            desc = strOrNull(raw.get("description"));
            Object pocObj = raw.get("pointOfContact");
            if (pocObj instanceof List<?> list) {
                pocs = (List<Map<String, Object>>) list;
            }
            Object rlObj = raw.get("resourceLinks");
            if (rlObj instanceof List<?> list) {
                resourceLinks = list.stream().map(String::valueOf).toList();
            }
        }
        return OpportunityAdminDto.builder()
                .id(opp.getId())
                .noticeId(opp.getNoticeId())
                .solicitationNumber(opp.getSolicitationNumber())
                .title(opp.getTitle())
                .type(opp.getType())
                .organizationName(opp.getOrganizationName())
                .postedDate(opp.getPostedDate())
                .responseDeadline(opp.getResponseDeadline())
                .active(opp.getActive())
                .uiLink(opp.getUiLink())
                .attachmentCount(attachmentCount)
                .manualFetchRequiredCount(manualFetchRequiredCount)
                .noticeCount(noticeCount)
                .setAside(setAside)
                .naicsCode(naics)
                .classificationCode(psc)
                .placeOfPerformance(pop)
                .description(desc)
                .pointOfContact(pocs)
                .resourceLinks(resourceLinks)
                .descriptionSummaryKo(opp.getDescriptionSummaryKo())
                .titleKo(opp.getTitleKo())
                .typeKo(opp.getTypeKo())
                .translatedAt(opp.getTranslatedAt())
                .build();
    }

    private static String strOrNull(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    public static OpportunityAdminDto fromList(Opportunity opp, long attachmentCount,
                                               long manualFetchRequiredCount, int noticeCount) {
        return OpportunityAdminDto.builder()
                .id(opp.getId())
                .noticeId(opp.getNoticeId())
                .solicitationNumber(opp.getSolicitationNumber())
                .title(opp.getTitle())
                .titleKo(opp.getTitleKo())
                .typeKo(opp.getTypeKo())
                .organizationName(opp.getOrganizationName())
                .postedDate(opp.getPostedDate())
                .responseDeadline(opp.getResponseDeadline())
                .attachmentCount(attachmentCount)
                .manualFetchRequiredCount(manualFetchRequiredCount)
                .noticeCount(noticeCount)
                .translatedAt(opp.getTranslatedAt())
                .build();
    }
}
