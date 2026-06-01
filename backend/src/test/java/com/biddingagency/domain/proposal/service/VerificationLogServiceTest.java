package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.VerificationLog;
import com.biddingagency.domain.proposal.entity.VerificationMethod;
import com.biddingagency.domain.proposal.entity.VerificationTargetType;
import com.biddingagency.domain.proposal.entity.Verdict;
import com.biddingagency.domain.proposal.repository.VerificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-031: 충실성·분량 검증 로그 서비스 — 정형 룰 평가 + verdict 분기 + 재시도 회차.
 */
@ExtendWith(MockitoExtension.class)
class VerificationLogServiceTest {

    @Mock private VerificationLogRepository repository;
    @InjectMocks private VerificationLogService service;

    private VerificationLog captureSaved() {
        ArgumentCaptor<VerificationLog> captor = ArgumentCaptor.forClass(VerificationLog.class);
        then(repository).should().save(captor.capture());
        return captor.getValue();
    }

    // ── BE 정형 룰: 공고문 ──────────────────────────────────────

    @Test
    @DisplayName("공고룰_첨부0건_짧은description_본문있음_환각위험표식")
    void 공고룰_첨부0_짧은원문_본문있음_위반() {
        // 첨부 0건 + description 100자 + contentJson 1000자 → 환각 위험 finding
        List<String> findings = service.evaluateNoticeRules(0, 100, 80, 1000, false, false);
        assertThat(findings).anyMatch(f -> f.contains("원문 정보 부족"));
    }

    @Test
    @DisplayName("공고룰_overview50자미만_분량미달표식")
    void 공고룰_overview짧음_위반() {
        List<String> findings = service.evaluateNoticeRules(1, 5000, 30, 1000, true, true);
        assertThat(findings).anyMatch(f -> f.contains("overview"));
    }

    @Test
    @DisplayName("공고룰_충분한원문_위반없음")
    void 공고룰_정상_통과() {
        // 첨부 2건 + 긴 원문 + overview 200자 + contentJson 2000자 → 위반 없음
        List<String> findings = service.evaluateNoticeRules(2, 5000, 200, 2000, true, true);
        assertThat(findings).isEmpty();
    }

    // ── BE 정형 룰: 제안서 section ──────────────────────────────

    @Test
    @DisplayName("섹션룰_block0개_작성실패표식")
    void 섹션룰_block0_위반() {
        List<String> findings = service.evaluateSectionRules(0, 0, 1500);
        assertThat(findings).anyMatch(f -> f.contains("block 0개"));
    }

    @Test
    @DisplayName("섹션룰_minWords미달_분량미달표식")
    void 섹션룰_분량미달_위반() {
        List<String> findings = service.evaluateSectionRules(3, 500, 1500);
        assertThat(findings).anyMatch(f -> f.contains("분량 미달"));
    }

    @Test
    @DisplayName("섹션룰_minWordsNull이면분량룰미적용")
    void 섹션룰_minWordsNull_분량룰스킵() {
        List<String> findings = service.evaluateSectionRules(3, 10, null);
        assertThat(findings).isEmpty();
    }

    // ── 저장 verdict 분기 ───────────────────────────────────────

    @Test
    @DisplayName("RULE저장_findings비면PASS")
    void RULE_findings비면_PASS() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
        service.recordRule(VerificationTargetType.PROPOSAL_SECTION, UUID.randomUUID(), List.of(), 1500, 1);
        VerificationLog saved = captureSaved();
        assertThat(saved.getVerdict()).isEqualTo(Verdict.PASS);
        assertThat(saved.getMethod()).isEqualTo(VerificationMethod.RULE);
    }

    @Test
    @DisplayName("RULE저장_findings있으면FAIL")
    void RULE_findings있으면_FAIL() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
        service.recordRule(VerificationTargetType.NOTICE, UUID.randomUUID(),
                List.of("분량 미달(10자)"), 10, 1);
        assertThat(captureSaved().getVerdict()).isEqualTo(Verdict.FAIL);
    }

    @Test
    @DisplayName("LLM저장_환각목록과verdict그대로보존")
    void LLM_환각보존() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
        List<Map<String, Object>> halls = List.of(Map.of("sentence", "가짜 실적", "reason", "근거 없음"));
        service.recordLlm(VerificationTargetType.PROPOSAL_SECTION, UUID.randomUUID(),
                "run-9", Verdict.FAIL, halls, List.of(), 1200, 2);
        VerificationLog saved = captureSaved();
        assertThat(saved.getMethod()).isEqualTo(VerificationMethod.LLM);
        assertThat(saved.getVerdict()).isEqualTo(Verdict.FAIL);
        assertThat(saved.hallucinationCount()).isEqualTo(1);
        assertThat(saved.getAttempt()).isEqualTo(2);
    }

    // ── 재시도 회차 ────────────────────────────────────────────

    @Test
    @DisplayName("nextAttempt_이력없으면1")
    void nextAttempt_최초_1() {
        UUID id = UUID.randomUUID();
        given(repository.findFirstByTargetTypeAndTargetIdOrderByVerifiedAtDesc(
                VerificationTargetType.PROPOSAL_SECTION, id)).willReturn(Optional.empty());
        assertThat(service.nextAttempt(VerificationTargetType.PROPOSAL_SECTION, id)).isEqualTo(1);
    }

    @Test
    @DisplayName("nextAttempt_최신attempt1이면2반환")
    void nextAttempt_재시도_증가() {
        UUID id = UUID.randomUUID();
        VerificationLog prev = VerificationLog.builder()
                .targetType(VerificationTargetType.PROPOSAL_SECTION).targetId(id)
                .verdict(Verdict.FAIL).attempt(1).build();
        given(repository.findFirstByTargetTypeAndTargetIdOrderByVerifiedAtDesc(
                VerificationTargetType.PROPOSAL_SECTION, id)).willReturn(Optional.of(prev));
        assertThat(service.nextAttempt(VerificationTargetType.PROPOSAL_SECTION, id)).isEqualTo(2);
    }
}
