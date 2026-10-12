package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TC-DEADLINE-001 ~ 003, 005 ~ 009 (CR-156): 입찰 요청 마감 알림 실행(BID-NOTIFY-002, POL-009).
 */
@ExtendWith(MockitoExtension.class)
class DeadlineAlertServiceTest {

    /** 시험 기준 시각 2026-10-12 09:00 KST */
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    private static final Clock KST_CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime TODAY_17H = LocalDateTime.of(2026, 10, 12, 17, 0);

    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private SystemSettingService systemSettingService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DeadlineAlertService service;

    @BeforeEach
    void setUp() {
        service = new DeadlineAlertService(bidRequestRepository, systemSettingService, eventPublisher, KST_CLOCK);
    }

    private void alertDays(String value) {
        given(systemSettingService.findValue(SettingKey.DEADLINE_ALERT_DAYS)).willReturn(Optional.ofNullable(value));
    }

    private BidRequest request(Opportunity opp) {
        BidRequest br = BidRequest.builder().opportunity(opp).state(BidRequestState.DOCS_PENDING).build();
        ReflectionTestUtils.setField(br, "id", UUID.randomUUID());
        return br;
    }

    private BidRequest request(LocalDateTime deadline) {
        return request(Opportunity.builder().title("USFK Facility").noticeId("W912-26-R-0001")
                .responseDeadline(deadline).build());
    }

    private void stored(BidRequest... requests) {
        given(bidRequestRepository.findByStateInWithOpportunity(DeadlineAlertService.ACTIVE_STATES))
                .willReturn(List.of(requests));
    }

    private DeadlineApproachingEvent publishedEvent() {
        ArgumentCaptor<DeadlineApproachingEvent> captor = ArgumentCaptor.forClass(DeadlineApproachingEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        return captor.getValue();
    }

    @ParameterizedTest(name = "마감 {0}일 전")
    @ValueSource(ints = {7, 3, 1})
    @DisplayName("TC-DEADLINE-001~003 마감7_3_1일전_예약작업_알림이벤트발행")
    void 마감7_3_1일전_예약작업_알림이벤트발행(int days) {
        alertDays("7,3,1");
        stored(request(TODAY_17H.plusDays(days)));

        service.publishDueAlerts();

        assertThat(publishedEvent().getDaysRemaining()).isEqualTo(days);
    }

    @ParameterizedTest(name = "마감 {0}일 전")
    @ValueSource(ints = {0, 2, 5, 8})
    @DisplayName("TC-DEADLINE-005 알림날이아닌날_예약작업_알림없음")
    void 알림날이아닌날_예약작업_알림없음(int days) {
        alertDays("7,3,1");
        stored(request(TODAY_17H.plusDays(days)));

        service.publishDueAlerts();

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("TC-DEADLINE-005 이벤트_입찰요청과마감시각_담김")
    void 마감7일전_예약작업_이벤트에요청과마감시각() {
        alertDays("7,3,1");
        BidRequest br = request(TODAY_17H.plusDays(7));
        stored(br);

        service.publishDueAlerts();

        DeadlineApproachingEvent event = publishedEvent();
        assertThat(List.of(event.getEntityId(), event.getDeadline())).containsExactly(br.getId(), TODAY_17H.plusDays(7));
    }

    @Test
    @DisplayName("TC-DEADLINE-006 공고없는요청_예약작업_건너뜀")
    void 공고없는요청_예약작업_건너뜀() {
        alertDays("7,3,1");
        stored(request((Opportunity) null), request(TODAY_17H.plusDays(3)));

        assertThat(service.publishDueAlerts()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-DEADLINE-006 마감일없는공고_예약작업_건너뜀")
    void 마감일없는공고_예약작업_건너뜀() {
        alertDays("7,3,1");
        stored(request((LocalDateTime) null));

        service.publishDueAlerts();

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("TC-DEADLINE-007 마감시각이9시전_한국날짜로_7일전")
    void 마감시각이9시전_예약작업_한국날짜로7일() {
        alertDays("7,3,1");
        stored(request(LocalDateTime.of(2026, 10, 19, 2, 0)));

        service.publishDueAlerts();

        assertThat(publishedEvent().getDaysRemaining()).isEqualTo(7);
    }

    @Test
    @DisplayName("TC-DEADLINE-007 서버시간대UTC_한국날짜로_7일전")
    void 서버시간대UTC_예약작업_한국날짜로셈() {
        service = new DeadlineAlertService(bidRequestRepository, systemSettingService, eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
        alertDays("7,3,1");
        // UTC 서버에서 JPA 가 읽은 마감 10/18 17:00 은 한국 10/19 02:00 이다
        stored(request(LocalDateTime.of(2026, 10, 18, 17, 0)));

        service.publishDueAlerts();

        assertThat(publishedEvent().getDaysRemaining()).isEqualTo(7);
    }

    @Test
    @DisplayName("TC-DEADLINE-008 설정값변경_14일전만_알림")
    void 설정값14_예약작업_14일전만알림() {
        alertDays("14");
        stored(request(TODAY_17H.plusDays(14)), request(TODAY_17H.plusDays(7)));

        assertThat(service.publishDueAlerts()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-DEADLINE-009 설정없음_예약작업_알림꺼짐")
    void 설정없음_예약작업_알림꺼짐() {
        alertDays(null);

        service.publishDueAlerts();

        then(bidRequestRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("TC-DEADLINE-009 설정값읽을수없음_예약작업_알림꺼짐")
    void 설정값읽을수없음_예약작업_알림꺼짐() {
        alertDays("7,a,1");

        service.publishDueAlerts();

        then(bidRequestRepository).shouldHaveNoInteractions();
    }
}
