package com.biddingagency.security.sns;

import com.biddingagency.config.SnsProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SNS 로그인 요청을 세션 대신 서명한 쿠키에 담는다 (CR-146)
 */
class CookieAuthorizationRequestRepositoryTest {

    private final CookieAuthorizationRequestRepository repository = repository("test-secret");

    private static CookieAuthorizationRequestRepository repository(String secret) {
        SnsProperties properties = new SnsProperties();
        properties.setRequestCookieTtl(Duration.ofMinutes(5));
        return new CookieAuthorizationRequestRepository(secret, properties);
    }

    private static OAuth2AuthorizationRequest request() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("kakao-client")
                .redirectUri("https://woojinusbid.com/api/login/oauth2/code/kakao")
                .scopes(Set.of())
                .state("state-123")
                .attributes(Map.of(OAuth2ParameterNames.REGISTRATION_ID, "kakao"))
                .build();
    }

    @Test
    @DisplayName("서명한쿠키_다시읽기_같은요청")
    void 저장한요청_다시읽힘() {
        OAuth2AuthorizationRequest original = request();

        OAuth2AuthorizationRequest restored = repository.decode(repository.encode(original));

        assertThat(restored.getState()).isEqualTo("state-123");
        assertThat(restored.getClientId()).isEqualTo("kakao-client");
        assertThat(restored.getRedirectUri()).isEqualTo(original.getRedirectUri());
        assertThat(restored.getAuthorizationRequestUri()).isEqualTo(original.getAuthorizationRequestUri());
        assertThat((String) restored.getAttribute(OAuth2ParameterNames.REGISTRATION_ID)).isEqualTo("kakao");
    }

    @Test
    @DisplayName("값을고친쿠키_읽기_버린다")
    void 고친쿠키_버림() {
        String value = repository.encode(request());
        String payload = value.substring(0, value.indexOf('.'));
        String tampered = payload.substring(0, payload.length() - 2) + "AA" + value.substring(value.indexOf('.'));

        assertThat(repository.decode(tampered)).isNull();
    }

    @Test
    @DisplayName("다른비밀키로서명한쿠키_읽기_버린다")
    void 다른키쿠키_버림() {
        String value = repository("other-secret").encode(request());

        assertThat(repository.decode(value)).isNull();
    }

    @Test
    @DisplayName("요청저장_응답쿠키_HttpOnly와SameSiteLax")
    void 저장쿠키_속성() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(request(), httpRequest, response);

        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).contains("HttpOnly", "SameSite=Lax", "Path=/api", "Max-Age=300");
    }

    @Test
    @DisplayName("저장된쿠키_꺼내기_요청돌려주고쿠키지움")
    void 꺼내면쿠키지움() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie(CookieAuthorizationRequestRepository.COOKIE_NAME, repository.encode(request())));
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(httpRequest, response);

        assertThat(removed.getState()).isEqualTo("state-123");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }
}
