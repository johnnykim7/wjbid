package com.biddingagency.integration.samgov.client;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.service.IndustryClassifier;
import com.biddingagency.domain.opportunity.service.OpportunitySelectionEnricher;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.domain.opportunity.service.OpportunityService.UpsertOutcome;
import com.biddingagency.domain.opportunity.service.OpportunityService.UpsertResult;
import com.biddingagency.domain.opportunity.service.OpportunityTranslationService;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse.OpportunityData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * TC-OPP-020 (CR-164): 번역 대기 칸이 차도 공고 넣기는 낙찰 정보 저장까지 이어 간다(BIZ-070).
 */
@ExtendWith(MockitoExtension.class)
class OpportunityCollectorServiceIngestTest {

    @Mock private SAMGovApiClient samGovApiClient;
    @Mock private OpportunityService opportunityService;
    @Mock private IndustryClassifier industryClassifier;
    @Mock private OpportunityTranslationService translationService;
    @Mock private OpportunitySelectionEnricher selectionEnricher;

    @InjectMocks
    private OpportunityCollectorService service;

    @Test
    @DisplayName("TC-OPP-020 번역대기칸참_낙찰정보는저장됨")
    void 번역대기칸참_낙찰정보는저장됨() {
        Map<String, Object> award = Map.of("awardee", Map.of("name", "ABC CO"), "amount", "100");
        OpportunityData data = new OpportunityData();
        data.setNoticeId("N-1");
        data.setType("Award Notice");
        data.setAward(award);
        Opportunity saved = Opportunity.builder().noticeId("N-1").title("t").active(true).build();
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(saved, "id", id);
        given(opportunityService.existsByNoticeId("N-1")).willReturn(false);
        given(opportunityService.existsLiveByContentHash(anyString())).willReturn(false);
        given(opportunityService.createOrUpdate(eq("N-1"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new UpsertOutcome(saved, UpsertResult.NEW));
        willThrow(new TaskRejectedException("full")).given(translationService).translateAsync(id);

        service.ingest(data);

        then(selectionEnricher).should().applyAward(id, award);
    }
}
