package com.biddingagency.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TC-AUTH-004 ~ TC-AUTH-008: JWT 토큰 관련 테스트
 */
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    private static final String SECRET = "test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-1234567890";
    private static final long ACCESS_EXPIRATION = 900_000L;    // 15분
    private static final long REFRESH_EXPIRATION = 604_800_000L; // 7일

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    // TC-AUTH-004: 정상 로그인 → Access Token 발급
    @Test
    @DisplayName("정상이메일_토큰생성_유효한JWT반환")
    void 올바른이메일_generateToken_유효한토큰() {
        // when
        String token = tokenProvider.generateToken("user@example.com");

        // then
        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("user@example.com");
    }

    // TC-AUTH-004: Access Token 만료 시간 확인
    @Test
    @DisplayName("AccessToken_만료시간_15분")
    void accessToken_만료시간_15분() {
        assertThat(tokenProvider.getJwtExpiration()).isEqualTo(900_000L);
    }

    // TC-AUTH-007: Refresh Token 정상 발급 및 검증
    @Test
    @DisplayName("정상_RefreshToken_발급및검증")
    void 유효한이메일_refreshToken_생성및검증성공() {
        // when
        String refreshToken = tokenProvider.generateRefreshToken("user@example.com");

        // then
        assertThat(refreshToken).isNotBlank();
        assertThat(tokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(tokenProvider.getEmailFromToken(refreshToken)).isEqualTo("user@example.com");
    }

    // TC-AUTH-008: 만료된 토큰 검증 실패
    @Test
    @DisplayName("만료된토큰_검증_false반환")
    void 만료된토큰_validateToken_false() {
        // given - 0ms 만료 시간
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(SECRET, 0L, 0L);
        String token = shortLivedProvider.generateToken("user@example.com");

        // when & then
        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    // 잘못된 토큰 검증
    @Test
    @DisplayName("잘못된토큰_검증_false반환")
    void 잘못된토큰_validateToken_false() {
        assertThat(tokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    // 빈 토큰 검증
    @Test
    @DisplayName("빈토큰_검증_false반환")
    void 빈토큰_validateToken_false() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }
}
