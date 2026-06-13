package com.biddingagency.domain.notice.service;

import com.biddingagency.domain.document.entity.DocumentTemplate;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.DocumentTemplateService;
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
    private final DocumentTemplateService documentTemplateService;
    private final com.biddingagency.domain.proposal.service.VerificationLogService verificationLogService;
    private final com.biddingagency.domain.opportunity.service.OpportunityTranslationService opportunityTranslationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * self-injection — @Async self-call이 AOP 프록시를 통과 못 해 동기 호출되는 함정 회피.
     * createNotice(@Transactional)에서 generateAsync 호출 시 부모 트랜잭션이 110초 워크플로우 응답까지
     * 열려있어 Aimbase save 콜백이 노티를 못 찾는 버그(2026-05-30 실측) 해결용.
     */
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private NoticeService self;

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

    /** 관리자 공고문 리스트 — DTO로 트랜잭션 안에서 변환해 lazy 접근 회피 */
    public Page<com.biddingagency.domain.notice.dto.NoticeAdminDto> findAllAsDto(Pageable pageable) {
        return noticeRepository.findAll(pageable).map(com.biddingagency.domain.notice.dto.NoticeAdminDto::fromList);
    }

    /** 관리자 공고문 상세 — DTO로 변환해 lazy 접근 회피 */
    public com.biddingagency.domain.notice.dto.NoticeAdminDto findByIdAsDto(UUID noticeId) {
        return com.biddingagency.domain.notice.dto.NoticeAdminDto.from(findById(noticeId));
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

    /** CR-024: 한 원본의 노출 중인 공고문 중 최신 1건 (BidRequest 한글 타이틀+필요서류 매핑용) */
    public Optional<Notice> findLatestVisibleByOpportunityId(UUID opportunityId) {
        return noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(
                opportunityId, OpportunityVisibility.VISIBLE);
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
    public UUID createNotice(UUID opportunityId, UUID createdBy) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

        // 이미 이 원본으로 만든 공고문이 있으면 새로 만들지 않고 기존 최신 공고문으로 안내 (중복 생성 방지)
        Optional<Notice> existing = noticeRepository.findFirstByOpportunityIdOrderByCreatedAtDesc(opportunityId);
        if (existing.isPresent()) {
            UUID existingId = existing.get().getId();
            log.info("[공고문] 기존 공고문 존재 → 재사용: opportunityId={}, noticeId={}", opportunityId, existingId);
            return existingId;
        }

        Notice notice = noticeRepository.save(Notice.builder()
                .opportunity(opportunity)
                .createdBy(createdBy)
                .build());

        notice.markAnalyzing(null);
        noticeRepository.save(notice);

        UUID noticeId = notice.getId();
        log.info("[공고문] 생성 + 한글화 트리거: opportunityId={}, noticeId={}", opportunityId, noticeId);

        self.generateAsync(noticeId, opportunityId);

        return noticeId;
    }

    /** 한글화 재생성 (기존 공고문 다시 돌림) */
    @Transactional
    public Notice regenerate(UUID noticeId) {
        Notice notice = findById(noticeId);
        notice.markAnalyzing(null);
        noticeRepository.save(notice);
        log.info("[공고문] 한글화 재생성: noticeId={}", noticeId);
        self.generateAsync(noticeId, notice.getOpportunity().getId());
        return notice;
    }

    /**
     * CR-039: 한글화/분석 강제 중단.
     *
     * ANALYZING 상태로 멈춰 무한 폴링되는(stuck) 공고문을 끊는다.
     * - ANALYZING이 아니면 무시(idempotent) — 이미 끝났거나 다른 상태면 할 일 없음(BIZ-021).
     * - Aimbase 워크플로우 취소는 best-effort(LLMPlatformClient.cancelWorkflowRun) — 응답·성공 여부와 무관하게
     *   우리 쪽 generationStatus는 FAILED로 전환(화면 잠금 즉시 해제 최우선, BIZ-021).
     * - FAILED가 되면 FE 재생성 버튼 disabled(=ANALYZING 조건)가 풀려 재시도 가능.
     *
     * @param reason 실패 사유 (수동 중단 / stuck 자동 정리 구분)
     * @return 중단 처리했으면 true, ANALYZING이 아니라 건너뛰었으면 false
     */
    @Transactional
    public boolean cancelAnalysis(UUID noticeId, String reason) {
        Notice notice = findById(noticeId);
        if (!notice.isAnalyzing()) {
            log.info("[공고문] 강제 중단 건너뜀(ANALYZING 아님): noticeId={}, status={}",
                    noticeId, notice.getGenerationStatus());
            return false;
        }
        // best-effort: Aimbase에 취소 요청(실패해도 무시)
        llmPlatformClient.cancelWorkflowRun(notice.getWorkflowRunId());
        // 응답 무관 무조건 FAILED 전환
        notice.markFailed(reason);
        noticeRepository.save(notice);
        log.info("[공고문] 한글화/분석 강제 중단: noticeId={}, reason={}", noticeId, reason);
        return true;
    }

    @Async("llmTaskExecutor")
    public void generateAsync(UUID noticeId, UUID opportunityId) {
        log.info("[공고문] 한글화/요약 시작: noticeId={}", noticeId);

        try {
            // CR-038: WF 입력 구성 전에 원문 본문(descriptionBody)을 보장한다.
            // 비어있으면 noticedesc fetch해 채움 → buildOpportunityText가 실본문을 WF에 전달(환각 방지).
            // 별도 트랜잭션(번역 흐름과 독립)에서 채우므로 아래 재조회로 최신 본문을 읽는다.
            opportunityTranslationService.ensureDescriptionBody(opportunityId);

            Opportunity opp = opportunityRepository.findById(opportunityId)
                    .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

            Map<String, Object> input = new HashMap<>();
            input.put("opportunityId", opportunityId.toString());
            input.put("noticeId", noticeId.toString());
            input.put("opportunityText", buildOpportunityText(opp));
            // CR-019: 실제 수집된(SUCCESS) 첨부의 다운로드 URL을 전달 → Aimbase가 parse_document로 발췌
            input.put("attachmentFiles", buildAttachmentFiles(opportunityId));
            // CR-021: NOTICE_VIEW 양식(TipTap JSON 골격)을 함께 입력 → LLM이 이 골격을 채워 contentJson 반환
            //         양식 미등록 시 input에 noticeViewTemplate 키 없음 → Aimbase는 contentJson 생략 가능
            buildNoticeViewTemplate().ifPresent(t -> input.put("noticeViewTemplate", t));

            WorkflowRunResponse response = llmPlatformClient.analyzeOpportunity(input);

            // Aimbase 워크플로우가 MCP save_opportunity_analysis를 콜백하여 결과 저장
            log.info("[공고문] 한글화/요약 완료 (Aimbase MCP 콜백으로 저장됨): noticeId={}, run={}",
                    noticeId, response != null ? response.getId() : null);

            // CR-031: 정제(콜백 저장) 완료 후 공고문 충실성 LLM 검증 1회.
            // saveResult 콜백 안의 BE 정형 룰(RULE)과 별개로, 본문 환각을 문장 단위로 검증한다.
            // 완료(COMPLETED) 상태일 때만 — FAILED 면 검증할 본문이 없다.
            verifyNoticeFidelity(noticeId);

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

    /**
     * CR-031: 공고문 충실성 LLM 검증 1회 (verify-fidelity WF, NOTICE 분기).
     * WF 가 get_notice_verify_input → parse_document → save_verification_result 로 자율주행.
     * COMPLETED 상태일 때만 (검증할 본문 존재). 검증 실패는 정제 자체를 막지 않는다(표식만).
     */
    private void verifyNoticeFidelity(UUID noticeId) {
        try {
            Notice notice = findById(noticeId);
            if (!notice.isGenerationCompleted()) {
                return; // FAILED — 검증 대상 없음
            }
            int attempt = verificationLogService.nextAttempt(
                    com.biddingagency.domain.proposal.entity.VerificationTargetType.NOTICE, noticeId);
            Map<String, Object> input = new HashMap<>();
            input.put("targetType", com.biddingagency.domain.proposal.entity.VerificationTargetType.NOTICE.name());
            input.put("targetId", noticeId.toString());
            input.put("attempt", attempt);
            llmPlatformClient.runVerifyFidelity(input);
            log.info("[CR-031] 공고문 충실성 검증 완료(콜백 저장): noticeId={}", noticeId);
        } catch (Exception e) {
            log.warn("[CR-031] 공고문 충실성 검증 실패(무시): noticeId={}", noticeId, e);
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
                             Map<String, Object> llmPromptPresetJson,
                             Map<String, Object> contentJson,
                             List<Map<String, Object>> extractedFactsJson) {
        Notice notice = findById(noticeId);

        // CR-004: 정제 출력 필수키 검증 — 누락 시 COMPLETED 대신 FAILED로 전이해 빈 한글화 노출 차단
        // CR-021: contentJson은 양식이 등록된 경우에만 검증 — 양식 미등록 시 LLM이 못 만들 수 있어 옵션
        List<String> missing = validateRequiredKeys(koreanTitle, summaryJson, requiredDocumentsJson);
        if (!missing.isEmpty()) {
            String reason = "정제 출력 필수 항목 누락: " + String.join(", ", missing);
            log.warn("[공고문] 정제 검증 실패 → FAILED: noticeId={}, {}", noticeId, reason);
            notice.markFailed(reason);
            noticeRepository.save(notice);
            eventPublisher.publishEvent(
                    new OpportunityAnalysisCompletedEvent(noticeId, notice.getOpportunity().getId(), false, reason));
            return notice;
        }

        notice.markCompleted(koreanTitle, summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson, contentJson, extractedFactsJson);
        Notice saved = noticeRepository.save(notice);

        // CR-031 BE 정형 룰 (LLM 0콜): 분량·첨부 0건 평가 → verification_log(RULE) 적재.
        // FAILED 로 막지 않고 PARTIAL 신호로 남긴다 — 관리자 콘솔이 배지로 노출, 재검증/재생성은 사람 판단.
        recordNoticeRuleCheck(saved, summaryJson, contentJson);
        return saved;
    }

    /**
     * CR-031: 공고문 정제 결과의 분량/첨부 정형 룰을 평가해 verification_log 에 RULE 결과 적재.
     * 첨부 0건 + description ≤ 300자에 본문이 작성되면 환각 위험으로 표식한다.
     */
    private void recordNoticeRuleCheck(Notice notice, Map<String, Object> summaryJson,
                                       Map<String, Object> contentJson) {
        try {
            UUID opportunityId = notice.getOpportunity().getId();
            int attachmentCount = attachmentRepository
                    .findByOpportunityIdAndDownloadStatus(opportunityId, AttachmentDownloadStatus.SUCCESS).size();
            int descriptionLen = descriptionLength(notice.getOpportunity());
            int overviewLen = summaryJson != null ? asString(summaryJson.get("overview")) != null
                    ? asString(summaryJson.get("overview")).length() : 0 : 0;
            int contentLen = jsonLength(contentJson);

            List<String> findings = verificationLogService.evaluateNoticeRules(
                    attachmentCount, descriptionLen, overviewLen, contentLen, false, false);
            int attempt = verificationLogService.nextAttempt(
                    com.biddingagency.domain.proposal.entity.VerificationTargetType.NOTICE, notice.getId());
            verificationLogService.recordRule(
                    com.biddingagency.domain.proposal.entity.VerificationTargetType.NOTICE,
                    notice.getId(), findings, contentLen, attempt);
        } catch (Exception e) {
            log.warn("[CR-031] 공고문 정형 룰 평가 실패(무시): noticeId={}", notice.getId(), e);
        }
    }

    private int descriptionLength(Opportunity opp) {
        if (opp.getRawJson() == null) return 0;
        Object desc = opp.getRawJson().get("description");
        return desc != null ? desc.toString().length() : 0;
    }

    private int jsonLength(Map<String, Object> json) {
        if (json == null || json.isEmpty()) return 0;
        try {
            return objectMapper.writeValueAsString(json).length();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * CR-004: 정제 출력 정형 포맷의 필수키 검증.
     * 필수: koreanTitle, summary.overview, requiredDocuments.documents(1건 이상).
     * documentFormats/llmPromptPreset는 보조 정보라 검증 제외.
     * 반환: 누락된 항목 라벨 목록 (비면 검증 통과).
     */
    private List<String> validateRequiredKeys(String koreanTitle,
                                              Map<String, Object> summaryJson,
                                              Map<String, Object> requiredDocumentsJson) {
        List<String> missing = new ArrayList<>();

        if (isBlank(koreanTitle)) {
            missing.add("koreanTitle(한글 제목)");
        }

        if (summaryJson == null || isBlank(asString(summaryJson.get("overview")))) {
            missing.add("summary.overview(요약 핵심)");
        }

        Object docs = requiredDocumentsJson == null ? null : requiredDocumentsJson.get("documents");
        if (!(docs instanceof List<?> list) || list.isEmpty()) {
            missing.add("requiredDocuments.documents(필요 서류 목록)");
        }

        return missing;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    @Transactional
    public Notice updateResult(UUID noticeId, String koreanTitle,
                               Map<String, Object> summaryJson,
                               Map<String, Object> documentFormatsJson,
                               Map<String, Object> requiredDocumentsJson,
                               Map<String, Object> llmPromptPresetJson,
                               Map<String, Object> contentJson) {
        Notice notice = findById(noticeId);
        notice.updateResult(koreanTitle, summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson, contentJson);
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

    /**
     * CR-021: NOTICE_VIEW 활성 템플릿(TipTap JSON 골격)을 조회해 Aimbase 입력에 실음.
     * 양식이 등록되지 않은 경우 Optional.empty — LLM은 contentJson 없이 기존 정형 JSON만 채움.
     */
    private java.util.Optional<Map<String, Object>> buildNoticeViewTemplate() {
        try {
            DocumentTemplate template = documentTemplateService.getActiveTemplate(DocumentType.NOTICE_VIEW);
            return java.util.Optional.ofNullable(template.getContentJson());
        } catch (java.util.NoSuchElementException e) {
            log.info("[공고문] NOTICE_VIEW 양식 미등록 — contentJson 생성 생략");
            return java.util.Optional.empty();
        }
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
        // CR-038: 실본문(descriptionBody, CR-032/035에서 noticedesc fetch로 채움) 우선.
        // rawJson.description 은 noticedesc URL 또는 일부 요약이라, 첨부 없는 공고(예: 관제탑)는
        // 실본문이 WF 입력에서 빠져 LLM 이 빈칸을 환각으로 채우던 근원. 실본문 있으면 그걸 사용.
        String body = opp.getDescriptionBody();
        if (body != null && !body.isBlank()) {
            sb.append("\n상세 설명(원문 본문):\n").append(body);
        } else if (opp.getRawJson() != null && !opp.getRawJson().isEmpty()) {
            Object description = opp.getRawJson().get("description");
            if (description != null) {
                sb.append("\n상세 설명:\n").append(description);
            }
        }
        return sb.toString();
    }
}
