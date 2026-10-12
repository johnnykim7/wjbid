package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.MarketGroup;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 메뉴 속 화면이 모은 공고에서 읽는 칸만 담은 것 (CR-135).
 *
 * <p>본문과 분석 결과까지 실린 공고 전체를 수백 건 읽지 않으려고 조회 단계에서 이 칸만 고른다.
 * 업종 코드는 원본 JSON 에만 있어 두 번째 생성자가 꺼낸다. 이행 장소는 짧은 이름이 비면 원본 JSON 값을 쓴다(CR-155).
 */
public record MarketSource(
        UUID id,
        String noticeId,
        String solicitationNumber,
        String title,
        String titleKo,
        String type,
        String typeKo,
        LocalDateTime postedDate,
        LocalDateTime responseDeadline,
        String awardAmount,
        BigDecimal awardAmountUsd,
        String awardeeName,
        String awardeeUei,
        String naicsCode,
        String organizationName,
        String placeOfPerformance
) {

    public MarketSource(UUID id, String noticeId, String solicitationNumber, String title, String titleKo,
                        String type, String typeKo, LocalDateTime postedDate, LocalDateTime responseDeadline,
                        String awardAmount, BigDecimal awardAmountUsd, String awardeeName,
                        String awardeeUei, String organizationName, String placeOfPerformanceShort,
                        Map<String, Object> rawJson) {
        this(id, noticeId, solicitationNumber, title, titleKo, type, typeKo, postedDate, responseDeadline,
                awardAmount, awardAmountUsd, awardeeName, awardeeUei, textOf(rawJson, "naicsCode"),
                organizationName, placeOf(placeOfPerformanceShort, rawJson));
    }

    public MarketGroup group() {
        return MarketGroup.of(type);
    }

    /** 공고번호 앞뒤 공백을 걷은 값. 비었으면 null (같은 공고 묶기에 쓰지 않음) */
    public String solKey() {
        if (solicitationNumber == null) return null;
        String s = solicitationNumber.trim();
        return s.isEmpty() ? null : s;
    }

    private static String placeOf(String placeShort, Map<String, Object> rawJson) {
        if (placeShort != null && !placeShort.isBlank()) return placeShort.trim();
        // 원본 JSON 의 이행 장소가 묶음 값이면 화면에 보일 글이 아니라 쓰지 않는다
        if (rawJson == null || !(rawJson.get("placeOfPerformance") instanceof String)) return null;
        return textOf(rawJson, "placeOfPerformance");
    }

    private static String textOf(Map<String, Object> rawJson, String key) {
        if (rawJson == null) return null;
        Object v = rawJson.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
