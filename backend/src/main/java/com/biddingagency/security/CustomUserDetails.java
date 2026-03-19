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
                member.getPasswordHash(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked (구독 체크 제거)
                authorities
        );
        this.member = member;
    }
}
