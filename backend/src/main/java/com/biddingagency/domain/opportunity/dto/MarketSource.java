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
 * 업종 코드는 원본 JSON 에만 있어 두 번째 생성자가 꺼낸다.
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
        String naicsCode
) {

    public MarketSource(UUID id, String noticeId, String solicitationNumber, String title, String titleKo,
                        String type, String typeKo, LocalDateTime postedDate, LocalDateTime responseDeadline,
                        String awardAmount, BigDecimal awardAmountUsd, String awardeeName,
                        String awardeeUei, Map<String, Object> rawJson) {
        this(id, noticeId, solicitationNumber, title, titleKo, type, typeKo, postedDate, responseDeadline,
                awardAmount, awardAmountUsd, awardeeName, awardeeUei, naicsOf(rawJson));
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

    private static String naicsOf(Map<String, Object> rawJson) {
        if (rawJson == null) return null;
        Object v = rawJson.get("naicsCode");
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
