package com.biddingagency.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 인증번호와 SNS 가입 요청 모양 (CR-146, T3-2)
 */
public final class AuthFlowRequests {

    private AuthFlowRequests() {
    }

    public record SendEmailCode(@Email @NotBlank String email) {
    }

    public record VerifyEmailCode(@Email @NotBlank String email, @NotBlank String code) {
    }

    public record SnsLogin(@NotBlank String ticket) {
    }

    /** 회사 정보와 동의는 이메일 가입(RegisterRequest)과 같다 (5.2 결정) */
    public record SnsRegister(
            @NotBlank String ticket,
            @Email @NotBlank String email,
            @NotBlank String companyName,
            @NotBlank String businessRegistrationNumber,
            @NotBlank String contactPerson,
            @NotBlank String phone,
            @AssertTrue boolean agreeTerms,
            @AssertTrue boolean agreePrivacy,
            boolean agreeMarketing) {
    }
}
