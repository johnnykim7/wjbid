package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * BIZ-096 문서 작성 시작, 제안서 설계 입력, 목차 저장, 커버리지가 같은 공고문으로 같은 문서 구성을 보게 한다.
 */
@Service
public class DocumentCompositionService {

    private final NoticeRepository noticeRepository;
    private final List<DocumentType> fallbackDocumentTypes;

    public DocumentCompositionService(NoticeRepository noticeRepository,
                                      @Value("${app.ai.auto-generate-document-types:COVER_LETTER,TECHNICAL_PROPOSAL}") String fallbackDocumentTypes) {
        this.noticeRepository = noticeRepository;
        this.fallbackDocumentTypes = parseDocumentTypes(fallbackDocumentTypes);
    }

    /** 제안서 설계가 보는 공고문. 노출 중이고 분석이 끝난 최신 1건 */
    public Optional<Notice> designNotice(UUID opportunityId) {
        return noticeRepository
                .findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(opportunityId, OpportunityVisibility.VISIBLE)
                .filter(Notice::isGenerationCompleted);
    }

    public DocumentComposition compose(UUID opportunityId) {
        return compose(designNotice(opportunityId).orElse(null));
    }

    public DocumentComposition compose(Notice notice) {
        return DocumentComposition.of(notice == null ? null : notice.getFactorTreeJson(), fallbackDocumentTypes);
    }

    private static List<DocumentType> parseDocumentTypes(String typesConfig) {
        return Arrays.stream(typesConfig.split(","))
                .map(String::trim)
                .map(s -> {
                    try { return DocumentType.valueOf(s.toUpperCase()); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
