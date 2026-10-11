package com.biddingagency.domain.base.repository;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 기지 찾기에 쓰는 공고 제목 조회 (CR-136).
 * 원본 JSON 과 본문을 읽지 않도록 필요한 칸만 꺼낸다.
 */
public interface OpportunityTitleRepository extends Repository<Opportunity, UUID> {

    @Query("SELECT new com.biddingagency.domain.base.repository.OpportunityTitleView(" +
            "o.id, o.noticeId, o.solicitationNumber, o.title, o.titleKo, o.type, o.postedDate, o.responseDeadline) " +
            "FROM Opportunity o WHERE o.deletedAt IS NULL")
    List<OpportunityTitleView> findAllLive();
}
