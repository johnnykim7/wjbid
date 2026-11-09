package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberConsentRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LoginAttemptServiceTest {

    private static final String EMAIL = "login@example.com";
    private static final String RIGHT = "right-pass";
    private static final String WRONG = "wrong-pass";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant T0 = Instant.parse("2026-11-10T01:00:00Z");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SystemSettingService settings = mock(SystemSettingService.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private Member member;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        member = Member.builder().email(EMAIL).passwordHash("x").companyName("시험").build();
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(memberRepository.findByEmailForUpdate(EMAIL)).willReturn(Optional.of(member));
        given(authenticationManager.authenticate(any())).willAnswer(inv -> {
            Authentication token = inv.getArgument(0);
            if (RIGHT.equals(token.getCredentials())) {
                return token;
            }
            throw new BadCredentialsException("bad");
        });
        loginService = serviceAt(T0);
    }

    private LoginService serviceAt(Instant now) {
        LoginAttemptService attempts =
                new LoginAttemptService(memberRepository, new AuthPolicy(settings), Clock.fixed(now, ZONE));
        return new LoginService(authenticationManager, attempts);
    }

    private void failTimes(int times) {
        for (int i = 0; i < times; i++) {
            catchThrowable(() -> loginService.authenticate(EMAIL, WRONG));
        }
    }

    @Test
    @DisplayName("TC-AUTH-040")
    void 로그인_틀림_횟수증가() {
        failTimes(1);

        assertThat(member.getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-AUTH-041")
    void 로그인_한도_잠김_맞아도거부() {
        failTimes(5);

        AuthFlowException failure = catchThrowableOfType(
                () -> loginService.authenticate(EMAIL, RIGHT), AuthFlowException.class);

        assertThat(failure.getError()).isEqualTo(AuthError.LOGIN_LOCKED);
        assertThat(failure.getDetails()).containsEntry("retryAfterMinutes", 15L);
    }

    @Test
    @DisplayName("TC-AUTH-042")
    void 로그인_잠금지남_성공_횟수지움() {
        failTimes(5);

        serviceAt(T0.plusSeconds(15 * 60 + 1)).authenticate(EMAIL, RIGHT);

        assertThat(member.getFailedLoginCount()).isZero();
        assertThat(member.getLoginLockedUntil()).isNull();
    }

    @Test
    @DisplayName("TC-AUTH-043")
    void 로그인_없는이메일_세지않음() {
        Throwable failure = catchThrowable(() -> loginService.authenticate("none@example.com", WRONG));

        assertThat(failure).isInstanceOf(BadCredentialsException.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-AUTH-044")
    void 재설정_성공_잠금풀림() {
        member.recordLoginFailure(1, LocalDateTime.now(Clock.fixed(T0, ZONE)).plusMinutes(15));
        MemberService memberService = new MemberService(memberRepository, mock(MemberConsentRepository.class),
                new BCryptPasswordEncoder(), mock(EmailCodeService.class), Clock.fixed(T0, ZONE));

        memberService.resetPassword(EMAIL, "new-pass-123");

        assertThat(member.getFailedLoginCount()).isZero();
        assertThat(member.getLoginLockedUntil()).isNull();
    }

    @Test
    @DisplayName("TC-AUTH-045")
    void 로그인_정책값변경_한도바뀜() {
        given(settings.findLong(SettingKey.LOGIN_MAX_ATTEMPTS)).willReturn(Optional.of(2L));
        failTimes(2);

        AuthFlowException failure = catchThrowableOfType(
                () -> loginService.authenticate(EMAIL, RIGHT), AuthFlowException.class);

        assertThat(failure.getError()).isEqualTo(AuthError.LOGIN_LOCKED);
    }
}
