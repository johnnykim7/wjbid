package com.biddingagency.domain.proposal.repository;

import com.biddingagency.domain.proposal.entity.VerificationLog;
import com.biddingagency.domain.proposal.entity.VerificationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, UUID> {

    /** target 별 최신 검증 1건 (관리자 콘솔 배지·패널). */
    Optional<VerificationLog> findFirstByTargetTypeAndTargetIdOrderByVerifiedAtDesc(
            VerificationTargetType targetType, UUID targetId);

    /** target 의 전체 검증 이력 (재시도 회차 포함). */
    List<VerificationLog> findByTargetTypeAndTargetIdOrderByVerifiedAtDesc(
            VerificationTargetType targetType, UUID targetId);
}
