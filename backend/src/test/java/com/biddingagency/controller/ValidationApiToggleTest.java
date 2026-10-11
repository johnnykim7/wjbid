package com.biddingagency.controller;

import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * TC-OWN-012 ~ TC-OWN-013: 무인증 확인용 통로는 설정으로 켠 곳에만 뜬다 (CR-143)
 */
class ValidationApiToggleTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ValidationController.class)
            .withBean(BidRequestRepository.class, () -> mock(BidRequestRepository.class))
            .withBean(OpportunityRepository.class, () -> mock(OpportunityRepository.class))
            .withBean(BidDocumentRepository.class, () -> mock(BidDocumentRepository.class));

    @Test
    @DisplayName("TC-OWN-012 설정없음_확인용통로_없음")
    void 설정없음_통로없음() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ValidationController.class));
    }

    @Test
    @DisplayName("TC-OWN-013 설정켬_확인용통로_있음")
    void 설정켬_통로있음() {
        runner.withPropertyValues("app.validation-api.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(ValidationController.class));
    }
}
