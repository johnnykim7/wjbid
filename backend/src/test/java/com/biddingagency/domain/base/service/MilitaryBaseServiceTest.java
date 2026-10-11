package com.biddingagency.domain.base.service;

import com.biddingagency.domain.base.entity.MilitaryBase;
import com.biddingagency.domain.base.repository.MilitaryBaseRepository;
import com.biddingagency.domain.base.repository.OpportunityTitleRepository;
import com.biddingagency.domain.base.repository.OpportunityTitleView;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.MarketRowAssembler;
import com.biddingagency.dto.BaseRequest;
import com.biddingagency.dto.BaseResponses.BaseStat;
import com.biddingagency.dto.BaseResponses.BaseStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

/**
 * TC-MKT-032 ~ TC-MKT-040 (CR-136): 기지 목록과 기지별 집계(BID-MKT-010, BID-MKT-011, BIZ-031).
 */
@ExtendWith(MockitoExtension.class)
class MilitaryBaseServiceTest {

    /** 시험 기준 시각 2026-10-11 10:00 KST */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime POSTED = LocalDateTime.of(2026, 9, 1, 0, 0);

    @Mock private MilitaryBaseRepository baseRepository;
    @Mock private OpportunityTitleRepository titleRepository;
    @Mock private OpportunityRepository opportunityRepository;
    @Mock private MarketRowAssembler rowAssembler;

    private MilitaryBaseService service;

    @BeforeEach
    void setUp() {
        service = new MilitaryBaseService(baseRepository, titleRepository, opportunityRepository, rowAssembler, NOW);
    }

    private static MilitaryBase base(String nameEn, String terms) {
        MilitaryBase b = MilitaryBase.builder().nameKo(nameEn).nameEn(nameEn).matchTerms(terms).sortOrder(0).build();
        ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
        return b;
    }

    private static OpportunityTitleView notice(String solicitationNumber, String title, String type) {
        return new OpportunityTitleView(UUID.randomUUID(), "N-" + UUID.randomUUID(), solicitationNumber, title, null,
                type, POSTED, null);
    }

    private static OpportunityTitleView bid(String solicitationNumber, String title) {
        return notice(solicitationNumber, title, "Solicitation");
    }

    private BaseStats stats(List<MilitaryBase> bases, OpportunityTitleView... titles) {
        given(baseRepository.findAllByOrderBySortOrderAscNameKoAsc()).willReturn(bases);
        given(titleRepository.findAllLive()).willReturn(Arrays.asList(titles));
        return service.stats();
    }

    private static int count(BaseStats s, String nameEn) {
        return s.bases().stream().filter(b -> b.nameEn().equals(nameEn)).findFirst().orElseThrow().opportunityCount();
    }

    @Test
    @DisplayName("TC-MKT-032 찾을 낱말이 다른 단어의 일부이면 걸리지 않는다")
    void 낱말이단어일부_기지매칭_안걸림() {
        BaseStats s = stats(List.of(base("Osan AB", "OSAN")), bid("S1", "OSANNA SUPPLY"));

        assertThat(count(s, "Osan AB")).isZero();
    }

    @Test
    @DisplayName("TC-MKT-033 대소문자가 달라도 낱말 단위로 맞으면 걸린다")
    void 대소문자다름_기지매칭_걸림() {
        BaseStats s = stats(List.of(base("Camp Carroll", "Camp Carroll")), bid("S1", "repair at CAMP CARROLL"));

        assertThat(count(s, "Camp Carroll")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-MKT-034 제목에 기지가 둘이면 두 기지에 하나씩 센다")
    void 제목에기지둘_기지집계_둘다셈() {
        BaseStats s = stats(List.of(base("USAG Humphreys", "HUMPHREYS"), base("Osan AB", "OSAN")),
                bid("S1", "USAG HUMPHREYS AND OSAN AB"));

        assertThat(s.bases()).extracting(BaseStat::nameEn, BaseStat::opportunityCount)
                .containsExactly(tuple("USAG Humphreys", 1), tuple("Osan AB", 1));
    }

    @Test
    @DisplayName("TC-MKT-035 같은 공고번호의 여러 판은 한 번만 센다")
    void 같은공고번호여러판_기지집계_한번셈() {
        BaseStats s = stats(List.of(base("Osan AB", "OSAN")),
                bid("S1", "OSAN AB repair"), bid("S1", "OSAN AB repair amendment 1"), bid("S1", "OSAN AB repair amendment 2"));

        assertThat(count(s, "Osan AB")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-MKT-036 낙찰 공지 제목에 기지가 없으면 같은 공고번호 입찰 공고의 기지로 센다")
    void 낙찰공지제목에기지없음_기지집계_같은공고번호공고로셈() {
        BaseStats s = stats(List.of(base("Osan AB", "OSAN AB")),
                bid("S1", "Repair at OSAN AB"), notice("S1", "Award for repair", "Award Notice"));

        assertThat(s.bases()).extracting(BaseStat::awardCount).containsExactly(1);
    }

    @Test
    @DisplayName("TC-MKT-037 기지 목록이 비어 있으면 빈 목록과 전체 공고 수만 낸다")
    void 기지목록0건_기지집계_빈목록과전체건수() {
        BaseStats s = stats(List.of(), bid("S1", "A"), bid("S2", "B"), bid("S3", "C"));

        assertThat(tuple(s.bases().size(), s.totalNumbers(), s.unmatchedNumbers())).isEqualTo(tuple(0, 3, 3));
    }

    @Test
    @DisplayName("TC-MKT-038 이미 있는 영문 이름으로 기지를 더하면 409 로 거부한다")
    void 영문이름겹침_기지추가_거부() {
        given(baseRepository.existsByNameEnIgnoreCase("Osan AB")).willReturn(true);

        ResponseStatusException e = catchThrowableOfType(() -> service.create(
                new BaseRequest("오산 공군기지", "Osan AB", List.of("OSAN"), null, null, null, null)),
                ResponseStatusException.class);

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("TC-MKT-039 찾을 낱말이 비어 있으면 400 으로 거부한다")
    void 찾을낱말비어있음_기지추가_거부() {
        ResponseStatusException e = catchThrowableOfType(() -> service.create(
                new BaseRequest("오산 공군기지", "Osan AB", List.of(" , "), null, null, null, null)),
                ResponseStatusException.class);

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-MKT-040 기지를 못 찾은 공고는 어느 낱말에도 걸리지 않은 제목만 낸다")
    void 못찾은공고_기지관리_낱말안걸린제목만() {
        given(baseRepository.findAllByOrderBySortOrderAscNameKoAsc()).willReturn(List.of(base("Osan AB", "OSAN")));
        given(titleRepository.findAllLive()).willReturn(List.of(
                bid("S1", "OSAN AB repair"), bid("S2", "Generator service"), bid("S3", "Fence work")));

        assertThat(service.unmatched().count()).isEqualTo(2);
    }
}
