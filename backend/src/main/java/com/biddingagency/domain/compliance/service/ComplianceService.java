package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.compliance.dto.ComplianceIssue;
import com.biddingagency.domain.compliance.dto.ComplianceMatrixView;
import com.biddingagency.domain.compliance.dto.CoverageResult;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.dto.ValidationResult;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentStatus;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Compliance Service
 *
 * Core validation engine with BLOCKER detection system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceService {

    private final BidRequestRepository bidRequestRepository;
    private final OpportunityRequirementItemRepository requirementRepository;
    private final RequirementFulfillmentMapRepository fulfillmentMapRepository;
    private final BidDocumentRepository documentRepository;
    private final NoticeRepository noticeRepository;
    private final ProposalService proposalService;
    private final Clock clock;

    /**
     * Validate bid request for submission readiness
     * This is the BLOCKER system - prevents submission if requirements not met
     */
    public ValidationResult validateBidRequest(UUID bidRequestId) {
        log.info("Validating bid request: {}", bidRequestId);

        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        List<ComplianceIssue> blockers = new ArrayList<>();
        List<ComplianceIssue> warnings = new ArrayList<>();

        Opportunity opportunity = bidRequest.getOpportunity();

        // 1. Check deadline
        if (opportunity.getResponseDeadline() != null &&
                opportunity.getResponseDeadline().isBefore(LocalDateTime.now())) {
            blockers.add(ComplianceIssue.builder()
                    .severity(ComplianceIssue.Severity.BLOCKER)
                    .category("DEADLINE")
                    .message("Opportunity deadline has passed: " + opportunity.getResponseDeadline())
                    .suggestedAction("Cannot submit after deadline")
                    .build());
        }

        // 2. Check document status
        List<BidDocument> documents = documentRepository.findByBidRequestId(bidRequestId);
        for (BidDocument doc : documents) {
            if (doc.getStatus() == DocumentStatus.DRAFT) {
                warnings.add(ComplianceIssue.builder()
                        .severity(ComplianceIssue.Severity.WARNING)
                        .category("DOCUMENT_STATUS")
                        .message("Document " + doc.getDocumentType().getDisplayName() + " is still in DRAFT status")
                        .suggestedAction("Review and approve document")
                        .build());
            }
        }

        // 3. Check requirement fulfillment
        List<OpportunityRequirementItem> requirements =
                requirementRepository.findByOpportunityIdAndReqIdIsNullOrderBySortOrderAsc(opportunity.getId());

        List<OpportunityRequirementItem> blockerRequirements =
                requirements.stream().filter(OpportunityRequirementItem::getIsBlocker).toList();

        int totalRequirements = blockerRequirements.size();
        int fulfilledCount = 0;

        for (OpportunityRequirementItem requirement : blockerRequirements) {
            RequirementFulfillmentMap fulfillment = fulfillmentMapRepository
                    .findByBidRequestIdAndRequirementItemId(bidRequestId, requirement.getId())
                    .orElse(null);

            if (fulfillment == null || fulfillment.getStatus() == FulfillmentStatus.MISSING) {
                blockers.add(ComplianceIssue.builder()
                        .severity(ComplianceIssue.Severity.BLOCKER)
                        .category("REQUIREMENT")
                        .requirementItemId(requirement.getId())
                        .requirementTitle(requirement.getTitle())
                        .message("Required item not fulfilled: " + requirement.getTitle())
                        .suggestedAction("Map requirement to document section or attachment")
                        .build());
            } else if (fulfillment.getStatus() == FulfillmentStatus.FULFILLED) {
                fulfilledCount++;
            }
        }

        // Calculate fulfillment rate
        double fulfillmentRate = totalRequirements > 0 ?
                (double) fulfilledCount / totalRequirements * 100 : 100.0;

        // Determine overall status
        ValidationResult.Status status;
        if (!blockers.isEmpty()) {
            status = ValidationResult.Status.BLOCKER;
        } else if (!warnings.isEmpty()) {
            status = ValidationResult.Status.WARNING;
        } else {
            status = ValidationResult.Status.PASS;
        }

        ValidationResult result = ValidationResult.builder()
                .status(status)
                .blockers(blockers)
                .warnings(warnings)
                .fulfillmentRate(fulfillmentRate)
                .totalRequirements(totalRequirements)
                .fulfilledRequirements(fulfilledCount)
                .validatedAt(LocalDateTime.now())
                .build();

        log.info("Validation complete: status={}, blockers={}, warnings={}, fulfillmentRate={}%",
                status, blockers.size(), warnings.size(), fulfillmentRate);

        return result;
    }

    /**
     * Get blocker issues only
     */
    public List<ComplianceIssue> getBlockers(UUID bidRequestId) {
        ValidationResult result = validateBidRequest(bidRequestId);
        return result.getBlockers();
    }

    /**
     * Create or update fulfillment mapping
     */
    @Transactional
    public RequirementFulfillmentMap mapRequirement(UUID bidRequestId, UUID requirementItemId,
                                                     FulfillmentType fulfillmentType,
                                                     UUID documentId, String sectionPath,
                                                     String attachmentRef, String notes,
                                                     UUID mappedBy) {
        log.info("Mapping requirement {} for bid request {}", requirementItemId, bidRequestId);

        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        OpportunityRequirementItem requirement = requirementRepository.findById(requirementItemId)
                .orElseThrow(() -> new IllegalArgumentException("Requirement not found: " + requirementItemId));

        BidDocument document = null;
        if (documentId != null) {
            document = documentRepository.findById(documentId).orElse(null);
        }

        // Find existing or create new
        RequirementFulfillmentMap map = fulfillmentMapRepository
                .findByBidRequestIdAndRequirementItemId(bidRequestId, requirementItemId)
                .orElse(RequirementFulfillmentMap.builder()
                        .bidRequest(bidRequest)
                        .requirementItem(requirement)
                        .build());

        // Update fulfillment details
        map.updateFulfillment(fulfillmentType, documentId, sectionPath, attachmentRef, notes, mappedBy);

        if (document != null) {
            map.setDocument(document);
        }

        RequirementFulfillmentMap saved = fulfillmentMapRepository.save(map);

        log.info("Requirement mapping saved: {}", saved.getId());

        return saved;
    }

    /**
     * Get all fulfillment maps for bid request
     */
    public List<RequirementFulfillmentMap> getFulfillmentMaps(UUID bidRequestId) {
        return fulfillmentMapRepository.findByBidRequestId(bidRequestId);
    }

    // ===== CR-010 요구사항 슬롯 =====
    // 슬롯 조회, 올리기, 빼기, 자동 채우기는 CR-141 에서 PreCheckService 로 옮겼다(본인 확인, 요구 항목 기준).

    /**
     * BLOCKER 슬롯이 모두 충족되었는지 (FSM DOCS_PENDING→DOCS_RECEIVED 게이트, BIZ-015)
     * @return 미충족 BLOCKER 요구사항 목록 (빈 리스트면 전이 가능)
     */
    public List<OpportunityRequirementItem> getUnfulfilledBlockerSlots(UUID bidRequestId) {
        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        List<OpportunityRequirementItem> blockers = requirementRepository
                .findByOpportunityIdAndIsBlocker(bidRequest.getOpportunity().getId(), true);

        List<OpportunityRequirementItem> unfulfilled = new ArrayList<>();
        for (OpportunityRequirementItem requirement : blockers) {
            RequirementFulfillmentMap map = fulfillmentMapRepository
                    .findByBidRequestIdAndRequirementItemId(bidRequestId, requirement.getId())
                    .orElse(null);
            if (map == null || map.getStatus() != FulfillmentStatus.FULFILLED) {
                unfulfilled.add(requirement);
            }
        }
        return unfulfilled;
    }

    // ===== CR-159 컴플라이언스 매트릭스와 커버리지 검증 =====

    /** 그 공고의 매트릭스 줄, REQ ID 순 */
    public List<OpportunityRequirementItem> matrixLines(UUID opportunityId) {
        return requirementRepository.findByOpportunityIdAndReqIdIsNotNullOrderBySortOrderAsc(opportunityId);
    }

    /** 관리자 공고문 관리 상세의 매트릭스 영역. 매트릭스 줄이 없으면 빈 목록 */
    public ComplianceMatrixView getComplianceMatrix(UUID noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공고문을 찾을 수 없습니다"));
        List<ComplianceMatrixView.Line> lines = new ArrayList<>();
        List<ComplianceMatrixView.Question> questions = new ArrayList<>();
        for (OpportunityRequirementItem item : matrixLines(notice.getOpportunity().getId())) {
            boolean queryCandidate = Boolean.parseBoolean(json(item, RequirementSyncService.KEY_QUERY_CANDIDATE));
            lines.add(new ComplianceMatrixView.Line(
                    item.getReqId(),
                    item.getReqLevel() != null ? item.getReqLevel().name() : null,
                    text(item),
                    json(item, RequirementSyncService.KEY_TRANSLATION),
                    json(item, RequirementSyncService.KEY_SOURCE_FILE),
                    json(item, RequirementSyncService.KEY_PAGE),
                    json(item, RequirementSyncService.KEY_CLAUSE),
                    json(item, RequirementSyncService.KEY_QUOTE),
                    json(item, RequirementSyncService.KEY_VERB),
                    item.getCategory().name(),
                    json(item, RequirementSyncService.KEY_FULFILLMENT_PARTY),
                    json(item, RequirementSyncService.KEY_FACTOR_ID),
                    json(item, RequirementSyncService.KEY_FACTOR_TITLE),
                    queryCandidate));
            if (queryCandidate) {
                questions.add(new ComplianceMatrixView.Question(item.getReqId(),
                        json(item, RequirementSyncService.KEY_AMBIGUITY),
                        json(item, RequirementSyncService.KEY_DRAFT_QUERY)));
            }
        }
        return new ComplianceMatrixView(lines, questions, notice.getFactorTreeJson());
    }

    /** 제안서 설계 입력에 싣는 매트릭스 줄 */
    public List<Map<String, Object>> designInputLines(UUID opportunityId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OpportunityRequirementItem item : matrixLines(opportunityId)) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("reqId", item.getReqId());
            line.put("level", item.getReqLevel() != null ? item.getReqLevel().name() : null);
            line.put("text", text(item));
            line.put("verb", json(item, RequirementSyncService.KEY_VERB));
            line.put("fulfillmentParty", json(item, RequirementSyncService.KEY_FULFILLMENT_PARTY));
            line.put("factorId", json(item, RequirementSyncService.KEY_FACTOR_ID));
            line.put("factorTitle", json(item, RequirementSyncService.KEY_FACTOR_TITLE));
            result.add(line);
        }
        return result;
    }

    /** 절 작성과 절 검증 입력에 싣는 그 절의 대응 REQ ID 원문. 매트릭스에 없는 번호는 싣지 않는다 */
    public List<Map<String, Object>> sectionRequirements(UUID documentId, List<String> requirementRefs) {
        if (requirementRefs == null || requirementRefs.isEmpty()) {
            return List.of();
        }
        Map<String, OpportunityRequirementItem> byReqId = matrixByReqId(opportunityIdOfDocument(documentId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (String reqId : new LinkedHashSet<>(requirementRefs)) {
            OpportunityRequirementItem item = byReqId.get(reqId);
            if (item == null) {
                continue;
            }
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("reqId", reqId);
            req.put("text", text(item));
            req.put("translation", json(item, RequirementSyncService.KEY_TRANSLATION));
            req.put("verb", json(item, RequirementSyncService.KEY_VERB));
            req.put("level", item.getReqLevel() != null ? item.getReqLevel().name() : null);
            req.put("factorId", json(item, RequirementSyncService.KEY_FACTOR_ID));
            req.put("factorTitle", json(item, RequirementSyncService.KEY_FACTOR_TITLE));
            result.add(req);
        }
        return result;
    }

    /**
     * BIZ-076: 절의 대응 REQ ID 는 매트릭스의 REQ ID 만 받는다. 없는 ID 가 하나라도 있으면 그 ID 들을 담아 거부한다.
     * 매트릭스 줄이 없는 공고는 검사하지 않는다.
     */
    public void requireKnownReqIds(UUID documentId, Collection<String> requirementRefs) {
        Set<String> known = matrixByReqId(opportunityIdOfDocument(documentId)).keySet();
        if (known.isEmpty()) {
            return;
        }
        List<String> unknown = requirementRefs.stream().filter(ref -> !known.contains(ref)).distinct().sorted().toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("매트릭스에 없는 ID: " + String.join(", ", unknown)
                    + ". 대응 REQ ID 는 get_proposal_design_input 의 complianceMatrix 에 있는 reqId 만 쓸 수 있습니다");
        }
    }

    /** BIZ-076: 대상 줄 가운데 이 문서의 어느 절도 대응 REQ ID 로 들지 않은 것 */
    public List<String> unassignedReqIds(UUID documentId) {
        Set<String> mapped = new HashSet<>();
        for (ProposalSection section : proposalService.getAllSections(documentId)) {
            mapped.addAll(refsOf(section));
        }
        return matrixLines(opportunityIdOfDocument(documentId)).stream()
                .filter(ComplianceService::isCoverageTarget)
                .map(OpportunityRequirementItem::getReqId)
                .filter(reqId -> !mapped.contains(reqId))
                .toList();
    }

    /**
     * BIZ-077: 제안서 절 검증이 낸 REQ ID 별 답함 여부를 그 입찰 요청의 충족 매핑에 남긴다.
     * 그 절의 대응 REQ ID 가 아닌 번호와 매트릭스에 없는 번호는 버린다.
     * 그 절이 아닌 곳으로 이미 충족된 매핑은 건드리지 않는다.
     *
     * @return 남긴 매핑 수
     */
    @Transactional
    public int recordSectionChecks(UUID sectionId, List<Map<String, Object>> checks) {
        if (checks == null || checks.isEmpty()) {
            return 0;
        }
        ProposalSection section = proposalService.getSection(sectionId);
        ProposalChapter chapter = proposalService.getChapter(section.getChapterId());
        BidDocument document = documentRepository.findById(chapter.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + chapter.getDocumentId()));
        BidRequest bidRequest = document.getBidRequest();
        Map<String, OpportunityRequirementItem> byReqId = matrixByReqId(bidRequest.getOpportunity().getId());
        Set<String> refs = new HashSet<>(refsOf(section));
        String sectionKey = sectionId.toString();

        int recorded = 0;
        for (Map<String, Object> check : checks) {
            String reqId = check.get("reqId") == null ? null : check.get("reqId").toString();
            OpportunityRequirementItem item = byReqId.get(reqId);
            if (item == null || !refs.contains(reqId)) {
                continue;
            }
            Optional<RequirementFulfillmentMap> existing = fulfillmentMapRepository
                    .findByBidRequestIdAndRequirementItemId(bidRequest.getId(), item.getId());
            if (existing.isPresent() && existing.get().isFulfilledElsewhere(document.getId(), sectionKey)) {
                continue;
            }
            boolean answered = Boolean.parseBoolean(String.valueOf(check.get("answered")));
            Object note = answered ? check.get("evidenceQuote") : check.get("reason");
            RequirementFulfillmentMap map = existing.orElseGet(() -> RequirementFulfillmentMap.builder()
                    .bidRequest(bidRequest)
                    .requirementItem(item)
                    .fulfillmentType(FulfillmentType.DOCUMENT_SECTION)
                    .build());
            map.recordSectionAnswer(document, sectionKey, answered, note == null ? null : note.toString(),
                    LocalDateTime.now(clock));
            fulfillmentMapRepository.save(map);
            recorded++;
        }
        log.info("[CR-159] 절 검증 REQ 남김: sectionId={}, checks={}, recorded={}", sectionId, checks.size(), recorded);
        return recorded;
    }

    /**
     * BIZ-078 커버리지 검증. 대상은 Shall 이면서 플랫폼 생성인 매트릭스 줄이다.
     * 누락 REQ 는 대상 가운데 이 문서의 절로 충족된 매핑이 없는 줄이고, 그중 어느 절에도 걸리지 않은 줄이 매핑 안 된 REQ 다.
     */
    public CoverageResult coverage(UUID documentId) {
        BidDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제안서 문서를 찾을 수 없습니다"));
        BidRequest bidRequest = document.getBidRequest();
        List<OpportunityRequirementItem> lines = matrixLines(bidRequest.getOpportunity().getId());
        if (lines.isEmpty()) {
            return CoverageResult.notReady();
        }
        Map<String, OpportunityRequirementItem> byReqId = new LinkedHashMap<>();
        lines.forEach(item -> byReqId.put(item.getReqId(), item));

        Set<UUID> fulfilled = fulfillmentMapRepository.findByBidRequestId(bidRequest.getId()).stream()
                .filter(map -> map.isFulfilledByDocument(documentId))
                .map(map -> map.getRequirementItem().getId())
                .collect(Collectors.toSet());

        Set<String> mapped = new HashSet<>();
        List<CoverageResult.UnknownId> unknownIds = new ArrayList<>();
        List<CoverageResult.SectionRefs> sections = new ArrayList<>();
        for (ProposalService.ChapterWithSections chapter : proposalService.getTree(documentId)) {
            for (ProposalSection section : chapter.sections()) {
                List<CoverageResult.Ref> refs = new ArrayList<>();
                for (String reqId : new LinkedHashSet<>(refsOf(section))) {
                    OpportunityRequirementItem item = byReqId.get(reqId);
                    if (item == null) {
                        unknownIds.add(new CoverageResult.UnknownId(reqId, section.getId(), section.getTitle()));
                    } else {
                        mapped.add(reqId);
                    }
                    refs.add(new CoverageResult.Ref(reqId, item != null && fulfilled.contains(item.getId())));
                }
                sections.add(new CoverageResult.SectionRefs(section.getId(), refs));
            }
        }

        List<OpportunityRequirementItem> targets = lines.stream().filter(ComplianceService::isCoverageTarget).toList();
        List<CoverageResult.Missing> missing = targets.stream()
                .filter(item -> !fulfilled.contains(item.getId()))
                .map(item -> new CoverageResult.Missing(item.getReqId(), text(item), !mapped.contains(item.getReqId())))
                .toList();
        return new CoverageResult(true, targets.size(), targets.size() - missing.size(), missing, unknownIds, sections);
    }

    private static boolean isCoverageTarget(OpportunityRequirementItem item) {
        return item.getReqLevel() == RequirementLevel.SHALL
                && FulfillmentParty.PLATFORM_GENERATED.name().equals(json(item, RequirementSyncService.KEY_FULFILLMENT_PARTY));
    }

    private Map<String, OpportunityRequirementItem> matrixByReqId(UUID opportunityId) {
        Map<String, OpportunityRequirementItem> byReqId = new LinkedHashMap<>();
        matrixLines(opportunityId).forEach(item -> byReqId.put(item.getReqId(), item));
        return byReqId;
    }

    private UUID opportunityIdOfDocument(UUID documentId) {
        BidDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        return document.getBidRequest().getOpportunity().getId();
    }

    private static List<String> refsOf(ProposalSection section) {
        return section.getRequirementRefs() != null ? section.getRequirementRefs() : List.of();
    }

    private static String text(OpportunityRequirementItem item) {
        String text = json(item, RequirementSyncService.KEY_TEXT);
        return text != null ? text : item.getTitle();
    }

    private static String json(OpportunityRequirementItem item, String key) {
        Object value = item.getRequirementJson() == null ? null : item.getRequirementJson().get(key);
        return value == null ? null : value.toString();
    }
}
