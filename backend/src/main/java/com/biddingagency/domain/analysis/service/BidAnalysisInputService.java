package com.biddingagency.domain.analysis.service;

import com.biddingagency.domain.analysis.entity.BidAnalysis;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.service.ComplianceService;
import com.biddingagency.domain.eligibility.entity.EligibilityAnswer;
import com.biddingagency.domain.eligibility.repository.EligibilityAnswerRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * get_bid_analysis_input (CR-170, T3-2). 단계마다 필요한 것만 싣는다.
 *
 * 한 응답이 앱 도구 응답 한도(CR-169 2.11)를 넘지 않게, 앞 단계 산출물은 뒤 단계가 읽을 칸만 싣는다.
 * 고객 회사 사실은 회원 칸, 고객 서류 사실, 참가 자격 답에서만 온다(BIZ-093).
 */
@Service
@RequiredArgsConstructor
public class BidAnalysisInputService {

    public enum Step {
        REPORT,
        CAPTURE_PLAN,
        DECISION
    }

    /** 같은 기관 최근 공고 수. 스킬 01 STEP 3 발주처 동향을 웹 자료 대신 모은 공고로 본다 */
    static final int AGENCY_RECENT_LIMIT = 20;
    static final String ELIGIBILITY_SOURCE = "참가 자격 답(고객이 스스로 답한 것)";
    /** DECISION 이 읽는 종합 분석 리포트 칸(자격, 경쟁사와 BCC, 공정성, 발주 이력) */
    private static final List<String> DECISION_REPORT_KEYS = List.of("eligibility", "competitors", "fairness", "orderHistory");

    private final BidAnalysisService analysisService;
    private final BidRequestRepository bidRequestRepository;
    private final NoticeRepository noticeRepository;
    private final OpportunityRepository opportunityRepository;
    private final ComplianceService complianceService;
    private final DocumentReadingService documentReadingService;
    private final EligibilityAnswerRepository eligibilityAnswerRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> input(UUID bidRequestId, UUID analysisId, Step step) {
        BidAnalysis analysis = analysisService.analysisOf(bidRequestId, analysisId);
        BidRequest bidRequest = bidRequestRepository.findByIdWithDetails(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));
        Opportunity opp = bidRequest.getOpportunity();
        Notice notice = noticeRepository
                .findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(opp.getId(), OpportunityVisibility.VISIBLE)
                .filter(Notice::isGenerationCompleted)
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bidRequestId", bidRequestId.toString());
        result.put("analysisId", analysisId.toString());
        result.put("step", step.name());
        result.put("opportunity", meta(opp));
        switch (step) {
            case REPORT -> {
                result.put("noticeAnalysis", noticeAnalysis(opp, notice));
                result.put("sameSolicitationNotices", sameSolicitation(opp));
                result.put("agencyRecentNotices", agencyRecent(opp));
                result.put("company", company(bidRequest.getMember()));
                result.put("clientDocuments", documentReadingService.designInputDocuments(bidRequestId));
                result.put("eligibilityAnswers", eligibilityAnswers(bidRequest.getMember(), notice));
            }
            case CAPTURE_PLAN -> {
                result.put("noticeSummary", notice != null ? notice.getSummaryJson() : null);
                result.put("factorTree", notice != null ? notice.getFactorTreeJson() : null);
                result.put("company", company(bidRequest.getMember()));
                result.put("clientDocuments", documentReadingService.designInputDocuments(bidRequestId));
                result.put("report", parse(analysis.getReportJson()));
            }
            case DECISION -> {
                result.put("report", reportForDecision(parse(analysis.getReportJson())));
                result.put("capturePlan", parse(analysis.getCapturePlanJson()));
                result.put("company", company(bidRequest.getMember()));
                result.put("complianceMatrix", matrixSummary(opp.getId()));
                PwinPolicy policy = analysisService.policy();
                Map<String, Object> pwin = new LinkedHashMap<>();
                pwin.put("weights", policy.weights());
                pwin.put("thresholds", policy.thresholds());
                result.put("pwinPolicy", pwin);
            }
        }
        return result;
    }

    private Map<String, Object> meta(Opportunity opp) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("opportunityId", opp.getId().toString());
        m.put("solicitationNumber", opp.getSolicitationNumber());
        m.put("title", opp.getTitle());
        m.put("titleKo", opp.getTitleKo());
        m.put("type", opp.getType());
        m.put("organizationName", opp.getOrganizationName());
        m.put("naicsCode", raw(opp, "naicsCode"));
        m.put("pscCode", raw(opp, "classificationCode"));
        m.put("setAside", opp.getSetAsideKo());
        m.put("placeOfPerformance", opp.getPlaceOfPerformanceShort());
        m.put("postedDate", opp.getPostedDate());
        m.put("responseDeadline", opp.getResponseDeadline());
        return m;
    }

    /** get_proposal_design_input 과 같은 칸 */
    private Map<String, Object> noticeAnalysis(Opportunity opp, Notice notice) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("opportunityText", opportunityText(opp));
        if (notice != null) {
            m.put("noticeSummary", nullSafe(notice.getSummaryJson()));
            m.put("requiredDocuments", nullSafe(notice.getRequiredDocumentsJson()));
            m.put("documentFormats", nullSafe(notice.getDocumentFormatsJson()));
            m.put("noticeContent", nullSafe(notice.getContentJson()));
            m.put("factorTree", notice.getFactorTreeJson());
            m.put("priceItems", notice.getPriceItemsJson());
        } else {
            m.put("factorTree", null);
            m.put("priceItems", null);
        }
        m.put("complianceMatrix", complianceService.designInputLines(opp.getId()));
        return m;
    }

    /** 같은 공고번호의 사전 조사, 사전 공고, 특별 공고, 정정. 스킬 01 STEP 7 징후 1, 2 와 STEP 2 일정에 쓴다 */
    private List<Map<String, Object>> sameSolicitation(Opportunity opp) {
        String sol = opp.getSolicitationNumber() == null ? null : opp.getSolicitationNumber().trim();
        if (sol == null || sol.isEmpty()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Opportunity o : opportunityRepository.findSameSolicitation(sol, opp.getId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("noticeId", o.getNoticeId());
            m.put("type", o.getType());
            m.put("title", o.getTitle());
            m.put("postedDate", o.getPostedDate());
            m.put("responseDeadline", o.getResponseDeadline());
            m.put("summaryKo", o.getDescriptionSummaryKo());
            m.put("awardeeName", o.getAwardeeName());
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> agencyRecent(Opportunity opp) {
        if (opp.getOrganizationName() == null || opp.getOrganizationName().isBlank()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (MarketSource s : opportunityRepository.findRecentMarketSourcesByOrganization(
                opp.getOrganizationName(), opp.getId(), PageRequest.of(0, AGENCY_RECENT_LIMIT))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", s.title());
            m.put("type", s.type());
            m.put("solicitationNumber", s.solicitationNumber());
            m.put("postedDate", s.postedDate());
            m.put("naicsCode", s.naicsCode());
            m.put("awardeeName", s.awardeeName());
            out.add(m);
        }
        return out;
    }

    /** 회원 칸. 연락처는 분석에 쓰지 않아 싣지 않는다 */
    private static Map<String, Object> company(Member member) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("companyName", member.getCompanyName());
        m.put("businessRegistrationNumber", member.getBusinessRegistrationNumber());
        m.put("address", member.getAddress());
        m.put("uei", member.getUei());
        m.put("cageCode", member.getCageCode());
        return m;
    }

    /** 고객이 스스로 답한 것이다. 판정이 아니다(BIZ-032) */
    private List<Map<String, Object>> eligibilityAnswers(Member member, Notice notice) {
        if (notice == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (EligibilityAnswer a : eligibilityAnswerRepository.findByMemberIdAndNoticeId(member.getId(), notice.getId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemNo", a.getItemNo());
            m.put("itemTitle", a.getItemTitle());
            m.put("answer", a.getAnswer() != null ? a.getAnswer().name() : null);
            m.put("documentAttached", a.getDocumentId() != null);
            m.put("source", ELIGIBILITY_SOURCE);
            out.add(m);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> reportForDecision(Object report) {
        if (!(report instanceof Map)) return null;
        Map<String, Object> full = (Map<String, Object>) report;
        Map<String, Object> m = new LinkedHashMap<>();
        for (String key : DECISION_REPORT_KEYS) {
            m.put(key, full.get(key));
        }
        return m;
    }

    /** 매트릭스 줄 수와 고객이 내야 하는 줄 */
    private Map<String, Object> matrixSummary(UUID opportunityId) {
        List<Map<String, Object>> lines = complianceService.designInputLines(opportunityId);
        List<Map<String, Object>> client = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            if (FulfillmentParty.CLIENT_UPLOAD.name().equals(String.valueOf(line.get("fulfillmentParty")))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("reqId", line.get("reqId"));
                m.put("text", line.get("text"));
                client.add(m);
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lineCount", lines.size());
        m.put("clientLines", client);
        return m;
    }

    private static String opportunityText(Opportunity opp) {
        StringBuilder sb = new StringBuilder();
        sb.append("공고 제목: ").append(opp.getTitle()).append("\n");
        if (opp.getOrganizationName() != null) sb.append("기관: ").append(opp.getOrganizationName()).append("\n");
        if (opp.getSolicitationNumber() != null) sb.append("공고번호: ").append(opp.getSolicitationNumber()).append("\n");
        String body = opp.getDescriptionBody();
        if (body != null && !body.isBlank()) {
            sb.append("\n상세 설명(원문 본문):\n").append(body);
        } else if (opp.getRawJson() != null && opp.getRawJson().get("description") != null) {
            sb.append("\n상세 설명:\n").append(opp.getRawJson().get("description"));
        }
        return sb.toString();
    }

    private static String raw(Opportunity opp, String key) {
        if (opp.getRawJson() == null || opp.getRawJson().get(key) == null) return null;
        return String.valueOf(opp.getRawJson().get(key)).trim();
    }

    private static Map<String, Object> nullSafe(Map<String, Object> m) {
        return m != null ? m : Map.of();
    }

    private Object parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}
