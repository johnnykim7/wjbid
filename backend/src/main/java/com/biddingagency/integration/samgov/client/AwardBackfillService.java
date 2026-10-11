package com.biddingagency.integration.samgov.client;

import com.biddingagency.domain.opportunity.service.AwardInfo;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse;
import com.biddingagency.integration.samgov.scheduler.OpportunityCollectionScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CR-136: 지난 낙찰 공지 다시 받기(BID-MKT-008).
 *
 * 2026-10-11 까지는 낙찰 공지의 업체 이름을 버려, 이미 모은 낙찰 공지에 업체가 없다.
 * 관리자가 기간을 주어 한 번 돌리면 정기 수집과 같은 발주처 조건에 낙찰 공지 유형만 골라 SAM 을 검색하고,
 * 정기 수집과 같은 경로(ingest)로 넣거나 채운다. SAM 호출은 하루 한도를 쓰므로 검색 쪽 수만큼만 부른다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwardBackfillService {

    /** SAM 검색의 공고 유형 값 중 낙찰 공지 */
    private static final String PTYPE_AWARD = "a";
    /** SAM 검색 한 번의 최대 건수 */
    private static final int PAGE_LIMIT = 1000;

    private final SAMGovApiClient samGovApiClient;
    private final OpportunityCollectorService collectorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BackfillResult backfill(LocalDate postedFrom, LocalDate postedTo) {
        validate(postedFrom, postedTo);
        if (!running.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "지난 낙찰 공지 다시 받기가 이미 돌고 있습니다");
        }
        try {
            Counter c = new Counter();
            for (String keyword : OpportunityCollectionScheduler.MAIN_KEYWORDS) {
                fetchKeyword(keyword, postedFrom, postedTo, c);
            }
            BackfillResult result = new BackfillResult(c.samCalls, c.fetched, c.filled, c.created, c.withoutAwardee);
            log.info("[CR-136] 지난 낙찰 공지 다시 받기 {} ~ {}: {}", postedFrom, postedTo, result);
            return result;
        } finally {
            running.set(false);
        }
    }

    private void fetchKeyword(String keyword, LocalDate postedFrom, LocalDate postedTo, Counter c) {
        int offset = 0;
        while (true) {
            SAMOpportunityResponse response;
            try {
                c.samCalls++;
                response = samGovApiClient.searchOpportunities(keyword, postedFrom, postedTo, PAGE_LIMIT, offset, PTYPE_AWARD);
            } catch (RuntimeException e) {
                log.warn("[CR-136] SAM 검색 실패 keyword={}, offset={}: {}", keyword, offset, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SAM 검색 실패: " + e.getMessage());
            }
            var page = response.getOpportunitiesData();
            if (page == null || page.isEmpty()) return;
            for (SAMOpportunityResponse.OpportunityData data : page) {
                if (!AwardInfo.isAwardNotice(data.getType())) continue; // ptype 이 무시돼도 낙찰 공지만 다룬다
                if (!collectorService.organizationMatches(data, keyword)) continue;
                c.fetched++;
                if (AwardInfo.from(data.getAward()).awardeeName() == null) c.withoutAwardee++;
                try {
                    OpportunityCollectorService.IngestOutcome outcome = collectorService.ingest(data);
                    if (outcome.result() == OpportunityService.UpsertResult.NEW) {
                        c.created++;
                    } else if (outcome.awardChanged()) {
                        c.filled++;
                    }
                } catch (Exception e) {
                    log.error("[CR-136] 낙찰 공지 넣기 실패 noticeId={}", data.getNoticeId(), e);
                }
            }
            int total = response.getTotalRecords() != null ? response.getTotalRecords() : 0;
            offset += page.size();
            if (page.size() < PAGE_LIMIT || offset >= total) return;
        }
    }

    private void validate(LocalDate postedFrom, LocalDate postedTo) {
        if (postedFrom == null || postedTo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일과 끝일을 넣어 주세요");
        }
        if (postedFrom.isAfter(postedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일이 끝일보다 늦습니다");
        }
        // SAM 검색은 게시일 기간을 한 해 안으로만 받는다
        if (postedFrom.plusYears(1).isBefore(postedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기간은 한 해 안이어야 합니다");
        }
    }

    private static final class Counter {
        int samCalls;
        int fetched;
        int filled;
        int created;
        int withoutAwardee;
    }

    /**
     * @param samCalls       이번에 부른 SAM 검색 수
     * @param fetched        받은 낙찰 공지 수
     * @param filled         이미 있던 공지에 업체·금액을 채운 수
     * @param created        새로 넣은 수
     * @param withoutAwardee 업체 이름이 오지 않은 수
     */
    public record BackfillResult(int samCalls, int fetched, int filled, int created, int withoutAwardee) {
    }
}
