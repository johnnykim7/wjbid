package com.biddingagency.security;

import com.biddingagency.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Custom UserDetails implementation
 * Extends Spring Security User to include Member entity
 */
@Getter
public class CustomUserDetails extends User {

    private final Member member;

    public CustomUserDetails(Member member, Collection<? extends GrantedAuthority> authorities) {
        super(
                member.getEmail(),
                // 스프링 User 는 null 비밀번호를 받지 않는다. 빈 해시는 어떤 비밀번호와도 맞지 않아
                // SNS 로만 가입한 회원의 비밀번호 로그인은 틀린 비밀번호와 같은 응답으로 막힌다 (CR-146, BIZ-064)
                member.getPasswordHash() != null ? member.getPasswordHash() : "",
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked (구독 체크 제거)
                authorities
        );
        this.member = member;
    }
}
