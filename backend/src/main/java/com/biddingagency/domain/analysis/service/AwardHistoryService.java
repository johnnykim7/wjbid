package com.biddingagency.domain.analysis.service;

import com.biddingagency.domain.federal.entity.FederalContract;
import com.biddingagency.domain.federal.repository.FederalContractRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.AwardInfo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 낙찰 이력 찾기 (CR-170, search_award_history).
 *
 * 미 연방 계약(federal_contracts)과 모은 낙찰 공고를 같이 찾아 줄마다 출처를 단다. 읽기만 하고 원본 JSON 은 내지 않는다.
 * 날짜는 계약은 시작일, 낙찰 공고는 낙찰일(없으면 게시일)로 거르고 늦은 순으로 합친다.
 */
@Service
@RequiredArgsConstructor
public class AwardHistoryService {

    public static final String FEDERAL_CONTRACT = "FEDERAL_CONTRACT";
    public static final String SAM_AWARD_NOTICE = "SAM_AWARD_NOTICE";
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final FederalContractRepository contractRepository;
    private final OpportunityRepository opportunityRepository;

    public record Criteria(List<String> naicsCodes, List<String> pscCodes, String recipientName, String keyword,
                           String solicitationNumber, LocalDate fromDate, LocalDate toDate, Integer limit) {

        boolean isEmpty() {
            return naicsCodes.isEmpty() && pscCodes.isEmpty() && recipientName == null && keyword == null
                    && solicitationNumber == null && fromDate == null && toDate == null;
        }

        int effectiveLimit() {
            if (limit == null || limit < 1) return DEFAULT_LIMIT;
            return Math.min(limit, MAX_LIMIT);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> search(Criteria c) {
        if (c.isEmpty()) {
            throw new IllegalArgumentException("조건이 하나도 없습니다. naicsCodes, pscCodes, recipientName, keyword, "
                    + "solicitationNumber, fromDate, toDate 가운데 하나는 있어야 합니다");
        }
        int limit = c.effectiveLimit();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FederalContract fc : contractRepository.findAll(contractSpec(c),
                PageRequest.of(0, limit, Sort.by(Sort.Order.desc("startDate"))))) {
            rows.add(contractRow(fc));
        }
        opportunityRepository.findByTypeNotDeleted(AwardInfo.AWARD_NOTICE_TYPE).stream()
                .filter(o -> noticeMatches(o, c))
                .map(this::noticeRow)
                .forEach(rows::add);
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (LocalDate) r.get("startDate"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return rows.size() > limit ? new ArrayList<>(rows.subList(0, limit)) : rows;
    }

    private static Specification<FederalContract> contractSpec(Criteria c) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (!c.naicsCodes().isEmpty()) where.add(root.get("naicsCode").in(c.naicsCodes()));
            if (!c.pscCodes().isEmpty()) where.add(root.get("pscCode").in(c.pscCodes()));
            if (c.recipientName() != null) {
                where.add(cb.like(cb.lower(root.get("recipientName")), "%" + lower(c.recipientName()) + "%"));
            }
            if (c.keyword() != null) {
                where.add(cb.like(cb.lower(root.get("description")), "%" + lower(c.keyword()) + "%"));
            }
            if (c.solicitationNumber() != null) {
                where.add(cb.equal(cb.trim(root.get("solicitationNumber")), c.solicitationNumber()));
            }
            if (c.fromDate() != null) where.add(cb.greaterThanOrEqualTo(root.get("startDate"), c.fromDate()));
            if (c.toDate() != null) where.add(cb.lessThanOrEqualTo(root.get("startDate"), c.toDate()));
            return cb.and(where.toArray(new Predicate[0]));
        };
    }

    private static boolean noticeMatches(Opportunity o, Criteria c) {
        if (!c.naicsCodes().isEmpty() && !c.naicsCodes().contains(raw(o, "naicsCode"))) return false;
        if (!c.pscCodes().isEmpty() && !c.pscCodes().contains(raw(o, "classificationCode"))) return false;
        if (c.recipientName() != null && !contains(o.getAwardeeName(), c.recipientName())) return false;
        if (c.keyword() != null && !contains(o.getTitle(), c.keyword())) return false;
        if (c.solicitationNumber() != null
                && (o.getSolicitationNumber() == null || !o.getSolicitationNumber().trim().equals(c.solicitationNumber()))) {
            return false;
        }
        LocalDate date = awardDate(o);
        if (c.fromDate() != null && (date == null || date.isBefore(c.fromDate()))) return false;
        return c.toDate() == null || (date != null && !date.isAfter(c.toDate()));
    }

    private Map<String, Object> contractRow(FederalContract fc) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("source", FEDERAL_CONTRACT);
        row.put("number", fc.getPiid());
        row.put("solicitationNumber", fc.getSolicitationNumber());
        row.put("title", fc.getDescription());
        row.put("recipientName", fc.getRecipientName());
        row.put("recipientUei", fc.getRecipientUei());
        row.put("amountUsd", fc.getAmountUsd());
        row.put("startDate", fc.getStartDate());
        row.put("endDate", fc.getEndDate());
        row.put("naicsCode", fc.getNaicsCode());
        row.put("pscCode", fc.getPscCode());
        row.put("office", fc.getAwardingSubAgency());
        row.put("contractType", fc.getAwardType() != null ? fc.getAwardType().name() : null);
        return row;
    }

    private Map<String, Object> noticeRow(Opportunity o) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("source", SAM_AWARD_NOTICE);
        row.put("number", awardText(o, "number"));
        row.put("solicitationNumber", o.getSolicitationNumber());
        row.put("title", o.getTitle());
        row.put("recipientName", o.getAwardeeName());
        row.put("recipientUei", o.getAwardeeUei());
        BigDecimal amount = o.getAwardAmountUsd();
        row.put("amountUsd", amount);
        row.put("startDate", awardDate(o));
        row.put("endDate", null);
        row.put("naicsCode", raw(o, "naicsCode"));
        row.put("pscCode", raw(o, "classificationCode"));
        row.put("office", o.getOrganizationName());
        row.put("contractType", o.getType());
        return row;
    }

    /** 낙찰일. SAM 응답의 award.date 가 날짜가 아니면 게시일 */
    private static LocalDate awardDate(Opportunity o) {
        String date = awardText(o, "date");
        if (date != null) {
            try {
                return LocalDate.parse(date.length() > 10 ? date.substring(0, 10) : date);
            } catch (DateTimeParseException ignored) {
                // 게시일로 본다
            }
        }
        return o.getPostedDate() != null ? o.getPostedDate().toLocalDate() : null;
    }

    private static String awardText(Opportunity o, String key) {
        if (o.getRawJson() == null || !(o.getRawJson().get("award") instanceof Map<?, ?> award)) return null;
        Object v = award.get(key);
        return v == null || String.valueOf(v).isBlank() ? null : String.valueOf(v).trim();
    }

    private static String raw(Opportunity o, String key) {
        if (o.getRawJson() == null) return null;
        Object v = o.getRawJson().get(key);
        return v == null ? null : Objects.toString(v).trim();
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lower(needle));
    }

    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
