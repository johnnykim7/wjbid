package com.biddingagency.dto;

import java.util.List;

/**
 * 인증번호와 SNS 가입 응답 모양 (CR-146, T3-2)
 */
public final class AuthFlowResponses {

    private AuthFlowResponses() {
    }

    public record EmailCodeSent(long expiresInSeconds, long resendAfterSeconds) {
    }

    public record EmailCodeVerified(boolean verified) {
    }

    /** startPath 는 API 주소(/api) 아래 SNS 로그인 시작 경로다 */
    public record SnsProviderItem(String provider, String startPath) {
    }

    public record SnsProviders(List<SnsProviderItem> providers) {
    }

    public record SnsSignupInfo(String provider, String email, boolean emailVerifiedByProvider) {
    }
}
