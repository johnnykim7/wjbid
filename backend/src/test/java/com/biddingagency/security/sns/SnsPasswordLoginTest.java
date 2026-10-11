package com.biddingagency.security.sns;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TC-AUTH-029: SNS 로만 가입한 회원의 비밀번호 로그인 (CR-146, BIZ-064)
 */
class SnsPasswordLoginTest {

    @Test
    @DisplayName("비밀번호없는회원_비밀번호로그인_틀린비밀번호와같은응답")
    void 비밀번호없는회원_로그인_BadCredentials() {
        Member snsOnly = Member.builder().email("sns@example.com").companyName("한빛환경").build();
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder());
        provider.setUserDetailsService(email -> new CustomUserDetails(snsOnly,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken("sns@example.com", "anything123")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
