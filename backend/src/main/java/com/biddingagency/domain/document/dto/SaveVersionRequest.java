package com.biddingagency.domain.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class SaveVersionRequest {

    @NotNull(message = "contentJson is required")
    private Map<String, Object> contentJson;

    private String changeSummary;
}
