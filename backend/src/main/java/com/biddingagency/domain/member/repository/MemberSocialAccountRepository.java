package com.biddingagency.domain.member.repository;

import com.biddingagency.domain.member.entity.MemberSocialAccount;
import com.biddingagency.domain.member.entity.SnsProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 회원과 이어진 SNS 계정 저장소 (CR-146)
 */
public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, UUID> {

    Optional<MemberSocialAccount> findByProviderAndProviderUserId(SnsProvider provider, String providerUserId);
}
