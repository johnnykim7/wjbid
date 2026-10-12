package com.biddingagency.domain.notice.service;

import com.biddingagency.domain.event.OpportunityAnalysisCompletedEvent;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.dto.OpportunityDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CR-004: NoticeService.saveResult 정제 출력 필수키 검증 테스트.
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private com.biddingagency.domain.opportunity.service.MarketRowAssembler marketRowAssembler;

    @Mock
    private com.biddingagency.integration.llmplatform.LLMPlatformClient llmPlatformClient;

    @Mock
    private com.biddingagency.domain.proposal.service.VerificationLogService verificationLogService;

    @Mock
    private com.biddingagency.domain.compliance.service.RequirementSyncService requirementSyncService;

    @InjectMocks
    private NoticeService noticeService;

    private Notice newAnalyzingNotice(UUID noticeId) {
        Opportunity opp = Opportunity.builder().noticeId("N-1").title("T").build();
        Notice notice = Notice.builder().opportunity(opp).build();
        notice.markAnalyzing(null);
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));
        given(noticeRepository.save(any(Notice.class))).willAnswer(inv -> inv.getArgument(0));
        return notice;
    }

    private Map<String, Object> validSummary() {
        return Map.of("overview", "핵심 요약 2-3문장");
    }

    private Map<String, Object> validRequiredDocs() {
        return Map.of("documents", List.of(Map.of("name", "사업자등록증")));
    }

    // CR-004: 필수키 모두 충족 → COMPLETED
    @Test
    @DisplayName("정제저장_필수키모두충족_COMPLETED저장됨")
    void 정제저장_필수키충족_완료() {
        UUID noticeId = UUID.randomUUID();
        newAnalyzingNotice(noticeId);

        Notice result = noticeService.saveResult(
                noticeId, "한글 제목", validSummary(), null, validRequiredDocs(), null, null, null);

        assertThat(result.getGenerationStatus()).isEqualTo(NoticeGenerationStatus.COMPLETED);
        then(eventPublisher).should(never()).publishEvent(any(OpportunityAnalysisCompletedEvent.class));
    }

    // CR-004: koreanTitle 누락 → FAILED
    @Test
    @DisplayName("정제저장_한글제목누락_FAILED전이됨")
    void 정제저장_한글제목누락_실패() {
        UUID noticeId = UUID.randomUUID();
        newAnalyzingNotice(noticeId);

        Notice result = noticeService.saveResult(
                noticeId, "  ", validSummary(), null, validRequiredDocs(), null, null, null);

        assertThat(result.getGenerationStatus()).isEqualTo(NoticeGenerationStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("koreanTitle");
    }

    // CR-004: summary.overview 누락 → FAILED
    @Test
    @DisplayName("정제저장_요약핵심누락_FAILED전이됨")
    void 정제저장_요약누락_실패() {
        UUID noticeId = UUID.randomUUID();
        newAnalyzingNotice(noticeId);

        Notice result = noticeService.saveResult(
                noticeId, "한글 제목", Map.of("scope", "범위만 있음"), null, validRequiredDocs(), null, null, null);

        assertThat(result.getGenerationStatus()).isEqualTo(NoticeGenerationStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("summary.overview");
    }

    // CR-004: requiredDocuments.documents 빈 목록 → FAILED
    @Test
    @DisplayName("정제저장_필요서류빈목록_FAILED전이됨")
    void 정제저장_필요서류빈목록_실패() {
        UUID noticeId = UUID.randomUUID();
        newAnalyzingNotice(noticeId);

        Notice result = noticeService.saveResult(
                noticeId, "한글 제목", validSummary(), null, Map.of("documents", List.of()), null, null, null);

        assertThat(result.getGenerationStatus()).isEqualTo(NoticeGenerationStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("requiredDocuments.documents");
    }

    // CR-004: 검증 실패 시 실패 이벤트 발행
    @Test
    @DisplayName("정제저장_필수키누락_실패이벤트발행됨")
    void 정제저장_누락_실패이벤트발행() {
        UUID noticeId = UUID.randomUUID();
        newAnalyzingNotice(noticeId);

        noticeService.saveResult(noticeId, null, null, null, null, null, null, null);

        ArgumentCaptor<OpportunityAnalysisCompletedEvent> captor =
                ArgumentCaptor.forClass(OpportunityAnalysisCompletedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getErrorMessage()).contains("koreanTitle");
    }

    // CR-039: ANALYZING 상태 강제 중단 → FAILED 전이
    @Test
    @DisplayName("강제중단_ANALYZING상태_FAILED전이됨")
    void 강제중단_분석중_실패전이() {
        UUID noticeId = UUID.randomUUID();
        newAnalyzingNotice(noticeId);

        boolean cancelled = noticeService.cancelAnalysis(noticeId, "관리자 강제 중단");

        assertThat(cancelled).isTrue();
    }

    // CR-039: ANALYZING이 아니면 무시(idempotent) — 상태 안 바뀜
    @Test
    @DisplayName("강제중단_COMPLETED상태_무시됨")
    void 강제중단_완료상태_무시() {
        UUID noticeId = UUID.randomUUID();
        Opportunity opp = Opportunity.builder().noticeId("N-1").title("T").build();
        Notice notice = Notice.builder().opportunity(opp).build();
        notice.markCompleted("제목", validSummary(), null, validRequiredDocs(), null, null, null);
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        boolean cancelled = noticeService.cancelAnalysis(noticeId, "관리자 강제 중단");

        assertThat(cancelled).isFalse();
        assertThat(notice.getGenerationStatus()).isEqualTo(NoticeGenerationStatus.COMPLETED);
    }

    // CR-039: 강제 중단 시 Aimbase 취소 호출 후 우리 쪽 FAILED + 사유 기록 (best-effort)
    @Test
    @DisplayName("강제중단_분석중_Aimbase취소호출되고_FAILED사유기록됨")
    void 강제중단_분석중_Aimbase호출_실패전이() {
        UUID noticeId = UUID.randomUUID();
        Notice notice = newAnalyzingNotice(noticeId);
        // cancelWorkflowRun은 내부에서 RestClientException을 삼키는 best-effort (목은 기본 no-op)

        noticeService.cancelAnalysis(noticeId, "관리자 강제 중단");

        then(llmPlatformClient).should().cancelWorkflowRun(any());
        assertThat(notice.getGenerationStatus()).isEqualTo(NoticeGenerationStatus.FAILED);
        assertThat(notice.getErrorMessage()).isEqualTo("관리자 강제 중단");
    }

    // CR-040: 비노출+분석중아님 → 삭제 + 고아 검증로그 정리
    @Test
    @DisplayName("삭제_비노출FAILED_삭제되고검증로그정리됨")
    void 삭제_정상_삭제됨() {
        UUID noticeId = UUID.randomUUID();
        Opportunity opp = Opportunity.builder().noticeId("N-1").title("T").build();
        Notice notice = Notice.builder().opportunity(opp).build();
        notice.markFailed("이전 실패");  // HIDDEN 기본, FAILED
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        noticeService.deleteNotice(noticeId);

        then(verificationLogService).should().deleteByTarget(
                com.biddingagency.domain.proposal.entity.VerificationTargetType.NOTICE, noticeId);
        then(noticeRepository).should().delete(notice);
    }

    // CR-040: 노출 중 → 삭제 거부
    @Test
    @DisplayName("삭제_노출중_거부됨")
    void 삭제_노출중_거부() {
        UUID noticeId = UUID.randomUUID();
        Opportunity opp = Opportunity.builder().noticeId("N-1").title("T").build();
        Notice notice = Notice.builder().opportunity(opp).build();
        notice.markCompleted("제목", validSummary(), null, validRequiredDocs(), null, null, null);
        notice.publish();  // VISIBLE
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.deleteNotice(noticeId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("노출 중");
        then(noticeRepository).should(never()).delete(any());
    }

    // CR-040: 분석 중 → 삭제 거부
    @Test
    @DisplayName("삭제_분석중_거부됨")
    void 삭제_분석중_거부() {
        UUID noticeId = UUID.randomUUID();
        Opportunity opp = Opportunity.builder().noticeId("N-1").title("T").build();
        Notice notice = Notice.builder().opportunity(opp).build();
        notice.markAnalyzing(null);  // ANALYZING (save stub 불필요)
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.deleteNotice(noticeId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("분석 중");
        then(noticeRepository).should(never()).delete(any());
    }

    // ===== CR-152: 원본 공고 상세 =====

    private Opportunity originOpp(UUID id, String titleKo, String body) {
        Opportunity opp = Opportunity.builder().noticeId("N-152").title("Laundry Service")
                .titleKo(titleKo).descriptionBody(body).build();
        ReflectionTestUtils.setField(opp, "id", id);
        given(opportunityRepository.findById(id)).willReturn(Optional.of(opp));
        return opp;
    }

    @Test
    @DisplayName("TC-BROWSE-006: 공개공고문없음_원본상세조회_한글제목과원문본문")
    void 공개공고문없음_원본상세조회_한글제목과원문본문() {
        UUID id = UUID.randomUUID();
        originOpp(id, "세탁 서비스", "Full description body");
        given(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(id, OpportunityVisibility.VISIBLE))
                .willReturn(Optional.empty());
        given(marketRowAssembler.samUrl("N-152")).willReturn("https://sam.gov/opp/N-152/view");

        OpportunityDto dto = noticeService.findOriginDetail(id).orElseThrow();

        assertThat(dto).extracting(OpportunityDto::id, OpportunityDto::title, OpportunityDto::originalTitle,
                        OpportunityDto::description, OpportunityDto::publicNoticeId, OpportunityDto::analysis, OpportunityDto::samUrl)
                .containsExactly(id.toString(), "세탁 서비스", "Laundry Service", "Full description body", null, null,
                        "https://sam.gov/opp/N-152/view");
    }

    @Test
    @DisplayName("TC-BROWSE-007: 공개공고문있음_원본상세조회_공개공고문id")
    void 공개공고문있음_원본상세조회_공개공고문id() {
        UUID id = UUID.randomUUID();
        Opportunity opp = originOpp(id, "세탁 서비스", null);
        UUID noticeId = UUID.randomUUID();
        Notice visible = Notice.builder().opportunity(opp).build();
        ReflectionTestUtils.setField(visible, "id", noticeId);
        given(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(id, OpportunityVisibility.VISIBLE))
                .willReturn(Optional.of(visible));

        assertThat(noticeService.findOriginDetail(id).orElseThrow().publicNoticeId()).isEqualTo(noticeId.toString());
    }

    @Test
    @DisplayName("TC-BROWSE-008: 한글제목없음_원본상세조회_원문제목")
    void 한글제목없음_원본상세조회_원문제목() {
        UUID id = UUID.randomUUID();
        originOpp(id, " ", null);
        given(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(id, OpportunityVisibility.VISIBLE))
                .willReturn(Optional.empty());

        OpportunityDto dto = noticeService.findOriginDetail(id).orElseThrow();

        assertThat(List.of(dto.title(), dto.originalTitle())).containsOnly("Laundry Service");
    }

    @Test
    @DisplayName("TC-BROWSE-009: 지운공고_원본상세조회_없음")
    void 지운공고_원본상세조회_없음() {
        UUID deletedId = UUID.randomUUID();
        Opportunity deleted = originOpp(deletedId, "세탁 서비스", null);
        ReflectionTestUtils.setField(deleted, "deletedAt", LocalDateTime.of(2026, 10, 1, 0, 0));
        UUID missingId = UUID.randomUUID();
        given(opportunityRepository.findById(missingId)).willReturn(Optional.empty());

        assertThat(List.of(noticeService.findOriginDetail(deletedId), noticeService.findOriginDetail(missingId)))
                .allMatch(Optional::isEmpty);
    }

    // ===== CR-155: 한글 본문, 관심 공고 번호 =====

    @Test
    @DisplayName("TC-BROWSE-016: 한글본문있음_원본상세_descriptionKo")
    void 한글본문있음_원본상세_descriptionKo() {
        UUID id = UUID.randomUUID();
        Opportunity opp = originOpp(id, "세탁 서비스", "Full description body");
        opp.applyDescriptionTranslation("세탁 서비스 본문");
        given(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(id, OpportunityVisibility.VISIBLE))
                .willReturn(Optional.empty());

        OpportunityDto dto = noticeService.findOriginDetail(id).orElseThrow();

        assertThat(List.of(dto.description(), dto.descriptionKo()))
                .containsExactly("Full description body", "세탁 서비스 본문");
    }

    @Test
    @DisplayName("TC-BROWSE-017: 공고문상세_opportunityId_모은공고번호")
    void 공고문상세_opportunityId_모은공고번호() {
        UUID oppId = UUID.randomUUID();
        Opportunity opp = Opportunity.builder().noticeId("N-155").title("Laundry Service").build();
        ReflectionTestUtils.setField(opp, "id", oppId);
        UUID noticeId = UUID.randomUUID();
        Notice visible = Notice.builder().opportunity(opp).build();
        ReflectionTestUtils.setField(visible, "id", noticeId);
        given(noticeRepository.findByIdAndVisibility(noticeId, OpportunityVisibility.VISIBLE))
                .willReturn(Optional.of(visible));

        OpportunityDto dto = noticeService.findVisibleDetail(noticeId).orElseThrow();

        assertThat(List.of(dto.id(), dto.opportunityId())).containsExactly(noticeId.toString(), oppId.toString());
    }
}
