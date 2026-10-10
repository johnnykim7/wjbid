package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.event.InquiryReceivedEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-132 1:1 문의 접수 관리자 알림 (TC-INQ-009~012).
 */
@ExtendWith(MockitoExtension.class)
class InquiryNotificationListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private MemberRepository memberRepository;
    @Mock private SystemSettingService systemSettingService;

    @InjectMocks
    private InquiryNotificationListener listener;

    private final UUID inquiryId = UUID.randomUUID();

    private InquiryReceivedEvent event() {
        return new InquiryReceivedEvent(inquiryId, "서비스 이용 문의", "제목", "내용", "홍길동",
                "guest@example.com", null, LocalDateTime.of(2026, 10, 11, 10, 0));
    }

    private Member admin(String email) {
        Member m = Member.builder().email(email).role(Member.Role.ADMIN).build();
        ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
        return m;
    }

    private List<String> sentTo() {
        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        then(notificationService).should(atLeast(0)).sendNotification(eq(NotificationType.INQUIRY_RECEIVED),
                to.capture(), any(), any(), anyMap(), any(), any(), any());
        return to.getAllValues();
    }

    @Test
    @DisplayName("TC-INQ-009 알림 주소가 설정돼 있으면 그 주소마다 보내고 관리자 계정에는 보내지 않는다")
    void 알림주소설정있음_문의접수_설정주소마다발송() {
        given(systemSettingService.findValue(SettingKey.INQUIRY_NOTIFY_EMAILS))
                .willReturn(Optional.of("a@x.com, b@x.com"));
        given(memberRepository.findByEmail(any())).willReturn(Optional.empty());

        listener.onInquiryReceived(event());

        assertThat(sentTo()).containsExactly("a@x.com", "b@x.com");
    }

    @Test
    @DisplayName("TC-INQ-010 알림 주소가 비어 있으면 관리자 전원에게 보낸다")
    void 알림주소비어있음_문의접수_관리자전원발송() {
        given(systemSettingService.findValue(SettingKey.INQUIRY_NOTIFY_EMAILS)).willReturn(Optional.empty());
        given(memberRepository.findByRole(Member.Role.ADMIN))
                .willReturn(List.of(admin("admin1@x.com"), admin("admin2@x.com")));

        listener.onInquiryReceived(event());

        assertThat(sentTo()).containsExactly("admin1@x.com", "admin2@x.com");
    }

    @Test
    @DisplayName("TC-INQ-011 형식이 틀린 주소는 건너뛴다")
    void 틀린형식섞임_문의접수_틀린주소건너뜀() {
        given(systemSettingService.findValue(SettingKey.INQUIRY_NOTIFY_EMAILS))
                .willReturn(Optional.of("a@x.com, 잘못된값"));
        given(memberRepository.findByEmail("a@x.com")).willReturn(Optional.empty());

        listener.onInquiryReceived(event());

        assertThat(sentTo()).containsExactly("a@x.com");
    }

    @Test
    @DisplayName("TC-INQ-012 받는 사람마다 다른 멱등 키를 쓴다")
    void 수신자별멱등키_문의접수_받는사람마다다른키() {
        given(systemSettingService.findValue(SettingKey.INQUIRY_NOTIFY_EMAILS))
                .willReturn(Optional.of("a@x.com,b@x.com"));
        given(memberRepository.findByEmail(any())).willReturn(Optional.empty());

        listener.onInquiryReceived(event());

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        then(notificationService).should(times(2)).sendNotification(any(), any(), any(), any(), anyMap(), any(),
                any(), keys.capture());
        assertThat(keys.getAllValues()).containsExactly(
                "INQUIRY_RECEIVED_" + inquiryId + "_a@x.com",
                "INQUIRY_RECEIVED_" + inquiryId + "_b@x.com");
    }
}
