package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 원본 공고(Opportunity) DTO — CR-016.
 * 원본은 선별 풀. 노출/분석은 공고문(Notice)으로 이동 → noticeCount(딸린 공고문 수)만 표시.
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

    public static OpportunityAdminDto from(Opportunity opp, long attachmentCount,
                                           long manualFetchRequiredCount, int noticeCount) {
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
                .build();
    }

    public static OpportunityAdminDto fromList(Opportunity opp, long attachmentCount,
                                               long manualFetchRequiredCount, int noticeCount) {
        return OpportunityAdminDto.builder()
                .id(opp.getId())
                .noticeId(opp.getNoticeId())
                .solicitationNumber(opp.getSolicitationNumber())
                .title(opp.getTitle())
                .organizationName(opp.getOrganizationName())
                .responseDeadline(opp.getResponseDeadline())
                .attachmentCount(attachmentCount)
                .manualFetchRequiredCount(manualFetchRequiredCount)
                .noticeCount(noticeCount)
                .build();
    }
}
