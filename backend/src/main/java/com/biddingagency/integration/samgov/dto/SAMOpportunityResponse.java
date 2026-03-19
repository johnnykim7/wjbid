package com.biddingagency.integration.samgov.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SAM.gov API response DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SAMOpportunityResponse {

    @JsonProperty("totalRecords")
    private Integer totalRecords;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("opportunitiesData")
    private List<OpportunityData> opportunitiesData;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpportunityData {
        @JsonProperty("noticeId")
        private String noticeId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("solicitationNumber")
        private String solicitationNumber;

        @JsonProperty("type")
        private String type;

        @JsonProperty("typeOfSetAsideDescription")
        private String typeOfSetAsideDescription;

        @JsonProperty("classificationCode")
        private String classificationCode;

        @JsonProperty("naicsCode")
        private String naicsCode;

        @JsonProperty("fullParentPathName")
        private String organizationName;

        @JsonProperty("postedDate")
        private String postedDate;

        @JsonProperty("responseDeadLine")
        private String responseDeadLine;

        @JsonProperty("active")
        private String active;

        @JsonProperty("uiLink")
        private String uiLink;

        @JsonProperty("description")
        private List<Map<String, String>> description;

        @JsonProperty("resourceLinks")
        private List<String> resourceLinks;

        @JsonProperty("officeAddress")
        private Map<String, Object> officeAddress;

        @JsonProperty("pointOfContact")
        private List<Map<String, Object>> pointOfContact;
    }
}
