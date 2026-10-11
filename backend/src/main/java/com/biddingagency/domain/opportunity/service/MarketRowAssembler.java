package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.MarketGroup;
import com.biddingagency.domain.opportunity.NoticeTypeTranslator;
import com.biddingagency.domain.opportunity.dto.MarketRowDto;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 모은 공고를 메뉴 속 화면 목록 줄로 바꾼다 (CR-135, T3-2 C-2 「목록 줄」).
 *
 * <p>한 줄을 채우려면 같은 공고번호의 다른 판(본 공고 나옴, 원래 공고)과 노출 공고문 목록이 함께 필요하다.
 * 그 둘을 {@link Context} 로 한 번 읽어 두고 줄마다 찾는다.
 */
@Component
@RequiredArgsConstructor
public class MarketRowAssembler {

    /** 공고 시각은 KST 벽시계로 저장돼 있다(DB 접속 serverTimezone=Asia/Seoul) */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String NOTICE_ID_PLACEHOLDER = "{noticeId}";

    private final OpportunityRepository opportunityRepository;
    private final NoticeRepository noticeRepository;

    @Value("${app.sam-gov.public-opportunity-url}")
    private String publicOpportunityUrl;

    /** 공고들을 목록 줄로. 같은 공고번호의 다른 판은 DB 에서 함께 읽는다 */
    @Transactional(readOnly = true)
    public List<MarketRowDto> toRows(List<Opportunity> opportunities) {
        List<MarketSource> sources = opportunities.stream().map(MarketRowAssembler::sourceOf).toList();
        Set<String> sols = sources.stream().map(MarketSource::solKey).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<MarketSource> related = sols.isEmpty()
                ? List.of()
                : opportunityRepository.findMarketSourcesBySolicitationNumbers(sols);
        Context ctx = context(related);
        return sources.stream().map(s -> toRow(s, ctx)).toList();
    }

    /** 같은 공고번호 찾기와 노출 공고문 찾기에 쓸 묶음. sources 는 지운 것을 뺀 모은 공고여야 한다 */
    Context context(List<MarketSource> sources) {
        Map<String, List<MarketSource>> bySol = sources.stream()
                .filter(s -> s.solKey() != null)
                .collect(Collectors.groupingBy(MarketSource::solKey));
        Map<UUID, UUID> publicNotice = new HashMap<>();
        for (Object[] r : noticeRepository.findOpportunityAndNoticeIds(OpportunityVisibility.VISIBLE)) {
            publicNotice.put((UUID) r[0], (UUID) r[1]);
        }
        return new Context(bySol, publicNotice);
    }

    MarketRowDto toRow(MarketSource s, Context ctx) {
        MarketGroup group = s.group();
        List<MarketSource> same = s.solKey() == null ? List.of() : ctx.bySol().getOrDefault(s.solKey(), List.of());

        Boolean hasMainNotice = group == MarketGroup.PRE
                ? same.stream().anyMatch(x -> x.group() == MarketGroup.BID)
                : null;
        MarketSource original = group == MarketGroup.AWARD
                ? same.stream()
                    .filter(x -> x.group() != MarketGroup.AWARD)
                    .max(Comparator.comparing(MarketSource::postedDate,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null)
                : null;

        return new MarketRowDto(
                s.id(),
                s.noticeId(),
                s.solicitationNumber(),
                s.title(),
                s.titleKo(),
                s.type(),
                s.typeKo() != null ? s.typeKo() : NoticeTypeTranslator.toKorean(s.type()),
                group,
                s.postedDate() == null ? null : s.postedDate().toLocalDate(),
                s.responseDeadline() == null ? null : s.responseDeadline().atZone(KST).toOffsetDateTime(),
                group == MarketGroup.AWARD ? s.awardAmount() : null,
                null,
                ctx.publicNoticeByOpportunity().get(s.id()),
                samUrl(s.noticeId()),
                hasMainNotice,
                original == null ? null : original.id(),
                original == null ? null : ctx.publicNoticeByOpportunity().get(original.id()),
                original == null ? null : samUrl(original.noticeId())
        );
    }

    String samUrl(String noticeId) {
        if (noticeId == null || noticeId.isBlank()) return null;
        return publicOpportunityUrl.replace(NOTICE_ID_PLACEHOLDER,
                URLEncoder.encode(noticeId.trim(), StandardCharsets.UTF_8));
    }

    static MarketSource sourceOf(Opportunity o) {
        return new MarketSource(o.getId(), o.getNoticeId(), o.getSolicitationNumber(), o.getTitle(),
                o.getTitleKo(), o.getType(), o.getTypeKo(), o.getPostedDate(), o.getResponseDeadline(),
                o.getAwardAmount(), o.getRawJson());
    }

    static boolean isOpen(MarketSource s, LocalDateTime nowKst) {
        return s.responseDeadline() != null && s.responseDeadline().isAfter(nowKst);
    }

    record Context(Map<String, List<MarketSource>> bySol, Map<UUID, UUID> publicNoticeByOpportunity) {
    }
}
