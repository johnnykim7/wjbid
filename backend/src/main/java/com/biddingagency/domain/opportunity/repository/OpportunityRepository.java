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
     * Find active opportunities.
     * 정렬 고정: 게시일(postedDate) DESC → 동률 시 수집순(createdAt) DESC.
     * (postedDate가 같은날 일괄 수집으로 동률이 많아 2차 정렬 필수)
     */
    Page<Opportunity> findByActiveTrueOrderByPostedDateDescCreatedAtDesc(Pageable pageable);

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

    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND (LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.solicitationNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.noticeId) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Opportunity> searchAdmin(@Param("keyword") String keyword, Pageable pageable);

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
     * CR-118: 관리자 원본 공고 검색/필터 통합 쿼리.
     * 모든 파라미터는 nullable — null이면 해당 조건 무시(전체).
     *  - keyword: 제목(title/titleKo) + 본문(descriptionBody/descriptionSummaryKo) + 공고번호 LIKE
     *  - type: 공고유형(type) 정확 일치
     *  - hasAttachment: TRUE면 첨부 존재(EXISTS), FALSE면 첨부 없음(NOT EXISTS), null이면 무시
     * 정렬: 게시일 DESC → 수집순 DESC (목록 기본 정렬과 동일).
     */
    @Query("SELECT o FROM Opportunity o WHERE o.active = true " +
            "AND (:keyword IS NULL OR " +
            "     LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.titleKo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.descriptionBody) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.descriptionSummaryKo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.solicitationNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.noticeId) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:type IS NULL OR o.type = :type) " +
            "AND (:hasAttachment IS NULL " +
            "  OR (:hasAttachment = TRUE  AND EXISTS (SELECT 1 FROM OpportunityAttachment a WHERE a.opportunity = o)) " +
            "  OR (:hasAttachment = FALSE AND NOT EXISTS (SELECT 1 FROM OpportunityAttachment a WHERE a.opportunity = o))) " +
            "ORDER BY o.postedDate DESC, o.createdAt DESC")
    Page<Opportunity> searchAdminFiltered(@Param("keyword") String keyword,
                                          @Param("type") String type,
                                          @Param("hasAttachment") Boolean hasAttachment,
                                          Pageable pageable);

    /**
     * CR-118: 공고유형 셀렉트 옵션용 — 수집된 type/typeKo DISTINCT 목록.
     * type 기준 정렬. typeKo는 null일 수 있어 화면에서 type fallback.
     */
    @Query("SELECT DISTINCT o.type, o.typeKo FROM Opportunity o " +
            "WHERE o.active = true AND o.type IS NOT NULL ORDER BY o.type")
    List<Object[]> findDistinctTypes();

    /**
     * Find the latest posted date across all opportunities (for incremental sync)
     */
    @Query("SELECT MAX(o.postedDate) FROM Opportunity o")
    Optional<LocalDateTime> findLatestPostedDate();
}
