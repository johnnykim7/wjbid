package com.biddingagency.integration.samgov.scheduler;

import com.biddingagency.domain.collection.entity.CollectorRun;
import com.biddingagency.domain.collection.repository.CollectorRunRepository;
import com.biddingagency.domain.event.CollectionFailedEvent;
import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.integration.samgov.client.OpportunityCollectorService;
import com.biddingagency.integration.samgov.quota.SamApiCallLog;
import com.biddingagency.integration.samgov.quota.SamApiCallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

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
    @Mock
    private CollectorRunRepository collectorRunRepository;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T06:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final UUID RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000163");

    private OpportunityCollectionScheduler scheduler;

    @BeforeEach
    void enableScheduler() {
        scheduler = new OpportunityCollectionScheduler(
                collectorService, eventPublisher, samApiCallLogRepository, collectorRunRepository, CLOCK);
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        // 저장할 때 ID 가 붙는 것을 흉내 낸다. 시작과 끝에 같은 회차를 두 번 저장한다
        lenient().when(collectorRunRepository.save(any(CollectorRun.class))).thenAnswer(inv -> {
            CollectorRun run = inv.getArgument(0);
            if (run.getId() == null) ReflectionTestUtils.setField(run, "id", RUN_ID);
            return run;
        });
    }

    private CollectorRun savedRun() {
        ArgumentCaptor<CollectorRun> captor = ArgumentCaptor.forClass(CollectorRun.class);
        then(collectorRunRepository).should(times(2)).save(captor.capture());
        return captor.getValue();
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

    // CR-163: 수집 회차가 수집 이력에 남지 않아 수집 이력 조회가 늘 비고, 수집 완료 메일 멱등키가 COLLECTION_null 로 고정됐다

    @Test
    @DisplayName("스케줄수집_오류0건_성공회차로남음")
    void 스케줄수집_오류0건_성공회차로남음() {
        givenCollectionResult(5, 3, 0);
        givenTodaySearchLog(200, null);

        scheduler.collectOpportunities();

        CollectorRun run = savedRun();
        assertThat(run.getSuccess()).isTrue();
        assertThat(run.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 15, 15, 0));
        assertThat(run.getTriggeredBy()).isEqualTo("SCHEDULED");
        assertThat(run.getNewCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("스케줄수집_오류있음_실패회차로남음")
    void 스케줄수집_오류있음_실패회차로남음() {
        givenCollectionResult(0, 0, 1);
        givenTodaySearchLog(401, null);

        scheduler.collectOpportunities();

        CollectorRun run = savedRun();
        assertThat(run.getSuccess()).isFalse();
        assertThat(run.getCompletedAt()).isNotNull();
        assertThat(run.getErrorMessage()).isEqualTo("errors=1");
    }

    @Test
    @DisplayName("스케줄수집_완료이벤트에_회차ID실림")
    void 스케줄수집_완료이벤트에_회차ID실림() {
        givenCollectionResult(5, 3, 0);
        givenTodaySearchLog(200, null);

        scheduler.collectOpportunities();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(atLeastOnce()).publishEvent(captor.capture());
        OpportunitiesCollectedEvent event = captor.getAllValues().stream()
                .filter(OpportunitiesCollectedEvent.class::isInstance)
                .map(OpportunitiesCollectedEvent.class::cast)
                .findFirst().orElseThrow();
        assertThat(event.getEntityId()).isEqualTo(RUN_ID);
    }

    @Test
    @DisplayName("수동수집_MANUAL회차로남음")
    void 수동수집_MANUAL회차로남음() {
        givenCollectionResult(2, 1, 0);

        scheduler.triggerManualCollection(7);

        CollectorRun run = savedRun();
        assertThat(run.getTriggeredBy()).isEqualTo("MANUAL");
        assertThat(run.getSuccess()).isTrue();
    }
}
