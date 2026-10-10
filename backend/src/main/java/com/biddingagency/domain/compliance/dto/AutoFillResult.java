package com.biddingagency.domain.compliance.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * CR-125 2단계: 서류함 자동채움 결과.
 *
 * 화면(ProposalDetailPage)은 이 응답을 받고 getClientDocuments로 슬롯을 재조회한다.
 */
@Getter
@Builder
public class AutoFillResult {

    /** 자동 채워진 슬롯 수 */
    private int filledCount;

    /** 채워진 서류명 목록(토스트/안내용) */
    private List<String> filledNames;

    /** 서류함에 없어 못 채운 서류명(있으면 수동 업로드 안내) */
    private List<String> unmatchedNames;
}
