package com.biddingagency.mcp.tool;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.compliance.service.ComplianceService;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.proposal.entity.ProposalBlock;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.entity.VerificationTargetType;
import com.biddingagency.domain.proposal.entity.Verdict;
import com.biddingagency.domain.proposal.service.DocumentComposition;
import com.biddingagency.domain.proposal.service.DocumentCompositionService;
import com.biddingagency.domain.proposal.service.ProposalService;
import com.biddingagency.domain.proposal.service.ProposalService.BlockInput;
import com.biddingagency.domain.proposal.service.ProposalService.ChapterInput;
import com.biddingagency.domain.proposal.service.ProposalService.SectionInput;
import com.biddingagency.domain.proposal.service.VerificationLogService;
import com.biddingagency.domain.proposal.entity.BlockType;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.service.ReferenceSampleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * MCP 도구: 제안서 파이프라인 3단계 (CR-028).
 *
 * design / write-section 워크플로우가 자율주행하며 호출하는 도구 모음.
 * assemble 은 서버가 한다 (CR-169, ProposalAssembleService).
 *
 * - get_proposal_design_input : design WF — documentId 로 공고/factorTree/공고텍스트 조회
 * - save_proposal_structure   : design WF 콜백 — chapter/section 트리 통째 저장
 * - get_section_context       : write-section WF — sectionId 로 scope/요구사항/참조샘플 조회
 * - save_section_blocks       : write-section WF 콜백 — 해당 section block 교체 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProposalMcpTool {

    private final BidRequestService bidRequestService;
    private final BidDocumentService bidDocumentService;
    private final NoticeRepository noticeRepository;
    private final ProposalService proposalService;
    private final ReferenceSampleService referenceSampleService;
    private final VerificationLogService verificationLogService;
    private final ComplianceService complianceService;
    private final DocumentCompositionService documentCompositionService;
    private final DocumentReadingService documentReadingService;
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_proposal_design_input",
            "description", "제안서 구조 설계(design)에 필요한 입력을 조회합니다. " +
                    "documentId 로 연결된 공고의 한글화 요약·요구서류·공고 본문(content)과, 정제 시 도출된 FACTOR/Subfactor 트리(factorTree)를 반환합니다. " +
                    "factorTree 가 있으면 그대로 chapter/section 으로 복사·검증하고, 없으면(null) 공고 본문(noticeContent/opportunityText)의 Section L/M 에서 FACTOR/Subfactor 를 직접 발췌하세요. " +
                    "CR-159: complianceMatrix(컴플라이언스 매트릭스 줄 [{reqId, level, text, verb, fulfillmentParty, factorId, factorTitle}])가 있으면 " +
                    "section 마다 답할 reqId 를 requirementRefs 로 정하세요. level=SHALL 이면서 fulfillmentParty=PLATFORM_GENERATED 인 줄은 빠짐없이 어느 section 엔가 걸어야 합니다. " +
                    "CR-169: clientDocuments(고객 서류 [{readingId, fileName, origin, category, fulfills, status, factCount, facts[{category, key, value}]}])를 보고 " +
                    "section 마다 쓸 근거 서류의 readingId 를 sourceReadingIds 로 정하세요. status=DONE 인 서류만 쓸 수 있습니다. " +
                    "CR-180: factorTree, complianceMatrix, priceItems 는 이 문서가 담을 평가 항목의 것만 옵니다. " +
                    "factorTree.factors 에 없는 평가 항목은 다른 문서가 담으므로 공고 본문에서 찾아 chapter 로 더하지 마세요.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "documentId", Map.of("type", "string", "description", "BidDocument UUID")
                ),
                "required", List.of("documentId")
            )
        ),
        Map.of(
            "name", "save_proposal_structure",
            "description", "design 단계 결과인 chapter/section 트리를 저장합니다 (기존 트리를 비우고 통째 재생성). " +
                    "공고가 박은 라벨·순서를 그대로 보존하세요(창작 금지). LOCKED section 이 하나라도 있으면 거부됩니다. " +
                    "CR-159: 공고에 컴플라이언스 매트릭스가 있으면 requirementRefs 는 그 reqId 만 받고, 없는 ID 가 하나라도 있으면 전체를 거부하며 그 ID 를 알려 줍니다. " +
                    "저장되면 unassignedReqIds(어느 section 에도 걸리지 않은 SHALL 이면서 PLATFORM_GENERATED 인 reqId)를 돌려줍니다. " +
                    "CR-180: 매트릭스에 있어도 이 문서의 complianceMatrix 에 없는 reqId(다른 문서의 평가 항목 줄)가 있으면 전체를 거부합니다. " +
                    "CR-169: sourceReadingIds 는 get_proposal_design_input 의 clientDocuments 가운데 status=DONE 인 readingId 만 받고, 아닌 id 가 있으면 전체를 거부하며 unknownReadingIds 로 알려 줍니다. " +
                    "저장되면 unusedReadingIds(어느 section 에도 걸리지 않은 읽음 서류)를 돌려줍니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "documentId", Map.of("type", "string", "description", "BidDocument UUID"),
                    "chapters", Map.ofEntries(
                        Map.entry("type", "array"),
                        Map.entry("description", "FACTOR 단위 chapter 배열 (공고 순서 그대로)"),
                        Map.entry("items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "factorLabel", Map.of("type", "string", "description", "공고가 박은 라벨 그대로 (예: 'I', 'A', 'Factor 1')"),
                                "factorTitle", Map.of("type", "string", "description", "FACTOR 제목"),
                                "sourceSection", Map.of("type", "string", "description", "NOTICE_L | NOTICE_M | SECTION_K (기본 NOTICE_M)"),
                                "sections", Map.of("type", "array", "description",
                                    "Subfactor 단위 section 배열. Subfactor 강제 안 한 FACTOR 는 section 1개(subfactorLabel=null)로 통합. " +
                                    "각 항목: {subfactorLabel, title, scope(작성 지침), requirementRefs(대응 REQ ID 배열, complianceMatrix 의 reqId), " +
                                    "sourceReadingIds(근거 고객 서류 readingId 배열, clientDocuments 의 status=DONE), minWords(최소 단어 수)}")
                            )
                        ))
                    )
                ),
                "required", List.of("documentId", "chapters")
            )
        ),
        Map.of(
            "name", "get_section_context",
            "description", "한 section 의 본문 작성에 필요한 컨텍스트를 조회합니다. " +
                    "section 의 scope(작성 지침)·title·minWords·requirementRefs 와, 소속 공고유형(industryType)에 매칭되는 참조 샘플 파일 목록(referenceSamples)을 반환합니다. " +
                    "CR-159: requirements 에 대응 REQ ID 마다 원문(text), 번역, 요구 동사(verb), 구분(level), 평가항목을 줍니다. 요구 동사가 정하는 형태로 답하세요. " +
                    "referenceSamples 의 downloadUrl 을 parse_document(url=...)로 읽어 few-shot 으로 참조하되, 사실(회사명·실적·수치)은 복붙하지 마세요. " +
                    "CR-169: sourceDocuments(이 section 의 근거 고객 서류 [{readingId, fileName, category, facts[{category, key, value, page, anchor, confidence, readMode}]}])가 있으면 " +
                    "회사명·실적·인력·면허·수치는 이 facts 에서만 가져오고, 쓴 사실의 readingId 와 page 를 block 의 sourceEvidence 에 남기세요.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "sectionId", Map.of("type", "string", "description", "ProposalSection UUID")
                ),
                "required", List.of("sectionId")
            )
        ),
        Map.of(
            "name", "save_section_blocks",
            "description", "write-section 단계 결과인 한 section 의 block 목록을 저장합니다 (해당 section 의 기존 block 을 전량 교체). " +
                    "section 이 LOCKED 이면 거부됩니다. 저장 후 section 상태는 DRAFTED 로 전이됩니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "sectionId", Map.of("type", "string", "description", "ProposalSection UUID"),
                    "blocks", Map.ofEntries(
                        Map.entry("type", "array"),
                        Map.entry("description", "block 배열 (작성 순서대로)"),
                        Map.entry("items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "blockType", Map.of("type", "string", "description", "PARAGRAPH | BULLET_LIST | TABLE | IMAGE | EVIDENCE (기본 PARAGRAPH)"),
                                "contentJson", Map.of("type", "object", "description", "TipTap node JSON"),
                                "sourceEvidence", Map.of("type", "object", "description", "근거 출처 (어떤 client doc/sample 에서 가져왔는지, optional)")
                            )
                        ))
                    )
                ),
                "required", List.of("sectionId", "blocks")
            )
        ),
        // ── CR-031 충실성 검증 ──
        Map.of(
            "name", "get_section_verify_input",
            "description", "한 section 의 충실성 검증에 필요한 입력을 조회합니다 (CR-031). " +
                    "section 의 scope·요구사항(requirementRefs)·현재 작성된 본문(blocks 의 text 평탄화)·" +
                    "공고 본문 텍스트(opportunityText)·참조 샘플 목록을 반환합니다. " +
                    "생성된 본문의 각 문장이 공고 본문/첨부에 근거하는지 검증하세요 — 근거 없는 문장은 환각입니다. " +
                    "CR-159: requirements 의 reqId 마다 본문이 그 요구에 답했는지 보고 save_verification_result 의 requirementChecks 로 보내세요. " +
                    "CR-169: sourceDocuments(get_section_context 와 같은 꼴)도 근거입니다. 고객 회사에 관한 사실이 sourceDocuments 의 facts 에 없으면 환각입니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "sectionId", Map.of("type", "string", "description", "ProposalSection UUID")
                ),
                "required", List.of("sectionId")
            )
        ),
        Map.of(
            "name", "get_notice_verify_input",
            "description", "공고문 정제 결과의 충실성 검증에 필요한 입력을 조회합니다 (CR-031). " +
                    "noticeId 로 한글 제목·요약(summary)·생성 본문(contentJson 평탄화)·요구서류와 " +
                    "원문 공고 텍스트(opportunityText)·첨부 파일 목록(attachmentFiles)을 반환합니다. " +
                    "생성된 본문/요약의 각 사실이 원문/첨부에 근거하는지 검증하세요 — 근거 없는 내용은 환각입니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "noticeId", Map.of("type", "string", "description", "Notice UUID")
                ),
                "required", List.of("noticeId")
            )
        ),
        Map.of(
            "name", "save_verification_result",
            "description", "충실성 검증 결과를 verification_log 에 저장합니다 (CR-031). " +
                    "환각(근거 없는 문장)이 1건 이상이면 verdict=FAIL, 없으면 PASS. " +
                    "targetType 은 NOTICE(공고문) 또는 PROPOSAL_SECTION(제안서 section).",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.ofEntries(
                    Map.entry("targetType", Map.of("type", "string", "description", "NOTICE | PROPOSAL_SECTION")),
                    Map.entry("targetId", Map.of("type", "string", "description", "noticeId 또는 sectionId UUID")),
                    Map.entry("verdict", Map.of("type", "string", "description", "PASS | FAIL")),
                    Map.entry("attempt", Map.of("type", "integer", "description", "자동 재시도 회차 (기본 1)")),
                    Map.entry("hallucinations", Map.of("type", "array", "description",
                            "근거 없는 문장 배열. 각 항목 {sentence, reason}")),
                    Map.entry("missingFromSource", Map.of("type", "array", "description",
                            "원문에 있는데 결과물에 누락된 항목 배열. 각 항목 {source_quote, reason}")),
                    Map.entry("wordCount", Map.of("type", "integer", "description", "결과물 단어 수 (optional)")),
                    Map.entry("requirementChecks", Map.of("type", "array", "description",
                            "CR-159 PROPOSAL_SECTION 만. 대응 REQ ID 마다 본문이 답했는지. 각 항목 {reqId, answered(boolean), evidenceQuote(답한 본문 인용), reason(답하지 않았으면 사유)}"))
                ),
                "required", List.of("targetType", "targetId", "verdict")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    /** design WF — 공고 한글화 요약 + factorTree + 공고 본문 텍스트 반환. */
    @Transactional(readOnly = true)
    public String getProposalDesignInput(Map<String, Object> args) {
        UUID documentId = uuid(args.get("documentId"));
        BidDocument doc = bidDocumentService.findById(documentId);
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(doc.getBidRequest().getId());
        Opportunity opp = bidRequest.getOpportunity();
        // BIZ-095: 평가받지 않는 문서는 평가 항목과 매트릭스를 받지 않아 그 문서 종류의 꼴로 짠다
        boolean evaluated = doc.getDocumentType().isEvaluated();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", documentId.toString());
        result.put("bidRequestId", bidRequest.getId().toString());
        result.put("documentType", doc.getDocumentType().name());
        result.put("evaluatedDocument", evaluated);
        result.put("opportunityId", opp.getId().toString());
        result.put("opportunityTitle", opp.getTitle());
        result.put("organizationName", opp.getOrganizationName());
        result.put("opportunityText", buildOpportunityText(opp));

        // 노출 중인 공고문(Notice) 최신 1건 — 한글화 요약·요구서류·본문·factorTree
        Notice notice = documentCompositionService.designNotice(opp.getId()).orElse(null);
        // BIZ-096: 문서마다 자기 평가 항목과 그 항목의 매트릭스 줄, 가격 항목만 받는다
        DocumentComposition composition = documentCompositionService.compose(notice);
        DocumentType documentType = doc.getDocumentType();
        if (notice != null) {
            result.put("noticeSummary", nullSafe(notice.getSummaryJson()));
            result.put("requiredDocuments", nullSafe(notice.getRequiredDocumentsJson()));
            result.put("documentFormats", nullSafe(notice.getDocumentFormatsJson()));
            result.put("noticeContent", nullSafe(notice.getContentJson()));
            result.put("factorTree", evaluated ? composition.factorTreeFor(documentType, notice.getFactorTreeJson()) : null);   // 평가받는 문서에서 null 이면 WF 가 본문 발췌
            result.put("priceItems", evaluated && composition.priceOwner() == documentType ? notice.getPriceItemsJson() : null);
        } else {
            result.put("factorTree", null);
            result.put("priceItems", null);
        }
        result.put("complianceMatrix", evaluated ? complianceService.designInputLines(opp.getId()).stream()
                .filter(line -> composition.owns(documentType, (String) line.get("factorId")))
                .toList() : List.of());
        result.put("clientDocuments", documentReadingService.designInputDocuments(bidRequest.getId()));
        return toJson(result);
    }

    /** design WF 콜백 — chapter/section 트리 통째 저장. */
    public String saveProposalStructure(Map<String, Object> args) {
        UUID documentId = uuid(args.get("documentId"));
        List<Map<String, Object>> chaptersRaw = castList(args.get("chapters"));
        if (chaptersRaw.isEmpty()) {
            throw new IllegalArgumentException("chapters 가 비어있습니다");
        }

        List<ChapterInput> chapters = new ArrayList<>();
        for (Map<String, Object> c : chaptersRaw) {
            List<SectionInput> sections = new ArrayList<>();
            for (Map<String, Object> s : castList(c.get("sections"))) {
                sections.add(new SectionInput(
                        str(s.get("subfactorLabel")),
                        str(s.get("title")),
                        str(s.get("scope")),
                        castStringList(s.get("requirementRefs")),
                        castStringList(s.get("sourceReadingIds")),
                        intOrNull(s.get("minWords"))
                ));
            }
            chapters.add(new ChapterInput(
                    str(c.get("factorLabel")),
                    str(c.get("factorTitle")),
                    str(c.get("sourceSection")),
                    sections
            ));
        }

        complianceService.requireKnownReqIds(documentId, chapters.stream()
                .flatMap(ch -> ch.sections().stream())
                .flatMap(sec -> sec.requirementRefs().stream())
                .toList());
        UUID bidRequestId = bidDocumentService.findById(documentId).getBidRequest().getId();
        List<String> sourceReadingIds = chapters.stream()
                .flatMap(ch -> ch.sections().stream())
                .flatMap(sec -> sec.sourceReadingIds().stream())
                .toList();
        documentReadingService.requireReadableSources(bidRequestId, sourceReadingIds);
        List<ProposalChapter> saved = proposalService.saveStructure(documentId, chapters);
        int sectionCount = saved.stream()
                .mapToInt(ch -> proposalService.getSections(ch.getId()).size()).sum();

        log.info("MCP save_proposal_structure: documentId={}, chapters={}, sections={}",
                documentId, saved.size(), sectionCount);

        // write-section 단계가 순회할 section id 목록을 반환 (BE 파이프라인이 이를 보고 write 호출)
        List<Map<String, Object>> sectionList = new ArrayList<>();
        for (ProposalChapter ch : saved) {
            for (ProposalSection s : proposalService.getSections(ch.getId())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("sectionId", s.getId().toString());
                e.put("chapterId", ch.getId().toString());
                e.put("title", s.getTitle());
                sectionList.add(e);
            }
        }
        return toJson(Map.of(
                "documentId", documentId.toString(),
                "status", "SAVED",
                "chapterCount", saved.size(),
                "sectionCount", sectionCount,
                "sections", sectionList,
                "unassignedReqIds", complianceService.unassignedReqIds(documentId),
                "unusedReadingIds", documentReadingService.unusedReadingIds(bidRequestId, sourceReadingIds)
        ));
    }

    /** write-section WF — section scope + 참조 샘플 반환. */
    @Transactional(readOnly = true)
    public String getSectionContext(Map<String, Object> args) {
        UUID sectionId = uuid(args.get("sectionId"));
        ProposalSection section = proposalService.getSection(sectionId);

        // section → chapter → document → bidRequest → opportunity (industryType)
        ProposalChapter chapter = proposalService.getChapter(section.getChapterId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sectionId", sectionId.toString());
        result.put("subfactorLabel", section.getSubfactorLabel());
        result.put("title", section.getTitle());
        result.put("scope", section.getScope());
        result.put("minWords", section.getMinWords());
        result.put("requirementRefs", section.getRequirementRefs());
        result.put("status", section.getStatus().name());
        result.put("factorLabel", chapter.getFactorLabel());
        result.put("factorTitle", chapter.getFactorTitle());
        result.put("requirements", complianceService.sectionRequirements(chapter.getDocumentId(), section.getRequirementRefs()));

        // 참조 샘플 — section 이 속한 문서의 공고유형 매칭
        IndustryType industryType = industryTypeOf(chapter);
        List<Map<String, Object>> samples = industryType != null
                ? referenceSampleService.collect(industryType) : List.of();
        result.put("industryType", industryType != null ? industryType.name() : null);
        result.put("referenceSamples", samples);
        UUID bidRequestId = bidDocumentService.findById(chapter.getDocumentId()).getBidRequest().getId();
        result.put("sourceDocuments", documentReadingService.sourceDocuments(bidRequestId, section.getSourceReadingIds()));
        return toJson(result);
    }

    /** write-section WF 콜백 — section block 교체 저장. */
    public String saveSectionBlocks(Map<String, Object> args) {
        UUID sectionId = uuid(args.get("sectionId"));
        List<Map<String, Object>> blocksRaw = castList(args.get("blocks"));

        List<BlockInput> blocks = new ArrayList<>();
        for (Map<String, Object> b : blocksRaw) {
            blocks.add(new BlockInput(
                    parseBlockType(b.get("blockType")),
                    castMap(b.get("contentJson")),
                    castMap(b.get("sourceEvidence"))
            ));
        }

        proposalService.replaceBlocks(sectionId, blocks);
        log.info("MCP save_section_blocks: sectionId={}, blocks={}", sectionId, blocks.size());
        return toJson(Map.of(
                "sectionId", sectionId.toString(),
                "status", "DRAFTED",
                "blockCount", blocks.size(),
                "message", "section block 저장 완료"
        ));
    }

    // ─── CR-031 충실성 검증 도구 ──────────────────────────────────────────

    /** verify-fidelity WF — section 원문/scope/현재 본문/참조 샘플 반환. */
    @Transactional(readOnly = true)
    public String getSectionVerifyInput(Map<String, Object> args) {
        UUID sectionId = uuid(args.get("sectionId"));
        ProposalSection section = proposalService.getSection(sectionId);
        ProposalChapter chapter = proposalService.getChapter(section.getChapterId());

        // section → document → bidRequest → opportunity
        BidDocument doc = bidDocumentService.findById(chapter.getDocumentId());
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(doc.getBidRequest().getId());
        Opportunity opp = bidRequest.getOpportunity();

        // 현재 작성된 본문 (block text 평탄화)
        List<ProposalBlock> blocks = proposalService.getBlocks(sectionId);
        StringBuilder body = new StringBuilder();
        for (ProposalBlock b : blocks) {
            if (b.getContentJson() != null) collectText(b.getContentJson(), body);
        }

        IndustryType industryType = opp.getIndustryType();
        List<Map<String, Object>> samples = industryType != null
                ? referenceSampleService.collect(industryType) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sectionId", sectionId.toString());
        result.put("title", section.getTitle());
        result.put("scope", section.getScope());
        result.put("minWords", section.getMinWords());
        result.put("requirementRefs", section.getRequirementRefs());
        result.put("requirements", complianceService.sectionRequirements(chapter.getDocumentId(), section.getRequirementRefs()));
        result.put("generatedBody", body.toString().trim());
        result.put("blockCount", blocks.size());
        result.put("opportunityText", buildOpportunityText(opp));
        result.put("referenceSamples", samples);
        result.put("sourceDocuments", documentReadingService.sourceDocuments(bidRequest.getId(), section.getSourceReadingIds()));
        return toJson(result);
    }

    /** verify-fidelity WF — 공고문 정제 원문/생성 본문 반환 (NOTICE 분기). */
    @Transactional(readOnly = true)
    public String getNoticeVerifyInput(Map<String, Object> args) {
        UUID noticeId = uuid(args.get("noticeId"));
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + noticeId));
        Opportunity opp = notice.getOpportunity();

        // 생성 본문 (contentJson TipTap 평탄화) + 요약 overview 를 검증 대상 본문으로 결합
        StringBuilder body = new StringBuilder();
        if (notice.getContentJson() != null) collectText(notice.getContentJson(), body);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("noticeId", noticeId.toString());
        result.put("koreanTitle", notice.getKoreanTitle());
        result.put("summary", nullSafe(notice.getSummaryJson()));
        result.put("requiredDocuments", nullSafe(notice.getRequiredDocumentsJson()));
        result.put("generatedBody", body.toString().trim());
        result.put("opportunityText", buildOpportunityText(opp));
        return toJson(result);
    }

    /** verify-fidelity WF 콜백 — 충실성 검증 결과 저장 (NOTICE/PROPOSAL_SECTION 공통). */
    public String saveVerificationResult(Map<String, Object> args) {
        VerificationTargetType targetType = parseTargetType(args.get("targetType"));
        UUID targetId = uuid(args.get("targetId"));
        Verdict verdict = parseVerdict(args.get("verdict"));
        int attempt = args.get("attempt") instanceof Number n ? n.intValue()
                : verificationLogService.nextAttempt(targetType, targetId);
        List<Map<String, Object>> hallucinations = castList(args.get("hallucinations"));
        List<Map<String, Object>> missing = castList(args.get("missingFromSource"));
        Integer wordCount = intOrNull(args.get("wordCount"));

        verificationLogService.recordLlm(targetType, targetId, null, verdict,
                hallucinations, missing, wordCount, attempt);
        int requirementsRecorded = targetType == VerificationTargetType.PROPOSAL_SECTION
                ? complianceService.recordSectionChecks(targetId, castList(args.get("requirementChecks"))) : 0;

        log.info("MCP save_verification_result: target={}/{}, verdict={}, 환각={}, attempt={}",
                targetType, targetId, verdict, hallucinations.size(), attempt);
        return toJson(Map.of(
                "targetType", targetType.name(),
                "targetId", targetId.toString(),
                "verdict", verdict.name(),
                "hallucinationCount", hallucinations.size(),
                "requirementsRecorded", requirementsRecorded,
                "status", "SAVED"
        ));
    }

    private VerificationTargetType parseTargetType(Object raw) {
        if (raw == null) throw new IllegalArgumentException("targetType 이 필요합니다 (NOTICE | PROPOSAL_SECTION)");
        try {
            return VerificationTargetType.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 targetType: " + raw);
        }
    }

    private Verdict parseVerdict(Object raw) {
        if (raw == null) throw new IllegalArgumentException("verdict 가 필요합니다 (PASS | FAIL)");
        try {
            return Verdict.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 verdict: " + raw);
        }
    }

    /** TipTap node 의 text 평탄화 (공백 결합). */
    private void collectText(Object node, StringBuilder sb) {
        if (node instanceof Map<?, ?> map) {
            Object text = map.get("text");
            if (text instanceof String s) sb.append(s).append(' ');
            Object content = map.get("content");
            if (content instanceof List<?> list) {
                for (Object child : list) collectText(child, sb);
            }
        } else if (node instanceof List<?> list) {
            for (Object child : list) collectText(child, sb);
        }
    }

    // ─── 내부 조회 헬퍼 ────────────────────────────────────────────────────

    private IndustryType industryTypeOf(ProposalChapter chapter) {
        BidDocument doc = bidDocumentService.findById(chapter.getDocumentId());
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(doc.getBidRequest().getId());
        return bidRequest.getOpportunity().getIndustryType();
    }

    private String buildOpportunityText(Opportunity opp) {
        StringBuilder sb = new StringBuilder();
        sb.append("공고 제목: ").append(opp.getTitle()).append("\n");
        if (opp.getOrganizationName() != null) sb.append("기관: ").append(opp.getOrganizationName()).append("\n");
        if (opp.getSolicitationNumber() != null) sb.append("공고번호: ").append(opp.getSolicitationNumber()).append("\n");
        // CR-038: 실본문(descriptionBody) 우선, 없으면 rawJson.description 폴백.
        String body = opp.getDescriptionBody();
        if (body != null && !body.isBlank()) {
            sb.append("\n상세 설명(원문 본문):\n").append(body);
        } else if (opp.getRawJson() != null && opp.getRawJson().get("description") != null) {
            sb.append("\n상세 설명:\n").append(opp.getRawJson().get("description"));
        }
        return sb.toString();
    }

    // ─── 파싱/캐스팅 유틸 ──────────────────────────────────────────────────

    private UUID uuid(Object raw) {
        if (raw == null) throw new IllegalArgumentException("UUID 가 필요합니다");
        return UUID.fromString((String) raw);
    }

    private String str(Object raw) {
        return raw != null ? raw.toString() : null;
    }

    private Integer intOrNull(Object raw) {
        return raw instanceof Number n ? n.intValue() : null;
    }

    private BlockType parseBlockType(Object raw) {
        if (raw == null) return BlockType.PARAGRAPH;
        try {
            return BlockType.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BlockType.PARAGRAPH;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object obj) {
        return obj instanceof List ? (List<Map<String, Object>>) obj : List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object obj) {
        if (!(obj instanceof List<?> list)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object o : list) if (o != null) out.add(o.toString());
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : null;
    }

    private Map<String, Object> nullSafe(Map<String, Object> m) {
        return m != null ? m : Map.of();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
