package com.biddingagency.domain.notice.repository;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    /** 한 원본에 딸린 공고문들 (1:N) */
    List<Notice> findByOpportunityId(UUID opportunityId);

    /** 한 원본의 가장 최근 공고문 1건 (중복 생성 방지용 — 이미 있으면 이걸 재사용) */
    Optional<Notice> findFirstByOpportunityIdOrderByCreatedAtDesc(UUID opportunityId);

    boolean existsByOpportunityId(UUID opportunityId);

    long countByGenerationStatus(NoticeGenerationStatus status);

    /**
     * 고객 노출 공고문 (CR-016: VISIBLE만).
     * CR-009: includeExpired=false면 마감 지난 공고 제외(마감일 NULL은 유지). 정렬은 Pageable.
     */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o WHERE n.visibility = :visibility "
            + "AND (:includeExpired = true OR o.responseDeadline IS NULL OR o.responseDeadline >= :now)")
    Page<Notice> findVisible(@Param("visibility") OpportunityVisibility visibility,
                             @Param("includeExpired") boolean includeExpired,
                             @Param("now") java.time.LocalDateTime now,
                             Pageable pageable);

    /** 고객 상세 — 노출 공고문 단건 (opportunity FETCH: 트랜잭션 밖 DTO 변환 시 LazyInit 방지) */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o WHERE n.id = :id AND n.visibility = :visibility")
    Optional<Notice> findByIdAndVisibility(@Param("id") UUID id,
                                           @Param("visibility") OpportunityVisibility visibility);

    /** 작성 컨텍스트용 — 한 원본의 노출 중인 공고문 중 최신 1건 (AIWorkflow P1) */
    Optional<Notice> findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(
            UUID opportunityId, OpportunityVisibility visibility);

    /**
     * 고객 검색 — 노출 공고문의 한글 제목 또는 원본 제목에 키워드 포함.
     * CR-009: includeExpired=false면 마감 지난 공고 제외(마감일 NULL은 유지). 정렬은 Pageable.
     */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o WHERE n.visibility = :visibility "
            + "AND (:includeExpired = true OR o.responseDeadline IS NULL OR o.responseDeadline >= :now) "
            + "AND (LOWER(n.koreanTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Notice> searchVisibleByKeyword(@Param("keyword") String keyword,
                                        @Param("visibility") OpportunityVisibility visibility,
                                        @Param("includeExpired") boolean includeExpired,
                                        @Param("now") java.time.LocalDateTime now,
                                        Pageable pageable);

    /** 마감 임박 노출 공고문 (now < deadline < limit) */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o WHERE n.visibility = :visibility "
            + "AND o.responseDeadline > :now AND o.responseDeadline < :limit ORDER BY o.responseDeadline ASC")
    List<Notice> findVisibleNearDeadline(@Param("now") java.time.LocalDateTime now,
                                         @Param("limit") java.time.LocalDateTime limit,
                                         @Param("visibility") OpportunityVisibility visibility);

    /** 최근 게시 노출 공고문 (posted >= since) */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o WHERE n.visibility = :visibility "
            + "AND o.postedDate >= :since ORDER BY o.postedDate DESC")
    List<Notice> findVisibleRecentlyPosted(@Param("since") java.time.LocalDateTime since,
                                           @Param("visibility") OpportunityVisibility visibility);
}
