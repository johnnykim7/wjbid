package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.dto.AnalysisResultDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 공고 분석의 제출 서류로 그 공고의 요구 항목을 맞춘다 (CR-141, BIZ-045).
 *
 * 평가 항목별 제출물(factors)이 있으면 제출물마다 한 항목, 없으면 평면 목록(documents)마다 한 항목이다.
 * 평면 목록은 맡는 쪽이 적혀 있지 않아 고객 올림으로 본다.
 * 항목 열쇠(sourceKey)로 앞 분석의 항목을 찾아 고쳐 써서, 다시 분석해도 고객이 이어 둔 서류가 떨어지지 않게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementSyncService {

    public static final String KEY_SOURCE_KEY = "sourceKey";
    public static final String KEY_FULFILLMENT_PARTY = "fulfillmentParty";
    public static final String KEY_MANDATORY = "mandatory";
    public static final String KEY_FORMAT = "format";
    public static final String KEY_PAGE_LIMIT = "pageLimit";
    public static final String KEY_SOURCE_REF = "sourceRef";
    public static final String KEY_NOTES = "notes";
    public static final String KEY_FACTOR_ID = "factorId";
    public static final String KEY_FACTOR_TITLE = "factorTitle";

    private static final int TITLE_MAX = 500;
    private static final String DUPLICATE_KEY_SEPARATOR = "#";

    private final OpportunityRequirementItemRepository requirementRepository;
    private final RequirementFulfillmentMapRepository fulfillmentMapRepository;

    @Transactional
    public void syncFromNotice(Notice notice) {
        Opportunity opportunity = notice.getOpportunity();
        List<Desired> desired = desiredItems(notice.getRequiredDocumentsJson());

        Map<String, OpportunityRequirementItem> existingByKey = new HashMap<>();
        List<OpportunityRequirementItem> removed = new ArrayList<>();
        for (OpportunityRequirementItem item : requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunity.getId())) {
            Object key = item.getRequirementJson() == null ? null : item.getRequirementJson().get(KEY_SOURCE_KEY);
            if (key == null || existingByKey.putIfAbsent(key.toString(), item) != null) {
                removed.add(item);
            }
        }

        int updated = 0;
        List<OpportunityRequirementItem> created = new ArrayList<>();
        for (int i = 0; i < desired.size(); i++) {
            Desired d = desired.get(i);
            OpportunityRequirementItem item = existingByKey.remove(d.key());
            if (item != null) {
                item.syncFromAnalysis(d.title(), d.description(), d.isBlocker(), d.json(), i);
                updated++;
            } else {
                created.add(OpportunityRequirementItem.builder()
                        .opportunity(opportunity)
                        .category(RequirementCategory.DOCUMENT)
                        .title(d.title())
                        .description(d.description())
                        .isBlocker(d.isBlocker())
                        .requirementJson(d.json())
                        .sortOrder(i)
                        .build());
            }
        }
        removed.addAll(existingByKey.values());

        if (!removed.isEmpty()) {
            List<UUID> removedIds = removed.stream().map(OpportunityRequirementItem::getId).toList();
            int unlinked = fulfillmentMapRepository.deleteByRequirementItemIds(removedIds);
            requirementRepository.deleteAll(removed);
            log.info("[CR-141] 요구 항목 지움: opportunity={}, items={}, links={}", opportunity.getId(), removed.size(), unlinked);
        }
        requirementRepository.saveAll(created);
        log.info("[CR-141] 요구 항목 맞춤: notice={}, opportunity={}, updated={}, created={}, removed={}",
                notice.getId(), opportunity.getId(), updated, created.size(), removed.size());
    }

    private record Desired(String key, String title, String description, boolean isBlocker, Map<String, Object> json) {
    }

    private List<Desired> desiredItems(Map<String, Object> requiredDocumentsJson) {
        AnalysisResultDto.RequiredDocumentsDto rd = AnalysisResultDto.RequiredDocumentsDto.from(requiredDocumentsJson);
        List<Desired> result = new ArrayList<>();
        if (rd == null) {
            return result;
        }
        Set<String> usedKeys = new HashSet<>();
        if (rd.factors() != null && !rd.factors().isEmpty()) {
            for (AnalysisResultDto.FactorDto factor : rd.factors()) {
                if (factor.subfactors() == null) {
                    continue;
                }
                for (AnalysisResultDto.SubfactorDto s : factor.subfactors()) {
                    String title = firstNonBlank(s.name(), s.subfactorId());
                    if (title == null) {
                        continue;
                    }
                    FulfillmentParty party = FulfillmentParty.fromAnalysis(s.fulfillmentParty());
                    boolean mandatory = Boolean.TRUE.equals(s.mandatory());
                    String key = uniqueKey(firstNonBlank(s.subfactorId(), s.name()), usedKeys);
                    Map<String, Object> json = json(key, party, mandatory, s.format(), s.pageLimit(),
                            s.sourceRef(), s.notes(), factor.factorId(), factor.factorTitle());
                    result.add(new Desired(key, truncate(title), s.description(),
                            isBlocker(mandatory, party), json));
                }
            }
        } else if (rd.documents() != null) {
            for (AnalysisResultDto.RequiredDocumentItem d : rd.documents()) {
                if (isBlank(d.name())) {
                    continue;
                }
                boolean mandatory = Boolean.TRUE.equals(d.mandatory());
                String key = uniqueKey(d.name(), usedKeys);
                Map<String, Object> json = json(key, FulfillmentParty.CLIENT_UPLOAD, mandatory, d.format(),
                        d.pageLimit(), null, d.notes(), null, null);
                result.add(new Desired(key, truncate(d.name()), d.description(),
                        isBlocker(mandatory, FulfillmentParty.CLIENT_UPLOAD), json));
            }
        }
        return result;
    }

    /** BIZ-046: 필수이고 고객 올림인 항목만 서류 접수 넘기기를 막는다 */
    private static boolean isBlocker(boolean mandatory, FulfillmentParty party) {
        return mandatory && party == FulfillmentParty.CLIENT_UPLOAD;
    }

    private static Map<String, Object> json(String key, FulfillmentParty party, boolean mandatory, String format,
                                            String pageLimit, String sourceRef, String notes,
                                            String factorId, String factorTitle) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put(KEY_SOURCE_KEY, key);
        json.put(KEY_FULFILLMENT_PARTY, party.name());
        json.put(KEY_MANDATORY, mandatory);
        putIfPresent(json, KEY_FORMAT, format);
        putIfPresent(json, KEY_PAGE_LIMIT, pageLimit);
        putIfPresent(json, KEY_SOURCE_REF, sourceRef);
        putIfPresent(json, KEY_NOTES, notes);
        putIfPresent(json, KEY_FACTOR_ID, factorId);
        putIfPresent(json, KEY_FACTOR_TITLE, factorTitle);
        return json;
    }

    private static void putIfPresent(Map<String, Object> json, String key, String value) {
        if (!isBlank(value)) {
            json.put(key, value);
        }
    }

    private static String uniqueKey(String base, Set<String> usedKeys) {
        String key = base;
        int n = 2;
        while (!usedKeys.add(key)) {
            key = base + DUPLICATE_KEY_SEPARATOR + n++;
        }
        return key;
    }

    private static String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a.trim() : !isBlank(b) ? b.trim() : null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String truncate(String title) {
        return title.length() > TITLE_MAX ? title.substring(0, TITLE_MAX) : title;
    }
}
