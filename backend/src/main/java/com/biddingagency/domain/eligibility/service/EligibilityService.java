package com.biddingagency.domain.eligibility.service;

import com.biddingagency.domain.eligibility.entity.EligibilityAnswer;
import com.biddingagency.domain.eligibility.entity.EligibilityAnswerValue;
import com.biddingagency.domain.eligibility.exception.EligibilityAnswerInvalidException;
import com.biddingagency.domain.eligibility.exception.EligibilityItemInvalidException;
import com.biddingagency.domain.eligibility.exception.EligibilityNoticeNotFoundException;
import com.biddingagency.domain.eligibility.repository.EligibilityAnswerRepository;
import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.dto.AnalysisResultDto.EligibilityDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
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
import java.time.LocalDateTime;
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
 * 저장하는 것은 회원의 답뿐이고, 참여 가능 여부를 판정하지 않는다(BIZ-032).
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

    private record Target(Notice notice, List<EligibilityDto> items) {
    }

    public List<NoticeRow> listNotices(UUID memberId) {
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
        return targets.stream()
                .map(t -> {
                    Opportunity o = t.notice().getOpportunity();
                    int answered = (int) answers.getOrDefault(t.notice().getId(), List.of()).stream()
                            .filter(a -> current(a, t.items())).count();
                    return new NoticeRow(t.notice().getId(), o.getSolicitationNumber(), t.notice().getKoreanTitle(),
                            o.getResponseDeadline(), open(o, now), t.items().size(), answered);
                })
                .sorted(Comparator.comparing((NoticeRow r) -> !r.open()).thenComparing(EligibilityService::byDeadline))
                .toList();
    }

    public Detail detail(UUID memberId, UUID noticeId) {
        Target t = load(noticeId);
        List<Item> items = items(memberId, t);
        return new Detail(head(t.notice()), items, summary(items));
    }

    @Transactional
    public Saved answer(UUID memberId, UUID noticeId, int itemNo, String rawAnswer) {
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
        List<Item> items = items(memberId, t);
        return new Saved(items.get(itemNo), summary(items));
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
        Map<Integer, EligibilityAnswer> mine = answerRepository.findByMemberIdAndNoticeId(memberId, t.notice().getId())
                .stream().collect(Collectors.toMap(EligibilityAnswer::getItemNo, Function.identity()));
        Map<DocumentCategory, List<MemberDocument>> vault = memberDocumentRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .collect(Collectors.groupingBy(MemberDocument::getCategory));
        return IntStream.range(0, t.items().size())
                .mapToObj(i -> item(i, t.items().get(i), mine.get(i), vault))
                .toList();
    }

    private static Item item(int no, EligibilityDto e, EligibilityAnswer answer,
                             Map<DocumentCategory, List<MemberDocument>> vault) {
        boolean matches = answer != null && answer.matches(titleOf(e));
        // 서류함 고정 분류 이름과 증빙 서류 글이 맞을 때만 붙인다. 내용이 맞는지는 회원이 본다(BIZ-032).
        DocumentCategory category = DocumentCategory.matchByText(e.evidenceBy());
        List<VaultDocument> docs = category == null ? List.of()
                : vault.getOrDefault(category, List.of()).stream()
                .map(d -> new VaultDocument(d.getId(), documentName(d), d.getCreatedAt()))
                .toList();
        return new Item(no, titleOf(e), e.description(), Boolean.TRUE.equals(e.mandatory()),
                Boolean.TRUE.equals(e.isGate()), e.sourceRef(), e.evidenceBy(),
                matches ? answer.getAnswer() : null, answer != null && !matches,
                matches ? answer.getUpdatedAt() : null, category, category == null ? null : category.getDisplayName(), docs);
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

    private static boolean current(EligibilityAnswer a, List<EligibilityDto> items) {
        return a.getItemNo() >= 0 && a.getItemNo() < items.size() && a.matches(titleOf(items.get(a.getItemNo())));
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
