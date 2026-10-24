package com.biddingagency.domain.reading.service;

import com.biddingagency.common.ContentHash;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.bid.service.AIWorkflowService;
import com.biddingagency.domain.bid.service.DeadlineAlertService;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.reading.DocumentReadingException;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Detail;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Fulfill;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Item;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Origin;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Overview;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.SourceDocument;
import com.biddingagency.domain.reading.entity.DocumentReading;
import com.biddingagency.domain.reading.entity.ReadAs;
import com.biddingagency.domain.reading.entity.ReadingStatus;
import com.biddingagency.domain.reading.repository.DocumentReadingRepository;
import com.biddingagency.integration.storage.StorageService;
import com.biddingagency.security.McpAccessGuard;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 고객 서류 읽기 (CR-169, BID-DOC-004, BID-DOC-005, BIZ-085~089).
 *
 * 입찰 건의 후보는 그 건의 공고별 서류와 그 회원의 만료 전 공통 서류함 서류다. 저장하지 않고 부를 때마다 모으며,
 * 내용 해시가 같은 파일은 한 후보로 묶는다. 읽은 결과는 해시 하나에 한 줄이라 다른 건에서 읽은 내용은 다시 읽지 않는다.
 * Aimbase 호출과 기다림은 {@link AIWorkflowService} 가 맡고, 이 서비스는 후보, 상태, 결과 저장만 한다.
 */
@Slf4j
@Service
public class DocumentReadingService {

    public static final String TIMEOUT_REASON = "읽기 시간 초과";
    private static final int DESIGN_VALUE_LIMIT = 300;
    private static final Set<BidRequestState> BEFORE_ANALYSIS = EnumSet.of(
            BidRequestState.CREATED, BidRequestState.DOCS_PENDING, BidRequestState.DOCS_RECEIVED, BidRequestState.CLOSED);
    private static final Set<String> CATEGORIES = Set.of(
            "COMPANY", "LICENSE", "PAST_PERFORMANCE", "PERSONNEL", "EQUIPMENT",
            "FINANCIAL", "INSURANCE", "CERTIFICATION", "OTHER");
    private static final Set<String> CONFIDENCES = Set.of("HIGH", "MEDIUM", "LOW");
    private static final String VISION = "VISION";
    private static final ZoneId SERVICE_ZONE = ZoneId.of(DeadlineAlertService.SERVICE_ZONE);
    private static final TypeReference<List<Map<String, Object>>> FACT_LIST = new TypeReference<>() {
    };

    private final DocumentReadingRepository readingRepository;
    private final BidRequestRepository bidRequestRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final MemberDocumentRepository memberDocumentRepository;
    private final RequirementFulfillmentMapRepository fulfillmentMapRepository;
    private final StorageService storageService;
    private final McpAccessGuard mcpAccessGuard;
    private final AIWorkflowService aiWorkflowService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    /** 읽는 중 기한. 워크플로우를 기다리는 상한(폴링 간격 x 횟수)과 같게 둔다(BIZ-087) */
    private final Duration readingLimit;

    public DocumentReadingService(DocumentReadingRepository readingRepository,
                                  BidRequestRepository bidRequestRepository,
                                  ClientDocumentRepository clientDocumentRepository,
                                  MemberDocumentRepository memberDocumentRepository,
                                  RequirementFulfillmentMapRepository fulfillmentMapRepository,
                                  StorageService storageService,
                                  McpAccessGuard mcpAccessGuard,
                                  @Lazy AIWorkflowService aiWorkflowService,
                                  ObjectMapper objectMapper,
                                  Clock clock,
                                  @Value("${app.aimbase.polling.interval-ms:3000}") long pollingIntervalMs,
                                  @Value("${app.aimbase.polling.max-attempts:60}") int maxPollingAttempts) {
        this.readingRepository = readingRepository;
        this.bidRequestRepository = bidRequestRepository;
        this.clientDocumentRepository = clientDocumentRepository;
        this.memberDocumentRepository = memberDocumentRepository;
        this.fulfillmentMapRepository = fulfillmentMapRepository;
        this.storageService = storageService;
        this.mcpAccessGuard = mcpAccessGuard;
        this.aiWorkflowService = aiWorkflowService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.readingLimit = Duration.ofMillis(pollingIntervalMs * maxPollingAttempts);
    }

    // ─── 관리자 화면 ─────────────────────────────────────────────────────

    @Transactional
    public Overview overview(UUID bidRequestId) {
        BidRequest bidRequest = bidRequest(bidRequestId);
        List<Candidate> candidates = collect(bidRequest);
        return toOverview(bidRequest, candidates);
    }

    @Transactional(readOnly = true)
    public Detail detail(UUID readingId) {
        DocumentReading r = readingRepository.findById(readingId).orElseThrow(DocumentReadingException::notFound);
        return new Detail(r.getId(), r.getFileName(), r.getStatus(), r.getReadAs(), r.isVisionUsed(),
                r.getFailureReason(), r.getReadAt(), facts(r));
    }

    /**
     * 다시 읽기. readingIds 가 비면 읽음이 아닌 후보 전부, force 면 읽음도 다시 읽는다. 읽는 중인 서류는 건너뛴다.
     * 읽기는 이 요청이 저장된 뒤 시작한다.
     */
    @Transactional
    public Overview rerun(UUID bidRequestId, List<UUID> readingIds, boolean force) {
        BidRequest bidRequest = bidRequest(bidRequestId);
        if (bidRequest.getState() != BidRequestState.ANALYZING) {
            throw DocumentReadingException.notAnalyzing();
        }
        List<Candidate> candidates = collect(bidRequest);
        Set<String> known = knownContents(candidates);
        Map<UUID, DocumentReading> rows = ensureRows(candidates);

        Collection<DocumentReading> chosen;
        if (readingIds == null || readingIds.isEmpty()) {
            chosen = rows.values();
        } else {
            chosen = new ArrayList<>();
            for (UUID id : new LinkedHashSet<>(readingIds)) {
                DocumentReading row = rows.get(id);
                if (row == null) {
                    throw DocumentReadingException.notCandidate();
                }
                chosen.add(row);
            }
        }
        LocalDateTime now = now();
        List<UUID> targets = new ArrayList<>();
        for (DocumentReading row : chosen) {
            ReadingStatus status = row.getStatus();
            // 분석 중에 고객이 새로 올린 서류는 방금 대기 중으로 생겨 아무도 맡지 않았으므로 이 요청이 맡는다
            if (status == ReadingStatus.WAITING && !known.contains(row.getContentSha256())) {
                targets.add(row.getId());
                continue;
            }
            if (status.isInFlight() || status == ReadingStatus.UNSUPPORTED || (status == ReadingStatus.DONE && !force)) {
                continue;
            }
            row.markWaiting(now);
            targets.add(row.getId());
        }
        log.info("[CR-169] 다시 읽기: bidRequest={}, 대상={}건, force={}", bidRequestId, targets.size(), force);
        if (!targets.isEmpty()) {
            afterCommit(() -> aiWorkflowService.readClientDocumentsAsync(bidRequestId, targets));
        }
        return toOverview(bidRequest, candidates);
    }

    // ─── 상태 전이 ───────────────────────────────────────────────────────

    /** BIZ-087: 분석에서 문서 작성 중으로 갈 때 읽는 중(기한 안)인 후보가 있으면 막는다 */
    @Transactional
    public void requireNoReadingInProgress(UUID bidRequestId) {
        BidRequest bidRequest = bidRequest(bidRequestId);
        if (collect(bidRequest).stream().anyMatch(Candidate::inFlight)) {
            throw DocumentReadingException.inProgress();
        }
    }

    /**
     * 분석 단계 진입 때 읽을 서류를 줄 세운다(BIZ-085). 처음 보는 내용과 읽지 못한 내용만 읽고,
     * 읽음과 다른 건에서 읽는 중인 내용은 그대로 둔다. 전이와 같은 트랜잭션에서 대기 중으로 잡아
     * 읽기 작업이 실행 대기열에 밀려 있어도 그 사이 문서 작성 중으로 넘어가지 못하게 한다(BIZ-087).
     */
    @Transactional
    public List<UUID> queueForAnalysis(UUID bidRequestId) {
        BidRequest bidRequest = bidRequest(bidRequestId);
        List<Candidate> candidates = collect(bidRequest);
        Set<String> known = knownContents(candidates);
        Map<UUID, DocumentReading> rows = ensureRows(candidates);
        LocalDateTime now = now();
        List<UUID> targets = new ArrayList<>();
        for (DocumentReading row : rows.values()) {
            boolean created = !known.contains(row.getContentSha256());
            if (created && row.getStatus() == ReadingStatus.WAITING) {
                targets.add(row.getId());
            } else if (row.getStatus() == ReadingStatus.FAILED) {
                row.markWaiting(now);
                targets.add(row.getId());
            }
        }
        log.info("[CR-169] 분석 진입 읽기 대상: bidRequest={}, 후보={}건, 대상={}건",
                bidRequestId, candidates.size(), targets.size());
        if (!targets.isEmpty()) {
            afterCommit(() -> aiWorkflowService.readClientDocumentsAsync(bidRequestId, targets));
        }
        return targets;
    }

    // ─── 워크플로우 입력과 결과 ──────────────────────────────────────────

    /**
     * 대기 중인 줄을 읽는 중으로 바꾸고 워크플로우 입력을 만든다(T3-2 고객 서류 읽기 워크플로우 입력).
     * 대기 중이 아닌 줄은 다른 요청이 이미 맡았으므로 넣지 않는다.
     */
    @Transactional
    public Map<String, Object> beginReading(UUID bidRequestId, List<UUID> readingIds) {
        LocalDateTime now = now();
        List<Map<String, Object>> files = new ArrayList<>();
        List<Map<String, Object>> hwpFiles = new ArrayList<>();
        List<Map<String, Object>> targets = new ArrayList<>();
        for (DocumentReading row : readingRepository.findAllById(readingIds)) {
            if (row.getStatus() != ReadingStatus.WAITING || row.getReadAs() == ReadAs.NONE) {
                continue;
            }
            row.markReading(now);
            String id = row.getId().toString();
            String workspacePath = "docs/" + id + "." + ReadableForm.workspaceExtension(row.getFileName(), row.getContentType());
            files.add(Map.of(
                    "readingId", id,
                    "fileName", row.getFileName(),
                    "downloadUrl", mcpAccessGuard.documentReadingUrl(row.getId()),
                    "workspacePath", workspacePath));
            String targetPath = workspacePath;
            if (row.getReadAs() == ReadAs.HWP) {
                targetPath = "docs/" + id + ".txt";
                hwpFiles.add(Map.of("readingId", id, "workspacePath", workspacePath, "convertedPath", targetPath));
            }
            targets.add(Map.of("readingId", id, "targetPath", targetPath));
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("bidRequestId", bidRequestId.toString());
        input.put("files", files);
        input.put("hwpFiles", hwpFiles);
        input.put("targets", targets);
        return input;
    }

    @Transactional
    public void recordRun(List<UUID> readingIds, String workflowRunId) {
        for (DocumentReading row : readingRepository.findAllById(readingIds)) {
            if (row.getStatus().isInFlight()) {
                row.recordRun(workflowRunId);
            }
        }
    }

    /** 워크플로우가 끝났는데 결과가 오지 않은 줄을 읽지 못함으로 둔다 */
    @Transactional
    public void closeUnfinished(List<UUID> readingIds, String reason) {
        LocalDateTime now = now();
        for (DocumentReading row : readingRepository.findAllById(readingIds)) {
            if (row.getStatus().isInFlight()) {
                row.markFailed(truncate(reason, 500), now);
                log.warn("[CR-169] 읽지 못함: reading={}, 사유={}", row.getId(), reason);
            }
        }
    }

    /**
     * save_client_document_facts. extract 결과 원소마다 순번(index)으로 targets 의 줄을 찾고,
     * 사실에 sourceFile 이 있으면 그 경로의 줄로 보낸다. targets 에 없는 경로와 이 건 후보가 아닌 readingId 는 버린다.
     * 결과가 온 줄은 사실이 0개여도 읽음이다.
     */
    @Transactional
    public SaveResult saveFacts(UUID bidRequestId, List<Map<String, Object>> targets, List<Object> results) {
        Map<UUID, DocumentReading> candidates = new HashMap<>();
        for (Candidate c : collect(bidRequest(bidRequestId))) {
            if (c.reading != null) candidates.put(c.reading.getId(), c.reading);
        }

        List<String> targetPaths = new ArrayList<>();
        Map<String, UUID> readingByPath = new LinkedHashMap<>();
        int ignored = 0;
        for (Map<String, Object> t : targets) {
            String path = str(t.get("targetPath"));
            UUID readingId = uuidOrNull(t.get("readingId"));
            targetPaths.add(path);
            if (path == null || readingId == null || !candidates.containsKey(readingId)) {
                ignored++;
                continue;
            }
            readingByPath.put(path, readingId);
        }

        Map<UUID, List<Map<String, Object>>> factsByReading = new LinkedHashMap<>();
        for (Object raw : results) {
            Map<String, Object> element = asMap(raw);
            if (element == null || "failed".equals(element.get("status"))) {
                continue;
            }
            String elementPath = null;
            if (element.get("index") instanceof Number n && n.intValue() >= 0 && n.intValue() < targetPaths.size()) {
                elementPath = targetPaths.get(n.intValue());
            }
            UUID elementReading = elementPath != null ? readingByPath.get(elementPath) : null;
            if (elementReading != null) {
                factsByReading.computeIfAbsent(elementReading, k -> new ArrayList<>());
            }
            for (Map<String, Object> fact : factsOf(element)) {
                String sourceFile = str(fact.get("sourceFile"));
                UUID readingId = sourceFile != null ? readingByPath.get(sourceFile) : elementReading;
                if (readingId == null) {
                    ignored++;
                    continue;
                }
                factsByReading.computeIfAbsent(readingId, k -> new ArrayList<>()).add(normalize(fact));
            }
        }

        LocalDateTime now = now();
        int saved = 0;
        for (Map.Entry<UUID, List<Map<String, Object>>> e : factsByReading.entrySet()) {
            List<Map<String, Object>> facts = e.getValue();
            boolean vision = facts.stream().anyMatch(f -> VISION.equals(f.get("readMode")));
            candidates.get(e.getKey()).markDone(toJson(facts), facts.size(), vision, now);
            saved++;
        }
        log.info("[CR-169] 고객 서류 사실 저장: bidRequest={}, 읽음={}건, 버림={}건", bidRequestId, saved, ignored);
        return new SaveResult(saved, ignored);
    }

    public record SaveResult(int saved, int ignored) {
    }

    // ─── 제안서 도구 (BID-DOC-005) ────────────────────────────────────────

    /** get_proposal_design_input 의 clientDocuments. 읽음이 아닌 후보도 상태와 함께 주고 사실 값은 300자까지 */
    @Transactional
    public List<Map<String, Object>> designInputDocuments(UUID bidRequestId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Candidate c : collect(bidRequest(bidRequestId))) {
            if (c.reading == null) {
                continue;
            }
            DocumentReading r = c.reading;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("readingId", r.getId().toString());
            m.put("fileName", c.fileName);
            m.put("origin", c.origin.name());
            m.put("category", c.category);
            m.put("fulfills", c.fulfills);
            m.put("status", r.getStatus().name());
            m.put("factCount", r.getFactCount());
            if (r.getStatus() == ReadingStatus.DONE) {
                List<Map<String, Object>> summary = new ArrayList<>();
                for (Map<String, Object> f : facts(r)) {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("category", f.get("category"));
                    s.put("key", f.get("key"));
                    s.put("value", truncate(str(f.get("value")), DESIGN_VALUE_LIMIT));
                    summary.add(s);
                }
                m.put("facts", summary);
            }
            out.add(m);
        }
        return out;
    }

    /** BIZ-088: 근거 서류는 이 건 후보 가운데 읽음인 것만. 아니면 저장 전체를 거부한다 */
    @Transactional
    public void requireReadableSources(UUID bidRequestId, Collection<String> sourceReadingIds) {
        Set<String> allowed = doneReadingIds(bidRequestId);
        List<String> unknown = sourceReadingIds.stream().filter(id -> !allowed.contains(id)).distinct().sorted().toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknownReadingIds: " + unknown
                    + ". 근거 서류는 get_proposal_design_input 의 clientDocuments 가운데 status DONE 인 readingId 만 쓸 수 있습니다");
        }
    }

    /** 어느 절에도 걸리지 않은 읽음 서류 */
    @Transactional
    public List<String> unusedReadingIds(UUID bidRequestId, Collection<String> usedReadingIds) {
        Set<String> used = new LinkedHashSet<>(usedReadingIds);
        return doneReadingIds(bidRequestId).stream().filter(id -> !used.contains(id)).toList();
    }

    /** get_section_context, get_section_verify_input 의 sourceDocuments. 읽음인 근거 서류의 사실 전부(BIZ-089) */
    @Transactional
    public List<Map<String, Object>> sourceDocuments(UUID bidRequestId, List<String> sourceReadingIds) {
        if (sourceReadingIds == null || sourceReadingIds.isEmpty()) {
            return List.of();
        }
        Map<String, Candidate> byReading = new HashMap<>();
        for (Candidate c : collect(bidRequest(bidRequestId))) {
            if (c.reading != null) byReading.put(c.reading.getId().toString(), c);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (String id : sourceReadingIds) {
            Candidate c = byReading.get(id);
            if (c == null || c.reading.getStatus() != ReadingStatus.DONE) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("readingId", id);
            m.put("fileName", c.fileName);
            m.put("category", c.category);
            m.put("facts", facts(c.reading));
            out.add(m);
        }
        return out;
    }

    /** 관리자 절 상세의 근거 서류 꼬리표 */
    @Transactional(readOnly = true)
    public List<SourceDocument> sourceDocumentTags(List<String> sourceReadingIds) {
        if (sourceReadingIds == null || sourceReadingIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, DocumentReading> rows = new HashMap<>();
        for (DocumentReading r : readingRepository.findAllById(sourceReadingIds.stream().map(this::uuidOrNull)
                .filter(java.util.Objects::nonNull).toList())) {
            rows.put(r.getId(), r);
        }
        List<SourceDocument> out = new ArrayList<>();
        for (String id : sourceReadingIds) {
            DocumentReading r = rows.get(uuidOrNull(id));
            if (r != null) {
                out.add(new SourceDocument(r.getId(), r.getFileName(), r.getStatus(), r.getFactCount()));
            }
        }
        return out;
    }

    // ─── 후보 모으기 ─────────────────────────────────────────────────────

    /**
     * 후보를 모은다. 공고별 서류를 먼저 넣어 같은 내용이면 공고별 서류의 이름과 출처를 쓴다(BIZ-085).
     * 해시가 빈 옛 줄은 저장소 파일로 채우고, 읽는 중이 기한을 넘긴 줄은 읽지 못함으로 바꾼다(BIZ-087).
     */
    private List<Candidate> collect(BidRequest bidRequest) {
        Map<UUID, List<Fulfill>> fulfillsByDocument = fulfillsByClientDocument(bidRequest.getId());
        LocalDate today = LocalDate.now(clock.withZone(SERVICE_ZONE));

        Map<String, Candidate> bySha = new LinkedHashMap<>();
        List<Candidate> unhashed = new ArrayList<>();
        for (ClientDocument doc : clientDocumentRepository.findByBidRequestId(bidRequest.getId())) {
            String sha = doc.getContentSha256();
            if (sha == null) {
                sha = hashOf(doc.getStorageUrl(), doc.getId());
                if (sha != null) doc.recordContentHash(sha);
            }
            List<Fulfill> fulfills = fulfillsByDocument.getOrDefault(doc.getId(), List.of());
            Candidate c = new Candidate(sha, doc.getFileName(), doc.getContentType(), Origin.BID_DOCUMENT,
                    doc.getId(), doc.getDocumentCategory(), new ArrayList<>(fulfills));
            add(bySha, unhashed, c);
        }
        for (MemberDocument doc : memberDocumentRepository.findByMemberIdOrderByCreatedAtDesc(bidRequest.getMember().getId())) {
            if (doc.getExpiresOn() != null && doc.getExpiresOn().isBefore(today)) {
                continue;
            }
            String sha = doc.getContentSha256();
            if (sha == null) {
                sha = hashOf(doc.getStorageUrl(), doc.getId());
                if (sha != null) doc.recordContentHash(sha);
            }
            Candidate c = new Candidate(sha, doc.getFileName(), doc.getContentType(), Origin.VAULT,
                    doc.getId(), doc.getCategory().name(), new ArrayList<>());
            add(bySha, unhashed, c);
        }

        LocalDateTime now = now();
        if (!bySha.isEmpty()) {
            for (DocumentReading r : readingRepository.findByContentSha256In(bySha.keySet())) {
                if (r.isStale(now, readingLimit)) {
                    r.markFailed(TIMEOUT_REASON, now);
                    log.warn("[CR-169] 읽기 시간 초과: reading={}", r.getId());
                }
                bySha.get(r.getContentSha256()).reading = r;
            }
        }
        List<Candidate> out = new ArrayList<>(bySha.values());
        out.addAll(unhashed);
        return out;
    }

    private void add(Map<String, Candidate> bySha, List<Candidate> unhashed, Candidate c) {
        if (c.sha256 == null) {
            unhashed.add(c);
            return;
        }
        Candidate existing = bySha.putIfAbsent(c.sha256, c);
        if (existing != null) {
            for (Fulfill f : c.fulfills) {
                if (!existing.fulfills.contains(f)) existing.fulfills.add(f);
            }
        }
    }

    private Map<UUID, List<Fulfill>> fulfillsByClientDocument(UUID bidRequestId) {
        Map<UUID, List<Fulfill>> out = new HashMap<>();
        for (RequirementFulfillmentMap map : fulfillmentMapRepository.findByBidRequestId(bidRequestId)) {
            if (map.getFulfillmentType() != FulfillmentType.CLIENT_DOCUMENT || map.getClientDocument() == null
                    || map.getRequirementItem() == null) {
                continue;
            }
            out.computeIfAbsent(map.getClientDocument().getId(), k -> new ArrayList<>())
                    .add(new Fulfill(map.getRequirementItem().getTitle(), map.getRequirementItem().getReqId()));
        }
        return out;
    }

    private String hashOf(String storageUrl, UUID documentId) {
        try {
            return ContentHash.of(storageService.load(storageUrl));
        } catch (Exception e) {
            log.warn("[CR-169] 서류 파일을 읽지 못해 해시를 내지 못함: document={}, reason={}", documentId, e.getMessage());
            return null;
        }
    }

    /** 읽기 줄이 없는 후보에 줄을 만든다. 읽을 수 없는 형식은 처음부터 UNSUPPORTED */
    /** ensureRows 전에 이미 읽기 줄이 있던 내용. 그 뒤 대기 중인 줄 가운데 여기 없는 것이 이번에 새로 생긴 줄이다 */
    private Set<String> knownContents(List<Candidate> candidates) {
        Set<String> known = new LinkedHashSet<>();
        for (Candidate c : candidates) {
            if (c.reading != null) known.add(c.sha256);
        }
        return known;
    }

    private Map<UUID, DocumentReading> ensureRows(List<Candidate> candidates) {
        LocalDateTime now = now();
        Map<UUID, DocumentReading> rows = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            if (c.sha256 == null) {
                continue;
            }
            if (c.reading == null) {
                ReadAs readAs = ReadableForm.readAs(c.fileName, c.contentType);
                c.reading = readingRepository.save(DocumentReading.builder()
                        .contentSha256(c.sha256)
                        .fileName(c.fileName)
                        .contentType(c.contentType)
                        .readAs(readAs)
                        .status(readAs == ReadAs.NONE ? ReadingStatus.UNSUPPORTED : ReadingStatus.WAITING)
                        .statusAt(now)
                        .build());
            }
            rows.put(c.reading.getId(), c.reading);
        }
        return rows;
    }

    private Set<String> doneReadingIds(UUID bidRequestId) {
        Set<String> out = new LinkedHashSet<>();
        for (Candidate c : collect(bidRequest(bidRequestId))) {
            if (c.reading != null && c.reading.getStatus() == ReadingStatus.DONE) {
                out.add(c.reading.getId().toString());
            }
        }
        return out;
    }

    private Overview toOverview(BidRequest bidRequest, List<Candidate> candidates) {
        List<Item> items = new ArrayList<>();
        boolean inProgress = false;
        for (Candidate c : candidates) {
            DocumentReading r = c.reading;
            inProgress |= c.inFlight();
            items.add(new Item(
                    r != null ? r.getId() : null,
                    c.fileName,
                    c.origin,
                    c.documentId,
                    c.category,
                    List.copyOf(c.fulfills),
                    r != null ? r.getStatus() : null,
                    r != null ? r.getReadAs() : ReadableForm.readAs(c.fileName, c.contentType),
                    r != null ? r.getFactCount() : 0,
                    r != null && r.isVisionUsed(),
                    r != null ? r.getFailureReason() : null,
                    r != null ? r.getReadAt() : null));
        }
        return new Overview(!BEFORE_ANALYSIS.contains(bidRequest.getState()), inProgress, items);
    }

    private BidRequest bidRequest(UUID bidRequestId) {
        return bidRequestRepository.findByIdWithDetails(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));
    }

    // ─── 사실 다루기 ─────────────────────────────────────────────────────

    private List<Map<String, Object>> factsOf(Map<String, Object> element) {
        Map<String, Object> structured = asMap(element.get("structured_data"));
        Object facts = structured != null ? structured.get("facts") : element.get("facts");
        if (facts instanceof String s) {
            facts = parse(s);
        }
        if (!(facts instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            Map<String, Object> m = asMap(o);
            if (m != null) out.add(m);
        }
        return out;
    }

    /** 저장할 칸만 남긴다. 분류와 확신도는 정한 값이 아니면 OTHER, 비움으로 둔다 */
    private Map<String, Object> normalize(Map<String, Object> fact) {
        String category = upper(fact.get("category"));
        String confidence = upper(fact.get("confidence"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("category", category != null && CATEGORIES.contains(category) ? category : "OTHER");
        m.put("key", str(fact.get("key")));
        m.put("value", str(fact.get("value")));
        m.put("page", str(fact.get("page")));
        m.put("anchor", str(fact.get("anchor")));
        m.put("confidence", confidence != null && CONFIDENCES.contains(confidence) ? confidence : null);
        m.put("readMode", VISION.equals(upper(fact.get("readMode"))) ? VISION : "TEXT");
        return m;
    }

    private List<Map<String, Object>> facts(DocumentReading r) {
        if (r.getFactsJson() == null || r.getFactsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(r.getFactsJson(), FACT_LIST);
        } catch (Exception e) {
            log.warn("[CR-169] 사실 목록을 읽지 못함: reading={}", r.getId());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object raw) {
        if (raw instanceof Map<?, ?> m) return (Map<String, Object>) m;
        if (raw instanceof String s && parse(s) instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }

    private Object parse(String json) {
        String trimmed = json.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return objectMapper.readValue(trimmed, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("사실 목록을 저장 꼴로 바꾸지 못함", e);
        }
    }

    // ─── 유틸 ───────────────────────────────────────────────────────────

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

    private UUID uuidOrNull(Object raw) {
        if (raw == null) return null;
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String str(Object raw) {
        if (raw == null) return null;
        String s = raw.toString();
        return s.isBlank() ? null : s;
    }

    private static String upper(Object raw) {
        String s = str(raw);
        return s != null ? s.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    /** 내용 해시 하나로 묶은 후보. reading 은 아직 읽기를 걸지 않았으면 비어 있다 */
    private static final class Candidate {
        private final String sha256;
        private final String fileName;
        private final String contentType;
        private final Origin origin;
        private final UUID documentId;
        private final String category;
        private final List<Fulfill> fulfills;
        private DocumentReading reading;

        private Candidate(String sha256, String fileName, String contentType, Origin origin,
                          UUID documentId, String category, List<Fulfill> fulfills) {
            this.sha256 = sha256;
            this.fileName = fileName;
            this.contentType = contentType;
            this.origin = origin;
            this.documentId = documentId;
            this.category = category;
            this.fulfills = fulfills;
        }

        private boolean inFlight() {
            return reading != null && reading.getStatus().isInFlight();
        }
    }
}
