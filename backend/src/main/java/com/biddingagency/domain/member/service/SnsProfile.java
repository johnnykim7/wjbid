package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.entity.SnsProvider;

/**
 * SNS 가 로그인 뒤 넘긴 계정 정보 (CR-146).
 *
 * @param email         SNS 가 준 이메일. 고객이 제공을 거절하면 없다
 * @param emailVerified SNS 가 그 이메일을 확인했다고 표시했는지. 네이버는 표시가 없어 늘 false 다
 */
public record SnsProfile(SnsProvider provider, String providerUserId, String email, boolean emailVerified) {
}
