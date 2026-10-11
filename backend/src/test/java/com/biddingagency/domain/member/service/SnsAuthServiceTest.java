package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberSocialAccount;
import com.biddingagency.domain.member.entity.SnsProvider;
import com.biddingagency.domain.member.entity.SnsTicket;
import com.biddingagency.domain.member.entity.SnsTicketKind;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.member.repository.MemberSocialAccountRepository;
import com.biddingagency.domain.member.repository.SnsTicketRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * TC-AUTH-021 ~ TC-AUTH-028: SNS 가입과 로그인 (CR-146, BID-AUTH-006, BID-AUTH-007)
 */
@ExtendWith(MockitoExtension.class)
class SnsAuthServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final String KAKAO_ID = "4100000001";
    private static final String KAKAO_EMAIL = "kim@kakao.com";
    private static final String TICKET = "ticket-value";

    @Mock
    private MemberSocialAccountRepository socialAccountRepository;
    @Mock
    private SnsTicketRepository ticketRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberService memberService;
    @Mock
    private EmailCodeService emailCodeService;
    @Mock
    private AuthPolicy policy;

    private SnsAuthService service;

    @BeforeEach
    void setUp() {
        service = new SnsAuthService(socialAccountRepository, ticketRepository, memberRepository, memberService,
                emailCodeService, policy, CLOCK);
    }

    private SnsTicket ticket(SnsTicketKind kind, SnsProvider provider, String email, boolean verified,
                             LocalDateTime expiresAt, UUID memberId) {
        return SnsTicket.builder()
                .tokenHash(SnsAuthService.hash(TICKET))
                .kind(kind)
                .provider(provider)
                .providerUserId(KAKAO_ID)
                .providerEmail(email)
                .emailVerifiedByProvider(verified)
                .memberId(memberId)
                .expiresAt(expiresAt)
                .createdAt(NOW.minusMinutes(1))
                .build();
    }

    private SnsAuthService.RegisterCommand command(String email) {
        return new SnsAuthService.RegisterCommand(TICKET, email, "한빛환경", "123-45-67890", "김민수", "010-1234-5678",
                true, true, false);
    }

    private Member savedMember(String email) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(UUID.randomUUID());
        given(member.getEmail()).willReturn(email);
        return member;
    }

    // TC-AUTH-021
    @Test
    @DisplayName("이어진회원없는SNS계정_들어옴_SIGNUP표")
    void 처음온계정_SIGNUP표() {
        given(policy.signupTicketTtl()).willReturn(Duration.ofMinutes(30));
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.KAKAO, KAKAO_ID)).willReturn(Optional.empty());
        given(memberRepository.existsByEmail(KAKAO_EMAIL)).willReturn(false);

        SnsAuthService.Entry entry = service.enter(new SnsProfile(SnsProvider.KAKAO, KAKAO_ID, KAKAO_EMAIL, true));

        assertThat(entry.outcome()).isEqualTo(SnsAuthService.Outcome.SIGNUP);
        ArgumentCaptor<SnsTicket> saved = ArgumentCaptor.forClass(SnsTicket.class);
        then(ticketRepository).should().save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(SnsAuthService.hash(entry.ticket()));
    }

    // TC-AUTH-022
    @Test
    @DisplayName("SNS확인메일그대로_SNS가입_인증번호없이회원과연결")
    void 확인메일_SNS가입() {
        SnsTicket signup = ticket(SnsTicketKind.SIGNUP, SnsProvider.KAKAO, KAKAO_EMAIL, true, NOW.plusMinutes(10), null);
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(signup));
        given(memberRepository.existsByEmail(KAKAO_EMAIL)).willReturn(false);
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.KAKAO, KAKAO_ID)).willReturn(Optional.empty());
        Member member = savedMember(KAKAO_EMAIL);
        given(memberService.registerSnsMember(eq(KAKAO_EMAIL), eq(NOW), anyString(), anyString(), anyString(), anyString(),
                eq(true), eq(true), eq(false))).willReturn(member);

        service.register(command(KAKAO_EMAIL));

        then(emailCodeService).should(never()).consumeVerified(any());
        ArgumentCaptor<MemberSocialAccount> link = ArgumentCaptor.forClass(MemberSocialAccount.class);
        then(socialAccountRepository).should().save(link.capture());
        assertThat(link.getValue().getProviderUserId()).isEqualTo(KAKAO_ID);
        assertThat(signup.getUsedAt()).isEqualTo(NOW);
    }

    // TC-AUTH-023
    @Test
    @DisplayName("네이버표_인증번호확인없음_SNS가입_EMAIL_NOT_VERIFIED")
    void 확인없는메일_SNS가입_막힘() {
        SnsTicket signup = ticket(SnsTicketKind.SIGNUP, SnsProvider.NAVER, "kim@naver.com", false, NOW.plusMinutes(10), null);
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(signup));
        given(memberRepository.existsByEmail("kim@naver.com")).willReturn(false);
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.NAVER, KAKAO_ID)).willReturn(Optional.empty());
        willThrow(new AuthFlowException(AuthError.EMAIL_NOT_VERIFIED)).given(emailCodeService).consumeVerified("kim@naver.com");

        assertThatThrownBy(() -> service.register(command("kim@naver.com")))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.EMAIL_NOT_VERIFIED));
        then(memberService).should(never()).registerSnsMember(any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("SNS확인메일과다른메일_SNS가입_인증번호를쓴다")
    void 다른메일_SNS가입_인증번호사용() {
        SnsTicket signup = ticket(SnsTicketKind.SIGNUP, SnsProvider.KAKAO, KAKAO_EMAIL, true, NOW.plusMinutes(10), null);
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(signup));
        given(memberRepository.existsByEmail("work@company.com")).willReturn(false);
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.KAKAO, KAKAO_ID)).willReturn(Optional.empty());
        Member member = savedMember("work@company.com");
        given(memberService.registerSnsMember(eq("work@company.com"), any(), any(), any(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean())).willReturn(member);

        service.register(command("work@company.com"));

        then(emailCodeService).should().consumeVerified("work@company.com");
    }

    // TC-AUTH-024
    @Test
    @DisplayName("SNS이메일로가입된회원있음_들어옴_잇지않고EMAIL_IN_USE")
    void 같은메일회원있음_EMAIL_IN_USE() {
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.KAKAO, KAKAO_ID)).willReturn(Optional.empty());
        given(memberRepository.existsByEmail(KAKAO_EMAIL)).willReturn(true);

        SnsAuthService.Entry entry = service.enter(new SnsProfile(SnsProvider.KAKAO, KAKAO_ID, KAKAO_EMAIL, true));

        assertThat(entry.outcome()).isEqualTo(SnsAuthService.Outcome.EMAIL_IN_USE);
        then(ticketRepository).should(never()).save(any());
    }

    // TC-AUTH-025
    @Test
    @DisplayName("이미이어진SNS계정_SNS가입_두번째연결거부")
    void 이어진계정_다시가입_거부() {
        SnsTicket signup = ticket(SnsTicketKind.SIGNUP, SnsProvider.KAKAO, KAKAO_EMAIL, true, NOW.plusMinutes(10), null);
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(signup));
        given(memberRepository.existsByEmail("other@example.com")).willReturn(false);
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.KAKAO, KAKAO_ID))
                .willReturn(Optional.of(MemberSocialAccount.builder().memberId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> service.register(command("other@example.com")))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.SNS_ACCOUNT_IN_USE));
    }

    // TC-AUTH-026
    @Test
    @DisplayName("시간지난SIGNUP표_가입화면값_410")
    void 시간지난가입표_EXPIRED() {
        SnsTicket signup = ticket(SnsTicketKind.SIGNUP, SnsProvider.KAKAO, KAKAO_EMAIL, true, NOW.minusSeconds(1), null);
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(signup));

        assertThatThrownBy(() -> service.signupInfo(TICKET))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.SNS_TICKET_EXPIRED));
    }

    // TC-AUTH-027
    @Test
    @DisplayName("이어진SNS계정_LOGIN표_회원과마지막로그인")
    void 이어진계정_로그인() {
        UUID memberId = UUID.randomUUID();
        SnsTicket login = ticket(SnsTicketKind.LOGIN, SnsProvider.KAKAO, KAKAO_EMAIL, true, NOW.plusSeconds(30), memberId);
        MemberSocialAccount account = MemberSocialAccount.builder().memberId(memberId)
                .provider(SnsProvider.KAKAO).providerUserId(KAKAO_ID).build();
        Member member = mock(Member.class);
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(login));
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.KAKAO, KAKAO_ID)).willReturn(Optional.of(account));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThat(service.login(TICKET)).isSameAs(member);
        assertThat(account.getLastLoginAt()).isEqualTo(NOW);
        then(member).should().updateLastLogin();
    }

    @Test
    @DisplayName("이어진SNS계정_들어옴_LOGIN표")
    void 이어진계정_들어옴_LOGIN표() {
        given(policy.loginTicketTtl()).willReturn(Duration.ofSeconds(60));
        given(socialAccountRepository.findByProviderAndProviderUserId(SnsProvider.KAKAO, KAKAO_ID))
                .willReturn(Optional.of(MemberSocialAccount.builder().memberId(UUID.randomUUID()).build()));

        SnsAuthService.Entry entry = service.enter(new SnsProfile(SnsProvider.KAKAO, KAKAO_ID, KAKAO_EMAIL, true));

        assertThat(entry.outcome()).isEqualTo(SnsAuthService.Outcome.LOGIN);
    }

    // TC-AUTH-028
    @Test
    @DisplayName("이미쓴LOGIN표_로그인_410")
    void 쓴로그인표_EXPIRED() {
        SnsTicket login = ticket(SnsTicketKind.LOGIN, SnsProvider.KAKAO, KAKAO_EMAIL, true, NOW.plusSeconds(30), UUID.randomUUID());
        login.markUsed(NOW.minusSeconds(5));
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(login));

        assertThatThrownBy(() -> service.login(TICKET))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.SNS_TICKET_EXPIRED));
    }

    @Test
    @DisplayName("SIGNUP표로_로그인_410")
    void 가입표로로그인_EXPIRED() {
        SnsTicket signup = ticket(SnsTicketKind.SIGNUP, SnsProvider.KAKAO, KAKAO_EMAIL, true, NOW.plusMinutes(10), null);
        given(ticketRepository.findByTokenHash(SnsAuthService.hash(TICKET))).willReturn(Optional.of(signup));

        assertThatThrownBy(() -> service.login(TICKET))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.SNS_TICKET_EXPIRED));
    }
}
