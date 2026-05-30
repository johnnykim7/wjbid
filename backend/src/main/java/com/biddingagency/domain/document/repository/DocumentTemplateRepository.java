package com.biddingagency.domain.document.repository;

import com.biddingagency.domain.document.entity.DocumentTemplate;
import com.biddingagency.domain.document.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID> {

    /** 특정 문서 타입의 활성 템플릿 조회 (버전 내림차순 → 최신 1개) */
    @Query("SELECT t FROM DocumentTemplate t WHERE t.documentType = :documentType AND t.active = true ORDER BY t.templateVersion DESC")
    Optional<DocumentTemplate> findLatestActiveByDocumentType(DocumentType documentType);

    /** 모든 활성 템플릿 목록 */
    List<DocumentTemplate> findAllByActiveTrueOrderByDocumentTypeAsc();

    /** 활성/비활성 무관 전체 목록 (관리자 화면용) */
    @Query("SELECT t FROM DocumentTemplate t ORDER BY t.documentType ASC, t.templateVersion DESC")
    List<DocumentTemplate> findAllOrderByDocumentTypeAndVersion();

    /** 특정 문서 타입의 전체 버전 이력 */
    List<DocumentTemplate> findByDocumentTypeOrderByTemplateVersionDesc(DocumentType documentType);
}
