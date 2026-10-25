package com.biddingagency.domain.analysis.service;

import com.biddingagency.domain.analysis.BidAnalysisException;
import com.biddingagency.domain.analysis.BidAnalysisException.Reason;
import com.biddingagency.domain.analysis.dto.BidAnalysisDto.Analysis;
import com.biddingagency.domain.analysis.dto.BidAnalysisDto.Overview;
import com.biddingagency.domain.analysis.dto.BidAnalysisDto.Person;
import com.biddingagency.domain.analysis.entity.BidAnalysis;
import com.biddingagency.domain.analysis.entity.BidDecision;
import com.biddingagency.domain.analysis.repository.BidAnalysisRepository;
import com.biddingagency.domain.analysis.service.BidAnalysisBodyValidator.Part;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.service.AIWorkflowService;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 입찰 분석과 Bid/No-Bid 결정 (CR-170, BID-DOC-006, BID-DOC-007, BIZ-090~094).
 *
 * 고객 서류를 다 읽은 분석 단계 건에 회차를 한 번 시작하고, 워크플로우가 세 단계로 저장한 산출물에 서버 계산을 붙인다.
 * 관리자 결정은 지금 분석(가장 늦게 시작한 회차)에 붙고, Go 나 조건부 Go 여야 문서 작성 중으로 전환된다.
 * Aimbase 호출과 기다림은 {@link AIWorkflowService} 가 맡는다.
 */
@Slf4j
@Service
public class BidAnalysisService {

    public static final String TIMEOUT_REASON = "분석 시간 초과";
    public static final String NO_DECISION_REASON = "Bid/No-Bid 결정이 저장되지 않음";
    static final String QUESTION_PART = "part";
    private static final TypeReference<List<Map<String, Object>>> QUESTION_LIST = new TypeReference<>() {
    };

    private final BidAnalysisRepository analysisRepository;
    private final BidRequestRepository bidRequestRepository;
    private final MemberRepository memberRepository;
    private final DocumentReadingService documentReadingService;
    private final AIWorkflowService aiWorkflowService;
    private final SystemSettingService settingService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    /** 분석 중 기한. 워크플로우를 기다리는 상한(폴링 간격 x 횟수)과 같게 둔다 */
    private final Duration analysisLimit;

    public BidAnalysisService(BidAnalysisRepository analysisRepository,
                              BidRequestRepository bidRequestRepository,
                              MemberRepository memberRepository,
                              DocumentReadingService documentReadingService,
                              @Lazy AIWorkflowService aiWorkflowService,
                              SystemSettingService settingService,
                              ObjectMapper objectMapper,
                              Clock clock,
                              @Value("${app.aimbase.polling.interval-ms:3000}") long pollingIntervalMs,
                              @Value("${app.aimbase.polling.max-attempts:60}") int maxPollingAttempts) {
        this.analysisRepository = analysisRepository;
        this.bidRequestRepository = bidRequestRepository;
        this.memberRepository = memberRepository;
        this.documentReadingService = documentReadingService;
        this.aiWorkflowService = aiWorkflowService;
        this.settingService = settingService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.analysisLimit = Duration.ofMillis(pollingIntervalMs * maxPollingAttempts);
    }

    // ─── 관리자 화면 ─────────────────────────────────────────────────────

    @Transactional
    public Overview overview(UUID bidRequestId) {
        bidRequest(bidRequestId);
        BidAnalysis current = current(bidRequestId);
        return new Overview(current != null ? toDto(current) : null,
                documentReadingService.hasReadingInProgress(bidRequestId));
    }

    /** 다시 분석. 신규 회차가 지금 분석이 되고 앞 회차의 결정은 이어지지 않는다(BIZ-092) */
    @Transactional
    public Overview rerun(UUID bidRequestId, UUID adminId) {
        BidRequest bidRequest = lockedBidRequest(bidRequestId);
        if (bidRequest.getState() != BidRequestState.ANALYZING) {
            throw BidAnalysisException.notAllowed(Reason.NOT_ANALYZING);
        }
        BidAnalysis current = current(bidRequestId);
        if (current != null && current.isRunning()) {
            throw BidAnalysisException.notAllowed(Reason.ANALYSIS_RUNNING);
        }
        if (documentReadingService.hasReadingInProgress(bidRequestId)) {
            throw BidAnalysisException.notAllowed(Reason.READING_IN_PROGRESS);
        }
        BidAnalysis started = start(bidRequestId, adminId);
        log.info("[CR-170] 다시 분석: bidRequest={}, analysis={}", bidRequestId, started.getId());
        return new Overview(toDto(started), false);
    }

    /** 관리자 결정(BIZ-092). 회차가 없으면 결정만 있는 회차를 만들고, 다시 정하면 덮어쓴다 */
    @Transactional
    public Overview decide(UUID bidRequestId, BidDecision decision, String note, UUID adminId) {
        BidRequest bidRequest = lockedBidRequest(bidRequestId);
        if (bidRequest.getState() != BidRequestState.ANALYZING) {
            throw BidAnalysisException.notAllowed(Reason.NOT_ANALYZING);
        }
        BidAnalysis current = current(bidRequestId);
        if (current != null && current.isRunning()) {
            throw BidAnalysisException.notAllowed(Reason.ANALYSIS_RUNNING);
        }
        String trimmed = note == null || note.isBlank() ? null : note.trim();
        if (decision.requiresNote() && trimmed == null) {
            throw BidAnalysisException.noteRequired();
        }
        LocalDateTime now = now();
        if (current == null) {
            current = analysisRepository.save(BidAnalysis.decisionOnly(bidRequestId, now));
        }
        current.decide(decision, trimmed, adminId, now);
        log.info("[CR-170] 관리자 결정: bidRequest={}, analysis={}, decision={}", bidRequestId, current.getId(), decision);
        return new Overview(toDto(current), documentReadingService.hasReadingInProgress(bidRequestId));
    }

    // ─── 상태 전이 ───────────────────────────────────────────────────────

    /** BIZ-092: 분석에서 문서 작성 중으로 갈 때 지금 분석의 관리자 결정이 Go 나 조건부 Go 여야 한다 */
    @Transactional
    public void requireGoDecision(UUID bidRequestId) {
        BidAnalysis current = current(bidRequestId);
        if (current == null || current.getAdminDecision() == null || !current.getAdminDecision().allowsGeneration()) {
            throw BidAnalysisException.decisionRequired();
        }
    }

    /**
     * BIZ-090: 분석 단계이고 회차가 없고 읽는 중인 후보가 없으면 입찰 분석을 한 번 시작한다.
     * 분석 단계 진입과 읽기 회차 끝에서 부른다. 이미 회차가 있는 건은 다시 시작하지 않는다.
     */
    @Transactional
    public boolean startIfReady(UUID bidRequestId) {
        BidRequest bidRequest = lockedBidRequest(bidRequestId);
        if (bidRequest.getState() != BidRequestState.ANALYZING || analysisRepository.existsByBidRequestId(bidRequestId)) {
            return false;
        }
        if (documentReadingService.hasReadingInProgress(bidRequestId)) {
            return false;
        }
        BidAnalysis started = start(bidRequestId, bidRequest.lastEnteredBy(BidRequestState.ANALYZING));
        log.info("[CR-170] 입찰 분석 시작: bidRequest={}, analysis={}", bidRequestId, started.getId());
        return true;
    }

    /**
     * 읽기 회차가 끝날 때 볼 건: 그 회차가 읽은 내용을 후보 서류로 가진 건 가운데 분석 단계이고 입찰 분석 회차가 없는 건.
     * 같은 내용을 다른 건이 읽은 경우도 잡히고, 그 내용을 쓰지 않는 건(배포 전부터 분석 단계에 있던 건 등)은 보지 않는다
     */
    @Transactional
    public List<UUID> pendingBidRequestIds(Collection<UUID> readingIds) {
        Set<String> contents = documentReadingService.contentsOf(readingIds);
        if (contents.isEmpty()) {
            return List.of();
        }
        return bidRequestRepository.findByStateIn(List.of(BidRequestState.ANALYZING)).stream()
                .map(BidRequest::getId)
                .filter(id -> !analysisRepository.existsByBidRequestId(id))
                .filter(id -> documentReadingService.usesAnyContent(id, contents))
                .toList();
    }

    // ─── 워크플로우 입력과 결과 ──────────────────────────────────────────

    @Transactional
    public void recordRun(UUID analysisId, String workflowRunId) {
        analysisRepository.findById(analysisId).ifPresent(a -> a.recordRun(workflowRunId));
    }

    /** 워크플로우가 끝났는데 분석 중이면 Bid/No-Bid 결정이 저장되지 않은 것이다. 저장된 산출물은 그대로 둔다 */
    @Transactional
    public void closeUnfinished(UUID analysisId) {
        analysisRepository.findById(analysisId).filter(BidAnalysis::isRunning).ifPresent(a -> {
            a.markFailed(NO_DECISION_REASON, now());
            log.warn("[CR-170] 입찰 분석 실패: analysis={}, 사유={}", analysisId, NO_DECISION_REASON);
        });
    }

    /** 회차가 이 입찰 건의 것인지 확인한다. 입력 도구와 저장 도구가 쓴다 */
    @Transactional(readOnly = true)
    public BidAnalysis analysisOf(UUID bidRequestId, UUID analysisId) {
        return analysisRepository.findById(analysisId)
                .filter(a -> a.getBidRequestId().equals(bidRequestId))
                .orElseThrow(() -> new IllegalArgumentException("이 입찰 건의 입찰 분석 회차가 아닙니다: " + analysisId));
    }

    /**
     * save_bid_analysis. 분석 중인 회차에 순서대로(REPORT, CAPTURE_PLAN, DECISION) 저장한다.
     * 꼴이 틀리면 아무것도 바꾸지 않고 틀린 곳을 IllegalArgumentException 으로 알린다.
     * 같은 단계를 다시 저장하면 본문과 그 단계의 확인 필요를 바꾼다.
     */
    @Transactional
    public Map<String, Object> save(UUID bidRequestId, UUID analysisId, Part part, Map<String, Object> body,
                                    List<Object> questions) {
        BidAnalysis a = analysisOf(bidRequestId, analysisId);
        if (!a.isRunning()) {
            throw new IllegalArgumentException("분석 중인 회차가 아니라 저장하지 않습니다: status=" + a.getStatus());
        }
        if (part == Part.CAPTURE_PLAN && a.getReportJson() == null) {
            throw new IllegalArgumentException("종합 분석 리포트(REPORT)를 먼저 저장해야 합니다");
        }
        if (part == Part.DECISION && a.getCapturePlanJson() == null) {
            throw new IllegalArgumentException("캡처플랜(CAPTURE_PLAN)을 먼저 저장해야 합니다");
        }
        BidAnalysisBodyValidator.validate(part, body);
        BidAnalysisBodyValidator.validateQuestions(questions);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("analysisId", analysisId.toString());
        out.put("part", part.name());
        switch (part) {
            case REPORT -> a.saveReport(toJson(body));
            case CAPTURE_PLAN -> a.saveCapturePlan(toJson(body));
            case DECISION -> {
                BidAnalysisScoring.Result r = BidAnalysisScoring.compute(body, parseMap(a.getReportJson()),
                        parseMap(a.getCapturePlanJson()), policy());
                a.markDone(toJson(body), r.pwinPercent(), r.verdict(), r.verdictReason(), toJson(r.scores()),
                        toJson(r.strengths()), toJson(r.weaknesses()), toJson(r.issueRanking()), now());
                out.put("pwinPercent", r.pwinPercent());
                out.put("aiVerdict", r.verdict().name());
                out.put("verdictReason", r.verdictReason());
                out.put("scores", r.scores());
                out.put("strengths", r.strengths());
                out.put("weaknesses", r.weaknesses());
                out.put("issueRanking", r.issueRanking());
            }
        }
        List<Map<String, Object>> merged = mergeQuestions(a.getQuestionsJson(), part, questions);
        a.saveQuestions(toJson(merged));
        out.put("questions", merged.size());
        out.put("status", a.getStatus().name());
        log.info("[CR-170] 입찰 분석 저장: analysis={}, part={}, 확인 필요={}건", analysisId, part, merged.size());
        return out;
    }

    /** 정책 POL-041. 변경하면 다음 계산부터 맞고 이미 계산한 회차는 그대로다 */
    public PwinPolicy policy() {
        return PwinPolicy.of(settingService.findValue(SettingKey.BID_PWIN_WEIGHTS).orElse(null),
                settingService.findValue(SettingKey.BID_PWIN_THRESHOLDS).orElse(null));
    }

    // ─── 내부 ───────────────────────────────────────────────────────────

    private BidAnalysis start(UUID bidRequestId, UUID startedBy) {
        BidAnalysis started = analysisRepository.save(BidAnalysis.running(bidRequestId, startedBy, now()));
        UUID analysisId = started.getId();
        afterCommit(() -> aiWorkflowService.runBidAnalysisAsync(bidRequestId, analysisId));
        return started;
    }

    /** 지금 분석. 분석 중이 대기 한도를 넘었으면 분석 실패로 바꾼다 */
    private BidAnalysis current(UUID bidRequestId) {
        BidAnalysis current = analysisRepository.findFirstByBidRequestIdOrderByStartedAtDesc(bidRequestId).orElse(null);
        if (current != null && current.isStale(now(), analysisLimit)) {
            current.markFailed(TIMEOUT_REASON, now());
            log.warn("[CR-170] 입찰 분석 시간 초과: analysis={}", current.getId());
        }
        return current;
    }

    private List<Map<String, Object>> mergeQuestions(String existingJson, Part part, List<Object> questions) {
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> q : parseQuestions(existingJson)) {
            if (!part.name().equals(q.get(QUESTION_PART))) merged.add(q);
        }
        for (Object raw : questions) {
            @SuppressWarnings("unchecked")
            Map<String, Object> q = new LinkedHashMap<>((Map<String, Object>) raw);
            q.put(QUESTION_PART, part.name());
            merged.add(q);
        }
        return merged;
    }

    private Analysis toDto(BidAnalysis a) {
        return new Analysis(
                a.getId(), a.getStatus(), a.getStartedAt(), a.getFinishedAt(), a.getFailureReason(),
                parse(a.getReportJson()), parse(a.getCapturePlanJson()), parse(a.getDecisionJson()),
                parseQuestions(a.getQuestionsJson()),
                a.getPwinPercent(), a.getAiVerdict(), a.getVerdictReason(),
                parse(a.getScoresJson()), parse(a.getStrengthsJson()), parse(a.getWeaknessesJson()),
                parse(a.getIssueRankingJson()),
                a.getAdminDecision(), a.getAdminNote(), person(a.getDecidedBy()), a.getDecidedAt(),
                person(a.getStartedBy()));
    }

    private Person person(UUID memberId) {
        if (memberId == null) return null;
        return memberRepository.findById(memberId)
                .map(m -> new Person(m.getId(), displayName(m)))
                .orElse(new Person(memberId, null));
    }

    /** 담당자명, 회사명, 이메일 순 */
    private static String displayName(Member m) {
        if (m.getContactPerson() != null && !m.getContactPerson().isBlank()) return m.getContactPerson();
        if (m.getCompanyName() != null && !m.getCompanyName().isBlank()) return m.getCompanyName();
        return m.getEmail();
    }

    private BidRequest bidRequest(UUID bidRequestId) {
        return bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));
    }

    private BidRequest lockedBidRequest(UUID bidRequestId) {
        return bidRequestRepository.findByIdForUpdate(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));
    }

    private List<Map<String, Object>> parseQuestions(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, QUESTION_LIST);
        } catch (Exception e) {
            log.warn("[CR-170] 확인 필요 목록을 읽지 못함: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        Object parsed = parse(json);
        return parsed instanceof Map ? (Map<String, Object>) parsed : Map.of();
    }

    private Object parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("[CR-170] 저장된 입찰 분석 JSON 을 읽지 못함: {}", e.getMessage());
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 으로 바꾸지 못했습니다: " + e.getMessage());
        }
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
