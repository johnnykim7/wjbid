package com.biddingagency.domain.member.repository;

import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberDocumentRepository extends JpaRepository<MemberDocument, UUID> {

    List<MemberDocument> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    /** 고정 카테고리 1슬롯=1서류 — 기존 보유분 조회(교체 판단) */
    Optional<MemberDocument> findFirstByMemberIdAndCategory(UUID memberId, DocumentCategory category);

    List<MemberDocument> findByMemberIdAndCategory(UUID memberId, DocumentCategory category);

    /** CR-136: 만료일이 그날인 서류(만료 메일 대상) */
    List<MemberDocument> findByExpiresOn(java.time.LocalDate expiresOn);
}
