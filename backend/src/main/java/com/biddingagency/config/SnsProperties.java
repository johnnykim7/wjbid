package com.biddingagency.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SNS 로그인 설정 (CR-146, application.yml 의 app.sns).
 * 키(클라이언트 ID, 비밀키)는 환경변수로만 넣는다. 키가 빈 SNS 는 등록하지 않아 단추도 보이지 않는다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.sns")
public class SnsProperties {

    /** SNS 가 돌려보낼 주소 틀. {baseUrl} 은 요청 주소에서 채워지므로 프록시 뒤 운영은 공개 주소로 박아 넣는다 */
    private String redirectUri;
    /** SNS 처리 뒤 보내는 고객 화면 주소 */
    private String portalBaseUrl;
    /** SNS 로 가는 동안 로그인 요청을 담아 두는 쿠키의 수명 */
    private Duration requestCookieTtl;
    private Map<String, Provider> providers = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String clientAuthenticationMethod;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userNameAttribute;
        private List<String> scopes = new ArrayList<>();

        public boolean isConfigured() {
            return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
        }
    }
}
