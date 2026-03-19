package com.biddingagency.integration.samgov.client;

import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.integration.samgov.dto.SAMOpportunityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Opportunity Collector Service
 *
 * Collects opportunities from SAM.gov and stores them in database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityCollectorService {

    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SAMGovApiClient samGovApiClient;
    private final OpportunityService opportunityService;

    /**
     * Determine start date for incremental sync:
     * - If DB has data: use latest posted_date - 1 day (1-day overlap for safety)
     * - If DB is empty: fall back to today - fallbackDaysBack (initial load)
     */
    private LocalDate determineStartDate(int fallbackDaysBack) {
        return opportunityService.findLatestPostedDate()
                .map(latestDate -> {
                    LocalDate startDate = latestDate.toLocalDate().minusDays(1);
                    log.info("Incremental sync: DB latest posted_date={}, using postedFrom={}",
                            latestDate.toLocalDate(), startDate);
                    return startDate;
                })
                .orElseGet(() -> {
                    LocalDate startDate = LocalDate.now().minusDays(fallbackDaysBack);
                    log.info("DB is empty, initial load: using postedFrom={} ({} days back)",
                            startDate, fallbackDaysBack);
                    return startDate;
                });
    }

    /**
     * Collect opportunities by keyword (incremental sync)
     *
     * @param keyword          SAM.gov keyword to search
     * @param fallbackDaysBack days to look back when DB is empty (initial load)
     */
    public CollectionResult collectByKeyword(String keyword, int fallbackDaysBack) {
        log.info("Starting collection for keyword: {}", keyword);

        LocalDate postedFrom = determineStartDate(fallbackDaysBack);
        LocalDate postedTo = LocalDate.now();

        int collected = 0;
        int duplicates = 0;
        int errors = 0;
        int offset = 0;
        int limit = 1000;

        try {
            boolean hasMore = true;

            while (hasMore) {
                SAMOpportunityResponse response = samGovApiClient.searchOpportunities(
                        keyword, postedFrom, postedTo, limit, offset);

                if (response.getOpportunitiesData() == null ||
                        response.getOpportunitiesData().isEmpty()) {
                    hasMore = false;
                    break;
                }

                for (SAMOpportunityResponse.OpportunityData data : response.getOpportunitiesData()) {
                    try {
                        boolean isNew = processOpportunity(data);
                        if (isNew) {
                            collected++;
                        } else {
                            duplicates++;
                        }
                    } catch (Exception e) {
                        log.error("Error processing opportunity: {}", data.getNoticeId(), e);
                        errors++;
                    }
                }

                // Check if there are more records
                if (response.getOpportunitiesData().size() < limit) {
                    hasMore = false;
                } else {
                    offset += limit;
                }
            }

        } catch (Exception e) {
            log.error("Error during collection for keyword: {}", keyword, e);
            errors++;
        }

        log.info("Collection completed for keyword '{}': {} new, {} duplicates, {} errors",
                keyword, collected, duplicates, errors);

        return new CollectionResult(collected, duplicates, errors);
    }

    /**
     * Process single opportunity
     */
    private boolean processOpportunity(SAMOpportunityResponse.OpportunityData data) {
        String noticeId = data.getNoticeId();

        // Check if already exists
        boolean exists = opportunityService.existsByNoticeId(noticeId);

        // Parse dates
        LocalDateTime postedDate = parseDate(data.getPostedDate());
        LocalDateTime responseDeadline = parseDate(data.getResponseDeadLine());

        // Create raw JSON — store all displayable fields for DTO mapping
        Map<String, Object> rawJson = new HashMap<>();
        rawJson.put("noticeId", data.getNoticeId());
        rawJson.put("title", data.getTitle());
        rawJson.put("type", data.getType());
        rawJson.put("organizationName", data.getOrganizationName());
        rawJson.put("naicsCode", data.getNaicsCode());
        rawJson.put("classificationCode", data.getClassificationCode());
        rawJson.put("setAside", data.getTypeOfSetAsideDescription());
        rawJson.put("description", extractDescription(data.getDescription()));
        rawJson.put("placeOfPerformance", extractPlaceOfPerformance(data.getOfficeAddress()));
        rawJson.put("resourceLinks", data.getResourceLinks());

        // Calculate content hash for change detection
        String contentHash = calculateHash(data);

        // Create or update
        opportunityService.createOrUpdate(
                noticeId,
                data.getSolicitationNumber(),
                data.getTitle(),
                data.getType(),
                data.getOrganizationName(),
                postedDate,
                responseDeadline,
                data.getUiLink(),
                null, // description link
                rawJson,
                contentHash
        );

        return !exists; // Return true if new
    }

    /**
     * Parse date string from SAM.gov API
     * Handles multiple formats:
     * - "yyyy-MM-dd" (postedDate)
     * - "yyyy-MM-ddTHH:mm:ss±HH:mm" (responseDeadLine, ISO-8601 with timezone)
     * - "yyyy-MM-dd HH:mm:ss" (legacy fallback)
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // yyyy-MM-dd (date only)
        try {
            return LocalDate.parse(dateStr, DATE_ONLY_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ignored) {}
        // ISO-8601 with timezone offset (e.g. 2026-03-05T16:30:00-04:00)
        try {
            return OffsetDateTime.parse(dateStr).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}
        // yyyy-MM-dd HH:mm:ss
        try {
            return LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException ignored) {}

        log.warn("Failed to parse date: {}", dateStr);
        return null;
    }

    /**
     * Extract plain text description from SAM.gov description list
     * API returns: [{"body": "...", "label": "..."}]
     */
    private String extractDescription(List<Map<String, String>> descriptionList) {
        if (descriptionList == null || descriptionList.isEmpty()) return null;
        return descriptionList.stream()
                .map(m -> m.getOrDefault("body", ""))
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * Extract place of performance from officeAddress map
     * API returns: {"city": "...", "state": "...", "countryCode": "US"}
     */
    private String extractPlaceOfPerformance(Map<String, Object> officeAddress) {
        if (officeAddress == null) return null;
        String city = String.valueOf(officeAddress.getOrDefault("city", "")).trim();
        String state = String.valueOf(officeAddress.getOrDefault("state", "")).trim();
        if (!city.isEmpty() && !state.isEmpty()) return city + ", " + state;
        if (!city.isEmpty()) return city;
        if (!state.isEmpty()) return state;
        return null;
    }

    /**
     * Calculate content hash for change detection
     */
    private String calculateHash(SAMOpportunityResponse.OpportunityData data) {
        String content = String.format("%s|%s|%s|%s|%s",
                data.getTitle(),
                data.getType(),
                data.getPostedDate(),
                data.getResponseDeadLine(),
                data.getOrganizationName());
        return DigestUtils.md5DigestAsHex(content.getBytes());
    }

    /**
     * Collection result
     */
    public record CollectionResult(int collected, int duplicates, int errors) {
    }
}
