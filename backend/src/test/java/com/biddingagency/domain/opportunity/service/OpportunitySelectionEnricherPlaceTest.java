package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.samgov.client.SAMGovApiClient;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse.OpportunityData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * TC-OPP-029 ~ TC-OPP-031 (CR-191): 짧은 수행 장소는 SAM 수행 장소의 도시와 주만 쓴다(BIZ-103).
 */
@ExtendWith(MockitoExtension.class)
class OpportunitySelectionEnricherPlaceTest {

    private static final Map<String, Object> APO_OFFICE = Map.of("city", "APO", "state", "AP", "zipcode", "96271-5289");

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private SamSetAsideKoMapper setAsideMapper;
    @Mock private NaicsTopLevelKoMapper naicsMapper;
    @Mock private SAMGovApiClient samGovApiClient;
    @Mock private OpportunityTranslationService translationService;

    @InjectMocks
    private OpportunitySelectionEnricher enricher;

    private String placeAfterMeta(Map<String, Object> pop, Map<String, Object> office) {
        Opportunity o = Opportunity.builder().noticeId("N1").title("t").active(true).build();
        ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
        given(opportunityRepository.findById(o.getId())).willReturn(Optional.of(o));
        OpportunityData data = new OpportunityData();
        data.setPlaceOfPerformance(pop);
        data.setOfficeAddress(office);

        enricher.applyMeta(o.getId(), data);

        return o.getPlaceOfPerformanceShort();
    }

    @Test
    @DisplayName("TC-OPP-029 수행장소도시주없음_계약사무소APO_비움")
    void 수행장소도시주없음_계약사무소APO_비움() {
        assertThat(placeAfterMeta(Map.of("streetAddress", "Vehicle Maintenance for Daegu", "zip", ""), APO_OFFICE))
                .isNull();
    }

    @Test
    @DisplayName("TC-OPP-030 수행장소군사우편_비움")
    void 수행장소군사우편_비움() {
        assertThat(placeAfterMeta(Map.of("city", Map.of("name", "APO"), "state", Map.of("code", "AP")), null))
                .isNull();
    }

    @Test
    @DisplayName("TC-OPP-031 수행장소도시주있음_그값")
    void 수행장소도시주있음_그값() {
        assertThat(placeAfterMeta(Map.of("city", Map.of("name", "Pyeongtaek"), "state", Map.of("name", "Gyeonggi")),
                APO_OFFICE)).isEqualTo("Pyeongtaek, Gyeonggi");
    }
}
