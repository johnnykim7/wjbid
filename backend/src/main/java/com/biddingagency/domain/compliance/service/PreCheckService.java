package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.compliance.dto.AutoFillResult;
import com.biddingagency.domain.compliance.entity.FileFormat;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.PreCheckVerdict;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.service.DocumentExpiryService;
import com.biddingagency.domain.member.service.MemberDocumentService;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.dto.PreCheckResponses;
import com.biddingagency.dto.PreCheckResponses.ClientItem;
import com.biddingagency.dto.PreCheckResponses.Detail;
import com.biddingagency.dto.PreCheckResponses.PlatformItem;
import com.biddingagency.dto.PreCheckResponses.Presence;
import com.biddingagency.dto.PreCheckResponses.Row;
import com.biddingagency.dto.PreCheckResponses.Summary;
import com.biddingagency.integration.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 제출 전 검사 (CR-141, BID-MKT-021~024).
 *
 * 입찰 건마다 고객 올림 요구 항목과 이어진 서류를 맞춰 빠짐, 형식, 쪽수를 판정한다.
 * 모든 동작은 그 입찰 건을 낸 회원만 한다(BIZ-049). 남의 입찰 건은 있는지조차 알리지 않으려고 404 로 답한다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PreCheckService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of(DocumentExpiryService.SERVICE_ZONE);
    private static final String CLIENT_DOCS_PREFIX = "client-docs/";
    private static final int DOCUMENT_CATEGORY_MAX = 100;
    /** 서류 접수로 전환하기 전까지만 서류를 잇고 바꾼다. 전환한 뒤에는 판정만 보인다 */
    private static final Set<BidRequestState> EDITABLE_STATES = EnumSet.of(BidRequestState.CREATED, BidRequestState.DOCS_PENDING);

    private final BidRequestRepository bidRequestRepository;
    private final OpportunityRequirementItemRepository requirementRepository;
    private final RequirementFulfillmentMapRepository fulfillmentMapRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final NoticeRepository noticeRepository;
    private final MemberDocumentService memberDocumentService;
    private final StorageService storageService;
    private final PreCheckJudge judge;
    private final PdfPageCounter pdfPageCounter;
    private final DataSize maxFileSize;

    public PreCheckService(BidRequestRepository bidRequestRepository,
                           OpportunityRequirementItemRepository requirementRepository,
                           RequirementFulfillmentMapRepository fulfillmentMapRepository,
                           ClientDocumentRepository clientDocumentRepository,
                           NoticeRepository noticeRepository,
                           MemberDocumentService memberDocumentService,
                           StorageService storageService,
                           PreCheckJudge judge,
                           PdfPageCounter pdfPageCounter,
                           @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
        this.bidRequestRepository = bidRequestRepository;
        this.requirementRepository = requirementRepository;
        this.fulfillmentMapRepository = fulfillmentMapRepository;
        this.clientDocumentRepository = clientDocumentRepository;
        this.noticeRepository = noticeRepository;
        this.memberDocumentService = memberDocumentService;
        this.storageService = storageService;
        this.judge = judge;
        this.pdfPageCounter = pdfPageCounter;
        this.maxFileSize = maxFileSize;
    }

    // ── 조회 ──────────────────────────────────────────────

    public List<Row> listMine(UUID memberId) {
        List<BidRequest> bidRequests = bidRequestRepository.findAllByMemberIdWithOpportunity(memberId);
        if (bidRequests.isEmpty()) {
            return List.of();
        }
        Set<UUID> opportunityIds = bidRequests.stream().map(br -> br.getOpportunity().getId()).collect(Collectors.toSet());
        Map<UUID, List<OpportunityRequirementItem>> itemsByOpportunity = requirementRepository.findByOpportunityIds(opportunityIds)
                .stream().collect(Collectors.groupingBy(ri -> ri.getOpportunity().getId()));
        Map<UUID, Map<UUID, RequirementFulfillmentMap>> mapsByBid = fulfillmentMapRepository
                .findWithClientDocumentByBidRequestIds(bidRequests.stream().map(BidRequest::getId).toList())
                .stream().collect(Collectors.groupingBy(m -> m.getBidRequest().getId(),
                        Collectors.toMap(m -> m.getRequirementItem().getId(), Function.identity(), (a, b) -> a)));
        Map<UUID, Notice> noticeByOpportunity = latestNotices(opportunityIds);
        Set<String> units = judge.pageUnits();

        return bidRequests.stream()
                .map(br -> {
                    Opportunity o = br.getOpportunity();
                    List<OpportunityRequirementItem> items = itemsByOpportunity.getOrDefault(o.getId(), List.of());
                    boolean ready = !items.isEmpty();
                    Summary summary = ready
                            ? summarize(clientItems(items, mapsByBid.getOrDefault(br.getId(), Map.of()), units))
                            : null;
                    return new Row(br.getId(), br.getState(), o.getSolicitationNumber(), o.getTitle(),
                            koreanTitle(noticeByOpportunity.get(o.getId()), o), toOffset(o.getResponseDeadline()),
                            ready, summary);
                })
                .sorted(Comparator.comparing(Row::responseDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public Detail detail(UUID memberId, UUID bidRequestId) {
        BidRequest br = ownedBidRequest(memberId, bidRequestId);
        return buildDetail(br);
    }

    // ── 올리기, 서류함에서 고르기, 빼기 ─────────────────────

    @Transactional
    public ClientItem upload(Member member, UUID bidRequestId, UUID requirementItemId, MultipartFile file) {
        BidRequest br = ownedBidRequest(member.getId(), bidRequestId);
        requireEditable(br);
        OpportunityRequirementItem item = clientUploadItem(br, requirementItemId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다");
        }
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기가 " + maxFileSize.toMegabytes() + "MB를 넘습니다");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일을 읽지 못했습니다");
        }
        String fileName = file.getOriginalFilename();
        String storageUrl = storageService.store(CLIENT_DOCS_PREFIX + bidRequestId, fileName, content);
        ClientDocument doc = clientDocumentRepository.save(ClientDocument.builder()
                .bidRequest(br)
                .member(member)
                .fileName(fileName)
                .fileSize(file.getSize())
                .contentType(file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .storageUrl(storageUrl)
                .documentCategory(documentCategory(item))
                .pageCount(pdfPageCounter.count(fileName, content))
                .build());
        RequirementFulfillmentMap map = link(br, item, doc, member.getId());
        log.info("[CR-141] 요구 서류 올림: bidRequest={}, item={}, doc={}, pages={}",
                bidRequestId, requirementItemId, doc.getId(), doc.getPageCount());
        return toClientItem(item, map, judge.pageUnits());
    }

    @Transactional
    public ClientItem fromVault(Member member, UUID bidRequestId, UUID requirementItemId, UUID memberDocumentId) {
        BidRequest br = ownedBidRequest(member.getId(), bidRequestId);
        requireEditable(br);
        OpportunityRequirementItem item = clientUploadItem(br, requirementItemId);
        if (memberDocumentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서류함 서류를 고르세요");
        }
        MemberDocument vaultDoc = memberDocumentService.getOwned(member.getId(), memberDocumentId);
        ClientDocument doc = copyFromVault(br, member, item, vaultDoc);
        RequirementFulfillmentMap map = link(br, item, doc, member.getId());
        log.info("[CR-141] 서류함에서 이음: bidRequest={}, item={}, vaultDoc={}, doc={}",
                bidRequestId, requirementItemId, memberDocumentId, doc.getId());
        return toClientItem(item, map, judge.pageUnits());
    }

    /** 연결만 지운다. 올린 파일은 입찰 건 서류로 남는다 */
    @Transactional
    public void unlink(UUID memberId, UUID bidRequestId, UUID requirementItemId) {
        BidRequest br = ownedBidRequest(memberId, bidRequestId);
        requireEditable(br);
        OpportunityRequirementItem item = clientUploadItem(br, requirementItemId);
        fulfillmentMapRepository.findByBidRequestIdAndRequirementItemId(br.getId(), item.getId())
                .ifPresent(map -> {
                    fulfillmentMapRepository.delete(map);
                    log.info("[CR-141] 요구 서류 뺌: bidRequest={}, item={}", bidRequestId, requirementItemId);
                });
    }

    /**
     * CR-125 자동 채우기를 요구 항목에 맞춘 것. 아직 이어진 서류가 없는 고객 올림 항목마다
     * 항목 이름으로 서류함 고정 분류를 찾아(DocumentCategory.matchByText) 그 분류 서류가 있으면 복사해 잇는다.
     */
    @Transactional
    public AutoFillResult autoFill(Member member, UUID bidRequestId) {
        BidRequest br = ownedBidRequest(member.getId(), bidRequestId);
        requireEditable(br);
        Map<UUID, RequirementFulfillmentMap> maps = mapsOf(br.getId());
        List<String> filled = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        for (OpportunityRequirementItem item : requirementRepository.findByOpportunityIdOrderBySortOrderAsc(br.getOpportunity().getId())) {
            if (party(item) != FulfillmentParty.CLIENT_UPLOAD || linkedDocument(maps.get(item.getId())) != null) {
                continue;
            }
            DocumentCategory category = DocumentCategory.matchByText(item.getTitle());
            if (category == null) {
                continue;
            }
            MemberDocument vaultDoc = memberDocumentService.findFixed(member.getId(), category);
            if (vaultDoc == null) {
                unmatched.add(item.getTitle());
                continue;
            }
            ClientDocument doc = copyFromVault(br, member, item, vaultDoc);
            link(br, item, doc, member.getId());
            filled.add(item.getTitle());
        }
        log.info("[CR-141] 자동 채우기: bidRequest={}, filled={}, unmatched={}", bidRequestId, filled.size(), unmatched.size());
        return AutoFillResult.builder()
                .filledCount(filled.size())
                .filledNames(filled)
                .unmatchedNames(unmatched)
                .build();
    }

    // ── 내부 ──────────────────────────────────────────────

    private Detail buildDetail(BidRequest br) {
        Opportunity o = br.getOpportunity();
        List<OpportunityRequirementItem> items = requirementRepository.findByOpportunityIdOrderBySortOrderAsc(o.getId());
        List<ClientItem> clientItems = clientItems(items, mapsOf(br.getId()), judge.pageUnits());
        List<PlatformItem> platformItems = items.stream()
                .filter(ri -> party(ri) != FulfillmentParty.CLIENT_UPLOAD)
                .map(ri -> new PlatformItem(ri.getId(), ri.getTitle(), party(ri).name(), text(ri, RequirementSyncService.KEY_SOURCE_REF)))
                .toList();
        boolean ready = !items.isEmpty();
        Summary summary = ready ? summarize(clientItems) : null;
        boolean canTransition = br.getState() == BidRequestState.DOCS_PENDING && (summary == null || summary.missing() == 0);
        boolean editable = EDITABLE_STATES.contains(br.getState());
        Notice notice = latestNotices(Set.of(o.getId())).get(o.getId());
        UUID noticeId = notice != null && notice.isVisible() ? notice.getId() : null;
        return new Detail(br.getId(), br.getState(), o.getSolicitationNumber(), o.getTitle(), koreanTitle(notice, o),
                toOffset(o.getResponseDeadline()), noticeId, ready, clientItems, platformItems, summary, canTransition, editable,
                maxFileSize.toMegabytes());
    }

    /** 필수 먼저, 그 안에서는 공고 분석 순서 */
    private List<ClientItem> clientItems(List<OpportunityRequirementItem> items,
                                         Map<UUID, RequirementFulfillmentMap> maps, Set<String> units) {
        return items.stream()
                .filter(ri -> party(ri) == FulfillmentParty.CLIENT_UPLOAD)
                .sorted(Comparator.comparing((OpportunityRequirementItem ri) -> !mandatory(ri))
                        .thenComparing(OpportunityRequirementItem::getSortOrder))
                .map(ri -> toClientItem(ri, maps.get(ri.getId()), units))
                .toList();
    }

    private ClientItem toClientItem(OpportunityRequirementItem item, RequirementFulfillmentMap map, Set<String> units) {
        ClientDocument doc = linkedDocument(map);
        String extension = doc == null ? null : FileFormat.extensionOf(doc.getFileName());
        String formatText = text(item, RequirementSyncService.KEY_FORMAT);
        String pageLimitText = text(item, RequirementSyncService.KEY_PAGE_LIMIT);
        PreCheckJudge.FormatResult format = judge.judgeFormat(formatText, doc != null, extension);
        PreCheckJudge.PageResult pages = judge.judgePages(pageLimitText, doc != null, extension,
                doc == null ? null : doc.getPageCount(), units);
        PreCheckResponses.Document document = doc == null ? null : new PreCheckResponses.Document(
                doc.getId(), doc.getFileName(), extension, doc.getPageCount(), doc.getFileSize(), toOffset(doc.getCreatedAt()));
        return new ClientItem(item.getId(), item.getTitle(), item.getDescription(), mandatory(item),
                Boolean.TRUE.equals(item.getIsBlocker()), text(item, RequirementSyncService.KEY_SOURCE_REF),
                text(item, RequirementSyncService.KEY_NOTES), formatText, pageLimitText, document,
                doc != null ? Presence.UPLOADED : Presence.MISSING,
                new PreCheckResponses.FormatJudgement(format.verdict(), format.basis(), format.allowed()),
                new PreCheckResponses.PageJudgement(pages.verdict(), pages.basis(), pages.limit()));
    }

    private static Summary summarize(List<ClientItem> items) {
        int required = 0;
        int uploaded = 0;
        int mismatch = 0;
        int needsHuman = 0;
        for (ClientItem item : items) {
            if (item.isBlocker()) {
                required++;
                if (item.presence() == Presence.UPLOADED) {
                    uploaded++;
                }
            }
            if (item.presence() != Presence.UPLOADED) {
                continue;
            }
            if (item.format().verdict().isProblem() || item.pages().verdict().isProblem()) {
                mismatch++;
            }
            if (item.format().verdict() == PreCheckVerdict.NEEDS_HUMAN || item.pages().verdict() == PreCheckVerdict.NEEDS_HUMAN) {
                needsHuman++;
            }
        }
        return new Summary(required, uploaded, required - uploaded, mismatch, needsHuman);
    }

    private BidRequest ownedBidRequest(UUID memberId, UUID bidRequestId) {
        return bidRequestRepository.findByIdWithDetails(bidRequestId)
                .filter(br -> br.getMember().getId().equals(memberId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "입찰 건을 찾을 수 없습니다"));
    }

    private static void requireEditable(BidRequest br) {
        if (!EDITABLE_STATES.contains(br.getState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "서류 접수로 전환한 뒤에는 서류를 바꿀 수 없습니다");
        }
    }

    private OpportunityRequirementItem clientUploadItem(BidRequest br, UUID requirementItemId) {
        OpportunityRequirementItem item = requirementRepository.findById(requirementItemId)
                .filter(ri -> ri.getOpportunity().getId().equals(br.getOpportunity().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요구 서류를 찾을 수 없습니다"));
        if (party(item) != FulfillmentParty.CLIENT_UPLOAD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "고객이 올리는 서류가 아닙니다");
        }
        return item;
    }

    private ClientDocument copyFromVault(BidRequest br, Member member, OpportunityRequirementItem item, MemberDocument vaultDoc) {
        byte[] content = storageService.load(vaultDoc.getStorageUrl());
        String storageUrl = storageService.store(CLIENT_DOCS_PREFIX + br.getId(), vaultDoc.getFileName(), content);
        return clientDocumentRepository.save(ClientDocument.builder()
                .bidRequest(br)
                .member(member)
                .fileName(vaultDoc.getFileName())
                .fileSize(vaultDoc.getFileSize())
                .contentType(vaultDoc.getContentType())
                .storageUrl(storageUrl)
                .documentCategory(documentCategory(item))
                .pageCount(pdfPageCounter.count(vaultDoc.getFileName(), content))
                .build());
    }

    private RequirementFulfillmentMap link(BidRequest br, OpportunityRequirementItem item, ClientDocument doc, UUID memberId) {
        RequirementFulfillmentMap map = fulfillmentMapRepository
                .findByBidRequestIdAndRequirementItemId(br.getId(), item.getId())
                .orElse(RequirementFulfillmentMap.builder()
                        .bidRequest(br)
                        .requirementItem(item)
                        .fulfillmentType(FulfillmentType.CLIENT_DOCUMENT)
                        .build());
        map.fulfillWithClientDocument(doc, memberId);
        return fulfillmentMapRepository.save(map);
    }

    private Map<UUID, RequirementFulfillmentMap> mapsOf(UUID bidRequestId) {
        return fulfillmentMapRepository.findWithClientDocumentByBidRequestIds(List.of(bidRequestId)).stream()
                .collect(Collectors.toMap(m -> m.getRequirementItem().getId(), Function.identity(), (a, b) -> a));
    }

    private static ClientDocument linkedDocument(RequirementFulfillmentMap map) {
        return map != null && map.getStatus() == FulfillmentStatus.FULFILLED ? map.getClientDocument() : null;
    }

    private Map<UUID, Notice> latestNotices(Set<UUID> opportunityIds) {
        return noticeRepository.findByOpportunityIdIn(opportunityIds).stream()
                .collect(Collectors.toMap(n -> n.getOpportunity().getId(), Function.identity(),
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b));
    }

    private static String koreanTitle(Notice notice, Opportunity o) {
        if (notice != null && notice.getKoreanTitle() != null && !notice.getKoreanTitle().isBlank()) {
            return notice.getKoreanTitle();
        }
        return o.getTitleKo();
    }

    private static FulfillmentParty party(OpportunityRequirementItem item) {
        return FulfillmentParty.fromAnalysis(text(item, RequirementSyncService.KEY_FULFILLMENT_PARTY));
    }

    private static boolean mandatory(OpportunityRequirementItem item) {
        Object v = item.getRequirementJson() == null ? null : item.getRequirementJson().get(RequirementSyncService.KEY_MANDATORY);
        return Boolean.TRUE.equals(v) || (v != null && Boolean.parseBoolean(v.toString()));
    }

    private static String text(OpportunityRequirementItem item, String key) {
        Object v = item.getRequirementJson() == null ? null : item.getRequirementJson().get(key);
        return v == null ? null : v.toString();
    }

    private static String documentCategory(OpportunityRequirementItem item) {
        String title = item.getTitle();
        return title.length() > DOCUMENT_CATEGORY_MAX ? title.substring(0, DOCUMENT_CATEGORY_MAX) : title;
    }

    private static OffsetDateTime toOffset(LocalDateTime time) {
        return time == null ? null : time.atZone(SERVICE_ZONE).toOffsetDateTime();
    }
}
