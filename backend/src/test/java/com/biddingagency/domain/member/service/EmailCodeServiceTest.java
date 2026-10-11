package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.EmailAlreadyExistsException;
import com.biddingagency.domain.member.entity.EmailVerificationCode;
import com.biddingagency.domain.member.repository.EmailVerificationCodeRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.integration.notification.BpNotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TC-AUTH-009 ~ TC-AUTH-016: 이메일 인증번호 보내기와 확인 (CR-146, BID-AUTH-005)
 */
@ExtendWith(MockitoExtension.class)
class EmailCodeServiceTest {

    private static final String EMAIL = "new@example.com";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T01:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private EmailVerificationCodeRepository codeRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BpNotificationClient notificationClient;
    @Mock
    private AuthPolicy policy;

    private EmailCodeService service;

    @BeforeEach
    void setUp() {
        service = new EmailCodeService(codeRepository, memberRepository, notificationClient, policy, CLOCK);
    }

    private void defaultPolicy() {
        given(policy.resendInterval()).willReturn(Duration.ofSeconds(60));
        given(policy.dailyLimit()).willReturn(10);
        given(policy.codeTtl()).willReturn(Duration.ofMinutes(10));
    }

    private EmailVerificationCode code(String plain, LocalDateTime createdAt, int attempts) {
        return EmailVerificationCode.builder()
                .email(EMAIL)
                .codeHash(EmailCodeService.hash(EMAIL, plain))
                .createdAt(createdAt)
                .expiresAt(createdAt.plusMinutes(10))
                .attempts(attempts)
                .build();
    }

    // TC-AUTH-009
    @Test
    @DisplayName("가입안된메일_인증번호요청_틀로메일보내고_유효시간600초")
    void 가입안된메일_인증번호요청_메일발송() {
        defaultPolicy();
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL)).willReturn(Optional.empty());
        given(notificationClient.sendEmail(eq(EMAIL), eq("BID_EMAIL_CODE"), anyMap()))
                .willReturn(new BpNotificationClient.SendResult(true, "r1", null));

        EmailCodeService.Sent sent = service.send(EMAIL);

        assertThat(sent.expiresInSeconds()).isEqualTo(600);
        ArgumentCaptor<EmailVerificationCode> saved = ArgumentCaptor.forClass(EmailVerificationCode.class);
        then(codeRepository).should().save(saved.capture());
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass((Class) Map.class);
        then(notificationClient).should().sendEmail(eq(EMAIL), eq("BID_EMAIL_CODE"), vars.capture());
        String plain = (String) vars.getValue().get("code");
        assertThat(saved.getValue().getCodeHash()).isEqualTo(EmailCodeService.hash(EMAIL, plain)).isNotEqualTo(plain);
    }

    // TC-AUTH-010
    @Test
    @DisplayName("가입된메일_인증번호요청_409이고_메일보내지않음")
    void 가입된메일_인증번호요청_거절() {
        given(memberRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> service.send(EMAIL)).isInstanceOf(EmailAlreadyExistsException.class);
        then(notificationClient).should(never()).sendEmail(any(), any(), any());
    }

    // TC-AUTH-011
    @Test
    @DisplayName("보낸지30초_간격60초_다시받기_429와남은30초")
    void 간격전_다시받기_TOO_SOON() {
        given(policy.resendInterval()).willReturn(Duration.ofSeconds(60));
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL))
                .willReturn(Optional.of(code("111111", NOW.minusSeconds(30), 0)));

        assertThatThrownBy(() -> service.send(EMAIL))
                .isInstanceOfSatisfying(AuthFlowException.class, e -> {
                    assertThat(e.getError()).isEqualTo(AuthError.EMAIL_CODE_TOO_SOON);
                    assertThat(e.getDetails()).containsEntry("retryAfterSeconds", 30L);
                });
    }

    // TC-AUTH-012
    @Test
    @DisplayName("오늘10번보낸메일_다시받기_429하루한도")
    void 하루한도_넘음_DAILY_LIMIT() {
        given(policy.resendInterval()).willReturn(Duration.ofSeconds(60));
        given(policy.dailyLimit()).willReturn(10);
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL))
                .willReturn(Optional.of(code("111111", NOW.minusMinutes(5), 0)));
        given(codeRepository.countByEmailAndCreatedAtGreaterThanEqual(EMAIL, LocalDateTime.of(2026, 10, 12, 0, 0))).willReturn(10L);

        assertThatThrownBy(() -> service.send(EMAIL))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.EMAIL_CODE_DAILY_LIMIT));
    }

    @Test
    @DisplayName("알림서버실패_인증번호요청_502")
    void 메일실패_SEND_FAILED() {
        defaultPolicy();
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL)).willReturn(Optional.empty());
        given(notificationClient.sendEmail(any(), any(), any()))
                .willReturn(new BpNotificationClient.SendResult(false, null, "no template"));

        assertThatThrownBy(() -> service.send(EMAIL))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.EMAIL_SEND_FAILED));
    }

    // TC-AUTH-013
    @Test
    @DisplayName("유효시간안_맞는번호_확인시각채움")
    void 맞는번호_확인됨() {
        EmailVerificationCode latest = code("123456", NOW.minusMinutes(1), 0);
        given(policy.maxAttempts()).willReturn(5);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL)).willReturn(Optional.of(latest));

        service.verify(EMAIL, "123456");

        assertThat(latest.getVerifiedAt()).isEqualTo(NOW);
    }

    // TC-AUTH-014
    @Test
    @DisplayName("틀린번호_틀린횟수0_400과남은4번")
    void 틀린번호_MISMATCH() {
        EmailVerificationCode latest = code("123456", NOW.minusMinutes(1), 0);
        given(policy.maxAttempts()).willReturn(5);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL)).willReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.verify(EMAIL, "000000"))
                .isInstanceOfSatisfying(AuthFlowException.class, e -> {
                    assertThat(e.getError()).isEqualTo(AuthError.EMAIL_CODE_MISMATCH);
                    assertThat(e.getDetails()).containsEntry("remainingAttempts", 4);
                });
    }

    // TC-AUTH-015
    @Test
    @DisplayName("다섯번틀린뒤_맞는번호_429이고_확인되지않음")
    void 다섯번틀린뒤_LOCKED() {
        EmailVerificationCode latest = code("123456", NOW.minusMinutes(1), 5);
        given(policy.maxAttempts()).willReturn(5);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL)).willReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.verify(EMAIL, "123456"))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.EMAIL_CODE_LOCKED));
        assertThat(latest.getVerifiedAt()).isNull();
    }

    // TC-AUTH-016
    @Test
    @DisplayName("유효시간지난_맞는번호_410")
    void 시간지난번호_EXPIRED() {
        EmailVerificationCode latest = code("123456", NOW.minusMinutes(11), 0);
        given(policy.maxAttempts()).willReturn(5);
        given(codeRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL)).willReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.verify(EMAIL, "123456"))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.EMAIL_CODE_EXPIRED));
    }

    @Test
    @DisplayName("확인뒤30분안_가입_번호를다쓴것으로")
    void 확인한번호_가입에쓰면_consumed() {
        EmailVerificationCode verified = code("123456", NOW.minusMinutes(5), 0);
        verified.markVerified(NOW.minusMinutes(4));
        given(policy.verifiedWindow()).willReturn(Duration.ofMinutes(30));
        given(codeRepository.findFirstByEmailAndVerifiedAtIsNotNullAndConsumedAtIsNullOrderByVerifiedAtDesc(EMAIL))
                .willReturn(Optional.of(verified));

        service.consumeVerified(EMAIL);

        assertThat(verified.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("확인한번호없음_가입_EMAIL_NOT_VERIFIED")
    void 확인없음_가입_막힘() {
        given(policy.verifiedWindow()).willReturn(Duration.ofMinutes(30));
        given(codeRepository.findFirstByEmailAndVerifiedAtIsNotNullAndConsumedAtIsNullOrderByVerifiedAtDesc(EMAIL))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumeVerified(EMAIL))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.EMAIL_NOT_VERIFIED));
    }
}
