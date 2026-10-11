package com.biddingagency.security.sns;

import com.biddingagency.domain.member.entity.SnsProvider;
import com.biddingagency.domain.member.service.SnsProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 네이버, 카카오 사용자 정보를 한 모양으로 읽는다 (CR-146)
 */
class SnsProfilesTest {

    @Test
    @DisplayName("네이버응답_읽기_메일확인안된것으로")
    void 네이버_메일확인없음() {
        SnsProfile profile = SnsProfiles.from(SnsProvider.NAVER,
                Map.of("resultcode", "00", "response", Map.of("id", "nv-1", "email", "kim@naver.com")));

        assertThat(profile).isEqualTo(new SnsProfile(SnsProvider.NAVER, "nv-1", "kim@naver.com", false));
    }

    @Test
    @DisplayName("카카오확인된메일_읽기_확인된것으로")
    void 카카오_확인된메일() {
        SnsProfile profile = SnsProfiles.from(SnsProvider.KAKAO, Map.of("id", 4100000001L,
                "kakao_account", Map.of("email", "kim@kakao.com", "is_email_valid", true, "is_email_verified", true)));

        assertThat(profile).isEqualTo(new SnsProfile(SnsProvider.KAKAO, "4100000001", "kim@kakao.com", true));
    }

    @Test
    @DisplayName("카카오메일미확인_읽기_확인안된것으로")
    void 카카오_미확인메일() {
        SnsProfile profile = SnsProfiles.from(SnsProvider.KAKAO, Map.of("id", 7L,
                "kakao_account", Map.of("email", "kim@kakao.com", "is_email_valid", true, "is_email_verified", false)));

        assertThat(profile.emailVerified()).isFalse();
    }

    @Test
    @DisplayName("카카오메일동의안함_읽기_메일없음")
    void 카카오_메일없음() {
        SnsProfile profile = SnsProfiles.from(SnsProvider.KAKAO, Map.of("id", 7L, "kakao_account", Map.of()));

        assertThat(profile.email()).isNull();
    }

    @Test
    @DisplayName("아이디없는응답_읽기_실패")
    void 아이디없음_실패() {
        assertThatThrownBy(() -> SnsProfiles.from(SnsProvider.NAVER, Map.of("response", Map.of("email", "a@b.c"))))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
