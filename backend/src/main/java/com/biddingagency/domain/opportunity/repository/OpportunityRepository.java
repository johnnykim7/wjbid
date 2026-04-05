package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Opportunity repository
 */
@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    /**
     * Find by notice ID
     */
    Optional<Opportunity> findByNoticeId(String noticeId);

    /**
     * Check if notice ID exists
     */
    boolean existsByNoticeId(String noticeId);

    /**
     * Find active opportunities
     */
    Page<Opportunity> findByActiveTrue(Pageable pageable);

    /**
     * Find by deadline range
     */
    Page<Opportunity> findByResponseDeadlineBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Find opportunities near deadline
     */
    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND o.responseDeadline > :now " +
            "AND o.responseDeadline < :deadline " +
            "ORDER BY o.responseDeadline ASC")
    List<Opportunity> findNearDeadline(@Param("now") LocalDateTime now,
                                       @Param("deadline") LocalDateTime deadline);

    /**
     * Search by title
     */
    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Opportunity> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Search by organization
     */
    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND LOWER(o.organizationName) LIKE LOWER(CONCAT('%', :organization, '%'))")
    Page<Opportunity> searchByOrganization(@Param("organization") String organization, Pageable pageable);

    /**
     * Find recently posted
     */
    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND o.postedDate > :since " +
            "ORDER BY o.postedDate DESC")
    List<Opportunity> findRecentlyPosted(@Param("since") LocalDateTime since);

    /**
     * Find the latest posted date across all opportunities (for incremental sync)
     */
    @Query("SELECT MAX(o.postedDate) FROM Opportunity o")
    Optional<LocalDateTime> findLatestPostedDate();

    /**
     * Find active + visible opportunities (CR-003: customer-facing)
     */
    Page<Opportunity> findByActiveTrueAndVisibility(OpportunityVisibility visibility, Pageable pageable);

    /**
     * Search by title with visibility filter (CR-003)
     */
    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND o.visibility = :visibility " +
            "AND LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Opportunity> searchByTitleAndVisibility(@Param("keyword") String keyword,
                                                 @Param("visibility") OpportunityVisibility visibility,
                                                 Pageable pageable);

    /**
     * Search by organization with visibility filter (CR-003)
     */
    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND o.visibility = :visibility " +
            "AND LOWER(o.organizationName) LIKE LOWER(CONCAT('%', :organization, '%'))")
    Page<Opportunity> searchByOrganizationAndVisibility(@Param("organization") String organization,
                                                        @Param("visibility") OpportunityVisibility visibility,
                                                        Pageable pageable);
}
