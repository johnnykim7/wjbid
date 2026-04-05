package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.entity.AnalysisStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAnalysis;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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
    private OpportunityVisibility visibility;
    private String uiLink;
    private long attachmentCount;
    private AnalysisStatus analysisStatus;
    private LocalDateTime analyzedAt;

    // 분석 결과 (상세 조회 시만)
    private Map<String, Object> summaryJson;
    private Map<String, Object> documentFormatsJson;
    private Map<String, Object> requiredDocumentsJson;

    public static OpportunityAdminDto from(Opportunity opp, long attachmentCount, OpportunityAnalysis analysis) {
        OpportunityAdminDtoBuilder builder = OpportunityAdminDto.builder()
                .id(opp.getId())
                .noticeId(opp.getNoticeId())
                .solicitationNumber(opp.getSolicitationNumber())
                .title(opp.getTitle())
                .type(opp.getType())
                .organizationName(opp.getOrganizationName())
                .postedDate(opp.getPostedDate())
                .responseDeadline(opp.getResponseDeadline())
                .active(opp.getActive())
                .visibility(opp.getVisibility())
                .uiLink(opp.getUiLink())
                .attachmentCount(attachmentCount);

        if (analysis != null) {
            builder.analysisStatus(analysis.getStatus())
                    .analyzedAt(analysis.getAnalyzedAt())
                    .summaryJson(analysis.getSummaryJson())
                    .documentFormatsJson(analysis.getDocumentFormatsJson())
                    .requiredDocumentsJson(analysis.getRequiredDocumentsJson());
        }

        return builder.build();
    }

    public static OpportunityAdminDto fromList(Opportunity opp, long attachmentCount, OpportunityAnalysis analysis) {
        OpportunityAdminDtoBuilder builder = OpportunityAdminDto.builder()
                .id(opp.getId())
                .noticeId(opp.getNoticeId())
                .solicitationNumber(opp.getSolicitationNumber())
                .title(opp.getTitle())
                .organizationName(opp.getOrganizationName())
                .responseDeadline(opp.getResponseDeadline())
                .visibility(opp.getVisibility())
                .attachmentCount(attachmentCount);

        if (analysis != null) {
            builder.analysisStatus(analysis.getStatus());
        }

        return builder.build();
    }
}
