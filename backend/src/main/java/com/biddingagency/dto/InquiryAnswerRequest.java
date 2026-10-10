package com.biddingagency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InquiryAnswerRequest {

    @NotBlank
    @Size(max = 5000)
    private String answer;
}
