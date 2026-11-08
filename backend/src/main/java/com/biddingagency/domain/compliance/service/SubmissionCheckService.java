package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.analysis.service.BidAnalysisService;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.bid.service.BidPriceService;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementCheckStatus;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.entity.SubmissionCheckNote;
import com.biddingagency.domain.compliance.entity.SubmissionDisposition;
import com.biddingagency.domain.compliance.entity.SubmissionNoteTarget;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.compliance.repository.SubmissionCheckNoteRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.entity.Verdict;
import com.biddingagency.domain.proposal.entity.VerificationLog;
import com.biddingagency.domain.proposal.entity.VerificationMethod;
import com.biddingagency.domain.proposal.entity.VerificationTargetType;
import com.biddingagency.domain.proposal.service.DocumentCompositionService;
import com.biddingagency.domain.proposal.service.ProposalService;
import com.biddingagency.domain.proposal.service.VerificationLogService;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 제출 검사 (CR-194, BIZ-105 ~ 107, BIZ-109). SHALL 줄 전부를 채우는 쪽과 상관없이 상태로 가르고 제출 가능 여부를 낸다.
 * 절 본문, 검증 기록, 충족 기록을 바꾸지 않는다. 입찰 상태 전이도 막지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SubmissionCheckService {

    private static final String REASON_SECTION_FAILED = "SECTION_FAILED";
    private static final String REASON_SECTION_UNVERIFIED = "SECTION_UNVERIFIED";
    private static final String REASON_INPUT_CHANGED = "INPUT_CHANGED";
    private static final String BLOCKER_NOT_READY = "NOT_READY";
    private static final String BLOCKER_SECTION_NOT_PASSED = "SECTION_NOT_PASSED";
    private static final String BLOCKER_INPUT_CHANGED = "INPUT_CHANGED";
    private static final String BLOCKER_CONDITION_UNRESOLVED = "CONDITION_UNRESOLVED";
    private static final String BLOCKER_PRICE_INCOMPLETE = "PRICE_INCOMPLETE";
    private static final String BLOCKER_LINE_PREFIX = "LINE_";

    private final BidRequestService bidRequestService;
    private final BidDocumentRepository documentRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final RequirementFulfillmentMapRepository fulfillmentMapRepository;
    private final SubmissionCheckNoteRepository noteRepository;
    private final ComplianceService complianceService;
    private final ProposalService proposalService;
    private final VerificationLogService verificationLogService;
    private final DocumentCompositionService documentCompositionService;
    private final DocumentReadingService documentReadingService;
    private final BidAnalysisService bidAnalysisService;
    private final BidPriceService bidPriceService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Check check(UUID bidRequestId) {
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(bidRequestId);
        Notice notice = notice(bidRequest);
        LocalDateTime analyzedAt = notice == null ? null : notice.getAnalyzedAt();
        Map<String, SubmissionCheckNote> notes = validNotes(bidRequestId, analyzedAt);

        List<SectionRow> sections = new ArrayList<>();
        Map<String, List<SectionRow>> sectionsByRef = new HashMap<>();
        Map<UUID, SectionRow> sectionsById = new HashMap<>();
        collectSections(bidRequest, notice, sections, sectionsByRef, sectionsById);

        List<LineRow> lines = lines(bidRequest, notes, sectionsByRef, sectionsById);
        List<ConditionRow> conditions = conditions(bidRequestId, notes);
        BidPriceService.PriceTable table = bidPriceService.adminTable(bidRequestId);
        PriceSummary price = new PriceSummary(table.status(), table.lineCount(), table.filledCount(), table.total());

        List<Blocker> blockers = blockers(lines, sections, conditions, price);
        return new Check(blockers.isEmpty(), blockers, sections, lines, conditions, price);
    }

    @Transactional
    public Check saveLineNote(UUID bidRequestId, String reqId, SubmissionDisposition disposition,
                              String reason, String sourceRef, String decidedBy) {
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(bidRequestId);
        if (disposition == null || disposition == SubmissionDisposition.RESOLVED) {
            throw badRequest("줄에 쓸 수 없는 처리입니다");
        }
        if (disposition == SubmissionDisposition.NOT_ANSWERED && (!StringUtils.hasText(reason) || !StringUtils.hasText(sourceRef))) {
            throw badRequest("답하지 않는 줄은 사유와 공고 원문 근거가 필요합니다");
        }
        if (disposition == SubmissionDisposition.EQUIVALENT && !StringUtils.hasText(reason)) {
            throw badRequest("동등 인정은 근거가 필요합니다");
        }
        boolean known = shallLines(bidRequest).stream().anyMatch(item -> reqId.equals(item.getReqId()));
        if (!known) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SHALL 요구 줄이 아닙니다: " + reqId);
        }
        write(bidRequest, SubmissionNoteTarget.REQ_LINE, reqId, disposition, reason, sourceRef, decidedBy);
        return check(bidRequestId);
    }

    @Transactional
    public Check clearLineNote(UUID bidRequestId, String reqId) {
        clear(bidRequestId, SubmissionNoteTarget.REQ_LINE, reqId);
        return check(bidRequestId);
    }

    @Transactional
    public Check resolveCondition(UUID bidRequestId, String key, String reason, String decidedBy) {
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(bidRequestId);
        if (!StringUtils.hasText(reason)) {
            throw badRequest("해소 근거가 필요합니다");
        }
        if (conditionKeys(bidRequestId).stream().noneMatch(key::equals)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "지금 입찰 분석의 결정 조건이 아닙니다: " + key);
        }
        write(bidRequest, SubmissionNoteTarget.DECISION_CONDITION, key, SubmissionDisposition.RESOLVED, reason, null, decidedBy);
        return check(bidRequestId);
    }

    @Transactional
    public Check clearCondition(UUID bidRequestId, String key) {
        clear(bidRequestId, SubmissionNoteTarget.DECISION_CONDITION, key);
        return check(bidRequestId);
    }

    /** 절 검증을 저장할 때 남기는 입력 묶음 (BIZ-107) */
    @Transactional(readOnly = true)
    public String currentBundle(UUID sectionId) {
        ProposalSection section = proposalService.getSection(sectionId);
        ProposalChapter chapter = proposalService.getChapter(section.getChapterId());
        BidDocument document = documentRepository.findById(chapter.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + chapter.getDocumentId()));
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(document.getBidRequest().getId());
        return bundle(sharedInput(bidRequest, notice(bidRequest)), section);
    }

    private void write(BidRequest bidRequest, SubmissionNoteTarget target, String key, SubmissionDisposition disposition,
                       String reason, String sourceRef, String decidedBy) {
        Notice notice = notice(bidRequest);
        if (notice == null || notice.getAnalyzedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "공고 분석이 끝나야 처리할 수 있습니다");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<SubmissionCheckNote> existing = noteRepository
                .findByBidRequestIdAndTargetTypeAndTargetKey(bidRequest.getId(), target, key);
        if (existing.isPresent()) {
            existing.get().rewrite(disposition, reason, sourceRef, notice.getAnalyzedAt(), decidedBy, now);
            return;
        }
        noteRepository.save(SubmissionCheckNote.builder()
                .bidRequestId(bidRequest.getId())
                .targetType(target)
                .targetKey(key)
                .disposition(disposition)
                .reason(reason)
                .sourceRef(sourceRef)
                .noticeAnalyzedAt(notice.getAnalyzedAt())
                .decidedBy(decidedBy)
                .decidedAt(now)
                .build());
    }

    private void clear(UUID bidRequestId, SubmissionNoteTarget target, String key) {
        noteRepository.findByBidRequestIdAndTargetTypeAndTargetKey(bidRequestId, target, key)
                .ifPresent(noteRepository::delete);
        noteRepository.flush();
    }

    private Map<String, SubmissionCheckNote> validNotes(UUID bidRequestId, LocalDateTime analyzedAt) {
        Map<String, SubmissionCheckNote> notes = new HashMap<>();
        for (SubmissionCheckNote note : noteRepository.findByBidRequestId(bidRequestId)) {
            if (note.getNoticeAnalyzedAt().equals(analyzedAt)) {
                notes.put(noteKey(note.getTargetType(), note.getTargetKey()), note);
            }
        }
        return notes;
    }

    private void collectSections(BidRequest bidRequest, Notice notice, List<SectionRow> sections,
                                 Map<String, List<SectionRow>> byRef, Map<UUID, SectionRow> byId) {
        SharedInput shared = sharedInput(bidRequest, notice);
        for (BidDocument document : documentRepository.findByBidRequestId(bidRequest.getId())) {
            for (ProposalService.ChapterWithSections chapter : proposalService.getTree(document.getId())) {
                for (ProposalSection section : chapter.sections()) {
                    SectionRow row = sectionRow(document.getId(), section, shared);
                    sections.add(row);
                    byId.put(section.getId(), row);
                    if (section.getRequirementRefs() != null) {
                        for (String ref : new HashSet<>(section.getRequirementRefs())) {
                            byRef.computeIfAbsent(ref, r -> new ArrayList<>()).add(row);
                        }
                    }
                }
            }
        }
    }

    /** 가장 늦은 기록이 절 규칙 검사면 본문이 그 뒤로 다시 쓰인 것이라 앞 검증을 쓰지 않는다 */
    private SectionRow sectionRow(UUID documentId, ProposalSection section, SharedInput shared) {
        Optional<VerificationLog> latest = verificationLogService
                .findLatest(VerificationTargetType.PROPOSAL_SECTION, section.getId());
        if (latest.isEmpty() || latest.get().getMethod() != VerificationMethod.LLM) {
            return new SectionRow(documentId, section.getId(), section.getTitle(), Verdict.UNVERIFIED.name(), false);
        }
        VerificationLog log = latest.get();
        boolean changed = log.getVerdict() != Verdict.UNVERIFIED
                && !Objects.equals(log.getInputBundleJson(), bundle(shared, section));
        return new SectionRow(documentId, section.getId(), section.getTitle(), log.getVerdict().name(), changed);
    }

    private List<LineRow> lines(BidRequest bidRequest, Map<String, SubmissionCheckNote> notes,
                                Map<String, List<SectionRow>> sectionsByRef, Map<UUID, SectionRow> sectionsById) {
        Map<UUID, RequirementFulfillmentMap> maps = new HashMap<>();
        for (RequirementFulfillmentMap map : fulfillmentMapRepository.findByBidRequestId(bidRequest.getId())) {
            maps.put(map.getRequirementItem().getId(), map);
        }
        List<LineRow> rows = new ArrayList<>();
        for (OpportunityRequirementItem item : shallLines(bidRequest)) {
            SubmissionCheckNote note = notes.get(noteKey(SubmissionNoteTarget.REQ_LINE, item.getReqId()));
            RequirementFulfillmentMap map = maps.get(item.getId());
            List<SectionRow> attached = attachedSections(item, map, sectionsByRef, sectionsById);
            LineJudgement judgement = note != null
                    ? new LineJudgement(RequirementCheckStatus.of(note.getDisposition()), null)
                    : judge(map, attached);
            rows.add(new LineRow(
                    item.getReqId(),
                    ComplianceService.text(item),
                    ComplianceService.json(item, RequirementSyncService.KEY_TRANSLATION),
                    ComplianceService.json(item, RequirementSyncService.KEY_FULFILLMENT_PARTY),
                    judgement.status().name(),
                    map == null ? null : map.getNotes(),
                    judgement.reason(),
                    note == null ? null : new NoteView(note.getDisposition().name(), note.getReason(),
                            note.getSourceRef(), note.getDecidedBy(), note.getDecidedAt())));
        }
        return rows;
    }

    private static List<SectionRow> attachedSections(OpportunityRequirementItem item, RequirementFulfillmentMap map,
                                                     Map<String, List<SectionRow>> sectionsByRef,
                                                     Map<UUID, SectionRow> sectionsById) {
        if (map != null && map.getFulfillmentType() == FulfillmentType.DOCUMENT_SECTION) {
            SectionRow recorded = sectionOf(map, sectionsById);
            return recorded == null ? List.of() : List.of(recorded);
        }
        if (map == null || map.getStatus() == FulfillmentStatus.PENDING) {
            return sectionsByRef.getOrDefault(item.getReqId(), List.of());
        }
        return List.of();
    }

    private static SectionRow sectionOf(RequirementFulfillmentMap map, Map<UUID, SectionRow> sectionsById) {
        try {
            return map.getDocumentSectionPath() == null ? null : sectionsById.get(UUID.fromString(map.getDocumentSectionPath()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LineJudgement judge(RequirementFulfillmentMap map, List<SectionRow> attached) {
        for (SectionRow section : attached) {
            if (Verdict.UNVERIFIED.name().equals(section.verdict())) {
                return new LineJudgement(RequirementCheckStatus.UNVERIFIED, REASON_SECTION_UNVERIFIED);
            }
            if (section.inputChanged()) {
                return new LineJudgement(RequirementCheckStatus.UNVERIFIED, REASON_INPUT_CHANGED);
            }
        }
        if (map == null || map.getStatus() == FulfillmentStatus.PENDING) {
            return new LineJudgement(RequirementCheckStatus.NOT_CHECKED, null);
        }
        if (map.getStatus() == FulfillmentStatus.MISSING) {
            return new LineJudgement(RequirementCheckStatus.NOT_MET, null);
        }
        if (map.getFulfillmentType() == FulfillmentType.CLIENT_DOCUMENT) {
            return new LineJudgement(RequirementCheckStatus.MET, null);
        }
        if (map.getFulfillmentType() != FulfillmentType.DOCUMENT_SECTION || attached.isEmpty()) {
            return new LineJudgement(RequirementCheckStatus.NOT_CHECKED, null);
        }
        return Verdict.PASS.name().equals(attached.get(0).verdict())
                ? new LineJudgement(RequirementCheckStatus.MET, null)
                : new LineJudgement(RequirementCheckStatus.NOT_MET, REASON_SECTION_FAILED);
    }

    private List<ConditionRow> conditions(UUID bidRequestId, Map<String, SubmissionCheckNote> notes) {
        BidAnalysisService.DecisionConditions current = bidAnalysisService.decisionConditions(bidRequestId);
        List<ConditionRow> rows = new ArrayList<>();
        for (int i = 0; i < current.texts().size(); i++) {
            String key = conditionKey(current.analysisId(), i);
            SubmissionCheckNote note = notes.get(noteKey(SubmissionNoteTarget.DECISION_CONDITION, key));
            rows.add(new ConditionRow(key, current.texts().get(i), note != null, note == null ? null
                    : new NoteView(note.getDisposition().name(), note.getReason(), null, note.getDecidedBy(), note.getDecidedAt())));
        }
        return rows;
    }

    private List<String> conditionKeys(UUID bidRequestId) {
        BidAnalysisService.DecisionConditions current = bidAnalysisService.decisionConditions(bidRequestId);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < current.texts().size(); i++) {
            keys.add(conditionKey(current.analysisId(), i));
        }
        return keys;
    }

    private static List<Blocker> blockers(List<LineRow> lines, List<SectionRow> sections, List<ConditionRow> conditions,
                                          PriceSummary price) {
        List<Blocker> blockers = new ArrayList<>();
        if (lines.isEmpty() || sections.isEmpty()) {
            blockers.add(new Blocker(BLOCKER_NOT_READY, 1));
        }
        Map<String, Long> blockedLines = lines.stream()
                .filter(line -> !RequirementCheckStatus.valueOf(line.status()).isClear())
                .collect(Collectors.groupingBy(LineRow::status, LinkedHashMap::new, Collectors.counting()));
        blockedLines.forEach((status, count) -> blockers.add(new Blocker(BLOCKER_LINE_PREFIX + status, count)));
        addIfAny(blockers, BLOCKER_SECTION_NOT_PASSED, sections.stream().filter(s -> !Verdict.PASS.name().equals(s.verdict())).count());
        addIfAny(blockers, BLOCKER_INPUT_CHANGED, sections.stream().filter(SectionRow::inputChanged).count());
        addIfAny(blockers, BLOCKER_CONDITION_UNRESOLVED, conditions.stream().filter(c -> !c.resolved()).count());
        if (BidPriceService.NOT_READY.equals(price.status()) || BidPriceService.INCOMPLETE.equals(price.status())) {
            blockers.add(new Blocker(BLOCKER_PRICE_INCOMPLETE, Math.max(1, price.lineCount() - price.filledCount())));
        }
        return blockers;
    }

    private static void addIfAny(List<Blocker> blockers, String code, long count) {
        if (count > 0) {
            blockers.add(new Blocker(code, count));
        }
    }

    private List<OpportunityRequirementItem> shallLines(BidRequest bidRequest) {
        return complianceService.matrixLines(bidRequest.getOpportunity().getId()).stream()
                .filter(item -> item.getReqLevel() == RequirementLevel.SHALL)
                .toList();
    }

    private Notice notice(BidRequest bidRequest) {
        return documentCompositionService.designNotice(bidRequest.getOpportunity().getId()).orElse(null);
    }

    private SharedInput sharedInput(BidRequest bidRequest, Notice notice) {
        UUID analysisId = bidAnalysisService.decisionConditions(bidRequest.getId()).analysisId();
        Set<String> currentContents = clientDocumentRepository.findByBidRequestId(bidRequest.getId()).stream()
                .map(ClientDocument::getContentSha256)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new SharedInput(notice == null || notice.getAnalyzedAt() == null ? null : notice.getAnalyzedAt().toString(),
                companyHash(bidRequest.getMember()),
                analysisId == null ? null : analysisId.toString(),
                currentContents);
    }

    /** 고객이 서류를 바꿔 근거 내용이 입찰 건에서 빠져도 입력이 바뀐 것으로 잡히게 표시한다 */
    private String bundle(SharedInput shared, ProposalSection section) {
        Set<String> hashes = new TreeSet<>();
        if (section.getSourceReadingIds() != null && !section.getSourceReadingIds().isEmpty()) {
            List<UUID> ids = section.getSourceReadingIds().stream().map(UUID::fromString).toList();
            for (String content : documentReadingService.contentsOf(ids)) {
                hashes.add(shared.currentContents().contains(content) ? content : "removed:" + content);
            }
        }
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("noticeAnalyzedAt", shared.noticeAnalyzedAt());
        bundle.put("sourceReadingHashes", new ArrayList<>(hashes));
        bundle.put("companyHash", shared.companyHash());
        bundle.put("bidAnalysisId", shared.bidAnalysisId());
        try {
            return objectMapper.writeValueAsString(bundle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("입력 묶음을 만들지 못했습니다", e);
        }
    }

    private static String companyHash(Member member) {
        String joined = String.join("\u0001",
                String.valueOf(member.getCompanyName()),
                String.valueOf(member.getBusinessRegistrationNumber()),
                String.valueOf(member.getAddress()),
                String.valueOf(member.getUei()),
                String.valueOf(member.getCageCode()),
                String.valueOf(member.getContactPerson()),
                String.valueOf(member.getPhone()),
                String.valueOf(member.getEmail()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String conditionKey(UUID analysisId, int index) {
        return analysisId + ":" + index;
    }

    private static String noteKey(SubmissionNoteTarget target, String key) {
        return target.name() + "\u0001" + key;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record SharedInput(String noticeAnalyzedAt, String companyHash, String bidAnalysisId, Set<String> currentContents) {
    }

    private record LineJudgement(RequirementCheckStatus status, String reason) {
    }

    public record Check(boolean submittable, List<Blocker> blockers, List<SectionRow> sections, List<LineRow> lines,
                        List<ConditionRow> conditions, PriceSummary price) {
    }

    public record Blocker(String code, long count) {
    }

    public record SectionRow(UUID documentId, UUID sectionId, String title, String verdict, boolean inputChanged) {
    }

    public record LineRow(String reqId, String text, String translation, String fulfillmentParty, String status,
                          String evidenceQuote, String reason, NoteView note) {
    }

    public record ConditionRow(String key, String text, boolean resolved, NoteView note) {
    }

    public record NoteView(String disposition, String reason, String sourceRef, String decidedBy, LocalDateTime decidedAt) {
    }

    public record PriceSummary(String status, long lineCount, long filledCount, BigDecimal total) {
    }
}
