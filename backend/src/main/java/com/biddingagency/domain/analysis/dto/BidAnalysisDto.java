package com.biddingagency.domain.analysis.dto;

import com.biddingagency.domain.analysis.entity.BidAnalysisStatus;
import com.biddingagency.domain.analysis.entity.BidDecision;
import com.biddingagency.domain.analysis.entity.BidVerdict;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 입찰 분석 관리자 응답 (CR-170, T3-2) */
public final class BidAnalysisDto {

    private BidAnalysisDto() {
    }

    /** analysis 는 회차가 없으면 비어 있다 */
    public record Overview(Analysis analysis, boolean readingInProgress) {
    }

    public record Person(UUID id, String name) {
    }

    public record Analysis(
            UUID analysisId,
            BidAnalysisStatus status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String failureReason,
            Object report,
            Object capturePlan,
            Object decision,
            List<Map<String, Object>> questions,
            BigDecimal pwinPercent,
            BidVerdict aiVerdict,
            String verdictReason,
            Object scores,
            Object strengths,
            Object weaknesses,
            Object issueRanking,
            BidDecision adminDecision,
            String adminNote,
            Person decidedBy,
            LocalDateTime decidedAt,
            Person startedBy
    ) {
    }
}
