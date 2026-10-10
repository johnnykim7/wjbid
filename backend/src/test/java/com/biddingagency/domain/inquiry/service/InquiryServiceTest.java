package com.biddingagency.domain.inquiry.service;

import com.biddingagency.domain.event.InquiryReceivedEvent;
import com.biddingagency.domain.inquiry.entity.Inquiry;
import com.biddingagency.domain.inquiry.entity.InquiryStatus;
import com.biddingagency.domain.inquiry.entity.InquiryType;
import com.biddingagency.domain.inquiry.exception.InquiryAlreadyAnsweredException;
import com.biddingagency.domain.inquiry.exception.InquiryAnswerMailFailedException;
import com.biddingagency.domain.inquiry.exception.InquiryTypeInvalidException;
import com.biddingagency.domain.inquiry.repository.InquiryRepository;
import com.biddingagency.domain.inquiry.repository.InquiryTypeRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import com.biddingagency.domain.notification.service.NotificationService;
import com.biddingagency.dto.InquirySubmitRequest;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-132 고객센터 1:1 문의 접수·답변 (TC-INQ-001~008).
 */
@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), ZONE);

    @Mock private InquiryRepository inquiryRepository;
    @Mock private InquiryTypeRepository inquiryTypeRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private InquiryService inquiryService;

    @BeforeEach
    void setUp() {
        inquiryService = new InquiryService(inquiryRepository, inquiryTypeRepository, memberRepository,
                notificationService, notificationLogRepository, eventPublisher, CLOCK);
    }

    private InquirySubmitRequest request(String typeCode) {
        InquirySubmitRequest r = new InquirySubmitRequest();
        r.setTypeCode(typeCode);
        r.setTitle("SAM 등록 절차");
        r.setContent("UEI 발급이 얼마나 걸리나요");
        r.setName("홍길동");
        r.setEmail("guest@example.com");
        return r;
    }

    private void givenSaveAssignsId() {
        given(inquiryRepository.save(any(Inquiry.class))).willAnswer(inv -> {
            Inquiry i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(i, "createdAt", LocalDateTime.now(CLOCK));
            return i;
        });
    }

    private Member member(UUID id) {
        Member m = Member.builder().email("user@example.com").companyName("우진").build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private Inquiry inquiry(UUID id, InquiryStatus status) {
        Inquiry i = Inquiry.builder().typeCode("SERVICE").title("제목").content("내용")
                .name("홍길동").email("guest@example.com").status(status).build();
        ReflectionTestUtils.setField(i, "id", id);
        ReflectionTestUtils.setField(i, "createdAt", LocalDateTime.now(CLOCK));
        return i;
    }

    @Test
    @DisplayName("TC-INQ-001 비로그인 문의는 미답변으로 저장되고 접수 이벤트가 나간다")
    void 비로그인_문의등록_미답변으로저장되고이벤트발행됨() {
        given(inquiryTypeRepository.findById("SERVICE"))
                .willReturn(Optional.of(new InquiryType("SERVICE", "서비스 이용 문의", 1, true)));
        givenSaveAssignsId();

        inquiryService.submit(request("SERVICE"), null);

        ArgumentCaptor<InquiryReceivedEvent> event = ArgumentCaptor.forClass(InquiryReceivedEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getValue())
                .extracting(InquiryReceivedEvent::getTypeLabel, InquiryReceivedEvent::getEmail,
                        InquiryReceivedEvent::getMemberId)
                .containsExactly("서비스 이용 문의", "guest@example.com", null);
    }

    @Test
    @DisplayName("TC-INQ-002 로그인 고객의 문의는 그 회원과 묶인다")
    void 로그인회원_문의등록_회원과묶임() {
        UUID memberId = UUID.randomUUID();
        given(inquiryTypeRepository.findById("SERVICE"))
                .willReturn(Optional.of(new InquiryType("SERVICE", "서비스 이용 문의", 1, true)));
        givenSaveAssignsId();

        inquiryService.submit(request("SERVICE"), member(memberId));

        ArgumentCaptor<Inquiry> saved = ArgumentCaptor.forClass(Inquiry.class);
        then(inquiryRepository).should().save(saved.capture());
        assertThat(saved.getValue().getMemberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("TC-INQ-003 없는 유형 코드면 저장하지 않고 거부한다")
    void 없는유형코드_문의등록_거부됨() {
        given(inquiryTypeRepository.findById("NOPE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.submit(request("NOPE"), null))
                .isInstanceOf(InquiryTypeInvalidException.class);
        then(inquiryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("TC-INQ-004 쓰지 않는 유형이면 저장하지 않고 거부한다")
    void 사용안하는유형_문의등록_거부됨() {
        given(inquiryTypeRepository.findById("ETC"))
                .willReturn(Optional.of(new InquiryType("ETC", "기타", 6, false)));

        assertThatThrownBy(() -> inquiryService.submit(request("ETC"), null))
                .isInstanceOf(InquiryTypeInvalidException.class);
        then(inquiryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("TC-INQ-005 답변 메일이 성공하면 답변완료로 기록한다")
    void 미답변문의_답변메일성공_답변완료와기록남음() {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        given(inquiryRepository.findById(id)).willReturn(Optional.of(inquiry(id, InquiryStatus.RECEIVED)));
        given(notificationService.sendNotification(eq(NotificationType.INQUIRY_ANSWERED), any(), any(), any(),
                anyMap(), any(), any(), any())).willReturn(true);
        given(inquiryRepository.markAnswered(any(), any(), any(), any())).willReturn(1);

        inquiryService.answer(id, " 답변입니다 ", member(adminId));

        then(inquiryRepository).should().markAnswered(id, "답변입니다", LocalDateTime.now(CLOCK), adminId);
    }

    @Test
    @DisplayName("TC-INQ-006 답변 메일이 실패하면 상태를 바꾸지 않고 오류를 낸다")
    void 미답변문의_답변메일실패_상태그대로오류() {
        UUID id = UUID.randomUUID();
        given(inquiryRepository.findById(id)).willReturn(Optional.of(inquiry(id, InquiryStatus.RECEIVED)));
        given(notificationService.sendNotification(any(), any(), any(), any(), anyMap(), any(), any(), any()))
                .willReturn(false);

        assertThatThrownBy(() -> inquiryService.answer(id, "답변", member(UUID.randomUUID())))
                .isInstanceOf(InquiryAnswerMailFailedException.class);
        then(inquiryRepository).should(never()).markAnswered(any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-INQ-007 답변완료 문의에 다시 답하면 메일을 보내지 않고 거부한다")
    void 답변완료문의_다시답변_거부됨() {
        UUID id = UUID.randomUUID();
        given(inquiryRepository.findById(id)).willReturn(Optional.of(inquiry(id, InquiryStatus.ANSWERED)));

        assertThatThrownBy(() -> inquiryService.answer(id, "답변", member(UUID.randomUUID())))
                .isInstanceOf(InquiryAlreadyAnsweredException.class);
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("TC-INQ-008 실패 뒤 다시 답하면 새 멱등 키로 다시 보낸다")
    void 실패뒤다시답변_답변메일_새멱등키로발송됨() {
        UUID id = UUID.randomUUID();
        Clock[] clocks = {CLOCK, Clock.offset(CLOCK, java.time.Duration.ofSeconds(30))};
        given(inquiryRepository.findById(id)).willReturn(Optional.of(inquiry(id, InquiryStatus.RECEIVED)));
        given(notificationService.sendNotification(any(), any(), any(), any(), anyMap(), any(), any(), any()))
                .willReturn(false, true);
        given(inquiryRepository.markAnswered(any(), any(), any(), any())).willReturn(1);

        for (Clock clock : clocks) {
            InquiryService service = new InquiryService(inquiryRepository, inquiryTypeRepository, memberRepository,
                    notificationService, notificationLogRepository, eventPublisher, clock);
            try {
                service.answer(id, "답변", member(UUID.randomUUID()));
            } catch (InquiryAnswerMailFailedException ignored) {
                // 첫 시도는 메일 실패로 끝난다
            }
        }

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        then(notificationService).should(times(2)).sendNotification(any(), any(), any(), any(), anyMap(), any(),
                any(), keys.capture());
        assertThat(List.copyOf(keys.getAllValues())).doesNotHaveDuplicates();
    }
}
