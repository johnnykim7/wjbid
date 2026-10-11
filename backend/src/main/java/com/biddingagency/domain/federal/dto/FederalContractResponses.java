package com.biddingagency.domain.federal.dto;

import com.biddingagency.domain.federal.entity.ContractDateKind;
import com.biddingagency.domain.federal.entity.FederalAwardType;
import com.biddingagency.domain.federal.entity.FederalSyncMode;
import com.biddingagency.domain.federal.entity.FederalSyncStatus;
import com.biddingagency.domain.federal.entity.MemberContractSource;
import com.biddingagency.domain.federal.entity.RecompeteCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
     * @param category       고른 달 기준 구분(BIZ-050). 달과 상관없는 목록이면 null
     */
    public record ContractRow(UUID id, String piid, String parentPiid, FederalAwardType awardType,
                              String description, String recipientName, BigDecimal amountUsd,
                              String naicsCode, String naicsName, LocalDate startDate, LocalDate endDate,
                              LocalDate potentialEndDate, String solicitationNumber, UUID publicNoticeId,
                              RecompeteCategory category, ContractDateKind basisDateKind, LocalDate basisDate,
                              boolean oneTimeConstruction, String usaspendingUrl) {
    }

    public record RecompeteResponse(String month, Map<RecompeteCategory, Integer> counts, List<ContractRow> items,
                                    int page, int size, long totalElements, Source source) {
    }

    public record CalendarMonth(String month, int RECOMPETE, int EXTENDABLE, int IN_PARENT, int UNCONFIRMED) {
    }

    public record CalendarResponse(List<CalendarMonth> months, Source source) {
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

    public record LookupResponse(boolean found, ContractRow contract, boolean alreadyRegistered) {
    }

    /**
     * 내 계약 한 줄
     *
     * @param nextAlertOn   다음에 메일이 나갈 날. 알림이 꺼져 있거나 남은 시점이 없으면 null
     * @param lastAlertedAt 마지막으로 보낸 메일 시각(한국 시각)
     */
    public record MemberContractRow(UUID id, String piid, String description, MemberContractSource source,
                                    LocalDate endDate, LocalDate potentialEndDate, Integer endDaysLeft,
                                    Integer potentialDaysLeft, LocalDate nextAlertOn, ContractDateKind nextAlertKind,
                                    LocalDate datesChangedOn, LocalDateTime lastAlertedAt,
                                    ContractRow federalContract) {
    }

    public record MemberContractsResponse(List<Integer> alertDays, LocalDate today, List<MemberContractRow> contracts,
                                          Source source) {
    }

    public record SyncRunRow(UUID id, FederalSyncMode mode, FederalSyncStatus status, LocalDate modifiedSince,
                             LocalDateTime startedAt, LocalDateTime finishedAt, int searchCalls, int detailCalls,
                             int insertedCount, int updatedCount, String errorMessage, String triggeredBy) {
    }

    public record SyncStarted(UUID runId, FederalSyncMode mode) {
    }
}
