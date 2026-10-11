package com.biddingagency.domain.eligibility.repository;

import com.biddingagency.domain.eligibility.entity.EligibilityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EligibilityAnswerRepository extends JpaRepository<EligibilityAnswer, UUID> {

    List<EligibilityAnswer> findByMemberIdAndNoticeId(UUID memberId, UUID noticeId);

    List<EligibilityAnswer> findByMemberIdAndNoticeIdIn(UUID memberId, Collection<UUID> noticeIds);

    Optional<EligibilityAnswer> findByMemberIdAndNoticeIdAndItemNo(UUID memberId, UUID noticeId, Integer itemNo);
}
