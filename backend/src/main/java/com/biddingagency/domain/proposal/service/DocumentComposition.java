package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.document.entity.DocumentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * BIZ-096 공고가 요구하는 문서 구성. 공고 분석이 평가 항목 구조에 남긴 평가 항목별 문서 종류와 커버레터 요구 여부로 정한다.
 * 두 키가 다 없는 옛 공고는 설정 목록(POL-005)을 쓰고, 그때 매트릭스 줄은 모두 첫 평가받는 문서에 속한다.
 * 정부 양식 요구가 있으면 어느 쪽이든 맨 끝에 정부 양식 문서를 둔다(BIZ-102).
 *
 * @param documents    만들 문서 종류, 만드는 순서
 * @param primary      문서 종류가 없거나 모르는 평가 항목과 평가 항목이 없는 매트릭스 줄을 받는 문서. 구성의 첫 평가받는 문서
 * @param factorOwners 평가 항목 ID 와 하위 항목 ID 마다 그 항목을 담는 문서 종류
 */
public record DocumentComposition(List<DocumentType> documents, DocumentType primary, Map<String, DocumentType> factorOwners) {

    public static final String KEY_DOCUMENT_TYPE = "documentType";
    public static final String KEY_COVER_LETTER_REQUIRED = "coverLetterRequired";
    private static final String KEY_FACTORS = "factors";
    private static final String KEY_FACTOR_ID = "factorId";
    private static final String KEY_SUBFACTORS = "subfactors";
    private static final String KEY_SUBFACTOR_ID = "subfactorId";
    private static final String KEY_LINES = "lines";
    private static final String KEY_FULFILLMENT_PARTY = "fulfillmentParty";

    public static DocumentComposition of(Map<String, Object> factorTree, List<DocumentType> fallback, boolean formsRequired) {
        List<Map<String, Object>> factors = listOfMaps(factorTree == null ? null : factorTree.get(KEY_FACTORS));
        boolean declared = factorTree != null && (factorTree.containsKey(KEY_COVER_LETTER_REQUIRED)
                || factors.stream().anyMatch(factor -> factor.containsKey(KEY_DOCUMENT_TYPE)));
        if (!declared) {
            DocumentType primary = fallback.stream().filter(DocumentType::isEvaluated).findFirst().orElse(null);
            return new DocumentComposition(withForms(fallback, formsRequired), primary, Map.of());
        }

        TreeSet<DocumentType> evaluated = new TreeSet<>();
        factors.forEach(factor -> {
            DocumentType type = evaluatedType(factor.get(KEY_DOCUMENT_TYPE));
            if (type != null) evaluated.add(type);
        });
        if (evaluated.isEmpty()) evaluated.add(DocumentType.TECHNICAL_PROPOSAL);
        DocumentType primary = evaluated.first();

        Map<String, DocumentType> owners = new HashMap<>();
        for (Map<String, Object> factor : factors) {
            DocumentType type = evaluatedType(factor.get(KEY_DOCUMENT_TYPE));
            DocumentType owner = type != null ? type : primary;
            putOwner(owners, factor.get(KEY_FACTOR_ID), owner);
            listOfMaps(factor.get(KEY_SUBFACTORS)).forEach(sub -> putOwner(owners, sub.get(KEY_SUBFACTOR_ID), owner));
        }

        List<DocumentType> documents = new ArrayList<>();
        if (Boolean.parseBoolean(String.valueOf(factorTree.get(KEY_COVER_LETTER_REQUIRED)))) {
            documents.add(DocumentType.COVER_LETTER);
        }
        documents.addAll(evaluated);
        return new DocumentComposition(withForms(documents, formsRequired), primary, Map.copyOf(owners));
    }

    /** 공고 요구 서류의 하위 항목이나 매트릭스 줄 가운데 시스템이 양식으로 채우는 것이 하나라도 있는가 */
    public static boolean requiresForms(Map<String, Object> requiredDocuments, Map<String, Object> complianceMatrix) {
        boolean inDocuments = listOfMaps(requiredDocuments == null ? null : requiredDocuments.get(KEY_FACTORS)).stream()
                .flatMap(factor -> listOfMaps(factor.get(KEY_SUBFACTORS)).stream())
                .anyMatch(DocumentComposition::isSystemForm);
        return inDocuments || listOfMaps(complianceMatrix == null ? null : complianceMatrix.get(KEY_LINES)).stream()
                .anyMatch(DocumentComposition::isSystemForm);
    }

    /** 평가 항목에 적힌 문서 종류 가운데 평가받는 문서 종류가 아닌 값. 저장할 때 경고로 남긴다 */
    public static List<String> unknownDocumentTypes(Map<String, Object> factorTree) {
        if (factorTree == null) return List.of();
        return listOfMaps(factorTree.get(KEY_FACTORS)).stream()
                .map(factor -> factor.get(KEY_DOCUMENT_TYPE))
                .filter(value -> value != null && evaluatedType(value) == null)
                .map(String::valueOf)
                .toList();
    }

    /** 매트릭스 줄의 factorId(평가 항목이나 하위 항목 ID)가 담기는 문서 종류 */
    public DocumentType ownerOf(String factorId) {
        return factorId == null ? primary : factorOwners.getOrDefault(factorId.trim(), primary);
    }

    public boolean owns(DocumentType documentType, String factorId) {
        return documentType != null && documentType == ownerOf(factorId);
    }

    /** 가격 항목을 받는 문서. 가격 요약이 구성에 없으면 첫 평가받는 문서 */
    public DocumentType priceOwner() {
        return documents.contains(DocumentType.PRICING_SUMMARY) ? DocumentType.PRICING_SUMMARY : primary;
    }

    /** 그 문서가 담는 평가 항목만 남긴 평가 항목 구조. 평가 방식 같은 다른 키는 그대로 둔다 */
    public Map<String, Object> factorTreeFor(DocumentType documentType, Map<String, Object> factorTree) {
        if (factorTree == null) return null;
        Map<String, Object> scoped = new LinkedHashMap<>(factorTree);
        scoped.put(KEY_FACTORS, listOfMaps(factorTree.get(KEY_FACTORS)).stream()
                .filter(factor -> owns(documentType, factor.get(KEY_FACTOR_ID) == null ? null : factor.get(KEY_FACTOR_ID).toString()))
                .toList());
        return scoped;
    }

    private static DocumentType evaluatedType(Object value) {
        if (value == null) return null;
        try {
            DocumentType type = DocumentType.valueOf(value.toString().trim().toUpperCase());
            return type.isEvaluated() ? type : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<DocumentType> withForms(List<DocumentType> documents, boolean formsRequired) {
        List<DocumentType> result = new ArrayList<>(documents);
        if (formsRequired && !result.contains(DocumentType.GOVERNMENT_FORMS)) result.add(DocumentType.GOVERNMENT_FORMS);
        return List.copyOf(result);
    }

    private static boolean isSystemForm(Map<String, Object> item) {
        Object party = item.get(KEY_FULFILLMENT_PARTY);
        return party != null && FulfillmentParty.fromAnalysis(party.toString()) == FulfillmentParty.SYSTEM_FORM;
    }

    private static void putOwner(Map<String, DocumentType> owners, Object id, DocumentType owner) {
        if (id != null && !id.toString().isBlank()) owners.put(id.toString().trim(), owner);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
    }
}
