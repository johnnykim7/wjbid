package com.biddingagency.domain.analysis.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.analysis.service.AwardHistoryService.Criteria;
import com.biddingagency.domain.federal.entity.FederalContract;
import com.biddingagency.domain.federal.repository.FederalContractRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** TC-DOC-270~272: search_award_history (CR-170, BID-DOC-006) */
@ExtendWith(MockitoExtension.class)
class AwardHistoryServiceTest {

    private static final String NAICS = "561210";

    @Mock private FederalContractRepository contractRepository;
    @Mock private OpportunityRepository opportunityRepository;

    @InjectMocks
    private AwardHistoryService service;

    private static Criteria byNaics(Integer limit) {
        return new Criteria(List.of(NAICS), List.of(), null, null, null, null, null, limit);
    }

    private static FederalContract contract(int day) {
        return TestHelper.withId(FederalContract.builder()
                .piid("W91QVN-" + day).naicsCode(NAICS).startDate(LocalDate.of(2025, 1, 1).plusDays(day)).build());
    }

    private static Opportunity awardNotice(String naics) {
        return TestHelper.withId(Opportunity.builder()
                .type("Award Notice").title("Facility Support").solicitationNumber("W91QVN-25-R-0001")
                .postedDate(LocalDateTime.of(2025, 6, 1, 9, 0))
                .rawJson(Map.of("naicsCode", naics))
                .build());
    }

    @SuppressWarnings("unchecked")
    private void contractsAre(List<FederalContract> all) {
        given(contractRepository.findAll(any(Specification.class), any(Pageable.class))).willAnswer(inv -> {
            Pageable p = inv.getArgument(1);
            return new PageImpl<>(all.subList(0, Math.min(all.size(), p.getPageSize())), p, all.size());
        });
    }

    @Test
    @DisplayName("TC-DOC-270 낙찰이력_두출처합침")
    void 두출처합침() {
        contractsAre(List.of(contract(1), contract(2)));
        given(opportunityRepository.findByTypeNotDeleted("Award Notice"))
                .willReturn(List.of(awardNotice(NAICS), awardNotice("999999")));

        List<Map<String, Object>> rows = service.search(byNaics(null));

        assertThat(rows).extracting(r -> r.get("source"))
                .containsExactlyInAnyOrder("FEDERAL_CONTRACT", "FEDERAL_CONTRACT", "SAM_AWARD_NOTICE");
    }

    @Test
    @DisplayName("TC-DOC-271 낙찰이력_조건없음_거부")
    void 조건없음_거부() {
        Criteria empty = new Criteria(List.of(), List.of(), null, null, null, null, null, null);

        assertThatThrownBy(() -> service.search(empty)).isInstanceOf(IllegalArgumentException.class);
        then(contractRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("TC-DOC-272 낙찰이력_limit상한50")
    void limit상한50() {
        List<FederalContract> seventy = new ArrayList<>();
        for (int i = 0; i < 70; i++) seventy.add(contract(i));
        contractsAre(seventy);
        given(opportunityRepository.findByTypeNotDeleted("Award Notice")).willReturn(List.of());

        assertThat(service.search(byNaics(100))).hasSize(50);
    }
}
