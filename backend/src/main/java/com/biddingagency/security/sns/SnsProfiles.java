package com.biddingagency.security.sns;

import com.biddingagency.domain.member.entity.SnsProvider;
import com.biddingagency.domain.member.service.SnsProfile;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/**
 * SNS 마다 다른 사용자 정보 응답을 같은 모양으로 바꾼다 (CR-146).
 * 네이버: {response: {id, email}}. 이메일 확인 표시가 없어 인증번호를 받는다.
 * 카카오: {id, kakao_account: {email, is_email_valid, is_email_verified}}. 두 표시가 모두 참이면 확인된 메일이다.
 */
final class SnsProfiles {

    private SnsProfiles() {
    }

    static SnsProfile from(SnsProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case NAVER -> {
                Map<?, ?> response = map(attributes.get("response"));
                yield profile(provider, response.get("id"), response.get("email"), false);
            }
            case KAKAO -> {
                Map<?, ?> account = map(attributes.get("kakao_account"));
                boolean verified = Boolean.TRUE.equals(account.get("is_email_valid"))
                        && Boolean.TRUE.equals(account.get("is_email_verified"));
                yield profile(provider, attributes.get("id"), account.get("email"), verified);
            }
        };
    }

    private static SnsProfile profile(SnsProvider provider, Object id, Object email, boolean verified) {
        String userId = id == null ? "" : id.toString().trim();
        if (userId.isEmpty()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), "SNS user id missing");
        }
        String mail = email == null ? null : email.toString().trim();
        if (mail != null && mail.isEmpty()) {
            mail = null;
        }
        return new SnsProfile(provider, userId, mail, verified && mail != null);
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> m ? m : Map.of();
    }
}
