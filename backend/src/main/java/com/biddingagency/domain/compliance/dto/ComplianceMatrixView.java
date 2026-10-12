package com.biddingagency.domain.compliance.dto;

import java.util.List;
import java.util.Map;

/**
 * 관리자 공고문 관리 상세의 컴플라이언스 매트릭스 영역 (CR-159). 읽기만 한다.
 */
public record ComplianceMatrixView(List<Line> lines, List<Question> questions, Map<String, Object> factorTree) {

    public record Line(String reqId, String level, String text, String translation, String sourceFile, String page,
                       String clause, String quote, String verb, String category, String fulfillmentParty,
                       String factorId, String factorTitle, boolean queryCandidate) {
    }

    public record Question(String reqId, String ambiguity, String draftQuery) {
    }
}
