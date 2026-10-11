package com.biddingagency.domain.bid;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * CR-143: 입찰 건이 없거나, 낸 회원도 관리자도 아닌 회원이 찾은 경우.
 * 남의 건도 없는 건과 같은 404 로 답해 그 번호의 건이 있는지 알리지 않는다 (BIZ-060).
 */
public class BidRequestNotFoundException extends ResponseStatusException {

    public BidRequestNotFoundException() {
        super(HttpStatus.NOT_FOUND, "입찰 건을 찾을 수 없습니다");
    }
}
