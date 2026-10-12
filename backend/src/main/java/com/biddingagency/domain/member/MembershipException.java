package com.biddingagency.domain.member;

/**
 * 회원 전용 막음과 회원 기간 넣기, 지우기의 오류 (CR-157)
 */
public class MembershipException extends RuntimeException {

    private final MembershipError error;

    public MembershipException(MembershipError error) {
        super(error.message());
        this.error = error;
    }

    public MembershipError getError() {
        return error;
    }
}
