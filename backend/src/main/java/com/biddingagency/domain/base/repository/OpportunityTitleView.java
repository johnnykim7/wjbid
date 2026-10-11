package com.biddingagency.domain.base.repository;

import java.time.LocalDateTime;
import java.util.UUID;

/** 기지 찾기에 쓰는 공고 칸 (CR-136) */
public record OpportunityTitleView(UUID id, String noticeId, String solicitationNumber, String title, String titleKo,
                                   String type, LocalDateTime postedDate, LocalDateTime responseDeadline) {

    /** 공고번호 기준 묶음 키. 공고번호가 없으면 SAM 공고 ID 로 센다 */
    public String numberKey() {
        return solicitationNumber != null && !solicitationNumber.isBlank()
                ? solicitationNumber.trim() : "NOTICE:" + noticeId;
    }
}
