package com.biddingagency.domain.bookmark.repository;

import com.biddingagency.domain.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    @Query("SELECT b FROM Bookmark b JOIN FETCH b.opportunity WHERE b.member.id = :memberId")
    Page<Bookmark> findByMemberIdWithOpportunity(@Param("memberId") UUID memberId, Pageable pageable);

    Optional<Bookmark> findByMemberIdAndOpportunityId(UUID memberId, UUID opportunityId);

    boolean existsByMemberIdAndOpportunityId(UUID memberId, UUID opportunityId);

    void deleteByMemberIdAndOpportunityId(UUID memberId, UUID opportunityId);

    long countByMemberId(UUID memberId);
}
