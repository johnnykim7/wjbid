package com.biddingagency.security.sns;

import com.biddingagency.config.SnsProperties;
import com.biddingagency.domain.member.entity.SnsProvider;
import com.biddingagency.domain.member.service.SnsAuthService;
import com.biddingagency.domain.member.service.SnsProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SNS 에서 돌아온 뒤 고객 화면으로 보낸다 (CR-146, T3-2 GET /login/oauth2/code/{provider}).
 * 이어진 회원은 /auth/sns/done, 처음 온 계정은 /register/sns 로 표를 실어 보낸다. 토큰은 주소에 싣지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnsLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final String OAUTH_ACCESS_DENIED = "access_denied";

    private final SnsAuthService snsAuthService;
    private final SnsProperties properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityContextHolder.clearContext();
        String target;
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            SnsProvider provider = SnsProvider.fromRegistrationId(token.getAuthorizedClientRegistrationId())
                    .orElseThrow();
            SnsProfile profile = SnsProfiles.from(provider, token.getPrincipal().getAttributes());
            SnsAuthService.Entry entry = snsAuthService.enter(profile);
            target = switch (entry.outcome()) {
                case LOGIN -> portal("/auth/sns/done?ticket=" + entry.ticket());
                case SIGNUP -> portal("/register/sns?ticket=" + entry.ticket());
                case EMAIL_IN_USE -> loginNotice(SnsReturnNotice.EMAIL_IN_USE);
            };
        } catch (RuntimeException e) {
            log.error("SNS login handling failed", e);
            target = loginNotice(SnsReturnNotice.FAILED);
        }
        response.sendRedirect(target);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        boolean cancelled = exception instanceof OAuth2AuthenticationException e
                && OAUTH_ACCESS_DENIED.equals(e.getError().getErrorCode());
        log.warn("SNS login not completed: {}", exception.getMessage());
        response.sendRedirect(loginNotice(cancelled ? SnsReturnNotice.CANCELLED : SnsReturnNotice.FAILED));
    }

    private String loginNotice(SnsReturnNotice notice) {
        return portal("/login?sns=" + notice.name());
    }

    private String portal(String path) {
        String base = properties.getPortalBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
    }
}
