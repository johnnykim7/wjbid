package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.dto.AnalysisResultDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
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
 *
 * CR-159: 컴플라이언스 매트릭스 줄도 같은 때 같은 방법으로 맞춘다. 매트릭스 줄은 REQ ID 를 달고(BIZ-074),
 * 제출 서류 줄과는 서로의 열쇠를 보지 않는다.
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

    public static final String MATRIX_LINES = "lines";
    public static final String KEY_TEXT = "text";
    public static final String KEY_TRANSLATION = "translation";
    public static final String KEY_SOURCE_FILE = "sourceFile";
    public static final String KEY_PAGE = "page";
    public static final String KEY_CLAUSE = "clause";
    public static final String KEY_QUOTE = "quote";
    public static final String KEY_VERB = "verb";
    public static final String KEY_QUERY_CANDIDATE = "queryCandidate";
    public static final String KEY_AMBIGUITY = "ambiguity";
    public static final String KEY_DRAFT_QUERY = "draftQuery";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_CATEGORY = "category";

    private static final int TITLE_MAX = 500;
    private static final String DUPLICATE_KEY_SEPARATOR = "#";
    private static final String MATRIX_KEY_PREFIX = "REQ:";
    private static final String REQ_ID_FORMAT = "REQ-%03d";

    private final OpportunityRequirementItemRepository requirementRepository;
    private final RequirementFulfillmentMapRepository fulfillmentMapRepository;
    private final OpportunityRepository opportunityRepository;

    @Transactional
    public void syncFromNotice(Notice notice) {
        Opportunity opportunity = notice.getOpportunity();
        List<OpportunityRequirementItem> submissionRows = new ArrayList<>();
        List<OpportunityRequirementItem> matrixRows = new ArrayList<>();
        for (OpportunityRequirementItem item : requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunity.getId())) {
            (item.isMatrixLine() ? matrixRows : submissionRows).add(item);
        }

        List<OpportunityRequirementItem> removed = new ArrayList<>();
        List<OpportunityRequirementItem> created = new ArrayList<>();
        int updated = syncSubmissionItems(opportunity, desiredItems(notice.getRequiredDocumentsJson()),
                submissionRows, removed, created);
        int matrixUpdated = syncMatrixLines(opportunity, desiredLines(notice.getComplianceMatrixJson()),
                matrixRows, removed, created);

        if (!removed.isEmpty()) {
            List<UUID> removedIds = removed.stream().map(OpportunityRequirementItem::getId).toList();
            int unlinked = fulfillmentMapRepository.deleteByRequirementItemIds(removedIds);
            requirementRepository.deleteAll(removed);
            log.info("[CR-141] 요구 항목 지움: opportunity={}, items={}, links={}", opportunity.getId(), removed.size(), unlinked);
        }
        requirementRepository.saveAll(created);
        log.info("[CR-141] 요구 항목 맞춤: notice={}, opportunity={}, updated={}, matrixUpdated={}, created={}, removed={}",
                notice.getId(), opportunity.getId(), updated, matrixUpdated, created.size(), removed.size());
    }

    private int syncSubmissionItems(Opportunity opportunity, List<Desired> desired, List<OpportunityRequirementItem> rows,
                                    List<OpportunityRequirementItem> removed, List<OpportunityRequirementItem> created) {
        Map<String, OpportunityRequirementItem> existingByKey = byKey(rows, removed);
        int updated = 0;
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
        return updated;
    }

    /**
     * 같은 열쇠 줄은 REQ ID 를 그대로 두고 고쳐 쓰고, 처음 보는 줄은 그 공고의 다음 번호를 받는다.
     * 사라진 줄의 번호는 다시 쓰지 않는다(BIZ-074). 매트릭스 줄은 막는 항목이 아니다(BIZ-075).
     */
    private int syncMatrixLines(Opportunity opportunity, List<DesiredLine> desired, List<OpportunityRequirementItem> rows,
                                List<OpportunityRequirementItem> removed, List<OpportunityRequirementItem> created) {
        Map<String, OpportunityRequirementItem> existingByKey = byKey(rows, removed);
        int updated = 0;
        Opportunity numbering = null;
        for (DesiredLine d : desired) {
            OpportunityRequirementItem item = existingByKey.remove(d.key());
            if (item != null) {
                item.syncFromMatrix(d.title(), d.translation(), d.category(), d.level(), d.json());
                updated++;
                continue;
            }
            if (numbering == null) {
                numbering = opportunityRepository.findByIdForUpdate(opportunity.getId())
                        .orElseThrow(() -> new IllegalStateException("Opportunity not found: " + opportunity.getId()));
            }
            int reqNo = numbering.nextReqNo();
            created.add(OpportunityRequirementItem.builder()
                    .opportunity(opportunity)
                    .category(d.category())
                    .title(d.title())
                    .description(d.translation())
                    .isBlocker(false)
                    .requirementJson(d.json())
                    .sortOrder(reqNo)
                    .reqId(String.format(REQ_ID_FORMAT, reqNo))
                    .reqLevel(d.level())
                    .build());
        }
        removed.addAll(existingByKey.values());
        return updated;
    }

    /** 열쇠가 없거나 겹치는 줄은 지울 줄로 보낸다 */
    private static Map<String, OpportunityRequirementItem> byKey(List<OpportunityRequirementItem> rows,
                                                                 List<OpportunityRequirementItem> removed) {
        Map<String, OpportunityRequirementItem> existingByKey = new HashMap<>();
        for (OpportunityRequirementItem item : rows) {
            Object key = item.getRequirementJson() == null ? null : item.getRequirementJson().get(KEY_SOURCE_KEY);
            if (key == null || existingByKey.putIfAbsent(key.toString(), item) != null) {
                removed.add(item);
            }
        }
        return existingByKey;
    }

    private record Desired(String key, String title, String description, boolean isBlocker, Map<String, Object> json) {
    }

    private record DesiredLine(String key, String title, String translation, RequirementCategory category,
                               RequirementLevel level, Map<String, Object> json) {
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

    /** 원문이 빈 줄은 요구가 아니므로 받지 않는다(BIZ-073) */
    private List<DesiredLine> desiredLines(Map<String, Object> complianceMatrixJson) {
        List<DesiredLine> result = new ArrayList<>();
        Object lines = complianceMatrixJson == null ? null : complianceMatrixJson.get(MATRIX_LINES);
        if (!(lines instanceof List<?> list)) {
            return result;
        }
        Set<String> usedKeys = new HashSet<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> line)) {
                continue;
            }
            String text = string(line.get(KEY_TEXT));
            if (isBlank(text)) {
                continue;
            }
            String clause = string(line.get(KEY_CLAUSE));
            String translation = string(line.get(KEY_TRANSLATION));
            String key = uniqueKey(matrixKey(clause, text), usedKeys);

            Map<String, Object> json = new LinkedHashMap<>();
            json.put(KEY_SOURCE_KEY, key);
            json.put(KEY_TEXT, text);
            putIfPresent(json, KEY_TRANSLATION, translation);
            putIfPresent(json, KEY_SOURCE_FILE, string(line.get(KEY_SOURCE_FILE)));
            putIfPresent(json, KEY_PAGE, string(line.get(KEY_PAGE)));
            putIfPresent(json, KEY_CLAUSE, clause);
            putIfPresent(json, KEY_QUOTE, string(line.get(KEY_QUOTE)));
            putIfPresent(json, KEY_VERB, string(line.get(KEY_VERB)));
            json.put(KEY_FULFILLMENT_PARTY, FulfillmentParty.fromAnalysis(string(line.get(KEY_FULFILLMENT_PARTY))).name());
            putIfPresent(json, KEY_FACTOR_ID, string(line.get(KEY_FACTOR_ID)));
            putIfPresent(json, KEY_FACTOR_TITLE, string(line.get(KEY_FACTOR_TITLE)));
            json.put(KEY_QUERY_CANDIDATE, Boolean.parseBoolean(string(line.get(KEY_QUERY_CANDIDATE))));
            putIfPresent(json, KEY_AMBIGUITY, string(line.get(KEY_AMBIGUITY)));
            putIfPresent(json, KEY_DRAFT_QUERY, string(line.get(KEY_DRAFT_QUERY)));

            result.add(new DesiredLine(key, truncate(text.trim()), translation,
                    matrixCategory(string(line.get(KEY_CATEGORY))), RequirementLevel.fromAnalysis(line.get(KEY_LEVEL)), json));
        }
        return result;
    }

    /** DOCUMENT 는 제출 서류 줄의 분류라 매트릭스 줄에 쓰지 않는다(BIZ-075). 모르는 값도 그 밖으로 둔다 */
    private static RequirementCategory matrixCategory(String value) {
        if (value != null) {
            for (RequirementCategory category : RequirementCategory.values()) {
                if (category != RequirementCategory.DOCUMENT && category.name().equalsIgnoreCase(value.trim())) {
                    return category;
                }
            }
        }
        return RequirementCategory.OTHER;
    }

    /** 다시 분석해도 같은 조항의 같은 원문이면 같은 열쇠가 나오게 빈칸만 고르게 맞춘다 */
    static String matrixKey(String clause, String text) {
        String source = normalize(clause) + "\n" + normalize(text);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return MATRIX_KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
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

    private static String string(Object value) {
        return value == null ? null : value.toString();
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
