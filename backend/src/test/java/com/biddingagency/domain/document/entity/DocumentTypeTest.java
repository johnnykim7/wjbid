package com.biddingagency.domain.document.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-DOC-290: 평가받는 문서 (CR-178, BIZ-095)
 */
class DocumentTypeTest {

    @Test
    @DisplayName("TC-DOC-290 문서종류_평가받는문서_기술제안서과거실적가격요약만")
    void 문서종류_평가받는문서_기술제안서과거실적가격요약만() {
        assertThat(Arrays.stream(DocumentType.values()).filter(DocumentType::isEvaluated))
                .containsExactlyInAnyOrder(DocumentType.TECHNICAL_PROPOSAL, DocumentType.PAST_PERFORMANCE,
                        DocumentType.PRICING_SUMMARY);
    }
}
