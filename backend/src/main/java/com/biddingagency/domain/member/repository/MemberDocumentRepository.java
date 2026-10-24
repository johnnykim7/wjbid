package com.biddingagency.domain.member.repository;

import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.DocumentLanguage;
import com.biddingagency.domain.member.entity.MemberDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberDocumentRepository extends JpaRepository<MemberDocument, UUID> {

    List<MemberDocument> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    /** 고정 카테고리 1슬롯=1서류 — 기존 보유분 조회(교체 판단) */
    Optional<MemberDocument> findFirstByMemberIdAndCategory(UUID memberId, DocumentCategory category);

    /** 국문·영문 칸은 언어마다 한 건(CR-147, BIZ-065) */
    Optional<MemberDocument> findFirstByMemberIdAndCategoryAndLanguage(UUID memberId, DocumentCategory category,
                                                                      DocumentLanguage language);

    List<MemberDocument> findByMemberIdAndCategory(UUID memberId, DocumentCategory category);

    /** CR-136: 만료일이 그날인 서류(만료 메일 대상) */
    List<MemberDocument> findByExpiresOn(java.time.LocalDate expiresOn);

    /** CR-169: 같은 내용의 파일 하나를 읽는 꼴로 내준다 */
    Optional<MemberDocument> findFirstByContentSha256(String contentSha256);
}
