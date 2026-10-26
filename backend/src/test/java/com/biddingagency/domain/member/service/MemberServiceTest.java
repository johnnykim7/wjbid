package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.EmailAlreadyExistsException;
import com.biddingagency.domain.member.MembershipError;
import com.biddingagency.domain.member.MembershipException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberConsent;
import com.biddingagency.domain.member.repository.MemberConsentRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-AUTH-001 ~ TC-AUTH-003, TC-AUTH-018 ~ TC-AUTH-019: MemberService 단위 테스트
 * TC-MEM-017 ~ TC-MEM-020 (CR-183): 관리자 회원 정보 수정, 내 프로필 사업자등록번호
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberConsentRepository memberConsentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailCodeService emailCodeService;

    private MemberService memberService;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 9, 0);

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_COMPANY = "Test Corp";
    private static final String TEST_CONTACT = "John Doe";
    private static final String TEST_PHONE = "010-1234-5678";
    private static final String TEST_BRN = "123-45-67890";

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        memberService = new MemberService(memberRepository, memberConsentRepository, passwordEncoder,
                emailCodeService, clock);
    }

    // TC-AUTH-001: 정상 회원가입
    @Test
    @DisplayName("정상_회원가입_Member생성_CUSTOMER역할")
    void 유효한입력_회원가입_CUSTOMER_Member생성() {
        // given
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
        given(passwordEncoder.encode(TEST_PASSWORD)).willReturn("encoded_password");
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Member result = memberService.register(TEST_EMAIL, TEST_PASSWORD, TEST_COMPANY, TEST_CONTACT, TEST_PHONE,
                TEST_BRN, true, true, false);

        // then
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getRole()).isEqualTo(Member.Role.CUSTOMER);
        assertThat(result.getCompanyName()).isEqualTo(TEST_COMPANY);
        then(memberRepository).should().save(any(Member.class));
        then(memberConsentRepository).should().save(any(MemberConsent.class));
    }

    // CR-124: 필수 동의 미체크 시 가입 거부
    @Test
    @DisplayName("개인정보_미동의_회원가입_예외발생")
    void 필수동의_미체크_회원가입_IllegalArgumentException() {
        // given
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);

        // when & then (약관은 동의했으나 개인정보 미동의)
        assertThatThrownBy(() -> memberService.register(TEST_EMAIL, TEST_PASSWORD, TEST_COMPANY, TEST_CONTACT, TEST_PHONE,
                TEST_BRN, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("동의");

        then(memberRepository).should(never()).save(any());
        then(memberConsentRepository).should(never()).save(any());
    }

    // TC-AUTH-002: 중복 이메일 가입 시도
    @Test
    @DisplayName("중복이메일_가입시도_예외발생_BIZ008")
    void 이미존재하는이메일_회원가입_EmailAlreadyExistsException() {
        // given
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.register(TEST_EMAIL, TEST_PASSWORD, TEST_COMPANY, TEST_CONTACT, TEST_PHONE,
                TEST_BRN, true, true, false))
                .isInstanceOf(EmailAlreadyExistsException.class);

        then(memberRepository).should(never()).save(any());
        then(emailCodeService).should(never()).consumeVerified(any());
    }

    // TC-AUTH-018: 인증번호 확인 없이 가입
    @Test
    @DisplayName("확인한번호없음_가입_EMAIL_NOT_VERIFIED_BIZ062")
    void 확인없는이메일_회원가입_막힘() {
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
        willThrow(new AuthFlowException(AuthError.EMAIL_NOT_VERIFIED)).given(emailCodeService).consumeVerified(TEST_EMAIL);

        assertThatThrownBy(() -> memberService.register(TEST_EMAIL, TEST_PASSWORD, TEST_COMPANY, TEST_CONTACT, TEST_PHONE,
                TEST_BRN, true, true, false))
                .isInstanceOfSatisfying(AuthFlowException.class,
                        e -> assertThat(e.getError()).isEqualTo(AuthError.EMAIL_NOT_VERIFIED));

        then(memberRepository).should(never()).save(any());
    }

    // TC-AUTH-019: 확인 뒤 정상 가입
    @Test
    @DisplayName("확인한번호있음_가입_이메일확인시각채움")
    void 확인한이메일_회원가입_확인시각채움() {
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        Member result = memberService.register(TEST_EMAIL, TEST_PASSWORD, TEST_COMPANY, TEST_CONTACT, TEST_PHONE,
                TEST_BRN, true, true, false);

        assertThat(result.getEmailVerifiedAt()).isEqualTo(NOW);
        then(emailCodeService).should().consumeVerified(TEST_EMAIL);
    }

    // TC-AUTH-006: 존재하지 않는 이메일로 조회
    @Test
    @DisplayName("미등록이메일_조회_예외발생")
    void 미등록이메일_findByEmail_IllegalArgumentException() {
        // given
        given(memberRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.findByEmail("unknown@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Member not found");
    }

    // 비밀번호 변경 - 현재 비밀번호 불일치
    @Test
    @DisplayName("틀린현재비밀번호_비밀번호변경_예외발생")
    void 현재비밀번호불일치_changePassword_예외() {
        // given
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder()
                .email(TEST_EMAIL)
                .passwordHash("encoded_old")
                .companyName(TEST_COMPANY)
                .role(Member.Role.CUSTOMER)
                .build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPassword", "encoded_old")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.changePassword(memberId, "wrongPassword", "newPassword123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    private Member customer() {
        return Member.builder()
                .email(TEST_EMAIL)
                .companyName(TEST_COMPANY)
                .businessRegistrationNumber(TEST_BRN)
                .contactPerson(TEST_CONTACT)
                .phone(TEST_PHONE)
                .address("서울시 중구")
                .role(Member.Role.CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("TC-MEM-017 관리자수정_다섯칸바뀌고빈칸은지움")
    void 관리자수정_다섯칸바뀌고빈칸은지움() {
        UUID memberId = UUID.randomUUID();
        Member member = customer();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        memberService.updateByAdmin(memberId, "새 회사", "111-22-33333", "김담당", "  ", null);

        assertThat(member).extracting(Member::getCompanyName, Member::getBusinessRegistrationNumber,
                        Member::getContactPerson, Member::getPhone, Member::getAddress)
                .containsExactly("새 회사", "111-22-33333", "김담당", null, null);
    }

    @Test
    @DisplayName("TC-MEM-018 관리자수정_없는회원_MEMBER_NOT_FOUND")
    void 관리자수정_없는회원_MEMBER_NOT_FOUND() {
        UUID memberId = UUID.randomUUID();
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateByAdmin(memberId, "새 회사", null, null, null, null))
                .isInstanceOfSatisfying(MembershipException.class,
                        e -> assertThat(e.getError()).isEqualTo(MembershipError.MEMBER_NOT_FOUND));
        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("TC-MEM-019 내프로필수정_사업자등록번호보냄_저장")
    void 내프로필수정_사업자등록번호보냄_저장() {
        UUID memberId = UUID.randomUUID();
        Member member = customer();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        memberService.updateProfile(memberId, TEST_COMPANY, "222-33-44444", TEST_CONTACT, TEST_PHONE, null);

        assertThat(member.getBusinessRegistrationNumber()).isEqualTo("222-33-44444");
    }

    @Test
    @DisplayName("TC-MEM-020 내프로필수정_사업자등록번호안보냄_그대로")
    void 내프로필수정_사업자등록번호안보냄_그대로() {
        UUID memberId = UUID.randomUUID();
        Member member = customer();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        memberService.updateProfile(memberId, TEST_COMPANY, null, TEST_CONTACT, TEST_PHONE, null);

        assertThat(member.getBusinessRegistrationNumber()).isEqualTo(TEST_BRN);
    }
}
