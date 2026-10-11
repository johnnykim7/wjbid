package com.biddingagency.domain.member.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 가입과 로그인에 쓰는 SNS (CR-146). 이름의 소문자가 스프링 OAuth2 등록 id 이고 돌아올 주소의 끝이다.
 * 구글은 보류다(CR-146 7.3). 붙일 때 여기와 application.yml 의 app.sns.providers 에 더한다.
 */
public enum SnsProvider {
    NAVER,
    KAKAO;

    public String registrationId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<SnsProvider> fromRegistrationId(String registrationId) {
        return Arrays.stream(values())
                .filter(p -> p.registrationId().equals(registrationId))
                .findFirst();
    }
}
