package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.rfp.entity.IndustryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IndustryClassifier 단위 테스트 (CR-014, BIZ-018).
 * 우선순위(NAICS > PSC > title), 키워드 폴백, 미분류(null) 검증.
 */
class IndustryClassifierTest {

    private final IndustryClassifier classifier = new IndustryClassifier();

    @Test
    @DisplayName("NAICS코드_조경561730_GROUND_MAINTENANCE")
    void naics_조경_groundMaintenance() {
        IndustryType result = classifier.classify("561730", null, "irrelevant title");
        assertThat(result).isEqualTo(IndustryType.GROUND_MAINTENANCE);
    }

    @Test
    @DisplayName("NAICS코드_세탁812332_LAUNDRY")
    void naics_세탁_laundry() {
        IndustryType result = classifier.classify("812332", null, null);
        assertThat(result).isEqualTo(IndustryType.LAUNDRY);
    }

    @Test
    @DisplayName("PSC코드_S201_CUSTODIAL_NAICS없을때")
    void psc_청소_custodial() {
        IndustryType result = classifier.classify(null, "S201", null);
        assertThat(result).isEqualTo(IndustryType.CUSTODIAL);
    }

    @Test
    @DisplayName("title키워드_hvac_HVAC_NAICS와PSC없을때")
    void title_hvac_fallback() {
        IndustryType result = classifier.classify(null, null,
                "Heating, Ventilating and Air Conditioning (HVAC) Duct Service");
        assertThat(result).isEqualTo(IndustryType.HVAC);
    }

    @Test
    @DisplayName("title키워드_trash_WASTE")
    void title_waste_fallback() {
        IndustryType result = classifier.classify(null, null,
                "Trash Removal and Food Waste Collection at Chinhae");
        assertThat(result).isEqualTo(IndustryType.WASTE);
    }

    @Test
    @DisplayName("우선순위_NAICS가PSC와title보다우선")
    void priority_naics_over_psc_and_title() {
        // NAICS=HVAC, PSC=청소, title=세탁 → NAICS 채택
        IndustryType result = classifier.classify("238220", "S201", "laundry service");
        assertThat(result).isEqualTo(IndustryType.HVAC);
    }

    @Test
    @DisplayName("우선순위_PSC가title보다우선_NAICS미스시")
    void priority_psc_over_title() {
        // NAICS 미스(미등록), PSC=청소, title=세탁 → PSC 채택
        IndustryType result = classifier.classify("999999", "S201", "laundry service");
        assertThat(result).isEqualTo(IndustryType.CUSTODIAL);
    }

    @Test
    @DisplayName("미매칭_전부미스_null반환")
    void unmatched_returnsNull() {
        IndustryType result = classifier.classify("999999", "Z999", "office supplies procurement");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("전부null입력_null반환")
    void allNull_returnsNull() {
        assertThat(classifier.classify(null, null, null)).isNull();
    }
}
