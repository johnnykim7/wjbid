package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * NotificationService 테스트 — BIZ-012 멱등성 검증 포함
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("신규알림_이메일발송_NotificationLog저장")
    void 신규알림_sendNotification_이메일발송_로그저장() {
        // given
        String idempotencyKey = "TEST_KEY_" + UUID.randomUUID();
        given(notificationLogRepository.existsByIdempotencyKey(idempotencyKey)).willReturn(false);
        given(notificationLogRepository.save(any(NotificationLog.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.sendNotification(
                NotificationType.BID_REQUEST_CREATED,
                "test@test.com", UUID.randomUUID(),
                "테스트 제목", "테스트 본문",
                UUID.randomUUID(), "BidRequest", idempotencyKey
        );

        // then
        then(mailSender).should().send(any(SimpleMailMessage.class));
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        then(notificationLogRepository).should().save(logCaptor.capture());
        NotificationLog saved = logCaptor.getValue();
        assertThat(saved.getSuccess()).isTrue();
        assertThat(saved.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(saved.getRecipientEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("동일키중복발송_무시_BIZ012")
    void 중복키_sendNotification_무시_BIZ012() {
        // given
        String idempotencyKey = "DUPLICATE_KEY";
        given(notificationLogRepository.existsByIdempotencyKey(idempotencyKey)).willReturn(true);

        // when
        notificationService.sendNotification(
                NotificationType.DEADLINE_D7,
                "test@test.com", UUID.randomUUID(),
                "제목", "본문",
                UUID.randomUUID(), "BidRequest", idempotencyKey
        );

        // then — 이메일 발송 안 함, 로그 저장 안 함
        then(mailSender).shouldHaveNoInteractions();
        then(notificationLogRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이메일발송실패_로그에에러기록")
    void 이메일실패_sendNotification_에러로그저장() {
        // given
        String idempotencyKey = "FAIL_KEY_" + UUID.randomUUID();
        given(notificationLogRepository.existsByIdempotencyKey(idempotencyKey)).willReturn(false);
        willThrow(new RuntimeException("SMTP error")).given(mailSender).send(any(SimpleMailMessage.class));
        given(notificationLogRepository.save(any(NotificationLog.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.sendNotification(
                NotificationType.DOCUMENT_GENERATED,
                "test@test.com", UUID.randomUUID(),
                "제목", "본문",
                UUID.randomUUID(), "BidDocument", idempotencyKey
        );

        // then — 로그 저장되되 success=false
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        then(notificationLogRepository).should().save(logCaptor.capture());
        NotificationLog saved = logCaptor.getValue();
        assertThat(saved.getSuccess()).isFalse();
        assertThat(saved.getErrorMessage()).contains("SMTP error");
    }
}
