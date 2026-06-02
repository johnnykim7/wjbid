package com.biddingagency.domain.opportunity.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.rfp.entity.IndustryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Opportunity entity (공고)
 *
 * Represents a contract opportunity from SAM.gov
 */
@Entity
@Table(name = "opportunities",
        indexes = {
                @Index(name = "idx_opportunities_notice_id", columnList = "notice_id"),
                @Index(name = "idx_opportunities_posted_date", columnList = "posted_date"),
                @Index(name = "idx_opportunities_deadline", columnList = "response_deadline"),
                @Index(name = "idx_opportunities_active", columnList = "active"),
                @Index(name = "idx_opportunities_industry_type", columnList = "industry_type")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Opportunity extends BaseEntity {

    @Column(name = "notice_id", nullable = false, unique = true)
    private String noticeId;

    @Column(name = "solicitation_number")
    private String solicitationNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "organization_name")
    private String organizationName;

    /** 사업유형 자동분류 (CR-014, BIZ-018). null = 미분류. 성공 자산 매칭 키 */
    @Enumerated(EnumType.STRING)
    @Column(name = "industry_type", length = 30)
    private IndustryType industryType;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "ui_link", columnDefinition = "TEXT")
    private String uiLink;

    @Column(name = "description_link", columnDefinition = "TEXT")
    private String descriptionLink;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    /** CR-022 (재구현): 제목 한글 번역 결과. V21 컬럼 재활용. null = 미번역 */
    @Column(name = "title_ko", length = 500)
    private String titleKo;

    /** CR-022: 공고 유형(type) 한글 라벨. null = 미번역 */
    @Column(name = "type_ko", length = 100)
    private String typeKo;

    /** CR-022 (재구현): 본문(description) 한글 번역 결과. V23 컬럼 재활용. null = 미번역 또는 실패 */
    @Column(name = "description_summary_ko", length = 500)
    private String descriptionSummaryKo;

    /**
     * CR-032: SAM noticedesc에서 가져온 원문 본문(평문). null = 미수집 또는 실패.
     * search 응답의 description은 URL이라 별도 fetch해 여기에 저장(관리자 상세 노출용).
     */
    @Column(name = "description_body", columnDefinition = "MEDIUMTEXT")
    private String descriptionBody;

    /** CR-022 (재구현): 마지막 번역 성공 시각(제목/본문 어느 쪽이든 갱신). null = 한 번도 성공 안 함 */
    @Column(name = "translated_at")
    private LocalDateTime translatedAt;

    /** CR-022 2차: 낙찰가 등 금액 ($ 포함 문자열). null = 정보 없음 */
    @Column(name = "award_amount", length = 50)
    private String awardAmount;

    /** CR-022 2차: 수행 장소 단답(City, ST). null = 정보 없음 */
    @Column(name = "place_of_performance_short", length = 200)
    private String placeOfPerformanceShort;

    /** CR-022 2차: 우선조달 유형 한글 라벨. null = 해당 없음 */
    @Column(name = "set_aside_ko", length = 100)
    private String setAsideKo;

    /** CR-022 2차: NAICS top-level 한글 라벨. null = 미매핑 */
    @Column(name = "naics_label_ko", length = 200)
    private String naicsLabelKo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "JSON")
    private Map<String, Object> rawJson;

    // Business methods

    public void updateContent(String title, String type, String organizationName,
                              LocalDateTime postedDate, LocalDateTime responseDeadline,
                              String uiLink, String descriptionLink,
                              Map<String, Object> rawJson, String contentHash) {
        this.title = title;
        this.type = type;
        this.organizationName = organizationName;
        this.postedDate = postedDate;
        this.responseDeadline = responseDeadline;
        this.uiLink = uiLink;
        this.descriptionLink = descriptionLink;
        this.rawJson = rawJson;
        this.contentHash = contentHash;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void markAsInactive() {
        this.active = false;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /** 자동분류 결과 반영 (CR-014). null이면 미분류로 둔다 */
    public void assignIndustryType(IndustryType industryType) {
        this.industryType = industryType;
    }

    /** CR-032: noticedesc 원문 본문 저장. 빈 문자열/null은 무시. */
    public void applyDescriptionBody(String body) {
        if (body == null || body.isBlank()) return;
        this.descriptionBody = body;
    }

    /** CR-022 (재구현): 본문 한글 번역 결과 반영. 빈 문자열/null은 무시 (실패 시 영문 fallback) */
    public void applyDescriptionTranslation(String koreanText) {
        if (koreanText == null || koreanText.isBlank()) return;
        this.descriptionSummaryKo = koreanText.length() > 500
                ? koreanText.substring(0, 500) : koreanText;
        this.translatedAt = LocalDateTime.now();
    }

    /** CR-022 (재구현): 제목 한글 번역 결과 반영. 빈 문자열/null은 무시. */
    public void applyTitleTranslation(String koreanTitle) {
        if (koreanTitle == null || koreanTitle.isBlank()) return;
        this.titleKo = koreanTitle.length() > 500
                ? koreanTitle.substring(0, 500) : koreanTitle;
        this.translatedAt = LocalDateTime.now();
    }

    /** CR-022: type 한글 라벨 적용 (코드 매핑 결과). null/blank면 무시(영문 fallback 유지). */
    public void applyTypeKo(String koreanType) {
        if (koreanType == null || koreanType.isBlank()) return;
        this.typeKo = koreanType;
    }

    /** CR-022 2차: 코드 매핑·단순 추출 결과(수집 동기 단계). 자유텍스트 LLM 결과는 별도 메서드. */
    public void applySelectionMeta(String awardAmount, String placeOfPerformanceShort,
                                   String setAsideKo, String naicsLabelKo) {
        this.awardAmount = awardAmount;
        this.placeOfPerformanceShort = placeOfPerformanceShort;
        this.setAsideKo = setAsideKo;
        this.naicsLabelKo = naicsLabelKo;
    }

    public boolean isDeadlinePassed() {
        return responseDeadline != null && responseDeadline.isBefore(LocalDateTime.now());
    }

    public boolean isDeadlineNear(int days) {
        return responseDeadline != null
                && responseDeadline.isAfter(LocalDateTime.now())
                && responseDeadline.isBefore(LocalDateTime.now().plusDays(days));
    }
}
