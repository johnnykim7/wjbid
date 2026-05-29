package com.biddingagency.domain.notice.service;

import com.biddingagency.domain.event.OpportunityAnalysisCompletedEvent;
import com.biddingagency.domain.event.OpportunityApprovedEvent;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import com.biddingagency.integration.llmplatform.LLMPlatformException;
import com.biddingagency.integration.llmplatform.dto.WorkflowRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * NoticeService (CR-016) — 공고문 생성/한글화/노출 관리.
 *
 * 게이트①: createNotice → 관리자가 원본을 선별해 공고문 생성 + 한글화/요약 워크플로우 트리거.
 * 게이트②: publish/hide → 검수 후 고객 노출 토글.
 * 구 OpportunityAnalysisService 로직을 Notice(1:N) 기반으로 이전.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final OpportunityRepository opportunityRepository;
    private final OpportunityAttachmentRepository attachmentRepository;
    private final LLMPlatformClient llmPlatformClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.self-base-url:http://59.8.160.12:8183/api}")
    private String selfBaseUrl;

    // ── 조회 ──────────────────────────────────────────────

    public Notice findById(UUID noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + noticeId));
    }

    public List<Notice> findByOpportunityId(UUID opportunityId) {
        return noticeRepository.findByOpportunityId(opportunityId);
    }

    /** 관리자 공고문 리스트 (전체) */
    public Page<Notice> findAll(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    /** 고객 노출 공고문 목록 (VISIBLE). CR-009: includeExpired=false(기본)면 마감 지난 공고 제외 */
    public Page<Notice> findVisible(boolean includeExpired, Pageable pageable) {
        return noticeRepository.findVisible(
                OpportunityVisibility.VISIBLE, includeExpired, java.time.LocalDateTime.now(), pageable);
    }

    /** 고객 노출 공고문 단건 */
    public Optional<Notice> findVisibleById(UUID noticeId) {
        return noticeRepository.findByIdAndVisibility(noticeId, OpportunityVisibility.VISIBLE);
    }

    /** 고객 검색 (노출 공고문, 한글/원문 제목). CR-009: includeExpired=false(기본)면 마감 지난 공고 제외 */
    public Page<Notice> searchVisible(String keyword, boolean includeExpired, Pageable pageable) {
        return noticeRepository.searchVisibleByKeyword(
                keyword, OpportunityVisibility.VISIBLE, includeExpired, java.time.LocalDateTime.now(), pageable);
    }

    /** 마감 임박 노출 공고문 */
    public List<Notice> findVisibleNearDeadline(int days) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return noticeRepository.findVisibleNearDeadline(now, now.plusDays(days), OpportunityVisibility.VISIBLE);
    }

    /** 최근 게시 노출 공고문 */
    public List<Notice> findVisibleRecentlyPosted(int days) {
        return noticeRepository.findVisibleRecentlyPosted(
                java.time.LocalDateTime.now().minusDays(days), OpportunityVisibility.VISIBLE);
    }

    // ── 게이트① 선별 → 공고문 생성 + 한글화 트리거 ──────────

    @Transactional
    public Notice createNotice(UUID opportunityId, UUID createdBy) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

        Notice notice = noticeRepository.save(Notice.builder()
                .opportunity(opportunity)
                .createdBy(createdBy)
                .build());

        notice.markAnalyzing(null);
        noticeRepository.save(notice);

        log.info("[공고문] 생성 + 한글화 트리거: opportunityId={}, noticeId={}", opportunityId, notice.getId());

        generateAsync(notice.getId(), opportunityId);

        return notice;
    }

    /** 한글화 재생성 (기존 공고문 다시 돌림) */
    @Transactional
    public Notice regenerate(UUID noticeId) {
        Notice notice = findById(noticeId);
        notice.markAnalyzing(null);
        noticeRepository.save(notice);
        log.info("[공고문] 한글화 재생성: noticeId={}", noticeId);
        generateAsync(noticeId, notice.getOpportunity().getId());
        return notice;
    }

    @Async("llmTaskExecutor")
    public void generateAsync(UUID noticeId, UUID opportunityId) {
        log.info("[공고문] 한글화/요약 시작: noticeId={}", noticeId);

        try {
            Opportunity opp = opportunityRepository.findById(opportunityId)
                    .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

            Map<String, Object> input = new HashMap<>();
            input.put("opportunityId", opportunityId.toString());
            input.put("noticeId", noticeId.toString());
            input.put("opportunityText", buildOpportunityText(opp));
            // CR-019: 실제 수집된(SUCCESS) 첨부의 다운로드 URL을 전달 → Aimbase가 parse_document로 발췌
            input.put("attachmentFiles", buildAttachmentFiles(opportunityId));

            WorkflowRunResponse response = llmPlatformClient.analyzeOpportunity(input);

            // Aimbase 워크플로우가 MCP save_opportunity_analysis를 콜백하여 결과 저장
            log.info("[공고문] 한글화/요약 완료 (Aimbase MCP 콜백으로 저장됨): noticeId={}, run={}",
                    noticeId, response != null ? response.getId() : null);

            eventPublisher.publishEvent(
                    new OpportunityAnalysisCompletedEvent(noticeId, opportunityId, true, null));

        } catch (LLMPlatformException e) {
            log.error("[공고문] Aimbase 오류 (한글화): noticeId={}", noticeId, e);
            markFailed(noticeId, opportunityId, e.getMessage());
        } catch (Exception e) {
            log.error("[공고문] 예상치 못한 오류 (한글화): noticeId={}", noticeId, e);
            markFailed(noticeId, opportunityId, e.getMessage());
        }
    }

    @Transactional
    public void markFailed(UUID noticeId, UUID opportunityId, String errorMessage) {
        noticeRepository.findById(noticeId).ifPresent(notice -> {
            notice.markFailed(errorMessage);
            noticeRepository.save(notice);
        });
        eventPublisher.publishEvent(
                new OpportunityAnalysisCompletedEvent(noticeId, opportunityId, false, errorMessage));
    }

    // ── MCP 콜백 저장 ─────────────────────────────────────

    @Transactional
    public Notice saveResult(UUID noticeId, String koreanTitle,
                             Map<String, Object> summaryJson,
                             Map<String, Object> documentFormatsJson,
                             Map<String, Object> requiredDocumentsJson,
                             Map<String, Object> llmPromptPresetJson) {
        Notice notice = findById(noticeId);
        notice.markCompleted(koreanTitle, summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson);
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice updateResult(UUID noticeId, String koreanTitle,
                               Map<String, Object> summaryJson,
                               Map<String, Object> documentFormatsJson,
                               Map<String, Object> requiredDocumentsJson,
                               Map<String, Object> llmPromptPresetJson) {
        Notice notice = findById(noticeId);
        notice.updateResult(koreanTitle, summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson);
        return noticeRepository.save(notice);
    }

    // ── 게이트② 검수 → 노출/비노출 ────────────────────────

    @Transactional
    public Notice publish(UUID noticeId) {
        Notice notice = findById(noticeId);
        if (!notice.isGenerationCompleted()) {
            throw new IllegalStateException("한글화가 완료되지 않은 공고문은 노출할 수 없습니다: " + noticeId);
        }
        boolean wasHidden = !notice.isVisible();
        notice.publish();
        log.info("[공고문] 노출(VISIBLE): noticeId={}", noticeId);
        // 게이트②: 최초 노출 시 고객 신규 공고 알림 (구 approve 이벤트 재배선)
        if (wasHidden) {
            eventPublisher.publishEvent(
                    new OpportunityApprovedEvent(notice.getOpportunity().getId(), notice.getCreatedBy()));
        }
        return notice;
    }

    @Transactional
    public Notice hide(UUID noticeId) {
        Notice notice = findById(noticeId);
        notice.hide();
        log.info("[공고문] 비노출(HIDDEN): noticeId={}", noticeId);
        return notice;
    }

    /**
     * CR-019: SUCCESS 첨부만 다운로드 URL과 함께 평탄 목록으로 반환 (BIZ-020).
     * MANUAL_FETCH_REQUIRED/FAILED 첨부는 제외 — 못 가져온 내용이 한글 요약에 누락·추정으로 섞이는 것 방지.
     * 출력: [{attachmentId, fileName, contentType, downloadUrl}]
     */
    private List<Map<String, Object>> buildAttachmentFiles(UUID opportunityId) {
        List<OpportunityAttachment> success = attachmentRepository
                .findByOpportunityIdAndDownloadStatus(opportunityId, AttachmentDownloadStatus.SUCCESS);
        List<Map<String, Object>> files = new ArrayList<>();
        for (OpportunityAttachment a : success) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("attachmentId", a.getId().toString());
            f.put("fileName", a.getFileName());
            f.put("contentType", a.getContentType());
            f.put("downloadUrl", selfBaseUrl + "/mcp/opportunity-attachments/" + a.getId() + "/download");
            files.add(f);
        }
        return files;
    }

    private String buildOpportunityText(Opportunity opp) {
        StringBuilder sb = new StringBuilder();
        sb.append("공고 제목: ").append(opp.getTitle()).append("\n");
        if (opp.getOrganizationName() != null)
            sb.append("기관: ").append(opp.getOrganizationName()).append("\n");
        if (opp.getType() != null)
            sb.append("유형: ").append(opp.getType()).append("\n");
        if (opp.getResponseDeadline() != null)
            sb.append("마감일: ").append(opp.getResponseDeadline()).append("\n");
        if (opp.getSolicitationNumber() != null)
            sb.append("공고번호: ").append(opp.getSolicitationNumber()).append("\n");
        if (opp.getUiLink() != null)
            sb.append("링크: ").append(opp.getUiLink()).append("\n");
        if (opp.getRawJson() != null && !opp.getRawJson().isEmpty()) {
            Object description = opp.getRawJson().get("description");
            if (description != null) {
                sb.append("\n상세 설명:\n").append(description);
            }
        }
        return sb.toString();
    }
}
