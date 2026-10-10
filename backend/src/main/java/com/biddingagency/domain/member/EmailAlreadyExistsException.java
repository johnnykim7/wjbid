package com.biddingagency.domain.member;

/**
 * CR-131: 이미 가입된 이메일로 가입을 시도한 경우 (BIZ-008).
 * 고객 화면이 이 사유를 알아보고 이메일 칸에 안내하도록 409 와 오류 코드로 매핑된다.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("이미 가입된 이메일입니다");
    }
}
