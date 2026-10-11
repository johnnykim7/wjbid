package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.MarketGroup;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 메뉴 속 화면 목록 한 줄 (CR-135, T3-2 C-2 「목록 줄」). CR-136 의 낙찰 업체, 기지별 목록도 같은 줄을 쓴다.
 *
 * @param awardeeName            낙찰 업체 이름. 업체 칸이 생기기 전(CR-136 전)에는 늘 null
 * @param publicNoticeId         같은 공고의 공개 한글 공고문 id. 있으면 화면이 공고 상세로 보낸다
 * @param hasMainNotice          사전 공고 줄만. 같은 공고번호로 입찰 공고가 올라왔는지
 * @param originalId             낙찰 공지 줄만. 같은 공고번호의 원래 공고 중 마지막 판 id
 * @param originalPublicNoticeId 낙찰 공지 줄만. 원래 공고의 공개 한글 공고문 id
 * @param originalSamUrl         낙찰 공지 줄만. 원래 공고의 SAM.gov 원문 주소
 */
public record MarketRowDto(
        UUID id,
        String noticeId,
        String solicitationNumber,
        String title,
        String titleKo,
        String type,
        String typeKo,
        MarketGroup group,
        LocalDate postedDate,
        OffsetDateTime responseDeadline,
        String awardAmount,
        String awardeeName,
        UUID publicNoticeId,
        String samUrl,
        Boolean hasMainNotice,
        UUID originalId,
        UUID originalPublicNoticeId,
        String originalSamUrl
) {
}
