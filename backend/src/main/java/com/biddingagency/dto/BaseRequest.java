package com.biddingagency.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 기지 추가·고치기 본문 (CR-136). 검증은 MilitaryBaseService 가 한다.
 */
public record BaseRequest(String nameKo, String nameEn, List<String> matchTerms, String region,
                          BigDecimal latitude, BigDecimal longitude, Integer sortOrder) {
}
