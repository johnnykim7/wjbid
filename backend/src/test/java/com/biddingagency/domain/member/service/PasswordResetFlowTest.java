package com.biddingagency.domain.member.service;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.entity.EmailCodePurpose;
import com.biddingagency.domain.member.entity.EmailVerificationCode;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.EmailVerificationCodeRepository;
import com.biddingagency.domain.member.repository.MemberConsentRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.integration.notification.BpNotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 통합시험 IT-01-078 3·4, IT-01-080 6·7, IT-01-081 2·4 (CR-186, BID-AUTH-009, BIZ-097, POL-037).
 * 30분이 지난 확인과 가입 직후의 재요청 간격은 상용에서 기다려 볼 수 없어 시각을 고정해 본다.
 * 번호를 쓰임별로 찾는 것이 조회에 달려 있어 저장소는 실제 DB 를 쓴다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class PasswordResetFlowTest {

    private static final String EMAIL = "reset-flow@example.com";
    private static final String P1 = "first-pass-1";
    private static final String P2 = "second-pass-2";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T01:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Autowired
    private EmailVerificationCodeRepository codeRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberConsentRepository memberConsentRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final BpNotificationClient notificationClient = mock(BpNotificationClient.class);
    private EmailCodeService emailCodeService;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        AuthPolicy policy = mock(AuthPolicy.class);
        given(policy.resendInterval()).willReturn(Duration.ofSeconds(60));
        given(policy.dailyLimit()).willReturn(10);
        given(policy.codeTtl()).willReturn(Duration.ofMinutes(10));
        given(policy.verifiedWindow()).willReturn(Duration.ofMinutes(30));
        given(notificationClient.sendEmail(anyString(), anyString(), anyMap()))
                .willReturn(new BpNotificationClient.SendResult(true, "r1", null));
        emailCodeService = new EmailCodeService(codeRepository, memberRepository, notificationClient, policy, CLOCK);
        memberService = new MemberService(memberRepository, memberConsentRepository, encoder, emailCodeService, CLOCK);
    }

    private void member() {
        memberRepository.saveAndFlush(Member.builder()
                .email(EMAIL).passwordHash(encoder.encode(P1)).companyName("재설정 시험").build());
    }

    private void code(EmailCodePurpose purpose, LocalDateTime createdAt, LocalDateTime verifiedAt) {
        EmailVerificationCode code = EmailVerificationCode.builder()
                .email(EMAIL)
                .purpose(purpose)
                .codeHash(EmailCodeService.hash(EMAIL, "123456"))
                .createdAt(createdAt)
                .expiresAt(createdAt.plusMinutes(10))
                .attempts(0)
                .build();
        if (verifiedAt != null) {
            code.markVerified(verifiedAt);
        }
        codeRepository.saveAndFlush(code);
    }

    private boolean loginsWithP1() {
        return encoder.matches(P1, memberRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash());
    }

    @Test
    @DisplayName("IT-01-078 3 재설정번호확인뒤31분_비밀번호바꾸기_400과EMAIL_NOT_VERIFIED")
    void 확인뒤31분_바꾸기_NOT_VERIFIED() {
        member();
        code(EmailCodePurpose.PASSWORD_RESET, NOW.minusMinutes(33), NOW.minusMinutes(31));

        assertThatThrownBy(() -> memberService.resetPassword(EMAIL, P2))
                .isInstanceOfSatisfying(AuthFlowException.class, e -> assertThat(
                        e.getError().status() + " " + e.getError()).isEqualTo(HttpStatus.BAD_REQUEST + " " + AuthError.EMAIL_NOT_VERIFIED));
    }

    @Test
    @DisplayName("IT-01-078 4 재설정번호확인뒤31분_비밀번호바꾸기_처음비밀번호그대로")
    void 확인뒤31분_바꾸기_비밀번호그대로() {
        member();
        code(EmailCodePurpose.PASSWORD_RESET, NOW.minusMinutes(33), NOW.minusMinutes(31));

        catchThrowable(() -> memberService.resetPassword(EMAIL, P2));

        assertThat(loginsWithP1()).isTrue();
    }

    @Test
    @DisplayName("IT-01-080 6 확인마친가입번호만있음_비밀번호바꾸기_비밀번호그대로")
    void 가입번호로_바꾸기_비밀번호그대로() {
        member();
        code(EmailCodePurpose.SIGNUP, NOW.minusMinutes(5), NOW.minusMinutes(4));

        catchThrowable(() -> memberService.resetPassword(EMAIL, P2));

        assertThat(loginsWithP1()).isTrue();
    }

    @Test
    @DisplayName("IT-01-080 7 확인마친재설정번호만있음_가입_EMAIL_NOT_VERIFIED이고회원없음")
    void 재설정번호로_가입_막힘() {
        code(EmailCodePurpose.PASSWORD_RESET, NOW.minusMinutes(5), NOW.minusMinutes(4));

        Throwable thrown = catchThrowable(() -> memberService.register(EMAIL, P1, "재설정 시험", "담당", "010-0000-0000",
                "123-45-67890", true, true, false));

        assertThat(thrown instanceof AuthFlowException e && e.getError() == AuthError.EMAIL_NOT_VERIFIED
                && memberRepository.findByEmail(EMAIL).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("IT-01-081 2 가입번호받은지20초_재설정번호요청_429와EMAIL_CODE_TOO_SOON과남은40초")
    void 가입번호뒤20초_재설정요청_TOO_SOON() {
        member();
        code(EmailCodePurpose.SIGNUP, NOW.minusSeconds(20), NOW.minusSeconds(10));

        assertThatThrownBy(() -> emailCodeService.send(EMAIL, EmailCodePurpose.PASSWORD_RESET))
                .isInstanceOfSatisfying(AuthFlowException.class, e -> assertThat(
                        e.getError().status() + " " + e.getError() + " " + e.getDetails().get("retryAfterSeconds"))
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS + " " + AuthError.EMAIL_CODE_TOO_SOON + " 40"));
    }

    @Test
    @DisplayName("IT-01-081 4 가입번호받은지61초_재설정번호요청_재설정번호를보냄")
    void 가입번호뒤61초_재설정요청_보냄() {
        member();
        code(EmailCodePurpose.SIGNUP, NOW.minusSeconds(61), NOW.minusSeconds(50));

        emailCodeService.send(EMAIL, EmailCodePurpose.PASSWORD_RESET);

        assertThat(codeRepository.findAll().stream()
                .max(Comparator.comparing(EmailVerificationCode::getCreatedAt)).orElseThrow().getPurpose())
                .isEqualTo(EmailCodePurpose.PASSWORD_RESET);
    }
}
