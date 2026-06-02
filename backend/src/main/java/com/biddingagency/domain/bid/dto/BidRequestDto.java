package com.biddingagency.domain.bid.dto;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.StateTransition;
import com.biddingagency.domain.notice.entity.Notice;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BidRequestDto(
        String id,
        String opportunityId,
        /** CR-024: 노티 ID (고객 화면이 공고문 본문/필요서류 다시 조회할 때 사용). 노출 노티 없으면 null */
        String noticeId,
        /** opportunity.title (영문 원본). 한글 보이려면 displayTitle 사용 */
        String opportunityTitle,
        /** CR-024: notice.koreanTitle ?? opportunity.title — 고객 노출용 표시 타이틀 */
        String displayTitle,
        String solicitationNumber,
        String agencyName,
        String memberEmail,
        String state,
        String stateDisplay,
        String createdAt,
        String updatedAt,
        String submittedAt,
        List<StateTransition> history
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── admin 목록용: notice 매핑 없이(noticeId/displayTitle=null) memberEmail만 채움 ──
    public static BidRequestDto from(BidRequest br) {
        return build(br, null, null);
    }

    public static BidRequestDto withHistory(BidRequest br) {
        return build(br, null, br.getStateHistory());
    }

    // ── 고객용: notice 매핑 포함(noticeId/displayTitle 채움) ──
    public static BidRequestDto from(BidRequest br, Notice notice) {
        return build(br, notice, null);
    }

    public static BidRequestDto withHistory(BidRequest br, Notice notice) {
        return build(br, notice, br.getStateHistory());
    }

    private static BidRequestDto build(BidRequest br, Notice notice, List<StateTransition> history) {
        String englishTitle = br.getOpportunity() != null ? br.getOpportunity().getTitle() : null;
        String koreanTitle = notice != null ? notice.getKoreanTitle() : null;
        String display = (koreanTitle != null && !koreanTitle.isBlank()) ? koreanTitle : englishTitle;
        return new BidRequestDto(
                br.getId() != null ? br.getId().toString() : null,
                br.getOpportunity() != null ? br.getOpportunity().getId().toString() : null,
                notice != null && notice.getId() != null ? notice.getId().toString() : null,
                englishTitle,
                display,
                br.getOpportunity() != null ? br.getOpportunity().getSolicitationNumber() : null,
                br.getOpportunity() != null ? br.getOpportunity().getOrganizationName() : null,
                br.getMember() != null ? br.getMember().getEmail() : null,
                br.getState() != null ? br.getState().name() : null,
                br.getStateDisplay(),
                br.getCreatedAt() != null ? br.getCreatedAt().format(FMT) : null,
                br.getUpdatedAt() != null ? br.getUpdatedAt().format(FMT) : null,
                br.getSubmittedAt() != null ? br.getSubmittedAt().format(FMT) : null,
                history
        );
    }
}
