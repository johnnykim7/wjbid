package com.biddingagency.dto;

import com.biddingagency.domain.member.entity.EmailCodePurpose;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증번호, 비밀번호 재설정, SNS 가입 요청 모양 (CR-146, CR-186, T3-2)
 */
public final class AuthFlowRequests {

    private AuthFlowRequests() {
    }

    /** purpose 를 보내지 않으면 가입 번호다 (CR-186) */
    public record SendEmailCode(@Email @NotBlank String email, EmailCodePurpose purpose) {
        public EmailCodePurpose purposeOrSignup() {
            return purpose == null ? EmailCodePurpose.SIGNUP : purpose;
        }
    }

    public record VerifyEmailCode(@Email @NotBlank String email, @NotBlank String code, EmailCodePurpose purpose) {
        public EmailCodePurpose purposeOrSignup() {
            return purpose == null ? EmailCodePurpose.SIGNUP : purpose;
        }
    }

    /** 새 비밀번호 길이는 가입(RegisterRequest)과 같다 (CR-186) */
    public record ResetPassword(@Email @NotBlank String email, @NotBlank @Size(min = 8) String newPassword) {
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
