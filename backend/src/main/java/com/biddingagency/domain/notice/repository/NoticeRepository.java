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

    /** CR-141 제출 전 검사 목록: 여러 원본의 공고문을 한 번에 */
    List<Notice> findByOpportunityIdIn(java.util.Collection<UUID> opportunityIds);

    long countByGenerationStatus(NoticeGenerationStatus status);

    /**
     * CR-141: 분석은 끝났는데 요구 항목이 0행인 공고의 공고문. 서버가 뜰 때 요구 항목을 한 번 채운다(BIZ-045).
     * 한 공고에 공고문이 여럿이면 최근 분석이 앞에 온다.
     */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o "
            + "WHERE n.generationStatus = com.biddingagency.domain.notice.entity.NoticeGenerationStatus.COMPLETED "
            + "AND NOT EXISTS (SELECT 1 FROM OpportunityRequirementItem ri WHERE ri.opportunity = o) "
            + "ORDER BY n.analyzedAt DESC")
    List<Notice> findCompletedWithoutRequirementItems();

    /**
     * CR-039: stuck 자동 정리 대상 — ANALYZING 상태로 임계 시각 이전에 시작돼 멈춘 공고문.
     * analysisStartedAt이 NULL인 경우(구 데이터)는 V35 마이그레이션에서 updated_at으로 보정됨.
     */
    @Query("SELECT n FROM Notice n WHERE n.generationStatus = com.biddingagency.domain.notice.entity.NoticeGenerationStatus.ANALYZING "
            + "AND n.analysisStartedAt IS NOT NULL AND n.analysisStartedAt < :threshold")
    List<Notice> findStuckAnalyzing(@Param("threshold") java.time.LocalDateTime threshold);

    /**
     * 고객 노출 공고문 (CR-016: VISIBLE만).
     * CR-009: includeExpired=false면 마감 지난 공고 제외(마감일 NULL은 유지). 정렬은 Pageable.
     */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o WHERE n.visibility = :visibility "
            + "AND (:includeExpired = true OR o.responseDeadline IS NULL OR o.responseDeadline >= :now) "
            + "ORDER BY o.postedDate DESC, n.createdAt DESC")
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

    /** 최근 게시 노출 공고문 (posted >= since) */
    @Query("SELECT n FROM Notice n JOIN FETCH n.opportunity o WHERE n.visibility = :visibility "
            + "AND o.postedDate >= :since ORDER BY o.postedDate DESC")
    List<Notice> findVisibleRecentlyPosted(@Param("since") java.time.LocalDateTime since,
                                           @Param("visibility") OpportunityVisibility visibility);

    /**
     * CR-135: 노출 공고문의 (원본 id, 공고문 id). 한 원본에 노출 공고문이 여럿이면 뒤에 오는 것이 최근 분석본이다.
     */
    @Query("SELECT n.opportunity.id, n.id FROM Notice n WHERE n.visibility = :visibility "
            + "ORDER BY n.analyzedAt ASC NULLS FIRST")
    List<Object[]> findOpportunityAndNoticeIds(@Param("visibility") OpportunityVisibility visibility);
}
