package com.biddingagency.integration.samgov.scheduler;

import com.biddingagency.domain.event.CollectionFailedEvent;
import com.biddingagency.integration.samgov.client.OpportunityCollectorService;
import com.biddingagency.integration.samgov.quota.SamApiCallLog;
import com.biddingagency.integration.samgov.quota.SamApiCallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-126: 수집 실패 감지 테스트.
 *
 * <p>배경: 2026-08-03 SAM API 키 만료 후 9일간 수집이 401로 전패했으나 알림이 없었다.
 * 성공 이벤트의 소비자가 "타깃 신규 0건"이면 발송을 스킵하는 탓에(CR-015),
 * 인증 실패로 0건인 상황이 정상 0건에 묻혔기 때문이다.
 */
@ExtendWith(MockitoExtension.class)
class OpportunityCollectionSchedulerTest {

    @Mock
    private OpportunityCollectorService collectorService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SamApiCallLogRepository samApiCallLogRepository;

    @InjectMocks
    private OpportunityCollectionScheduler scheduler;

    @BeforeEach
    void enableScheduler() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
    }

    /** 수집 결과: 신규/타깃/변경/무변경/중복/오류 */
    private void givenCollectionResult(int collected, int targetNew, int errors) {
        given(collectorService.collectByKeyword(anyString(), anyInt()))
                .willReturn(new OpportunityCollectorService.CollectionResult(
                        collected, targetNew, 0, 0, 0, errors));
    }

    /** SamApiCallLog는 기본 생성자가 PROTECTED(JPA 전용)라 mock으로 세운다. */
    private void givenTodaySearchLog(Integer lastStatus, String errorBody) {
        SamApiCallLog logRow = mock(SamApiCallLog.class);
        given(logRow.getLastStatus()).willReturn(lastStatus);
        lenient().when(logRow.getLastErrorBody()).thenReturn(errorBody);
        given(samApiCallLogRepository.findByCallDateAndEndpoint(any(LocalDate.class), eq("search")))
                .willReturn(Optional.of(logRow));
    }

    private CollectionFailedEvent captureFailedEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(CollectionFailedEvent.class::isInstance)
                .map(CollectionFailedEvent.class::cast)
                .findFirst().orElse(null);
    }

    @Test
    @DisplayName("수집중_401발생_실패이벤트발행됨")
    void 수집중_401발생_실패이벤트발행() {
        // given: 키 만료 — 수집은 0건이고 계측에 401이 남는다
        givenCollectionResult(0, 0, 1);
        givenTodaySearchLog(401, "{\"code\":\"900901\",\"message\":\"Invalid Credentials\"}");

        // when
        scheduler.collectOpportunities();

        // then
        CollectionFailedEvent event = captureFailedEvent();
        assertThat(event).isNotNull();
        assertThat(event.isAuthFailure()).isTrue();
        assertThat(event.getLastStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("수집정상_신규0건_실패알림발행안됨")
    void 수집정상_신규0건_실패이벤트미발행() {
        // given: 이번 사건의 회귀 테스트 —
        // "신규 0건"은 실패가 아니다(그날 공고가 없었을 뿐). 200 정상이고 오류도 없다.
        givenCollectionResult(0, 0, 0);
        givenTodaySearchLog(200, null);

        // when
        scheduler.collectOpportunities();

        // then: 실패 이벤트는 나가지 않는다 (성공 이벤트만 발행)
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .noneMatch(CollectionFailedEvent.class::isInstance);
    }

    @Test
    @DisplayName("수집중_네트워크오류_authFailure아님으로발행됨")
    void 수집중_status없음_일반오류로발행() {
        // given: UnknownHostException 등 — 응답을 못 받아 계측 행이 없다
        givenCollectionResult(0, 0, 1);
        given(samApiCallLogRepository.findByCallDateAndEndpoint(any(LocalDate.class), eq("search")))
                .willReturn(Optional.empty());

        // when
        scheduler.collectOpportunities();

        // then: 발행은 되지만 키 만료로 단정하지 않는다
        CollectionFailedEvent event = captureFailedEvent();
        assertThat(event).isNotNull();
        assertThat(event.isAuthFailure()).isFalse();
        assertThat(event.getLastStatus()).isNull();
    }

    @Test
    @DisplayName("계측조회실패해도_실패알림은발행됨")
    void 계측조회예외_알림은발행() {
        // given: 계측 조회가 깨져도 알림 자체는 나가야 한다(그게 이 기능의 목적)
        givenCollectionResult(0, 0, 2);
        given(samApiCallLogRepository.findByCallDateAndEndpoint(any(LocalDate.class), eq("search")))
                .willThrow(new RuntimeException("DB down"));

        // when
        scheduler.collectOpportunities();

        // then
        CollectionFailedEvent event = captureFailedEvent();
        assertThat(event).isNotNull();
        assertThat(event.getErrorCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("수집성공_오류0건_실패이벤트미발행")
    void 수집성공_실패이벤트미발행() {
        // given: 정상 수집 (신규 5, 타깃 3)
        givenCollectionResult(5, 3, 0);
        givenTodaySearchLog(200, null);

        // when
        scheduler.collectOpportunities();

        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .noneMatch(CollectionFailedEvent.class::isInstance);
    }
}
