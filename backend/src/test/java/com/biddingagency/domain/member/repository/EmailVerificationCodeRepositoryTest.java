package com.biddingagency.domain.member.repository;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.member.entity.EmailVerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-AUTH-020 (CR-146): 가입에 쓴 번호는 가입 때 찾는 조회에서 빠져 같은 번호로 두 번 가입하지 못한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class EmailVerificationCodeRepositoryTest {

    private static final String EMAIL = "reuse@example.com";
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 12, 9, 0);

    @Autowired
    private EmailVerificationCodeRepository codeRepository;

    // TC-AUTH-020
    @Test
    @DisplayName("가입에쓴번호_다시가입_찾지못함")
    void 가입에쓴번호_가입조회_빠짐() {
        EmailVerificationCode code = verifiedCode();
        code.markConsumed(AT.plusMinutes(2));
        codeRepository.saveAndFlush(code);

        assertThat(codeRepository.findFirstByEmailAndVerifiedAtIsNotNullAndConsumedAtIsNullOrderByVerifiedAtDesc(EMAIL))
                .isEmpty();
    }

    @Test
    @DisplayName("확인만한번호_가입조회_찾음")
    void 확인만한번호_가입조회_나옴() {
        codeRepository.saveAndFlush(verifiedCode());

        assertThat(codeRepository.findFirstByEmailAndVerifiedAtIsNotNullAndConsumedAtIsNullOrderByVerifiedAtDesc(EMAIL))
                .isPresent();
    }

    private static EmailVerificationCode verifiedCode() {
        EmailVerificationCode code = EmailVerificationCode.builder()
                .email(EMAIL)
                .codeHash("h".repeat(64))
                .expiresAt(AT.plusMinutes(10))
                .attempts(0)
                .createdAt(AT)
                .build();
        code.markVerified(AT.plusMinutes(1));
        return code;
    }
}
