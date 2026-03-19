package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Member service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register new member (CUSTOMER role)
     */
    @Transactional
    public Member register(String email, String password, String companyName, String contactPerson, String phone) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        Member member = Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .companyName(companyName)
                .contactPerson(contactPerson)
                .phone(phone)
                .role(Member.Role.CUSTOMER)
                .build();

        Member saved = memberRepository.save(member);
        log.info("New member registered: {}", saved.getEmail());
        return saved;
    }

    public Member findById(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + email));
    }

    @Transactional
    public Member updateProfile(UUID memberId, String companyName, String contactPerson, String phone, String address) {
        Member member = findById(memberId);
        member.updateProfile(companyName, contactPerson, phone, address);
        log.info("Member profile updated: {}", member.getEmail());
        return member;
    }

    @Transactional
    public void changePassword(UUID memberId, String currentPassword, String newPassword) {
        Member member = findById(memberId);
        if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        member.updatePassword(passwordEncoder.encode(newPassword));
        log.info("Password changed for member: {}", member.getEmail());
    }

    @Transactional
    public void updateLastLogin(UUID memberId) {
        Member member = findById(memberId);
        member.updateLastLogin();
    }
}
