package com.biddingagency.domain.sharedfile;

import org.springframework.http.HttpStatus;

public enum SharedFileError {

    NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "빈 파일은 올릴 수 없습니다"),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "파일 이름을 읽을 수 없습니다"),
    BLOCKED_EXTENSION(HttpStatus.BAD_REQUEST, "이 종류의 파일은 올릴 수 없습니다"),
    TOO_LARGE(HttpStatus.BAD_REQUEST, "한 파일 크기 상한을 넘었습니다"),
    MAIL_FAILED(HttpStatus.BAD_GATEWAY, "비밀번호 메일을 보내지 못해 파일을 저장하지 않았습니다"),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 맞지 않습니다"),
    LOCKED(HttpStatus.TOO_MANY_REQUESTS, "많이 틀려 잠겼습니다"),
    NOT_OWNER(HttpStatus.FORBIDDEN, "올린 사람만 지울 수 있습니다"),
    CORRUPT(HttpStatus.INTERNAL_SERVER_ERROR, "저장 파일이 손상되어 받을 수 없습니다");

    private final HttpStatus status;
    private final String message;

    SharedFileError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
