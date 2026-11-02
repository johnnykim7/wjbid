package com.biddingagency.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-AUTH-039 (CR-186): 새 비밀번호가 짧으면 요청 검사에서 막혀 재설정(번호 쓰기)까지 가지 않는다
 */
class AuthFlowRequestsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // TC-AUTH-039
    @Test
    @DisplayName("새비밀번호7자_재설정요청_검증오류")
    void 새비밀번호8자미만_재설정요청_막힘() {
        var request = new AuthFlowRequests.ResetPassword("user@example.com", "1234567");

        assertThat(validator.validate(request))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("newPassword");
    }

    @Test
    @DisplayName("새비밀번호8자_재설정요청_통과")
    void 새비밀번호8자_재설정요청_통과() {
        var request = new AuthFlowRequests.ResetPassword("user@example.com", "12345678");

        assertThat(validator.validate(request)).isEmpty();
    }
}
