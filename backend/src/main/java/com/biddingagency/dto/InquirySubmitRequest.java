package com.biddingagency.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 1:1 문의 접수 요청 (CR-132). 칸 길이는 inquiries 열 길이와 같다.
 */
@Data
public class InquirySubmitRequest {

    @NotBlank
    @Size(max = 40)
    private String typeCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 5000)
    private String content;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
