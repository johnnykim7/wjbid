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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TC-OPP-006 ~ TC-OPP-011 (CR-139): 수집 때 제목 번역 결과가 저장되는지. 수집의 비동기 스레드처럼 바깥 저장 단위 없이 부른다.
 * TC-OPP-015 ~ TC-OPP-019 (CR-155): 수집 때 본문을 받아 한글로 옮기는지.
 * LLM 과 SAM 본문 조회는 Mock 이다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OpportunityTranslationServiceTest {

    private static final String TITLE_KO = "KSC Truck Company 차량 정비 장비 구매";
    private static final String BODY_LINK = "https://api.sam.gov/prod/opportunities/v1/noticedesc?noticeid=N-CR155";

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private LLMPlatformClient llmPlatformClient;
    private SAMGovApiClient samGovApiClient;
    private TransactionTemplate transactionTemplate;
    private OpportunityTranslationService service;
    private UUID id;

    @BeforeEach
    void setUp() {
        llmPlatformClient = mock(LLMPlatformClient.class);
        samGovApiClient = mock(SAMGovApiClient.class);
        transactionTemplate = new TransactionTemplate(transactionManager);
        service = new OpportunityTranslationService(
                opportunityRepository, llmPlatformClient, samGovApiClient, transactionTemplate);
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

    private Opportunity reload(UUID oppId) {
        return opportunityRepository.findById(oppId).orElseThrow();
    }

    /** 원본 JSON 본문 자리가 SAM 본문 주소인 새 공고 (CR-155) */
    private UUID saveWithBodyLink(String body, String bodyKo) {
        LocalDateTime at = LocalDateTime.of(2026, 10, 12, 9, 0);
        return opportunityRepository.save(Opportunity.builder()
                .noticeId("N-CR155")
                .solicitationNumber("W91QVN26RAA86")
                .title("Grounds Maintenance")
                .type("Solicitation")
                .rawJson(Map.of("description", BODY_LINK))
                .descriptionBody(body)
                .descriptionSummaryKo(bodyKo)
                .postedDate(at)
                .active(true)
                .firstSeenAt(at)
                .lastModifiedAt(at)
                .build()).getId();
    }

    // TC-OPP-006
    @Test
    @DisplayName("수집번역_번역받음_한글제목저장됨")
    void 수집번역_번역받음_한글제목저장됨() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willReturn(TITLE_KO);

        service.translateAsync(id);

        assertThat(reload().getTitleKo()).isEqualTo(TITLE_KO);
    }

    // TC-OPP-007
    @Test
    @DisplayName("수집번역_번역받음_번역시각저장됨")
    void 수집번역_번역받음_번역시각저장됨() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willReturn(TITLE_KO);

        service.translateAsync(id);

        assertThat(reload().getTranslatedAt()).isNotNull();
    }

    // TC-OPP-008
    @Test
    @DisplayName("수집번역_번역받음_유형한글저장됨")
    void 수집번역_번역받음_유형한글저장됨() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willReturn(TITLE_KO);

        service.translateAsync(id);

        assertThat(reload().getTypeKo()).isNotBlank();
    }

    // TC-OPP-009
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

    // TC-OPP-010
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

    // TC-OPP-011
    @Test
    @DisplayName("수집번역_번역실패_한글제목비어있음")
    void 수집번역_번역실패_한글제목비어있음() {
        given(llmPlatformClient.translateOpportunityTitle(any(), any()))
                .willThrow(new RuntimeException("cli_agent_offline"));

        service.translateAsync(id);

        assertThat(reload().getTitleKo()).isNull();
    }

    // TC-OPP-015
    @Test
    @DisplayName("수집번역_본문받음_원문과한글본문저장됨")
    void 수집번역_본문받음_원문과한글본문저장됨() {
        UUID oppId = saveWithBodyLink(null, null);
        given(samGovApiClient.fetchNoticeDescription(BODY_LINK)).willReturn("Scope of work");
        given(llmPlatformClient.translateOpportunityDescription(any(), any())).willReturn("작업 범위");

        service.translateAsync(oppId);

        Opportunity saved = reload(oppId);
        assertThat(new String[]{saved.getDescriptionBody(), saved.getDescriptionSummaryKo()})
                .containsExactly("Scope of work", "작업 범위");
    }

    // TC-OPP-016
    @Test
    @DisplayName("수집번역_본문받기실패_한글제목은저장됨")
    void 수집번역_본문받기실패_한글제목은저장됨() {
        UUID oppId = saveWithBodyLink(null, null);
        given(llmPlatformClient.translateOpportunityTitle(any(), any())).willReturn("조경 관리");
        given(samGovApiClient.fetchNoticeDescription(anyString())).willThrow(new RuntimeException("throttled"));

        service.translateAsync(oppId);

        Opportunity saved = reload(oppId);
        assertThat(new String[]{saved.getTitleKo(), saved.getDescriptionBody(), saved.getDescriptionSummaryKo()})
                .containsExactly("조경 관리", null, null);
    }

    // TC-OPP-017
    @Test
    @DisplayName("수집번역_본문번역실패_원문본문은남음")
    void 수집번역_본문번역실패_원문본문은남음() {
        UUID oppId = saveWithBodyLink(null, null);
        given(samGovApiClient.fetchNoticeDescription(BODY_LINK)).willReturn("Scope of work");
        given(llmPlatformClient.translateOpportunityDescription(any(), any()))
                .willThrow(new RuntimeException("cli_agent_offline"));

        service.translateAsync(oppId);

        Opportunity saved = reload(oppId);
        assertThat(new String[]{saved.getDescriptionBody(), saved.getDescriptionSummaryKo()})
                .containsExactly("Scope of work", null);
    }

    // TC-OPP-018
    @Test
    @DisplayName("수집번역_번역중관리자가한글본문채움_덮지않음")
    void 수집번역_번역중관리자가한글본문채움_덮지않음() {
        UUID oppId = saveWithBodyLink(null, null);
        given(samGovApiClient.fetchNoticeDescription(BODY_LINK)).willReturn("Scope of work");
        given(llmPlatformClient.translateOpportunityDescription(any(), any())).willAnswer(inv -> {
            transactionTemplate.executeWithoutResult(s -> opportunityRepository.findById(oppId).orElseThrow()
                    .applyDescriptionTranslation("관리자가 옮긴 본문"));
            return "작업 범위";
        });

        service.translateAsync(oppId);

        assertThat(reload(oppId).getDescriptionSummaryKo()).isEqualTo("관리자가 옮긴 본문");
    }

    // TC-OPP-019
    @Test
    @DisplayName("수집번역_원문과한글본문이미있음_본문다시받지않음")
    void 수집번역_원문과한글본문이미있음_본문다시받지않음() {
        UUID oppId = saveWithBodyLink("Scope of work", "작업 범위");

        service.translateAsync(oppId);

        verify(samGovApiClient, never()).fetchNoticeDescription(anyString());
        verify(llmPlatformClient, never()).translateOpportunityDescription(any(), any());
    }
}
