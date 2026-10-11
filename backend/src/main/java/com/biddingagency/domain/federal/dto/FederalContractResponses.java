package com.biddingagency.domain.federal.dto;

import com.biddingagency.domain.federal.entity.FederalAwardType;
import com.biddingagency.domain.federal.entity.FederalSyncMode;
import com.biddingagency.domain.federal.entity.FederalSyncStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 미 연방 계약 자료 응답 (CR-142, T3-2 B-5, C-4, C-5).
 */
public final class FederalContractResponses {

    private FederalContractResponses() {
    }

    /**
     * 자료 기준 (네 응답 공통)
     *
     * @param lastSyncedAt 마지막으로 성공한 받기의 끝난 시각(+09:00). 한 번도 없으면 null
     * @param lagDays      늦음 일수(POL-036). 비었으면 null
     */
    public record Source(long totalCount, OffsetDateTime lastSyncedAt, Integer lagDays, LocalDate dataFrom) {
    }

    /**
     * 계약 줄
     *
     * @param publicNoticeId 원래 공고번호의 공개 한글 공고문 id. 있으면 화면이 공고 상세로 보낸다
     */
    public record ContractRow(UUID id, String piid, String parentPiid, FederalAwardType awardType,
                              String description, String recipientName, BigDecimal amountUsd,
                              String naicsCode, String naicsName, LocalDate startDate, LocalDate endDate,
                              LocalDate potentialEndDate, String solicitationNumber, UUID publicNoticeId,
                              String usaspendingUrl) {
    }

    public enum RangeGroup { STANDALONE, ORDER }

    /** 범위 한 줄. count 가 minCount 미만이면 다섯 값은 null */
    public record RangeRow(RangeGroup group, int count, int excludedCount, int minCount, BigDecimal min,
                           BigDecimal p25, BigDecimal p50, BigDecimal p75, BigDecimal max) {
    }

    /** noticeNumber, noticeTitle 은 공고번호로 찾았을 때만 */
    public record AwardRangeResponse(String naicsCode, String naicsName, String noticeNumber, String noticeTitle,
                                     List<RangeRow> rows, List<ContractRow> evidence, Source source) {
    }

    public record NaicsOption(String code, String name, long count) {
    }

    public record NaicsResponse(List<NaicsOption> items, Source source) {
    }

    public record SyncRunRow(UUID id, FederalSyncMode mode, FederalSyncStatus status, LocalDate modifiedSince,
                             LocalDateTime startedAt, LocalDateTime finishedAt, int searchCalls, int detailCalls,
                             int insertedCount, int updatedCount, String errorMessage, String triggeredBy) {
    }

    public record SyncStarted(UUID runId, FederalSyncMode mode) {
    }
}
