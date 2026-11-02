package com.biddingagency.domain.member.repository;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.member.entity.EmailCodePurpose;
import com.biddingagency.domain.member.entity.EmailVerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-AUTH-020 (CR-146): 가입에 쓴 번호는 가입 때 찾는 조회에서 빠져 같은 번호로 두 번 가입하지 못한다.
 * TC-AUTH-035 ~ 037 (CR-186): 확인을 마친 번호도 받은 쓰임에서만 찾고, 한 번 쓰면 빠진다 (BIZ-097).
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
        EmailVerificationCode code = verifiedCode(EmailCodePurpose.SIGNUP);
        code.markConsumed(AT.plusMinutes(2));
        codeRepository.saveAndFlush(code);

        assertThat(findVerified(EmailCodePurpose.SIGNUP)).isEmpty();
    }

    @Test
    @DisplayName("확인만한번호_가입조회_찾음")
    void 확인만한번호_가입조회_나옴() {
        codeRepository.saveAndFlush(verifiedCode(EmailCodePurpose.SIGNUP));

        assertThat(findVerified(EmailCodePurpose.SIGNUP)).isPresent();
    }

    // TC-AUTH-035
    @Test
    @DisplayName("가입번호만확인_재설정조회_찾지못함")
    void 가입번호만확인_재설정조회_빠짐() {
        codeRepository.saveAndFlush(verifiedCode(EmailCodePurpose.SIGNUP));

        assertThat(findVerified(EmailCodePurpose.PASSWORD_RESET)).isEmpty();
    }

    // TC-AUTH-036
    @Test
    @DisplayName("재설정번호만확인_가입조회_찾지못함")
    void 재설정번호만확인_가입조회_빠짐() {
        codeRepository.saveAndFlush(verifiedCode(EmailCodePurpose.PASSWORD_RESET));

        assertThat(findVerified(EmailCodePurpose.SIGNUP)).isEmpty();
    }

    // TC-AUTH-037
    @Test
    @DisplayName("재설정에쓴번호_다시재설정_찾지못함")
    void 재설정에쓴번호_재설정조회_빠짐() {
        EmailVerificationCode code = verifiedCode(EmailCodePurpose.PASSWORD_RESET);
        code.markConsumed(AT.plusMinutes(2));
        codeRepository.saveAndFlush(code);

        assertThat(findVerified(EmailCodePurpose.PASSWORD_RESET)).isEmpty();
    }

    // TC-AUTH-038 의 조회 쪽
    @Test
    @DisplayName("가입번호만있음_재설정쓰임마지막번호_없음")
    void 가입번호만있음_재설정마지막번호_없음() {
        codeRepository.saveAndFlush(verifiedCode(EmailCodePurpose.SIGNUP));

        assertThat(codeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(EMAIL, EmailCodePurpose.PASSWORD_RESET))
                .isEmpty();
    }

    private Optional<EmailVerificationCode> findVerified(EmailCodePurpose purpose) {
        return codeRepository.findFirstByEmailAndPurposeAndVerifiedAtIsNotNullAndConsumedAtIsNullOrderByVerifiedAtDesc(
                EMAIL, purpose);
    }

    private static EmailVerificationCode verifiedCode(EmailCodePurpose purpose) {
        EmailVerificationCode code = EmailVerificationCode.builder()
                .email(EMAIL)
                .purpose(purpose)
                .codeHash("h".repeat(64))
                .expiresAt(AT.plusMinutes(10))
                .attempts(0)
                .createdAt(AT)
                .build();
        code.markVerified(AT.plusMinutes(1));
        return code;
    }
}
