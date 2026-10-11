package com.biddingagency.security.sns;

import com.biddingagency.config.SnsProperties;
import com.biddingagency.domain.member.entity.SnsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 키가 들어 있는 SNS 만 등록한다 (CR-146, TC-AUTH-030).
 * 스프링 기본 설정(spring.security.oauth2.client.registration)은 키가 비면 기동이 멈추므로 직접 만든다.
 */
@Slf4j
@Component
@EnableConfigurationProperties(SnsProperties.class)
public class SnsClientRegistrations implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistration> registrations;

    public SnsClientRegistrations(SnsProperties properties) {
        Map<String, ClientRegistration> map = new LinkedHashMap<>();
        for (SnsProvider provider : SnsProvider.values()) {
            SnsProperties.Provider p = properties.getProviders().get(provider.registrationId());
            if (p == null || !p.isConfigured()) {
                continue;
            }
            map.put(provider.registrationId(), ClientRegistration.withRegistrationId(provider.registrationId())
                    .clientName(provider.name())
                    .clientId(p.getClientId())
                    .clientSecret(p.getClientSecret())
                    .clientAuthenticationMethod(new ClientAuthenticationMethod(p.getClientAuthenticationMethod()))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(properties.getRedirectUri())
                    .scope(p.getScopes())
                    .authorizationUri(p.getAuthorizationUri())
                    .tokenUri(p.getTokenUri())
                    .userInfoUri(p.getUserInfoUri())
                    .userNameAttributeName(p.getUserNameAttribute())
                    .build());
        }
        this.registrations = Collections.unmodifiableMap(map);
        log.info("SNS login providers with keys: {}", map.keySet());
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return registrations.values().iterator();
    }

    public List<SnsProvider> configuredProviders() {
        return registrations.keySet().stream()
                .map(id -> SnsProvider.fromRegistrationId(id).orElseThrow())
                .toList();
    }
}
