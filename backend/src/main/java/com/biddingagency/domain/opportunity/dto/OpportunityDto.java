package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Opportunity DTO for API responses
 *
 * Maps Opportunity entity fields to frontend-compatible field names
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpportunityDto(
        String id,
        String solicitationNumber,
        String title,
        String agencyName,          // mapped from organizationName
        String naicsCode,           // extracted from rawJson
        String setAside,            // extracted from rawJson
        String responseDeadline,    // formatted date string
        String postedDate,          // formatted date string
        String placeOfPerformance,  // extracted from rawJson
        String description,         // extracted from rawJson
        String status,              // "active" or "closed"
        String type,
        String uiLink,
        List<String> resourceLinks  // attachment URLs from SAM.gov
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static OpportunityDto from(Opportunity opp) {
        Map<String, Object> raw = opp.getRawJson();

        return new OpportunityDto(
                opp.getId() != null ? opp.getId().toString() : null,
                opp.getSolicitationNumber(),
                opp.getTitle(),
                opp.getOrganizationName(),
                getString(raw, "naicsCode"),
                getString(raw, "setAside"),
                formatDate(opp.getResponseDeadline()),
                formatDate(opp.getPostedDate()),
                getString(raw, "placeOfPerformance"),
                getString(raw, "description"),
                Boolean.TRUE.equals(opp.getActive()) ? "active" : "closed",
                opp.getType(),
                opp.getUiLink(),
                getStringList(raw, "resourceLinks")
        );
    }

    private static String formatDate(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DATE_FORMATTER);
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val == null) return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : s;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof List<?> list && !list.isEmpty()) {
            return (List<String>) list;
        }
        return null;
    }
}
