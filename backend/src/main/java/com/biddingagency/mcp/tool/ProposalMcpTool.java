package com.biddingagency.mcp.tool;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalService;
import com.biddingagency.domain.proposal.service.ProposalService.BlockInput;
import com.biddingagency.domain.proposal.service.ProposalService.ChapterInput;
import com.biddingagency.domain.proposal.service.ProposalService.SectionInput;
import com.biddingagency.domain.proposal.entity.BlockType;
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
 * design / write-section / assemble 워크플로우가 자율주행하며 호출하는 도구 모음.
 * assemble 의 최종 저장은 기존 save_document_version({@link DocumentMcpTool}) 을 그대로 재사용한다.
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
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_proposal_design_input",
            "description", "제안서 구조 설계(design)에 필요한 입력을 조회합니다. " +
                    "documentId 로 연결된 공고의 한글화 요약·요구서류·공고 본문(content)과, 정제 시 도출된 FACTOR/Subfactor 트리(factorTree)를 반환합니다. " +
                    "factorTree 가 있으면 그대로 chapter/section 으로 복사·검증하고, 없으면(null) 공고 본문(noticeContent/opportunityText)의 Section L/M 에서 FACTOR/Subfactor 를 직접 발췌하세요.",
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
                    "공고가 박은 라벨·순서를 그대로 보존하세요(창작 금지). LOCKED section 이 하나라도 있으면 거부됩니다.",
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
                                    "각 항목: {subfactorLabel, title, scope(작성 지침), requirementRefs(Section L/M 발췌 ID 배열), minWords(최소 단어 수)}")
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
                    "referenceSamples 의 downloadUrl 을 parse_document(url=...)로 읽어 few-shot 으로 참조하되, 사실(회사명·실적·수치)은 복붙하지 마세요.",
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", documentId.toString());
        result.put("bidRequestId", bidRequest.getId().toString());
        result.put("documentType", doc.getDocumentType().name());
        result.put("opportunityId", opp.getId().toString());
        result.put("opportunityTitle", opp.getTitle());
        result.put("organizationName", opp.getOrganizationName());
        result.put("opportunityText", buildOpportunityText(opp));

        // 노출 중인 공고문(Notice) 최신 1건 — 한글화 요약·요구서류·본문·factorTree
        Notice notice = noticeRepository
                .findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(opp.getId(), OpportunityVisibility.VISIBLE)
                .filter(Notice::isGenerationCompleted)
                .orElse(null);
        if (notice != null) {
            result.put("noticeSummary", nullSafe(notice.getSummaryJson()));
            result.put("requiredDocuments", nullSafe(notice.getRequiredDocumentsJson()));
            result.put("documentFormats", nullSafe(notice.getDocumentFormatsJson()));
            result.put("noticeContent", nullSafe(notice.getContentJson()));
            result.put("factorTree", notice.getFactorTreeJson());   // null 이면 WF 가 본문 발췌
            result.put("priceItems", notice.getPriceItemsJson());
        } else {
            result.put("factorTree", null);
            result.put("priceItems", null);
        }
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
                "sections", sectionList
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

        // 참조 샘플 — section 이 속한 문서의 공고유형 매칭
        IndustryType industryType = industryTypeOf(chapter);
        List<Map<String, Object>> samples = industryType != null
                ? referenceSampleService.collect(industryType) : List.of();
        result.put("industryType", industryType != null ? industryType.name() : null);
        result.put("referenceSamples", samples);
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
        if (opp.getRawJson() != null && opp.getRawJson().get("description") != null) {
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
