package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-AUTH-001 ~ TC-AUTH-003: MemberService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_COMPANY = "Test Corp";
    private static final String TEST_CONTACT = "John Doe";
    private static final String TEST_PHONE = "010-1234-5678";

    // TC-AUTH-001: 정상 회원가입
    @Test
    @DisplayName("정상_회원가입_Member생성_CUSTOMER역할")
    void 유효한입력_회원가입_CUSTOMER_Member생성() {
        // given
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
        given(passwordEncoder.encode(TEST_PASSWORD)).willReturn("encoded_password");
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Member result = memberService.register(TEST_EMAIL, TEST_PASSWORD, TEST_COMPANY, TEST_CONTACT, TEST_PHONE);

        // then
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getRole()).isEqualTo(Member.Role.CUSTOMER);
        assertThat(result.getCompanyName()).isEqualTo(TEST_COMPANY);
        then(memberRepository).should().save(any(Member.class));
    }

    // TC-AUTH-002: 중복 이메일 가입 시도
    @Test
    @DisplayName("중복이메일_가입시도_예외발생_BIZ008")
    void 이미존재하는이메일_회원가입_IllegalArgumentException() {
        // given
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.register(TEST_EMAIL, TEST_PASSWORD, TEST_COMPANY, TEST_CONTACT, TEST_PHONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        then(memberRepository).should(never()).save(any());
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
}
