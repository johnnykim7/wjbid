package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.EmailAlreadyExistsException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberSocialAccount;
import com.biddingagency.domain.member.entity.SnsProvider;
import com.biddingagency.domain.member.entity.SnsTicket;
import com.biddingagency.domain.member.entity.SnsTicketKind;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.member.repository.MemberSocialAccountRepository;
import com.biddingagency.domain.member.repository.SnsTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * SNS 가입과 로그인 (CR-146, BID-AUTH-006, BID-AUTH-007).
 *
 * SNS 에서 돌아오면 이어진 회원은 LOGIN 표, 처음 온 계정은 SIGNUP 표를 만들어 화면 주소에 싣는다.
 * SNS 이메일로 이미 가입한 회원이 있으면 잇지 않는다(BIZ-063, 5.1 결정). 남이 그 메일로 만든 SNS 계정으로
 * 회원 계정이 넘어갈 길을 막으려는 것이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnsAuthService {

    private static final int TICKET_BYTES = 32;

    private final MemberSocialAccountRepository socialAccountRepository;
    private final SnsTicketRepository ticketRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final EmailCodeService emailCodeService;
    private final AuthPolicy policy;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public enum Outcome {
        LOGIN,
        SIGNUP,
        /** SNS 이메일로 이미 이메일 가입한 회원이 있다 */
        EMAIL_IN_USE
    }

    /** 표는 LOGIN, SIGNUP 일 때만 있다 */
    public record Entry(Outcome outcome, String ticket) {
    }

    public record SignupInfo(SnsProvider provider, String email, boolean emailVerifiedByProvider) {
    }

    public record RegisterCommand(String ticket, String email, String companyName, String businessRegistrationNumber,
                                  String contactPerson, String phone,
                                  boolean agreeTerms, boolean agreePrivacy, boolean agreeMarketing) {
    }

    @Transactional
    public Entry enter(SnsProfile profile) {
        var linked = socialAccountRepository.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId());
        if (linked.isPresent()) {
            String ticket = issue(SnsTicketKind.LOGIN, profile, linked.get().getMemberId(), policy.loginTicketTtl());
            return new Entry(Outcome.LOGIN, ticket);
        }
        if (profile.email() != null && memberRepository.existsByEmail(profile.email())) {
            log.info("SNS email already registered, not linking: provider={}", profile.provider());
            return new Entry(Outcome.EMAIL_IN_USE, null);
        }
        String ticket = issue(SnsTicketKind.SIGNUP, profile, null, policy.signupTicketTtl());
        return new Entry(Outcome.SIGNUP, ticket);
    }

    /** LOGIN 표를 한 번만 받아 이어진 회원을 돌려준다 */
    @Transactional
    public Member login(String ticketValue) {
        LocalDateTime now = LocalDateTime.now(clock);
        SnsTicket ticket = usable(ticketValue, SnsTicketKind.LOGIN, now);
        ticket.markUsed(now);
        MemberSocialAccount account = socialAccountRepository
                .findByProviderAndProviderUserId(ticket.getProvider(), ticket.getProviderUserId())
                .orElseThrow(() -> new AuthFlowException(AuthError.SNS_TICKET_EXPIRED));
        Member member = memberRepository.findById(account.getMemberId())
                .orElseThrow(() -> new AuthFlowException(AuthError.SNS_TICKET_EXPIRED));
        account.recordLogin(now);
        member.updateLastLogin();
        return member;
    }

    @Transactional(readOnly = true)
    public SignupInfo signupInfo(String ticketValue) {
        SnsTicket ticket = usable(ticketValue, SnsTicketKind.SIGNUP, LocalDateTime.now(clock));
        return new SignupInfo(ticket.getProvider(), ticket.getProviderEmail(), ticket.isEmailVerifiedByProvider());
    }

    /**
     * SNS 가입. SNS 가 확인한 이메일을 그대로 쓰면 인증번호 없이, 아니면 확인을 마친 인증번호가 있어야 한다(BIZ-062).
     */
    @Transactional
    public Member register(RegisterCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        SnsTicket ticket = usable(command.ticket(), SnsTicketKind.SIGNUP, now);
        String email = command.email() == null ? "" : command.email().trim();

        if (memberRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (socialAccountRepository.findByProviderAndProviderUserId(ticket.getProvider(), ticket.getProviderUserId()).isPresent()) {
            throw new AuthFlowException(AuthError.SNS_ACCOUNT_IN_USE);
        }
        boolean verifiedByProvider = ticket.isEmailVerifiedByProvider()
                && ticket.getProviderEmail() != null
                && ticket.getProviderEmail().equalsIgnoreCase(email);
        if (!verifiedByProvider) {
            emailCodeService.consumeVerified(email);
        }

        Member member = memberService.registerSnsMember(email, now, command.companyName(),
                command.contactPerson(), command.phone(), command.businessRegistrationNumber(),
                command.agreeTerms(), command.agreePrivacy(), command.agreeMarketing());
        socialAccountRepository.save(MemberSocialAccount.builder()
                .memberId(member.getId())
                .provider(ticket.getProvider())
                .providerUserId(ticket.getProviderUserId())
                .providerEmail(ticket.getProviderEmail())
                .createdAt(now)
                .lastLoginAt(now)
                .build());
        ticket.markUsed(now);
        member.updateLastLogin();
        log.info("SNS member registered: provider={}, email={}", ticket.getProvider(), member.getEmail());
        return member;
    }

    private String issue(SnsTicketKind kind, SnsProfile profile, UUID memberId, Duration ttl) {
        byte[] bytes = new byte[TICKET_BYTES];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        ticketRepository.save(SnsTicket.builder()
                .tokenHash(hash(value))
                .kind(kind)
                .provider(profile.provider())
                .providerUserId(profile.providerUserId())
                .providerEmail(profile.email())
                .emailVerifiedByProvider(profile.emailVerified())
                .memberId(memberId)
                .expiresAt(now.plus(ttl))
                .createdAt(now)
                .build());
        return value;
    }

    private SnsTicket usable(String value, SnsTicketKind kind, LocalDateTime now) {
        if (value == null || value.isBlank()) {
            throw new AuthFlowException(AuthError.SNS_TICKET_EXPIRED);
        }
        return ticketRepository.findByTokenHash(hash(value.trim()))
                .filter(t -> t.isUsable(kind, now))
                .orElseThrow(() -> new AuthFlowException(AuthError.SNS_TICKET_EXPIRED));
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
