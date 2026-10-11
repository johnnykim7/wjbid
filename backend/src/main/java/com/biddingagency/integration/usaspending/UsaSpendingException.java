package com.biddingagency.integration.usaspending;

/**
 * USAspending 호출 실패 (CR-142, BIZ-053).
 * clientError 는 다시 불러도 같은 결과인 4xx(429 제외)다. 상세 호출이면 그 계약만 건너뛴다.
 */
public class UsaSpendingException extends RuntimeException {

    private final int status;
    private final boolean clientError;

    public UsaSpendingException(String message, int status, boolean clientError, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.clientError = clientError;
    }

    /** 응답 코드. 연결 실패면 0 */
    public int getStatus() { return status; }

    public boolean isClientError() { return clientError; }
}
