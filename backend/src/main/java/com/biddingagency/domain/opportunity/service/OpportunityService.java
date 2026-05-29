package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.rfp.entity.IndustryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Opportunity service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;

    /**
     * Find opportunity by ID
     */
    public Opportunity findById(UUID id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + id));
    }

    /**
     * Find opportunity by notice ID
     */
    public Opportunity findByNoticeId(String noticeId) {
        return opportunityRepository.findByNoticeId(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + noticeId));
    }

    /**
     * Check if opportunity exists by notice ID
     */
    public boolean existsByNoticeId(String noticeId) {
        return opportunityRepository.existsByNoticeId(noticeId);
    }

    /**
     * Find all active opportunities
     */
    public Page<Opportunity> findAllActive(Pageable pageable) {
        return opportunityRepository.findByActiveTrue(pageable);
    }

    /**
     * Search opportunities by keyword
     */
    public Page<Opportunity> searchByKeyword(String keyword, Pageable pageable) {
        return opportunityRepository.searchByTitle(keyword, pageable);
    }

    /**
     * Search by organization
     */
    public Page<Opportunity> searchByOrganization(String organization, Pageable pageable) {
        return opportunityRepository.searchByOrganization(organization, pageable);
    }

    /**
     * Find opportunities near deadline
     */
    public List<Opportunity> findNearDeadline(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusDays(days);
        return opportunityRepository.findNearDeadline(now, deadline);
    }

    /**
     * Find recently posted
     */
    public List<Opportunity> findRecentlyPosted(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return opportunityRepository.findRecentlyPosted(since);
    }

    /**
     * Find the latest posted date for incremental sync
     */
    public Optional<LocalDateTime> findLatestPostedDate() {
        return opportunityRepository.findLatestPostedDate();
    }

    /**
     * Create or update opportunity.
     * CR-009: 반환을 UpsertOutcome으로 감싸 NEW/CHANGED/UNCHANGED를 구분한다.
     */
    @Transactional
    public UpsertOutcome createOrUpdate(String noticeId, String solicitationNumber,
                                        String title, String type, String organizationName,
                                        LocalDateTime postedDate, LocalDateTime responseDeadline,
                                        String uiLink, String descriptionLink,
                                        Map<String, Object> rawJson, String contentHash,
                                        IndustryType industryType) {
        // Check if exists
        return opportunityRepository.findByNoticeId(noticeId)
                .map(existing -> {
                    // Update if content changed
                    if (!contentHash.equals(existing.getContentHash())) {
                        existing.updateContent(title, type, organizationName, postedDate,
                                responseDeadline, uiLink, descriptionLink, rawJson, contentHash);
                        existing.assignIndustryType(industryType); // CR-014: 재수집 시 재분류
                        log.info("Opportunity updated: {}", noticeId);
                        return new UpsertOutcome(existing, UpsertResult.CHANGED);
                    }
                    return new UpsertOutcome(existing, UpsertResult.UNCHANGED);
                })
                .orElseGet(() -> {
                    // Create new
                    Opportunity opportunity = Opportunity.builder()
                            .noticeId(noticeId)
                            .solicitationNumber(solicitationNumber)
                            .title(title)
                            .type(type)
                            .organizationName(organizationName)
                            .industryType(industryType) // CR-014: 수집 시 자동분류 결과
                            .postedDate(postedDate)
                            .responseDeadline(responseDeadline)
                            .active(true)
                            .uiLink(uiLink)
                            .descriptionLink(descriptionLink)
                            .contentHash(contentHash)
                            .firstSeenAt(LocalDateTime.now())
                            .lastModifiedAt(LocalDateTime.now())
                            .rawJson(rawJson)
                            .build();
                    Opportunity saved = opportunityRepository.save(opportunity);
                    log.info("New opportunity created: {}", noticeId);
                    return new UpsertOutcome(saved, UpsertResult.NEW);
                });
    }

    /**
     * CR-009: 수집 시 upsert 결과 분류.
     * NEW=신규 생성, CHANGED=기존이지만 contentHash 변경, UNCHANGED=변동 없음.
     */
    public enum UpsertResult { NEW, CHANGED, UNCHANGED }

    public record UpsertOutcome(Opportunity opportunity, UpsertResult result) {
    }

    /**
     * Mark opportunity as inactive
     */
    @Transactional
    public void markAsInactive(UUID id) {
        Opportunity opportunity = findById(id);
        opportunity.markAsInactive();
        log.info("Opportunity marked as inactive: {}", opportunity.getNoticeId());
    }

}
