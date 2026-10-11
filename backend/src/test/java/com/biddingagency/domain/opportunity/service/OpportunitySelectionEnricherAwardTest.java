package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.samgov.client.SAMGovApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

/**
 * TC-MKT-019 ~ TC-MKT-022 (CR-136): 수집 때 낙찰 정보에서 업체 이름, UEI, 금액 숫자를 저장한다(BID-MKT-007).
 */
@ExtendWith(MockitoExtension.class)
class OpportunitySelectionEnricherAwardTest {

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private SamSetAsideKoMapper setAsideMapper;
    @Mock private NaicsTopLevelKoMapper naicsMapper;
    @Mock private SAMGovApiClient samGovApiClient;
    @Mock private OpportunityTranslationService translationService;

    @InjectMocks
    private OpportunitySelectionEnricher enricher;

    private static Opportunity opportunity(String awardAmount, String awardeeName, String awardeeUei, BigDecimal amountUsd) {
        Opportunity o = Opportunity.builder()
                .noticeId("N1").type("Award Notice")
                .awardAmount(awardAmount).awardeeName(awardeeName).awardeeUei(awardeeUei).awardAmountUsd(amountUsd)
                .build();
        ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
        return o;
    }

    private Opportunity stored(String awardAmount, String awardeeName, String awardeeUei, BigDecimal amountUsd) {
        Opportunity o = opportunity(awardAmount, awardeeName, awardeeUei, amountUsd);
        given(opportunityRepository.findById(o.getId())).willReturn(Optional.of(o));
        return o;
    }

    private static Map<String, Object> award(String name, String uei, String amount) {
        return Map.of("awardee", Map.of("name", name, "ueiSAM", uei), "amount", amount);
    }

    @Test
    @DisplayName("TC-MKT-019 새 공고의 낙찰 정보에서 업체 이름, UEI, 금액 숫자를 저장하고 금액 표시 문자열은 그대로 둔다")
    void 낙찰정보있음_새공고수집_업체이름UEI금액숫자저장() {
        Opportunity o = stored("$443,466.20", null, null, null);

        enricher.applyAward(o.getId(), award("ABC CO", "U1", "443466.20"));

        assertThat(o).extracting(Opportunity::getAwardeeName, Opportunity::getAwardeeUei,
                        Opportunity::getAwardAmountUsd, Opportunity::getAwardAmount)
                .containsExactly("ABC CO", "U1", new BigDecimal("443466.20"), "$443,466.20");
    }

    @Test
    @DisplayName("TC-MKT-020 이미 있는 공고가 낙찰 정보와 함께 다시 오면 빈 낙찰 칸을 채운다")
    void 이미있는공고다시옴_수집_낙찰정보채움() {
        Opportunity o = stored(null, null, null, null);

        boolean changed = enricher.applyAward(o.getId(), award("ABC CO", "U1", "100"));

        assertThat(tuple(changed, o.getAwardeeName(), o.getAwardeeUei(), o.getAwardAmountUsd()))
                .isEqualTo(tuple(true, "ABC CO", "U1", new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("TC-MKT-021 다시 온 응답에 낙찰 정보가 없으면 있던 낙찰 칸을 그대로 둔다")
    void 다시온응답에낙찰정보없음_수집_기존값유지() {
        Opportunity o = opportunity("$100.00", "ABC CO", "U1", new BigDecimal("100.00"));

        boolean changed = enricher.applyAward(o.getId(), null);

        assertThat(tuple(changed, o.getAwardeeName(), o.getAwardeeUei(), o.getAwardAmountUsd()))
                .isEqualTo(tuple(false, "ABC CO", "U1", new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("TC-MKT-022 금액이 숫자가 아니면 금액 숫자 칸은 비우고 업체 칸은 저장한다")
    void 금액숫자아님_수집_숫자칸비움() {
        Opportunity o = stored(null, null, null, null);

        enricher.applyAward(o.getId(), award("ABC CO", "U1", "N/A"));

        assertThat(tuple(o.getAwardAmountUsd(), o.getAwardeeName())).isEqualTo(tuple(null, "ABC CO"));
    }
}
