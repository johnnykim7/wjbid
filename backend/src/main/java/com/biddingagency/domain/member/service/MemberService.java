package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.EmailAlreadyExistsException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberConsent;
import com.biddingagency.domain.member.repository.MemberConsentRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Member service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberConsentRepository memberConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailCodeService emailCodeService;
    private final Clock clock;

    /** 현재 약관 버전 (CR-124) — 약관 개정 시 갱신, 동의이력에 기록 */
    private static final String CURRENT_TERMS_VERSION = "2026-06";

    /**
     * Register new member (CUSTOMER role) — CR-124: 법인 필드(사업자번호) + 가입 동의이력 저장.
     * CR-146: 인증번호 확인을 마친 메일만 받고, 그 번호는 이 가입에 쓰고 끝낸다(BIZ-062).
     */
    @Transactional
    public Member register(String email, String password, String companyName, String contactPerson,
                           String phone, String businessRegistrationNumber,
                           boolean agreeTerms, boolean agreePrivacy, boolean agreeMarketing) {
        // 이미 가입된 메일이면 인증 안내보다 로그인 안내가 먼저다
        if (memberRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        emailCodeService.consumeVerified(email);
        return createCustomer(email, passwordEncoder.encode(password), LocalDateTime.now(clock), companyName,
                contactPerson, phone, businessRegistrationNumber, agreeTerms, agreePrivacy, agreeMarketing);
    }

    /**
     * SNS 가입 (CR-146). 비밀번호 없이 만들고, 확인을 마친 이메일이므로 확인 시각을 채운다.
     * 이메일 확인과 SNS 연결은 부르는 쪽(SnsAuthService)이 같은 트랜잭션에서 한다.
     */
    @Transactional
    public Member registerSnsMember(String email, LocalDateTime emailVerifiedAt, String companyName,
                                    String contactPerson, String phone, String businessRegistrationNumber,
                                    boolean agreeTerms, boolean agreePrivacy, boolean agreeMarketing) {
        return createCustomer(email, null, emailVerifiedAt, companyName, contactPerson, phone,
                businessRegistrationNumber, agreeTerms, agreePrivacy, agreeMarketing);
    }

    private Member createCustomer(String email, String passwordHash, LocalDateTime emailVerifiedAt,
                                  String companyName, String contactPerson, String phone,
                                  String businessRegistrationNumber,
                                  boolean agreeTerms, boolean agreePrivacy, boolean agreeMarketing) {
        if (memberRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (!agreeTerms || !agreePrivacy) {
            throw new IllegalArgumentException("이용약관 및 개인정보처리방침 동의가 필요합니다");
        }

        Member member = Member.builder()
                .email(email)
                .emailVerifiedAt(emailVerifiedAt)
                .passwordHash(passwordHash)
                .companyName(companyName)
                .businessRegistrationNumber(businessRegistrationNumber)
                .contactPerson(contactPerson)
                .phone(phone)
                .role(Member.Role.CUSTOMER)
                .build();

        Member saved = memberRepository.save(member);

        // 동의 이력 저장 (법적 증거력 — 시점·항목·약관버전)
        memberConsentRepository.save(MemberConsent.builder()
                .memberId(saved.getId())
                .termsAgreed(agreeTerms)
                .privacyAgreed(agreePrivacy)
                .marketingAgreed(agreeMarketing)
                .termsVersion(CURRENT_TERMS_VERSION)
                .agreedAt(LocalDateTime.now())
                .build());

        log.info("New member registered: {}", saved.getEmail());
        return saved;
    }

    public Member findById(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + email));
    }

    @Transactional
    public Member updateProfile(UUID memberId, String companyName, String contactPerson, String phone, String address) {
        Member member = findById(memberId);
        member.updateProfile(companyName, contactPerson, phone, address);
        log.info("Member profile updated: {}", member.getEmail());
        return member;
    }

    @Transactional
    public void changePassword(UUID memberId, String currentPassword, String newPassword) {
        Member member = findById(memberId);
        if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        member.updatePassword(passwordEncoder.encode(newPassword));
        log.info("Password changed for member: {}", member.getEmail());
    }

    @Transactional
    public void updateLastLogin(UUID memberId) {
        Member member = findById(memberId);
        member.updateLastLogin();
    }
}
