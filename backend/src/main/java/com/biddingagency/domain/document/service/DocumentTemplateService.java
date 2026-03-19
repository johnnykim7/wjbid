package com.biddingagency.domain.document.service;

import com.biddingagency.domain.document.entity.DocumentTemplate;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTemplateService {

    private final DocumentTemplateRepository templateRepository;

    /** 특정 문서 타입의 최신 활성 템플릿 조회 */
    @Transactional(readOnly = true)
    public DocumentTemplate getActiveTemplate(DocumentType documentType) {
        return templateRepository.findLatestActiveByDocumentType(documentType)
            .orElseThrow(() -> new NoSuchElementException(
                documentType.name() + " 타입의 활성 템플릿이 없습니다. 어드민에서 등록해주세요."));
    }

    /** 전체 활성 템플릿 목록 */
    @Transactional(readOnly = true)
    public List<DocumentTemplate> findAll() {
        return templateRepository.findAllByActiveTrueOrderByDocumentTypeAsc();
    }

    /** 특정 문서 타입의 전체 버전 이력 */
    @Transactional(readOnly = true)
    public List<DocumentTemplate> findByDocumentType(DocumentType documentType) {
        return templateRepository.findByDocumentTypeOrderByTemplateVersionDesc(documentType);
    }

    /** 새 템플릿 저장 (버전 자동 계산) */
    @Transactional
    public DocumentTemplate save(String templateName, DocumentType documentType, Map<String, Object> contentJson) {
        // 현재 최신 버전 조회 후 +1
        int nextVersion = templateRepository.findByDocumentTypeOrderByTemplateVersionDesc(documentType)
            .stream().findFirst()
            .map(t -> t.getTemplateVersion() + 1)
            .orElse(1);

        DocumentTemplate template = DocumentTemplate.builder()
            .templateName(templateName)
            .documentType(documentType)
            .templateVersion(nextVersion)
            .contentJson(contentJson)
            .active(true)
            .build();

        DocumentTemplate saved = templateRepository.save(template);
        log.info("DocumentTemplate 저장: type={}, version={}, id={}", documentType, nextVersion, saved.getId());
        return saved;
    }

    /** 템플릿 비활성화 */
    @Transactional
    public void deactivate(UUID id) {
        DocumentTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("템플릿을 찾을 수 없습니다: " + id));
        template.deactivate();
        log.info("DocumentTemplate 비활성화: id={}, type={}", id, template.getDocumentType());
    }
}
