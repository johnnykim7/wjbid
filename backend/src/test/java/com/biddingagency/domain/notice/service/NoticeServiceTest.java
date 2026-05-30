package com.biddingagency.domain.notice.service;

import com.biddingagency.domain.event.OpportunityAnalysisCompletedEvent;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
                noticeId, "한글 제목", validSummary(), null, validRequiredDocs(), null, null);

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
                noticeId, "  ", validSummary(), null, validRequiredDocs(), null, null);

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
                noticeId, "한글 제목", Map.of("scope", "범위만 있음"), null, validRequiredDocs(), null, null);

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
                noticeId, "한글 제목", validSummary(), null, Map.of("documents", List.of()), null, null);

        assertThat(result.getGenerationStatus()).isEqualTo(NoticeGenerationStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("requiredDocuments.documents");
    }

    // CR-004: 검증 실패 시 실패 이벤트 발행
    @Test
    @DisplayName("정제저장_필수키누락_실패이벤트발행됨")
    void 정제저장_누락_실패이벤트발행() {
        UUID noticeId = UUID.randomUUID();
        newAnalyzingNotice(noticeId);

        noticeService.saveResult(noticeId, null, null, null, null, null, null);

        ArgumentCaptor<OpportunityAnalysisCompletedEvent> captor =
                ArgumentCaptor.forClass(OpportunityAnalysisCompletedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getErrorMessage()).contains("koreanTitle");
    }
}
