package com.biddingagency.security.sns;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 키가 없는 SNS 로 시작하려 하면 요청을 만들지 않는다 (CR-146).
 * 기본 처리기는 없는 SNS 를 예외로 보고 500 을 내므로, 여기서 걸러 그런 주소가 없을 때와 같게 404 가 나게 한다.
 */
@Component
public class SnsAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String REGISTRATION_ID = "registrationId";

    private final ClientRegistrationRepository registrations;
    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final RequestMatcher matcher = new AntPathRequestMatcher(
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/{" + REGISTRATION_ID + "}");

    public SnsAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        this.registrations = registrations;
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        RequestMatcher.MatchResult match = matcher.matcher(request);
        return match.isMatch() ? resolve(request, match.getVariables().get(REGISTRATION_ID)) : null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        if (registrationId == null || registrations.findByRegistrationId(registrationId) == null) {
            return null;
        }
        return delegate.resolve(request, registrationId);
    }
}
