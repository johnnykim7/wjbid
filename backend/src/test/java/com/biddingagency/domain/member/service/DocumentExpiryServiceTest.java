package com.biddingagency.domain.member.service;

import com.biddingagency.domain.event.DocumentExpiryApproachingEvent;
import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.member.service.DocumentExpiryService.ExpiryDocument;
import com.biddingagency.domain.member.service.DocumentExpiryService.ExpiryOverview;
import com.biddingagency.domain.member.service.DocumentExpiryService.ExpiryStatus;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
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
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TC-MKT-043 ~ TC-MKT-046, TC-MKT-049 (CR-136): 서류 만료 상태와 만료 알림 실행(BID-MKT-013, BID-MKT-014, POL-016).
 */
@ExtendWith(MockitoExtension.class)
class DocumentExpiryServiceTest {

    /** 시험 기준 시각 2026-10-11 09:00 KST */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-11T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private MemberDocumentRepository documentRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private SystemSettingService systemSettingService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DocumentExpiryService service;
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DocumentExpiryService(documentRepository, notificationLogRepository, systemSettingService,
                eventPublisher, NOW);
    }

    private void alertDays(String value) {
        given(systemSettingService.findValue(SettingKey.DOC_EXPIRY_ALERT_DAYS)).willReturn(Optional.ofNullable(value));
    }

    private MemberDocument doc(LocalDate expiresOn) {
        MemberDocument d = MemberDocument.builder().memberId(memberId).category(DocumentCategory.CAGE)
                .fileName("cage.pdf").displayName("CAGE").expiresOn(expiresOn).build();
        ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
        return d;
    }

    /** 만료일로 찾는 저장소를 주어진 서류로 흉내 낸다 */
    private void storedByExpiry(MemberDocument... docs) {
        given(documentRepository.findByExpiresOn(any())).willAnswer(inv -> Arrays.stream(docs)
                .filter(d -> Objects.equals(d.getExpiresOn(), inv.getArgument(0))).toList());
    }

    @Test
    @DisplayName("TC-MKT-043 남은 날로 지남, 곧 만료, 넉넉함, 만료일 없음을 나눈다")
    void 남은날로_만료상태_네가지구분() {
        alertDays("30,7");
        given(documentRepository.findByMemberIdOrderByCreatedAtDesc(memberId)).willReturn(List.of(
                doc(LocalDate.of(2026, 10, 10)), doc(LocalDate.of(2026, 10, 20)),
                doc(LocalDate.of(2026, 12, 31)), doc(null)));

        ExpiryOverview o = service.overview(memberId);

        assertThat(o.documents()).extracting(ExpiryDocument::status)
                .containsExactly(ExpiryStatus.EXPIRED, ExpiryStatus.SOON, ExpiryStatus.OK, ExpiryStatus.NONE);
    }

    @Test
    @DisplayName("TC-MKT-044 알림 설정이 비어 있으면 알림 날이 없고 곧 만료 기준도 없다")
    void 설정비어있음_만료상태_알림꺼짐() {
        alertDays(null);

        ExpiryOverview o = service.overview(memberId);

        assertThat(tuple(o.alertDays(), o.soonDays())).isEqualTo(tuple(List.of(), null));
    }

    @Test
    @DisplayName("TC-MKT-045 만료 30일 전인 서류는 남은 날 30 으로 알림 이벤트를 낸다")
    void 만료30일전_만료알림실행_이벤트발행() {
        alertDays("30,7");
        storedByExpiry(doc(LocalDate.of(2026, 11, 10)));

        service.publishDueAlerts();

        ArgumentCaptor<DocumentExpiryApproachingEvent> event = ArgumentCaptor.forClass(DocumentExpiryApproachingEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getAllValues()).extracting(DocumentExpiryApproachingEvent::getDaysRemaining).containsExactly(30);
    }

    @Test
    @DisplayName("TC-MKT-046 남은 날이 알림 날에 없으면 보내지 않는다")
    void 남은날설정에없음_만료알림실행_안보냄() {
        alertDays("30,7");
        storedByExpiry(doc(LocalDate.of(2026, 11, 9)));

        service.publishDueAlerts();

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("TC-MKT-049 알림 설정을 읽을 수 없으면 예외 없이 보내지 않는다")
    void 설정읽을수없음_만료알림실행_안보냄() {
        alertDays("abc");

        int published = service.publishDueAlerts();

        assertThat(published).isZero();
    }
}
