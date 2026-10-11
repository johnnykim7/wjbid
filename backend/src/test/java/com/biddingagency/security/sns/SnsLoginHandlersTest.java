package com.biddingagency.security.sns;

import com.biddingagency.config.SnsProperties;
import com.biddingagency.domain.member.service.SnsAuthService;
import com.biddingagency.domain.member.service.SnsProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * SNS 에서 돌아온 뒤 고객 화면으로 보내는 곳 (CR-146)
 */
class SnsLoginHandlersTest {

    private static final String PORTAL = "https://woojinusbid.com/";

    private SnsAuthService snsAuthService;
    private SnsLoginHandlers handlers;

    @BeforeEach
    void setUp() {
        snsAuthService = mock(SnsAuthService.class);
        SnsProperties properties = new SnsProperties();
        properties.setPortalBaseUrl(PORTAL);
        handlers = new SnsLoginHandlers(snsAuthService, properties);
    }

    private static OAuth2AuthenticationToken kakaoToken() {
        Map<String, Object> attributes = Map.of("id", 4100000001L,
                "kakao_account", Map.of("email", "kim@kakao.com", "is_email_valid", true, "is_email_verified", true));
        DefaultOAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, "id");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "kakao");
    }

    private String successRedirect(SnsAuthService.Entry entry) throws Exception {
        given(snsAuthService.enter(any(SnsProfile.class))).willReturn(entry);
        MockHttpServletResponse response = new MockHttpServletResponse();
        handlers.onAuthenticationSuccess(new MockHttpServletRequest(), response, kakaoToken());
        return response.getRedirectedUrl();
    }

    @Test
    @DisplayName("처음온계정_돌아옴_가입화면으로")
    void 가입표_가입화면() throws Exception {
        assertThat(successRedirect(new SnsAuthService.Entry(SnsAuthService.Outcome.SIGNUP, "t1")))
                .isEqualTo("https://woojinusbid.com/register/sns?ticket=t1");
    }

    @Test
    @DisplayName("이어진계정_돌아옴_로그인마무리화면으로")
    void 로그인표_마무리화면() throws Exception {
        assertThat(successRedirect(new SnsAuthService.Entry(SnsAuthService.Outcome.LOGIN, "t2")))
                .isEqualTo("https://woojinusbid.com/auth/sns/done?ticket=t2");
    }

    @Test
    @DisplayName("같은메일회원있음_돌아옴_로그인화면안내")
    void 같은메일_로그인안내() throws Exception {
        assertThat(successRedirect(new SnsAuthService.Entry(SnsAuthService.Outcome.EMAIL_IN_USE, null)))
                .isEqualTo("https://woojinusbid.com/login?sns=EMAIL_IN_USE");
    }

    @Test
    @DisplayName("처리중오류_돌아옴_로그인화면실패안내")
    void 처리오류_실패안내() throws Exception {
        given(snsAuthService.enter(any(SnsProfile.class))).willThrow(new IllegalStateException("db down"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.onAuthenticationSuccess(new MockHttpServletRequest(), response, kakaoToken());

        assertThat(response.getRedirectedUrl()).isEqualTo("https://woojinusbid.com/login?sns=FAILED");
    }

    @Test
    @DisplayName("고객이동의취소_돌아옴_취소안내")
    void 동의취소_취소안내() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://woojinusbid.com/login?sns=CANCELLED");
    }

    @Test
    @DisplayName("그밖의실패_돌아옴_실패안내")
    void 그밖의실패_실패안내() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.onAuthenticationFailure(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://woojinusbid.com/login?sns=FAILED");
    }
}
