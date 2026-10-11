package com.biddingagency.security.sns;

import com.biddingagency.config.SnsProperties;
import com.biddingagency.domain.member.entity.SnsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-AUTH-030: 키 없는 SNS 는 등록하지 않는다 (CR-146)
 */
class SnsClientRegistrationsTest {

    static SnsProperties.Provider provider(String id, String secret) {
        SnsProperties.Provider p = new SnsProperties.Provider();
        p.setClientId(id);
        p.setClientSecret(secret);
        p.setClientAuthenticationMethod("client_secret_post");
        p.setAuthorizationUri("https://sns.example/authorize");
        p.setTokenUri("https://sns.example/token");
        p.setUserInfoUri("https://sns.example/me");
        p.setUserNameAttribute("id");
        return p;
    }

    @Test
    @DisplayName("카카오키없음_SNS목록_카카오없음")
    void 키없는SNS_목록에없음() {
        SnsProperties properties = new SnsProperties();
        properties.setRedirectUri("{baseUrl}/login/oauth2/code/{registrationId}");
        properties.getProviders().put("naver", provider("naver-id", "naver-secret"));
        properties.getProviders().put("kakao", provider("", ""));

        SnsClientRegistrations registrations = new SnsClientRegistrations(properties);

        assertThat(registrations.configuredProviders()).containsExactly(SnsProvider.NAVER);
    }
}
