package com.biddingagency.domain.opportunity;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * SAM.gov noticeType(영문) → 한글 라벨 코드 매핑 (CR-022).
 *
 * <p>type은 SAM 표준값(유한 집합)이므로 LLM 번역이 아니라 정적 코드 매핑으로 처리한다.
 * 매핑에 없는 신규 type이 들어오면 null 반환 → 호출부는 영문 fallback + 로그.
 * 신규 type이 로그에 찍히면 이 MAP에 추가한다(단일 출처).
 */
@Slf4j
public final class NoticeTypeTranslator {

    private NoticeTypeTranslator() {}

    private static final Map<String, String> MAP = Map.of(
            "Solicitation", "입찰요청",
            "Combined Synopsis/Solicitation", "공고+입찰요청 통합",
            "Award Notice", "낙찰공지",
            "Sources Sought", "사전수요조사",
            "Presolicitation", "사전공고"
    );

    /**
     * 영문 type → 한글 라벨. 매핑 없으면 null (호출부에서 영문 fallback).
     */
    public static String toKorean(String type) {
        if (type == null || type.isBlank()) return null;
        String ko = MAP.get(type.trim());
        if (ko == null) {
            log.warn("[CR-022] 미매핑 noticeType 발견 — 영문 노출, NoticeTypeTranslator MAP에 추가 필요: type='{}'", type);
        }
        return ko;
    }
}
