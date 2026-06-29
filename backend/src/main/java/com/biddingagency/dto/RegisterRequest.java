package com.biddingagency.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Member registration request DTO
 */
@Data
public class RegisterRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Business registration number is required")
    private String businessRegistrationNumber;

    private String address;

    // 가입 동의 (CR-124) — 약관·개인정보는 필수, 마케팅은 선택
    @AssertTrue(message = "이용약관 동의가 필요합니다")
    private boolean agreeTerms;

    @AssertTrue(message = "개인정보처리방침 동의가 필요합니다")
    private boolean agreePrivacy;

    private boolean agreeMarketing;
}
