package com.biddingagency.domain.analysis;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 입찰 분석과 Bid/No-Bid 결정이 요청을 받지 못할 때 (CR-170).
 * 화면은 code 로 갈라 안내한다. 다시 분석과 결정이 막힌 때는 reason 이 그 사유다.
 */
@Getter
public class BidAnalysisException extends RuntimeException {

    public enum Reason {
        NOT_ANALYZING,
        ANALYSIS_RUNNING,
        READING_IN_PROGRESS
    }

    private final HttpStatus status;
    private final String code;
    private final Reason reason;

    private BidAnalysisException(HttpStatus status, String code, Reason reason, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.reason = reason;
    }

    public static BidAnalysisException notAllowed(Reason reason) {
        return new BidAnalysisException(HttpStatus.CONFLICT, "BID_ANALYSIS_NOT_ALLOWED", reason,
                "지금은 할 수 없습니다");
    }

    /** BIZ-092: 조건부 Go 는 조건, No-Go 는 사유가 있어야 한다 */
    public static BidAnalysisException noteRequired() {
        return new BidAnalysisException(HttpStatus.BAD_REQUEST, "DECISION_NOTE_REQUIRED", null,
                "조건부 Go 는 조건, No-Go 는 사유를 기재하세요");
    }

    /** BIZ-092: 지금 분석의 관리자 결정이 Go 나 조건부 Go 여야 문서 작성 중으로 전환한다 */
    public static BidAnalysisException decisionRequired() {
        return new BidAnalysisException(HttpStatus.CONFLICT, "BID_DECISION_REQUIRED", null,
                "Bid/No-Bid 결정을 Go 나 조건부 Go 로 정해야 문서 작성으로 넘어갑니다");
    }
}
