package com.biddingagency.domain.document.service;

import com.biddingagency.domain.document.entity.DocumentTemplate;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.repository.DocumentTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-TPL-001 ~ TC-TPL-003: DocumentTemplateService 테스트
 */
@ExtendWith(MockitoExtension.class)
class DocumentTemplateServiceTest {

    @Mock
    private DocumentTemplateRepository templateRepository;

    @InjectMocks
    private DocumentTemplateService templateService;

    // TC-TPL-001: 템플릿 등록
    @Test
    @DisplayName("유효데이터_템플릿등록_DocumentTemplate생성")
    void 유효데이터_save_템플릿생성() {
        // given
        given(templateRepository.findByDocumentTypeOrderByTemplateVersionDesc(DocumentType.COVER_LETTER))
                .willReturn(List.of());
        given(templateRepository.save(any(DocumentTemplate.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        DocumentTemplate result = templateService.save("커버레터 v1", DocumentType.COVER_LETTER, Map.of("type", "doc"));

        // then
        assertThat(result.getTemplateName()).isEqualTo("커버레터 v1");
        assertThat(result.getDocumentType()).isEqualTo(DocumentType.COVER_LETTER);
        assertThat(result.getTemplateVersion()).isEqualTo(1);
        assertThat(result.getActive()).isTrue();
    }

    // 기존 버전이 있을 때 버전 자동 증가
    @Test
    @DisplayName("기존버전존재_템플릿등록_버전자동증가")
    void 기존버전있음_save_버전증가() {
        // given
        DocumentTemplate existing = DocumentTemplate.builder()
                .templateName("커버레터 v1")
                .documentType(DocumentType.COVER_LETTER)
                .templateVersion(2)
                .contentJson(Map.of())
                .active(true)
                .build();
        given(templateRepository.findByDocumentTypeOrderByTemplateVersionDesc(DocumentType.COVER_LETTER))
                .willReturn(List.of(existing));
        given(templateRepository.save(any(DocumentTemplate.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        DocumentTemplate result = templateService.save("커버레터 v3", DocumentType.COVER_LETTER, Map.of("type", "doc"));

        // then
        assertThat(result.getTemplateVersion()).isEqualTo(3);
    }

    // TC-TPL-003: 비활성화
    @Test
    @DisplayName("활성템플릿_비활성화_active_false")
    void 활성템플릿_deactivate_비활성화() {
        // given
        UUID id = UUID.randomUUID();
        DocumentTemplate template = DocumentTemplate.builder()
                .templateName("Test")
                .documentType(DocumentType.COVER_LETTER)
                .templateVersion(1)
                .contentJson(Map.of())
                .active(true)
                .build();
        given(templateRepository.findById(id)).willReturn(Optional.of(template));

        // when
        templateService.deactivate(id);

        // then
        assertThat(template.getActive()).isFalse();
    }

    // 존재하지 않는 템플릿 비활성화 시도
    @Test
    @DisplayName("미존재템플릿_비활성화시도_NoSuchElementException")
    void 미존재_deactivate_예외() {
        // given
        UUID id = UUID.randomUUID();
        given(templateRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> templateService.deactivate(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    // 활성 템플릿 조회
    @Test
    @DisplayName("문서타입_활성템플릿조회_존재하면반환")
    void 유효타입_getActiveTemplate_반환() {
        // given
        DocumentTemplate template = DocumentTemplate.builder()
                .templateName("T")
                .documentType(DocumentType.TECHNICAL_PROPOSAL)
                .templateVersion(1)
                .contentJson(Map.of())
                .active(true)
                .build();
        given(templateRepository.findLatestActiveByDocumentType(DocumentType.TECHNICAL_PROPOSAL))
                .willReturn(Optional.of(template));

        // when
        DocumentTemplate result = templateService.getActiveTemplate(DocumentType.TECHNICAL_PROPOSAL);

        // then
        assertThat(result.getDocumentType()).isEqualTo(DocumentType.TECHNICAL_PROPOSAL);
    }

    // 활성 템플릿 없을 때
    @Test
    @DisplayName("활성템플릿없음_getActiveTemplate_NoSuchElementException")
    void 없음_getActiveTemplate_예외() {
        // given
        given(templateRepository.findLatestActiveByDocumentType(DocumentType.OTHER))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> templateService.getActiveTemplate(DocumentType.OTHER))
                .isInstanceOf(NoSuchElementException.class);
    }
}
