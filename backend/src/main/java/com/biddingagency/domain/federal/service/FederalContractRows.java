package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.federal.entity.FederalAwardType;
import com.biddingagency.domain.federal.entity.FederalContract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * USAspending 응답 줄을 미 연방 계약 행으로 옮긴다 (CR-142, BID-MKT-025, BID-MKT-026).
 */
final class FederalContractRows {

    /** 계약 고유 키 CONT_AWD_{계약번호}_{기관}_{상위 계약번호}_{상위 기관} 에서 상위 계약이 없을 때의 값 */
    private static final String NO_PARENT = "-NONE-";

    private FederalContractRows() {
    }

    /** 검색 결과 한 줄. 계약 고유 키나 계약번호가 없거나 계약 종류를 모르면 비어 있다 */
    static Optional<FederalContract> fromSearch(Map<String, Object> row) {
        String key = text(row.get("generated_internal_id"));
        String piid = text(row.get("Award ID"));
        Optional<FederalAwardType> type = FederalAwardType.fromSearchLabel(text(row.get("Contract Award Type")));
        if (key == null || piid == null || type.isEmpty()) return Optional.empty();
        Map<String, Object> naics = map(row.get("NAICS"));
        Map<String, Object> psc = map(row.get("PSC"));
        Map<String, Object> raw = new HashMap<>();
        raw.put("search", row);
        return Optional.of(FederalContract.builder()
                .awardKey(key)
                .piid(piid)
                .parentPiid(parentPiid(row, key))
                .awardType(type.get())
                .recipientName(text(row.get("Recipient Name")))
                .recipientUei(text(row.get("Recipient UEI")))
                .startDate(date(row.get("Start Date")))
                .endDate(date(row.get("End Date")))
                .amountUsd(amount(row.get("Award Amount")))
                .naicsCode(text(naics.get("code")))
                .naicsName(text(naics.get("description")))
                .pscCode(text(psc.get("code")))
                .description(text(row.get("Description")))
                .awardingSubAgency(text(row.get("Awarding Sub Agency")))
                .sourceModifiedDate(date(row.get("Last Modified Date")))
                .rawJson(raw)
                .build());
    }

    /** 상세 응답에서 옵션 끝날짜 */
    static LocalDate potentialEndDate(Map<String, Object> detail) {
        return date(map(detail.get("period_of_performance")).get("potential_end_date"));
    }

    /** 상세 응답에서 원래 공고번호 */
    static String solicitationNumber(Map<String, Object> detail) {
        return text(map(detail.get("latest_transaction_contract_data")).get("solicitation_identifier"));
    }

    /** 상세 응답 중 보관할 칸. 상세 전체는 크고 대부분 쓰지 않는다 */
    static Map<String, Object> detailRaw(Map<String, Object> detail) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("generated_unique_award_id", detail.get("generated_unique_award_id"));
        raw.put("period_of_performance", detail.get("period_of_performance"));
        raw.put("solicitation_identifier", solicitationNumber(detail));
        raw.put("parent_award", detail.get("parent_award"));
        raw.put("base_and_all_options", detail.get("base_and_all_options"));
        return raw;
    }

    private static String parentPiid(Map<String, Object> row, String key) {
        String parent = text(row.get("Parent Award ID"));
        if (parent != null) return parent;
        String[] parts = key.split("_");
        return parts.length > 4 && !NO_PARENT.equals(parts[4]) ? parts[4] : null;
    }

    private static Map<String, Object> map(Object o) {
        if (o instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked") Map<String, Object> cast = (Map<String, Object>) m;
            return cast;
        }
        return Map.of();
    }

    private static String text(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** "2026-06-28" 또는 "2026-06-28 15:19:35" 에서 날짜 */
    private static LocalDate date(Object o) {
        String s = text(o);
        if (s == null || s.length() < 10) return null;
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal amount(Object o) {
        if (o instanceof Number n) return new BigDecimal(n.toString());
        String s = text(o);
        if (s == null) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
