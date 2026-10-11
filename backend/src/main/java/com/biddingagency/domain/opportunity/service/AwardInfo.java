package com.biddingagency.domain.opportunity.service;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * CR-136: SAM 검색 응답의 낙찰 정보(award) 중 저장하는 값.
 * 형식이 맞지 않는 값은 버리고 null 로 둔다. 잘못 읽은 값을 저장하는 것보다 비워 두는 편이 낫다(BIZ-025).
 */
@Slf4j
public record AwardInfo(String awardeeName, String awardeeUei, BigDecimal amountUsd) {

    /** SAM 응답의 낙찰 공지 유형 이름 */
    public static final String AWARD_NOTICE_TYPE = "Award Notice";

    private static final int NAME_MAX = 255;
    private static final int UEI_MAX = 20;
    /** DECIMAL(15,2) 의 정수 자리 */
    private static final int AMOUNT_INTEGER_DIGITS = 13;

    public static boolean isAwardNotice(String samType) {
        return AWARD_NOTICE_TYPE.equalsIgnoreCase(samType);
    }

    public static AwardInfo from(Map<String, Object> award) {
        if (award == null) return new AwardInfo(null, null, null);
        String name = null;
        String uei = null;
        if (award.get("awardee") instanceof Map<?, ?> awardee) {
            name = text(awardee.get("name"));
            if (name != null && name.length() > NAME_MAX) name = name.substring(0, NAME_MAX);
            uei = text(awardee.get("ueiSAM"));
            if (uei != null && uei.length() > UEI_MAX) {
                log.warn("[CR-136] UEI 형식이 맞지 않아 버립니다: {}", uei);
                uei = null;
            }
        }
        return new AwardInfo(name, uei, amount(award.get("amount")));
    }

    static BigDecimal amount(Object raw) {
        String s = text(raw);
        if (s == null) return null;
        try {
            BigDecimal v = new BigDecimal(s.replace("$", "").replace(",", "").trim()).setScale(2, RoundingMode.HALF_UP);
            if (v.signum() < 0 || v.precision() - v.scale() > AMOUNT_INTEGER_DIGITS) {
                log.warn("[CR-136] 낙찰 금액이 범위를 벗어나 버립니다: {}", s);
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
