package com.biddingagency.integration.samgov.client;

import com.biddingagency.domain.opportunity.service.IndustryClassifier;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.domain.rfp.entity.IndustryType;
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
    private final IndustryClassifier industryClassifier;

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

        int created = 0;
        int targetCreated = 0;
        int changed = 0;
        int unchanged = 0;
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
                        ProcessResult result = processOpportunity(data);
                        switch (result.result()) {
                            case NEW -> {
                                created++;
                                // CR-015: IndustryClassifier 매칭(시설관리 타깃) 신규만 별도 집계.
                                // q=korea 전문검색이 끌어오는 무관 부품조달(미분류)은 메일 카운트에서 제외.
                                if (result.industryType() != null) {
                                    targetCreated++;
                                }
                            }
                            case CHANGED -> changed++;   // CR-009: 기존이지만 contentHash 변경
                            case UNCHANGED -> unchanged++; // CR-009: 변동 없음
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

        log.info("Collection completed for keyword '{}': {} new ({} target), {} changed, {} unchanged, {} errors",
                keyword, created, targetCreated, changed, unchanged, errors);

        return new CollectionResult(created, targetCreated, changed, unchanged, errors);
    }

    /**
     * Process single opportunity.
     * CR-009: upsert 결과(NEW/CHANGED/UNCHANGED)와 분류 결과를 반환한다.
     */
    private ProcessResult processOpportunity(SAMOpportunityResponse.OpportunityData data) {
        String noticeId = data.getNoticeId();

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

        // CR-014: 사업유형 자동분류 (BIZ-018). naics/PSC/title 기반, 미매칭 시 null
        IndustryType industryType = industryClassifier.classify(
                data.getNaicsCode(), data.getClassificationCode(), data.getTitle());

        // Create or update
        OpportunityService.UpsertOutcome outcome = opportunityService.createOrUpdate(
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
                contentHash,
                industryType,
                data.getResourceLinks() // CR-019: SAM 첨부 적재
        );

        return new ProcessResult(outcome.result(), industryType); // upsert 결과 + 분류 결과
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
     * Extract plain text description from SAM.gov description field.
     * API returns either:
     * - List of maps: [{"body": "...", "label": "..."}]
     * - Plain String: "description text"
     */
    @SuppressWarnings("unchecked")
    private String extractDescription(Object description) {
        if (description == null) return null;
        if (description instanceof String s) {
            return s.isBlank() ? null : s;
        }
        if (description instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object body = map.get("body");
                    if (body instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
            }
        }
        return null;
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
     * Collection result.
     * collected = 전체 신규(NEW), targetNew = 그중 IndustryClassifier 매칭(시설관리 타깃) 신규 (CR-015),
     * changed = 기존이지만 contentHash 변경(CR-009 실제 갱신), unchanged = 변동 없음.
     */
    public record CollectionResult(int collected, int targetNew, int changed, int unchanged, int errors) {
    }

    /** processOpportunity 결과: upsert 결과(NEW/CHANGED/UNCHANGED) + 분류된 사업유형(미분류 시 null). */
    private record ProcessResult(OpportunityService.UpsertResult result, IndustryType industryType) {
    }
}
