package com.biddingagency.integration.samgov.client;

import com.biddingagency.domain.opportunity.service.OpportunityService.UpsertResult;
import com.biddingagency.integration.samgov.client.AwardBackfillService.BackfillResult;
import com.biddingagency.integration.samgov.client.OpportunityCollectorService.IngestOutcome;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse.OpportunityData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * TC-MKT-023 ~ TC-MKT-026 (CR-136): 지난 낙찰 공지 다시 받기(BID-MKT-008). SAM 은 Mock 이다.
 */
@ExtendWith(MockitoExtension.class)
class AwardBackfillServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 2, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 11);

    @Mock private SAMGovApiClient samGovApiClient;
    @Mock private OpportunityCollectorService collectorService;

    @InjectMocks
    private AwardBackfillService service;

    private static OpportunityData awardNotice(String noticeId) {
        OpportunityData d = new OpportunityData();
        d.setNoticeId(noticeId);
        d.setType("Award Notice");
        d.setAward(Map.of("awardee", Map.of("name", "ABC CO", "ueiSAM", "U1"), "amount", "100"));
        return d;
    }

    private void samReturns(List<OpportunityData> page) {
        SAMOpportunityResponse r = new SAMOpportunityResponse();
        r.setTotalRecords(page.size());
        r.setOpportunitiesData(page);
        given(samGovApiClient.searchOpportunities(anyString(), eq(FROM), eq(TO), anyInt(), anyInt(), eq("a")))
                .willReturn(r);
        given(collectorService.organizationMatches(any(), anyString())).willReturn(true);
    }

    @Test
    @DisplayName("TC-MKT-023 이미 있는 낙찰 공지 2건이 오면 업체를 채운 행 2, 새로 넣은 행 0, SAM 호출 1")
    void 지난낙찰받기_있는공고_업체채움() {
        samReturns(List.of(awardNotice("N1"), awardNotice("N2")));
        given(collectorService.ingest(any())).willReturn(new IngestOutcome(UpsertResult.UNCHANGED, null, true));

        BackfillResult result = service.backfill(FROM, TO);

        assertThat(result).extracting(BackfillResult::filled, BackfillResult::created, BackfillResult::samCalls)
                .containsExactly(2, 0, 1);
    }

    @Test
    @DisplayName("TC-MKT-024 DB 에 없는 낙찰 공지는 받은 응답 그대로 새로 넣는다")
    void 지난낙찰받기_없는공고_새로넣음() {
        OpportunityData fresh = awardNotice("N9");
        samReturns(List.of(fresh));
        given(collectorService.ingest(fresh)).willReturn(new IngestOutcome(UpsertResult.NEW, null, false));

        BackfillResult result = service.backfill(FROM, TO);

        assertThat(result.created()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-MKT-025 SAM 이 한도로 막으면 502 로 실패하고 아무 행도 바꾸지 않는다")
    void SAM한도막힘_지난낙찰받기_실패와채움없음() {
        given(samGovApiClient.searchOpportunities(anyString(), eq(FROM), eq(TO), anyInt(), anyInt(), eq("a")))
                .willThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        ResponseStatusException e = catchThrowableOfType(() -> service.backfill(FROM, TO),
                ResponseStatusException.class);

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        then(collectorService).should(never()).ingest(any());
    }

    @Test
    @DisplayName("TC-MKT-026 시작일이 끝일보다 늦으면 400 으로 거부하고 SAM 을 부르지 않는다")
    void 기간뒤집힘_지난낙찰받기_거부() {
        ResponseStatusException e = catchThrowableOfType(
                () -> service.backfill(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 2, 1)),
                ResponseStatusException.class);

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(samGovApiClient);
    }
}
