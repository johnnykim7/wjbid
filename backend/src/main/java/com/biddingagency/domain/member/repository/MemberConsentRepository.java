package com.biddingagency.domain.member.repository;

import com.biddingagency.domain.member.entity.MemberConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 가입 동의 이력 저장소 (CR-124)
 */
public interface MemberConsentRepository extends JpaRepository<MemberConsent, UUID> {

    /** 특정 회원의 동의 이력 (최신순) */
    List<MemberConsent> findByMemberIdOrderByAgreedAtDesc(UUID memberId);
}
