package com.biddingagency.domain.eligibility.service;

import com.biddingagency.domain.eligibility.entity.EligibilityAnswer;
import com.biddingagency.domain.eligibility.entity.EligibilityAnswerValue;
import com.biddingagency.domain.eligibility.exception.EligibilityAnswerInvalidException;
import com.biddingagency.domain.eligibility.exception.EligibilityDocumentNotAllowedException;
import com.biddingagency.domain.eligibility.exception.EligibilityDocumentNotFoundException;
import com.biddingagency.domain.eligibility.exception.EligibilityItemInvalidException;
import com.biddingagency.domain.eligibility.exception.EligibilityNoticeNotFoundException;
import com.biddingagency.domain.eligibility.repository.EligibilityAnswerRepository;
import com.biddingagency.domain.eligibility.service.EligibilityVerdictJudge.Judgment;
import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.member.service.DocumentExpiryService;
import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.dto.AnalysisResultDto.EligibilityDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.dto.EligibilityResponses.AttachedDocument;
import com.biddingagency.dto.EligibilityResponses.Detail;
import com.biddingagency.dto.EligibilityResponses.Item;
import com.biddingagency.dto.EligibilityResponses.NoticeHead;
import com.biddingagency.dto.EligibilityResponses.NoticeRow;
import com.biddingagency.dto.EligibilityResponses.Saved;
import com.biddingagency.dto.EligibilityResponses.Summary;
import com.biddingagency.dto.EligibilityResponses.VaultDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 적격성 진단 (CR-137, BID-QUAL-001).
 *
 * 자격 항목은 공고문 분석 결과(required_documents_json 의 eligibility)를 그때그때 읽는다.
 * 저장하는 것은 회원의 답과 붙인 서류뿐이다. 판정(CR-140)은 조회할 때 그 답과 서류로만 낸다(BIZ-040).
 * 진단과 판정은 회원만 쓴다(CR-157, BIZ-071).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EligibilityService {

    private final NoticeRepository noticeRepository;
    private final EligibilityAnswerRepository answerRepository;
    private final MemberDocumentRepository memberDocumentRepository;
    private final Clock clock;
    private final MembershipService membershipService;

    // 서류 만료는 서류함 만료 알림과 같은 날짜로 본다
    private static final ZoneId SERVICE_ZONE = ZoneId.of(DocumentExpiryService.SERVICE_ZONE);

    private record Target(Notice notice, List<EligibilityDto> items) {
    }

    private record Vault(Map<DocumentCategory, List<MemberDocument>> byCategory, Map<UUID, MemberDocument> byId) {
    }

    public List<NoticeRow> listNotices(UUID memberId) {
        membershipService.requireMember(memberId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Target> targets = noticeRepository
                .findVisible(OpportunityVisibility.VISIBLE, true, now, Pageable.unpaged())
                .getContent().stream()
                .filter(Notice::isGenerationCompleted)
                .map(n -> new Target(n, items(n)))
                .filter(t -> !t.items().isEmpty())
                .toList();
        if (targets.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<EligibilityAnswer>> answers = answerRepository
                .findByMemberIdAndNoticeIdIn(memberId, targets.stream().map(t -> t.notice().getId()).toList())
                .stream().collect(Collectors.groupingBy(EligibilityAnswer::getNoticeId));
        Vault vault = vault(memberId);
        LocalDate today = today();
        return targets.stream()
                .map(t -> {
                    Opportunity o = t.notice().getOpportunity();
                    List<Item> items = items(t, byItemNo(answers.getOrDefault(t.notice().getId(), List.of())),
                            vault, today);
                    int answered = (int) items.stream().filter(i -> i.answer() != null).count();
                    int gates = (int) items.stream().filter(Item::gate).count();
                    Judgment j = EligibilityVerdictJudge.judge(items);
                    return new NoticeRow(t.notice().getId(), o.getSolicitationNumber(), t.notice().getKoreanTitle(),
                            o.getResponseDeadline(), open(o, now), items.size(), answered, gates,
                            j.verdict(), j.blockers());
                })
                .sorted(Comparator.comparing((NoticeRow r) -> !r.open()).thenComparing(EligibilityService::byDeadline))
                .toList();
    }

    public Detail detail(UUID memberId, UUID noticeId) {
        membershipService.requireMember(memberId);
        Target t = load(noticeId);
        List<Item> items = items(memberId, t);
        Judgment j = EligibilityVerdictJudge.judge(items);
        return new Detail(head(t.notice()), items, summary(items), j.verdict(), j.blockers());
    }

    @Transactional
    public Saved answer(UUID memberId, UUID noticeId, int itemNo, String rawAnswer) {
        membershipService.requireMember(memberId);
        EligibilityAnswerValue value = parse(rawAnswer);
        Target t = load(noticeId);
        if (itemNo < 0 || itemNo >= t.items().size()) {
            throw new EligibilityItemInvalidException();
        }
        String title = titleOf(t.items().get(itemNo));
        EligibilityAnswer answer = answerRepository.findByMemberIdAndNoticeIdAndItemNo(memberId, noticeId, itemNo)
                .map(a -> {
                    a.change(title, value);
                    return a;
                })
                .orElseGet(() -> EligibilityAnswer.builder()
                        .memberId(memberId).noticeId(noticeId).itemNo(itemNo)
                        .itemTitle(title).answer(value).build());
        // 응답에 저장 시각을 실으려면 수정 시각이 커밋 전에 채워져야 한다
        answerRepository.saveAndFlush(answer);
        return saved(memberId, t, itemNo);
    }

    /** documentId 가 null 이면 뗀다 (CR-140, BIZ-042) */
    @Transactional
    public Saved attach(UUID memberId, UUID noticeId, int itemNo, UUID documentId) {
        membershipService.requireMember(memberId);
        Target t = load(noticeId);
        if (itemNo < 0 || itemNo >= t.items().size()) {
            throw new EligibilityItemInvalidException();
        }
        String title = titleOf(t.items().get(itemNo));
        EligibilityAnswer answer = answerRepository.findByMemberIdAndNoticeIdAndItemNo(memberId, noticeId, itemNo)
                .filter(a -> a.matches(title) && a.getAnswer() == EligibilityAnswerValue.HAVE)
                .orElseThrow(EligibilityDocumentNotAllowedException::new);
        if (documentId != null && memberDocumentRepository.findById(documentId)
                .filter(d -> memberId.equals(d.getMemberId())).isEmpty()) {
            throw new EligibilityDocumentNotFoundException();
        }
        answer.attach(documentId);
        answerRepository.saveAndFlush(answer);
        return saved(memberId, t, itemNo);
    }

    private Saved saved(UUID memberId, Target t, int itemNo) {
        List<Item> items = items(memberId, t);
        Judgment j = EligibilityVerdictJudge.judge(items);
        return new Saved(items.get(itemNo), summary(items), j.verdict(), j.blockers());
    }

    private Target load(UUID noticeId) {
        Notice notice = noticeRepository.findByIdAndVisibility(noticeId, OpportunityVisibility.VISIBLE)
                .filter(Notice::isGenerationCompleted)
                .orElseThrow(EligibilityNoticeNotFoundException::new);
        List<EligibilityDto> items = items(notice);
        if (items.isEmpty()) {
            throw new EligibilityNoticeNotFoundException();
        }
        return new Target(notice, items);
    }

    private List<Item> items(UUID memberId, Target t) {
        Map<Integer, EligibilityAnswer> mine = byItemNo(
                answerRepository.findByMemberIdAndNoticeId(memberId, t.notice().getId()));
        return items(t, mine, vault(memberId), today());
    }

    private static List<Item> items(Target t, Map<Integer, EligibilityAnswer> mine, Vault vault, LocalDate today) {
        return IntStream.range(0, t.items().size())
                .mapToObj(i -> item(i, t.items().get(i), mine.get(i), vault, today))
                .toList();
    }

    private static Item item(int no, EligibilityDto e, EligibilityAnswer answer, Vault vault, LocalDate today) {
        boolean matches = answer != null && answer.matches(titleOf(e));
        // 서류함 고정 분류 이름과 맞을 때만 제안한다. 내용이 맞는지는 회원이 본다(BIZ-032).
        // 증빙 서류 글이 서류 종류를 말하지 않는 공고가 있어 자격 이름으로 한 번 더 맞춘다(CR-140)
        DocumentCategory category = DocumentCategory.matchByText(e.evidenceBy());
        if (category == null) {
            category = DocumentCategory.matchByText(e.title());
        }
        List<VaultDocument> docs = category == null ? List.of()
                : vault.byCategory().getOrDefault(category, List.of()).stream()
                .map(d -> new VaultDocument(d.getId(), documentName(d), d.getCreatedAt(), d.getExpiresOn(), expired(d, today)))
                .toList();
        boolean have = matches && answer.getAnswer() == EligibilityAnswerValue.HAVE;
        AttachedDocument attached = have && answer.getDocumentId() != null
                ? attached(vault.byId().get(answer.getDocumentId()), today) : null;
        return new Item(no, titleOf(e), e.description(), Boolean.TRUE.equals(e.mandatory()),
                Boolean.TRUE.equals(e.isGate()), e.sourceRef(), e.evidenceBy(),
                matches ? answer.getAnswer() : null, answer != null && !matches,
                matches ? answer.getUpdatedAt() : null, category, category == null ? null : category.getDisplayName(),
                docs, attached);
    }

    // 만료일 당일까지는 쓸 수 있는 서류로 본다(서류함 만료 알림과 같다)
    private static AttachedDocument attached(MemberDocument d, LocalDate today) {
        if (d == null) {
            return null;
        }
        return new AttachedDocument(d.getId(), documentName(d), d.getExpiresOn(), expired(d, today));
    }

    private static boolean expired(MemberDocument d, LocalDate today) {
        return d.getExpiresOn() != null && d.getExpiresOn().isBefore(today);
    }

    private Vault vault(UUID memberId) {
        List<MemberDocument> docs = memberDocumentRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return new Vault(docs.stream().collect(Collectors.groupingBy(MemberDocument::getCategory)),
                docs.stream().collect(Collectors.toMap(MemberDocument::getId, Function.identity())));
    }

    private static Map<Integer, EligibilityAnswer> byItemNo(List<EligibilityAnswer> answers) {
        return answers.stream().collect(Collectors.toMap(EligibilityAnswer::getItemNo, Function.identity()));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(SERVICE_ZONE));
    }

    private static Summary summary(List<Item> items) {
        List<Item> gates = items.stream().filter(Item::gate).toList();
        int have = count(gates, EligibilityAnswerValue.HAVE);
        int lack = count(gates, EligibilityAnswerValue.LACK);
        int unsure = count(gates, EligibilityAnswerValue.UNSURE);
        return new Summary(gates.size(), have, lack, unsure, gates.size() - have - lack - unsure, items.size());
    }

    private static int count(List<Item> items, EligibilityAnswerValue value) {
        return (int) items.stream().filter(i -> i.answer() == value).count();
    }

    private NoticeHead head(Notice n) {
        Opportunity o = n.getOpportunity();
        return new NoticeHead(n.getId(), o.getSolicitationNumber(), n.getKoreanTitle(), o.getOrganizationName(),
                o.getResponseDeadline(), open(o, LocalDateTime.now(clock)));
    }

    @SuppressWarnings("unchecked")
    private static List<EligibilityDto> items(Notice n) {
        Map<String, Object> rd;
        try {
            rd = n.getRequiredDocumentsJson();
        } catch (RuntimeException e) {
            // 분석 결과 하나가 깨져 있어도 다른 공고의 진단 목록은 보여야 한다
            log.warn("[적격성 진단] 분석 결과를 읽지 못함: noticeId={}", n.getId(), e);
            return List.of();
        }
        Object raw = rd == null ? null : rd.get("eligibility");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Map.class::isInstance)
                .map(m -> EligibilityDto.from((Map<String, Object>) m))
                .toList();
    }

    private static String titleOf(EligibilityDto e) {
        return e.title() == null ? "" : e.title();
    }

    private static String documentName(MemberDocument d) {
        return d.getDisplayName() == null || d.getDisplayName().isBlank() ? d.getFileName() : d.getDisplayName();
    }

    private static EligibilityAnswerValue parse(String raw) {
        if (raw == null) {
            throw new EligibilityAnswerInvalidException();
        }
        try {
            return EligibilityAnswerValue.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new EligibilityAnswerInvalidException();
        }
    }

    // 마감일이 없는 공고는 고객 공고 목록(findVisible)과 같이 마감 전으로 본다
    private static boolean open(Opportunity o, LocalDateTime now) {
        return o.getResponseDeadline() == null || !o.getResponseDeadline().isBefore(now);
    }

    // 마감 전은 마감이 가까운 순, 지난 공고는 최근에 마감한 순
    private static int byDeadline(NoticeRow a, NoticeRow b) {
        Comparator<LocalDateTime> order = a.open() ? Comparator.naturalOrder() : Comparator.reverseOrder();
        return Comparator.nullsLast(order).compare(a.responseDeadline(), b.responseDeadline());
    }
}
