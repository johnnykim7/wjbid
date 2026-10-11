package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-MKT-018 (CR-135): 메뉴 속 화면의 목록과 집계는 모두 이 두 조회로 모은 공고를 읽는다. 지운 공고는 조회에서 빠진다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class OpportunityRepositoryTest {

    @Autowired
    private OpportunityRepository opportunityRepository;

    private Opportunity kept;

    @BeforeEach
    void setUp() {
        kept = opportunityRepository.save(opportunity("N-KEPT"));
        Opportunity deleted = opportunity("N-DELETED");
        deleted.softDelete();
        opportunityRepository.saveAndFlush(deleted);
    }

    private static Opportunity opportunity(String noticeId) {
        LocalDateTime at = LocalDateTime.of(2026, 10, 1, 9, 0);
        return Opportunity.builder()
                .noticeId(noticeId)
                .solicitationNumber("W1")
                .title("title " + noticeId)
                .type("Solicitation")
                .postedDate(at)
                .active(true)
                .firstSeenAt(at)
                .lastModifiedAt(at)
                .build();
    }

    // TC-MKT-018
    @Test
    @DisplayName("지운공고_모은공고조회_빠짐")
    void 지운공고_모은공고조회_빠짐() {
        List<MarketSource> sources = opportunityRepository.findMarketSources();

        assertThat(sources).extracting(MarketSource::id).containsExactly(kept.getId());
    }

    // TC-MKT-018
    @Test
    @DisplayName("지운공고_공고번호조회_빠짐")
    void 지운공고_공고번호조회_빠짐() {
        List<MarketSource> sources = opportunityRepository.findMarketSourcesBySolicitationNumbers(Set.of("W1"));

        assertThat(sources).extracting(MarketSource::id).containsExactly(kept.getId());
    }
}
