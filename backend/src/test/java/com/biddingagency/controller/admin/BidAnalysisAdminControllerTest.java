package com.biddingagency.controller.admin;

import com.biddingagency.domain.analysis.service.BidAnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TC-DOC-285: 입찰 분석 화면과 결정은 관리자만 (CR-170, BIZ-094). 접근 거부는 보안 설정이 403 으로 낸다 */
@SpringJUnitConfig(BidAnalysisAdminControllerTest.Config.class)
class BidAnalysisAdminControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        BidAnalysisService bidAnalysisService() {
            return Mockito.mock(BidAnalysisService.class);
        }

        @Bean
        BidAnalysisAdminController bidAnalysisAdminController(BidAnalysisService service) {
            return new BidAnalysisAdminController(service);
        }
    }

    @Autowired
    private BidAnalysisAdminController controller;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("TC-DOC-285 고객토큰_입찰분석조회_거부")
    void 고객_조회거부() {
        signIn("ROLE_CUSTOMER");

        assertThatThrownBy(() -> controller.overview(UUID.randomUUID())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("TC-DOC-285 반례: 관리자는 조회한다")
    void 관리자_조회() {
        signIn("ROLE_ADMIN");

        assertThatCode(() -> controller.overview(UUID.randomUUID())).doesNotThrowAnyException();
    }
}
