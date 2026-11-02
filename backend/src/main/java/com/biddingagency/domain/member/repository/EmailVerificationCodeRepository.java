package com.biddingagency.domain.member.repository;

import com.biddingagency.domain.member.entity.EmailCodePurpose;
import com.biddingagency.domain.member.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 이메일 인증번호 저장소 (CR-146, CR-186)
 */
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    /** 그 이메일에 마지막으로 보낸 번호. 재요청 간격은 쓰임을 가리지 않는다 */
    Optional<EmailVerificationCode> findFirstByEmailOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtGreaterThanEqual(String email, LocalDateTime since);

    /** 그 쓰임으로 마지막에 보낸 번호 */
    Optional<EmailVerificationCode> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, EmailCodePurpose purpose);

    /** 그 쓰임에서 확인을 마쳤고 아직 쓰지 않은 번호 중 마지막 */
    Optional<EmailVerificationCode> findFirstByEmailAndPurposeAndVerifiedAtIsNotNullAndConsumedAtIsNullOrderByVerifiedAtDesc(
            String email, EmailCodePurpose purpose);
}
