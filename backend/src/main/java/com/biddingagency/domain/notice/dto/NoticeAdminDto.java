package com.biddingagency.domain.notice.dto;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.opportunity.dto.AnalysisResultDto;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 공고문(Notice) DTO — CR-016.
 * 한글화/요약 결과 + 노출 상태. 리스트는 요약 제외, 상세는 analysis 포함.
 */
@Data
@Builder
public class NoticeAdminDto {
    private UUID id;
    private UUID opportunityId;
    private String originTitle;          // 원본 영문 제목
    private String solicitationNumber;
    private String organizationName;
    private String noticeTypeKo;          // CR-117: 공고유형(한글) — 옛 NOTICE_VIEW "1.공고 기본정보" 행 복원
    private String naicsLabelKo;          // CR-117: NAICS 코드/라벨(한글)
    private LocalDateTime postedDate;     // 공고 게시일 (SAM 수집 확정값 — 화면 "공고일" 표시용)
    private LocalDateTime responseDeadline;
    private String koreanTitle;          // 한글화 제목
    private NoticeGenerationStatus generationStatus;
    private OpportunityVisibility visibility;
    private LocalDateTime analyzedAt;
    private String errorMessage;

    // 상세 조회 시만 (한글화 결과)
    private AnalysisResultDto analysis;

    public static NoticeAdminDto fromList(Notice notice) {
        var opp = notice.getOpportunity();
        return NoticeAdminDto.builder()
                .id(notice.getId())
                .opportunityId(opp.getId())
                .originTitle(opp.getTitle())
                .solicitationNumber(opp.getSolicitationNumber())
                .organizationName(opp.getOrganizationName())
                .noticeTypeKo(opp.getTypeKo() != null ? opp.getTypeKo() : opp.getType())
                .naicsLabelKo(opp.getNaicsLabelKo())
                .postedDate(opp.getPostedDate())
                .responseDeadline(opp.getResponseDeadline())
                .koreanTitle(notice.getKoreanTitle())
                .generationStatus(notice.getGenerationStatus())
                .visibility(notice.getVisibility())
                .analyzedAt(notice.getAnalyzedAt())
                .errorMessage(notice.getErrorMessage())
                .build();
    }

    public static NoticeAdminDto from(Notice notice) {
        NoticeAdminDto dto = fromList(notice);
        dto.setAnalysis(AnalysisResultDto.fromNotice(notice));
        return dto;
    }
}
