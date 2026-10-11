package com.biddingagency.security.sns;

import com.biddingagency.config.SnsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 키 없는 SNS 로 시작하는 주소는 요청을 만들지 않는다 (CR-146)
 */
class SnsAuthorizationRequestResolverTest {

    private final SnsAuthorizationRequestResolver resolver;

    SnsAuthorizationRequestResolverTest() {
        SnsProperties properties = new SnsProperties();
        properties.setRedirectUri("{baseUrl}/login/oauth2/code/{registrationId}");
        properties.getProviders().put("kakao", SnsClientRegistrationsTest.provider("kakao-id", "kakao-secret"));
        properties.getProviders().put("naver", SnsClientRegistrationsTest.provider("", ""));
        resolver = new SnsAuthorizationRequestResolver(new SnsClientRegistrations(properties));
    }

    private static MockHttpServletRequest start(String registrationId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/" + registrationId);
        request.setServletPath("/oauth2/authorization/" + registrationId);
        return request;
    }

    @Test
    @DisplayName("키있는SNS_시작_요청만듦")
    void 키있는SNS_요청() {
        assertThat(resolver.resolve(start("kakao")).getClientId()).isEqualTo("kakao-id");
    }

    @Test
    @DisplayName("키없는SNS_시작_요청없음")
    void 키없는SNS_요청없음() {
        assertThat(resolver.resolve(start("naver"))).isNull();
    }

    @Test
    @DisplayName("모르는SNS_시작_요청없음")
    void 모르는SNS_요청없음() {
        assertThat(resolver.resolve(start("google"))).isNull();
    }
}
