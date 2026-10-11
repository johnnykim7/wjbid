package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.dto.AwardeeResponses.Awardee;
import com.biddingagency.domain.opportunity.dto.AwardeeResponses.AwardeesResponse;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.AwardeeService.AwardeeSort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

/**
 * TC-MKT-027 ~ TC-MKT-031 (CR-136): 낙찰 업체 집계(BID-MKT-009, BIZ-029).
 */
@ExtendWith(MockitoExtension.class)
class AwardeeServiceTest {

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private MarketRowAssembler rowAssembler;

    @InjectMocks
    private AwardeeService service;

    private static MarketSource award(String name, String uei, String amount, LocalDate posted) {
        return new MarketSource(UUID.randomUUID(), "N-" + UUID.randomUUID(), null, "Award", null,
                "Award Notice", null, posted.atStartOfDay(), null, null,
                amount == null ? null : new BigDecimal(amount), name, uei, (String) null);
    }

    private AwardeesResponse run(AwardeeSort sort, MarketSource... sources) {
        given(opportunityRepository.findMarketSources()).willReturn(Arrays.asList(sources));
        return service.awardees(null, sort);
    }

    @Test
    @DisplayName("TC-MKT-027 UEI 가 같으면 이름 표기가 달라도 한 업체로 묶는다")
    void 같은UEI다른표기_업체묶음_한업체() {
        AwardeesResponse r = run(AwardeeSort.COUNT,
                award("ABC Co.", "U1", null, LocalDate.of(2026, 5, 1)),
                award("ABC CO", "U1", null, LocalDate.of(2026, 8, 1)));

        assertThat(r.awardees()).extracting(Awardee::awardCount).containsExactly(2);
    }

    @Test
    @DisplayName("TC-MKT-028 UEI 가 없으면 대문자로 바꾸고 앞뒤 공백과 마침표, 쉼표를 뺀 이름으로 묶는다")
    void UEI없음_이름묶음_마침표쉼표공백무시() {
        AwardeesResponse r = run(AwardeeSort.COUNT,
                award("ABC, Inc.", null, null, LocalDate.of(2026, 5, 1)),
                award(" abc inc ", null, null, LocalDate.of(2026, 8, 1)));

        assertThat(r.awardees()).extracting(Awardee::key).containsExactly("NAME:ABC INC");
    }

    @Test
    @DisplayName("TC-MKT-029 같은 업체의 표기가 여럿이면 가장 최근 공지의 이름을 쓴다")
    void 표기여럿_업체이름_최근공지이름() {
        AwardeesResponse r = run(AwardeeSort.COUNT,
                award("ABC Co.", "U1", null, LocalDate.of(2026, 5, 1)),
                award("ABC CO", "U1", null, LocalDate.of(2026, 8, 1)));

        assertThat(r.awardees()).extracting(Awardee::name).containsExactly("ABC CO");
    }

    @Test
    @DisplayName("TC-MKT-030 업체 이름 없는 공지는 업체 목록에 넣지 않고 건수로만 밝힌다")
    void 업체이름없는공지_업체집계_건수만따로() {
        AwardeesResponse r = run(AwardeeSort.COUNT,
                award("ABC CO", "U1", null, LocalDate.of(2026, 5, 1)),
                award("ABC CO", "U1", null, LocalDate.of(2026, 6, 1)),
                award(null, null, null, LocalDate.of(2026, 7, 1)),
                award(null, null, null, LocalDate.of(2026, 8, 1)),
                award(null, null, null, LocalDate.of(2026, 9, 1)));

        assertThat(tuple(r.totalAwards(), r.awardsWithAwardee(), r.awardees().size()))
                .isEqualTo(tuple(5L, 2L, 1));
    }

    @Test
    @DisplayName("TC-MKT-031 금액순이면 금액 합계가 큰 업체부터, 금액 없는 업체는 맨 뒤")
    void 금액순_업체집계_금액없는업체맨뒤() {
        AwardeesResponse r = run(AwardeeSort.AMOUNT,
                award("A CO", "UA", "100", LocalDate.of(2026, 5, 1)),
                award("B CO", "UB", null, LocalDate.of(2026, 6, 1)),
                award("C CO", "UC", "300", LocalDate.of(2026, 7, 1)));

        assertThat(r.awardees()).extracting(Awardee::name).containsExactly("C CO", "A CO", "B CO");
    }
}
