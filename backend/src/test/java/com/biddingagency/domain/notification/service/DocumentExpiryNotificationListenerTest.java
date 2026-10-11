package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.event.DocumentExpiryApproachingEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import com.biddingagency.integration.notification.BpNotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * TC-MKT-047, TC-MKT-048, TC-MKT-050 (CR-136): 서류 만료 메일 발송과 멱등(BID-MKT-014, BIZ-030).
 * 알림 서버는 Mock 이고, 멱등은 실제 NotificationService 가 알림 이력으로 판단한다.
 */
@ExtendWith(MockitoExtension.class)
class DocumentExpiryNotificationListenerTest {

    private static final LocalDate EXPIRES = LocalDate.of(2026, 11, 10);

    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private BpNotificationClient bpNotificationClient;
    @Mock private MemberRepository memberRepository;

    private DocumentExpiryNotificationListener listener;
    private final Map<String, NotificationLog> logs = new HashMap<>();
    private final UUID docId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new DocumentExpiryNotificationListener(
                new NotificationService(notificationLogRepository, bpNotificationClient), memberRepository);
        ReflectionTestUtils.setField(listener, "customerPortalUrl", "http://portal.test");

        Member member = Member.builder().email("cust@x.com").contactPerson("홍길동").build();
        ReflectionTestUtils.setField(member, "id", memberId);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(notificationLogRepository.findByIdempotencyKey(anyString()))
                .willAnswer(inv -> Optional.ofNullable(logs.get(inv.<String>getArgument(0))));
        given(notificationLogRepository.save(any(NotificationLog.class))).willAnswer(inv -> {
            NotificationLog l = inv.getArgument(0);
            logs.put(l.getIdempotencyKey(), l);
            return l;
        });
        given(bpNotificationClient.sendEmail(anyString(), anyString(), anyMap()))
                .willReturn(new BpNotificationClient.SendResult(true, "req-1", null));
    }

    private DocumentExpiryApproachingEvent event(LocalDate expiresOn) {
        return new DocumentExpiryApproachingEvent(docId, memberId, "CAGE 증빙", "CAGE 증빙", expiresOn, 30);
    }

    @Test
    @DisplayName("TC-MKT-047 같은 날 두 번 돌아도 메일은 한 번만 보낸다")
    void 같은날두번실행_만료알림_한번만발송() {
        listener.onDocumentExpiryApproaching(event(EXPIRES));
        listener.onDocumentExpiryApproaching(event(EXPIRES));

        then(bpNotificationClient).should(times(1)).sendEmail(anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("TC-MKT-048 보낸 뒤 만료일이 바뀌면 새 만료일로 다시 보낸다")
    void 만료일바뀜_만료알림_새로발송() {
        listener.onDocumentExpiryApproaching(event(EXPIRES));
        listener.onDocumentExpiryApproaching(event(LocalDate.of(2026, 12, 1)));

        then(bpNotificationClient).should(times(2)).sendEmail(anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("TC-MKT-050 만료 메일 틀 코드로 서류 이름, 만료일, 남은 날을 넘긴다")
    @SuppressWarnings("unchecked")
    void 이벤트수신_알림발송_만료틀코드와값() {
        listener.onDocumentExpiryApproaching(event(EXPIRES));

        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        then(bpNotificationClient).should().sendEmail(eq("cust@x.com"), eq("BID_DOC_EXPIRY"), vars.capture());
        assertThat(vars.getValue()).containsEntry("documentName", "CAGE 증빙")
                .containsEntry("expiresOn", "2026-11-10").containsEntry("daysRemaining", "30");
    }
}
