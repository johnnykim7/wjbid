package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.document.entity.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.biddingagency.domain.document.entity.DocumentType.COVER_LETTER;
import static com.biddingagency.domain.document.entity.DocumentType.PAST_PERFORMANCE;
import static com.biddingagency.domain.document.entity.DocumentType.PRICING_SUMMARY;
import static com.biddingagency.domain.document.entity.DocumentType.TECHNICAL_PROPOSAL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-DOC-292 ~ TC-DOC-296: 공고가 요구하는 문서 구성 (CR-179, BIZ-096)
 */
class DocumentCompositionTest {

    private static final List<DocumentType> FALLBACK = List.of(COVER_LETTER, TECHNICAL_PROPOSAL);

    private static Map<String, Object> factor(String id, Object documentType) {
        Map<String, Object> factor = new HashMap<>();
        factor.put("factorId", id);
        factor.put("title", "FACTOR " + id);
        if (documentType != null) factor.put("documentType", documentType);
        factor.put("subfactors", List.of(Map.of("subfactorId", id + ".1", "title", "Sub " + id)));
        return factor;
    }

    private static Map<String, Object> tree(Boolean coverLetterRequired, Map<?, ?>... factors) {
        Map<String, Object> tree = new HashMap<>();
        tree.put("method", "LPTA");
        tree.put("factors", List.of(factors));
        if (coverLetterRequired != null) tree.put("coverLetterRequired", coverLetterRequired);
        return tree;
    }

    private static Map<String, Object> groundMaintenance(boolean coverLetterRequired) {
        return tree(coverLetterRequired, factor("F1", "TECHNICAL_PROPOSAL"), factor("F2", "PAST_PERFORMANCE"),
                factor("F3", "PRICING_SUMMARY"));
    }

    @Test
    @DisplayName("TC-DOC-292 문서구성_평가항목문서종류대로_기술과거실적가격순")
    void 평가항목문서종류대로_기술과거실적가격순() {
        DocumentComposition composition = DocumentComposition.of(groundMaintenance(false), FALLBACK);

        assertThat(composition.documents()).containsExactly(TECHNICAL_PROPOSAL, PAST_PERFORMANCE, PRICING_SUMMARY);
    }

    @Test
    @DisplayName("TC-DOC-293 문서구성_커버레터요구_맨앞에커버레터")
    void 커버레터요구_맨앞에커버레터() {
        DocumentComposition composition = DocumentComposition.of(groundMaintenance(true), FALLBACK);

        assertThat(composition.documents()).containsExactly(COVER_LETTER, TECHNICAL_PROPOSAL, PAST_PERFORMANCE, PRICING_SUMMARY);
    }

    @Test
    @DisplayName("TC-DOC-294 문서구성_두키없는옛공고_설정목록")
    void 두키없는옛공고_설정목록() {
        DocumentComposition old = DocumentComposition.of(tree(null, factor("F1", null), factor("F2", null)), FALLBACK);
        DocumentComposition none = DocumentComposition.of(null, FALLBACK);

        assertThat(List.of(old.documents(), none.documents(), old.ownerOf("F2"))).containsExactly(FALLBACK, FALLBACK, TECHNICAL_PROPOSAL);
    }

    @Test
    @DisplayName("TC-DOC-295 문서구성_같은문서종류여러항목_한번만")
    void 같은문서종류여러항목_한번만() {
        DocumentComposition composition = DocumentComposition.of(
                tree(false, factor("F1", "TECHNICAL_PROPOSAL"), factor("F2", "TECHNICAL_PROPOSAL")), FALLBACK);

        assertThat(composition.documents()).containsExactly(TECHNICAL_PROPOSAL);
    }

    @Test
    @DisplayName("TC-DOC-296 문서구성_모르는문서종류_첫평가문서가받음")
    void 모르는문서종류_첫평가문서가받음() {
        Map<String, Object> tree = tree(false, factor("F1", "TECHNICAL_PROPOSAL"), factor("F2", "VOLUME_X"));
        DocumentComposition composition = DocumentComposition.of(tree, FALLBACK);

        assertThat(List.of(composition.documents(), composition.ownerOf("F2"), composition.ownerOf("F2.1"),
                DocumentComposition.unknownDocumentTypes(tree)))
                .containsExactly(List.of(TECHNICAL_PROPOSAL), TECHNICAL_PROPOSAL, TECHNICAL_PROPOSAL, List.of("VOLUME_X"));
    }
}
