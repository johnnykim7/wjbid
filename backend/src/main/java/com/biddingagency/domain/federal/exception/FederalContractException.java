package com.biddingagency.domain.federal.exception;

import org.springframework.http.HttpStatus;

/**
 * 미 연방 계약 자료와 회원 계약 요청 오류 (CR-142, T3-2 C-4, C-5, B-5). 본문은 {error, message}.
 */
public class FederalContractException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    private FederalContractException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public static FederalContractException badRequest(String error, String message) {
        return new FederalContractException(HttpStatus.BAD_REQUEST, error, message);
    }

    public static FederalContractException notFound(String error, String message) {
        return new FederalContractException(HttpStatus.NOT_FOUND, error, message);
    }

    public static FederalContractException conflict(String error, String message) {
        return new FederalContractException(HttpStatus.CONFLICT, error, message);
    }

    public static FederalContractException unprocessable(String error, String message) {
        return new FederalContractException(HttpStatus.UNPROCESSABLE_ENTITY, error, message);
    }

    public static FederalContractException badGateway(String error, String message) {
        return new FederalContractException(HttpStatus.BAD_GATEWAY, error, message);
    }

    public HttpStatus getStatus() { return status; }

    public String getError() { return error; }
}
