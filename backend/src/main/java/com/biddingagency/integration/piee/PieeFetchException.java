package com.biddingagency.integration.piee;

/** CR-160: PIEE 입찰서류를 받지 못했다. 메시지는 관리자 화면의 받기 실패 사유로 그대로 보인다 */
public class PieeFetchException extends RuntimeException {

    public PieeFetchException(String message) {
        super(message);
    }

    public PieeFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
