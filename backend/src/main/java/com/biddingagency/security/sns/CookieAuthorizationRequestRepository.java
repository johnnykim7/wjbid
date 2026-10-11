package com.biddingagency.security.sns;

import com.biddingagency.config.SnsProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * SNS 로 가는 동안 로그인 요청(state 등)을 세션 대신 쿠키에 담는다 (CR-146, T2-1).
 * 이 서버는 세션을 쓰지 않는다. 쿠키는 브라우저가 고칠 수 있으므로 JSON 으로 담고 서명해, 서명이 맞지 않으면 버린다.
 * 자바 직렬화는 쓰지 않는다(브라우저가 보낸 값을 역직렬화하지 않으려는 것이다).
 */
@Slf4j
@Component
public class CookieAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "sns_auth_request";
    private static final String HMAC = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] signingKey;
    private final Duration ttl;

    public CookieAuthorizationRequestRepository(@Value("${app.jwt.secret}") String secret, SnsProperties properties) {
        this.signingKey = sha256("sns-auth-request:" + secret);
        this.ttl = properties.getRequestCookieTtl();
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return decode(cookie.getValue());
            }
        }
        return null;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            writeCookie(request, response, "", Duration.ZERO);
            return;
        }
        writeCookie(request, response, encode(authorizationRequest), ttl);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest loaded = loadAuthorizationRequest(request);
        writeCookie(request, response, "", Duration.ZERO);
        return loaded;
    }

    String encode(OAuth2AuthorizationRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("authorizationUri", r.getAuthorizationUri());
        m.put("clientId", r.getClientId());
        m.put("redirectUri", r.getRedirectUri());
        m.put("scopes", new ArrayList<>(r.getScopes()));
        m.put("state", r.getState());
        m.put("additionalParameters", r.getAdditionalParameters());
        m.put("attributes", r.getAttributes());
        m.put("authorizationRequestUri", r.getAuthorizationRequestUri());
        try {
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(m));
            return payload + "." + sign(payload);
        } catch (Exception e) {
            throw new IllegalStateException("SNS 로그인 요청을 쿠키에 담지 못했습니다", e);
        }
    }

    @SuppressWarnings("unchecked")
    OAuth2AuthorizationRequest decode(String value) {
        int dot = value == null ? -1 : value.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String payload = value.substring(0, dot);
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII),
                value.substring(dot + 1).getBytes(StandardCharsets.US_ASCII))) {
            log.warn("SNS login request cookie signature mismatch, ignored");
            return null;
        }
        try {
            Map<String, Object> m = objectMapper.readValue(Base64.getUrlDecoder().decode(payload), MAP_TYPE);
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri((String) m.get("authorizationUri"))
                    .clientId((String) m.get("clientId"))
                    .redirectUri((String) m.get("redirectUri"))
                    .scopes(new LinkedHashSet<>((List<String>) m.get("scopes")))
                    .state((String) m.get("state"))
                    .additionalParameters((Map<String, Object>) m.get("additionalParameters"))
                    .attributes((Map<String, Object>) m.get("attributes"))
                    .authorizationRequestUri((String) m.get("authorizationRequestUri"))
                    .build();
        } catch (Exception e) {
            log.warn("SNS login request cookie unreadable, ignored: {}", e.getMessage());
            return null;
        }
    }

    private void writeCookie(HttpServletRequest request, HttpServletResponse response, String value, Duration maxAge) {
        String contextPath = request.getContextPath();
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(request.isSecure())
                // SNS 에서 돌아오는 것은 다른 사이트에서 오는 GET 이동이라 Lax 면 쿠키가 실린다
                .sameSite("Lax")
                .path(contextPath == null || contextPath.isEmpty() ? "/" : contextPath)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(signingKey, HMAC));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
