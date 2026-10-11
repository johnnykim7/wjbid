package com.biddingagency.domain.opportunity.service;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import com.biddingagency.integration.samgov.client.SAMGovApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * CR-139: 수집 때 제목 번역 결과가 저장되는지. 수집의 비동기 스레드처럼 바깥 저장 단위 없이 부른다.
 * LLM 은 Mock 이다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OpportunityTranslationServiceTest {

    private static final String TITLE_KO = "KSC Truck Company 차량 정비 장비 구매";

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private LLMPlatformClient llmPlatformClient;
    private TransactionTemplate transactionTemplate;
    private OpportunityTranslationService service;
    private UUID id;

    @BeforeEach
    void setUp() {
        llmPlatformClient = mock(LLMPlatformClient.class);
        transactionTemplate = new TransactionTemplate(transactionManager);
        service = new OpportunityTranslationService(
                opportunityRepository, llmPlatformClient, mock(SAMGovApiClient.class), transactionTemplate);
        LocalDateTime at = LocalDateTime.of(2026, 10, 8, 18, 0);
        id = opportunityRepository.save(Opportunity.builder()
                .noticeId("N-CR139")
                .solicitationNumber("W91QVN-26-Q-0001")
                .title("Purchase of Vehicle Maintenance Equipment for KSC Truck Company")
                .type("Award Notice")
                .postedDate(at)
                .active(true)
                .firstSeenAt(at)
                .lastModifiedAt(at)
                .build()).getId();
    }

    @AfterEach
    void tearDown() {
        opportunityRepository.deleteAll();
    }

    private Opportunity reload() {
        return opportunityRepository.findById(id).orElseThrow();
    }

    @Test
    @DisplayName("수집번역_번역받음_한글제목저장됨")
    void 수집번역_번역받음_한글제목저장됨() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willReturn(TITLE_KO);

        service.translateAsync(id);

        assertThat(reload().getTitleKo()).isEqualTo(TITLE_KO);
    }

    @Test
    @DisplayName("수집번역_번역받음_번역시각저장됨")
    void 수집번역_번역받음_번역시각저장됨() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willReturn(TITLE_KO);

        service.translateAsync(id);

        assertThat(reload().getTranslatedAt()).isNotNull();
    }

    @Test
    @DisplayName("수집번역_번역받음_유형한글저장됨")
    void 수집번역_번역받음_유형한글저장됨() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willReturn(TITLE_KO);

        service.translateAsync(id);

        assertThat(reload().getTypeKo()).isNotBlank();
    }

    @Test
    @DisplayName("수집번역_번역중낙찰정보저장_낙찰정보남음")
    void 수집번역_번역중낙찰정보저장_낙찰정보남음() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willAnswer(inv -> {
            transactionTemplate.executeWithoutResult(s -> opportunityRepository.findById(id).orElseThrow()
                    .applyAward("ACME KOREA", "UEI000000001", new BigDecimal("1000.00"), "$1,000.00", null));
            return TITLE_KO;
        });

        service.translateAsync(id);

        assertThat(reload().getAwardeeName()).isEqualTo("ACME KOREA");
    }

    @Test
    @DisplayName("수집번역_번역중관리자가먼저채움_덮지않음")
    void 수집번역_번역중관리자가먼저채움_덮지않음() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willAnswer(inv -> {
            transactionTemplate.executeWithoutResult(s -> opportunityRepository.findById(id).orElseThrow()
                    .applyTitleTranslation("관리자가 고친 제목"));
            return TITLE_KO;
        });

        service.translateAsync(id);

        assertThat(reload().getTitleKo()).isEqualTo("관리자가 고친 제목");
    }

    @Test
    @DisplayName("수집번역_번역실패_한글제목비어있음")
    void 수집번역_번역실패_한글제목비어있음() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any()))
                .willThrow(new RuntimeException("cli_agent_offline"));

        service.translateAsync(id);

        assertThat(reload().getTitleKo()).isNull();
    }
}
