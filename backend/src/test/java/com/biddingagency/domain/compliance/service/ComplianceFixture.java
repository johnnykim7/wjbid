package com.biddingagency.domain.compliance.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CR-159 시험이 같이 쓰는 공고, 입찰 요청, 제안서 문서, 매트릭스 줄.
 */
public final class ComplianceFixture {

    public static final String QC = "The offeror shall describe its quality control plan.";
    public static final String PP = "The offeror shall provide three past performance references.";
    /** CR-179 문서 구성이 있는 평가 항목 구조: F1 기술 제안서, F2 과거 수행 실적, F3 가격 요약, 커버레터 요구 없음 */
    public static final String COMPOSED_TREE = "{\"method\":\"LPTA\",\"coverLetterRequired\":false,\"factors\":["
            + "{\"factorId\":\"F1\",\"title\":\"FACTOR A. TECHNICAL\",\"documentType\":\"TECHNICAL_PROPOSAL\"},"
            + "{\"factorId\":\"F2\",\"title\":\"FACTOR B. PAST PERFORMANCE\",\"documentType\":\"PAST_PERFORMANCE\"},"
            + "{\"factorId\":\"F3\",\"title\":\"FACTOR C. PRICE\",\"documentType\":\"PRICING_SUMMARY\"}]}";
    /** 문서 구성이 없는 옛 평가 항목 구조 */
    public static final String OLD_TREE = "{\"method\":\"LPTA\",\"factors\":["
            + "{\"factorId\":\"F1\",\"title\":\"FACTOR A. TECHNICAL\"},{\"factorId\":\"F2\",\"title\":\"FACTOR B. PAST PERFORMANCE\"}]}";

    public final Opportunity opportunity = TestHelper.withId(Opportunity.builder().build());
    public final BidRequest bidRequest = TestHelper.withId(BidRequest.builder().opportunity(opportunity).build());
    public final BidDocument document = TestHelper.withId(BidDocument.builder()
            .bidRequest(bidRequest).documentType(DocumentType.TECHNICAL_PROPOSAL).build());
    public final ProposalChapter chapter = TestHelper.withId(ProposalChapter.builder()
            .documentId(document.getId()).factorLabel("1").factorTitle("Technical").build());

    public OpportunityRequirementItem line(String reqId, RequirementLevel level, FulfillmentParty party, String text) {
        Map<String, Object> json = new HashMap<>();
        json.put(RequirementSyncService.KEY_TEXT, text);
        json.put(RequirementSyncService.KEY_TRANSLATION, "번역 " + reqId);
        json.put(RequirementSyncService.KEY_VERB, "describe");
        json.put(RequirementSyncService.KEY_FULFILLMENT_PARTY, party.name());
        json.put(RequirementSyncService.KEY_FACTOR_ID, "1");
        return TestHelper.withId(OpportunityRequirementItem.builder()
                .opportunity(opportunity)
                .category(RequirementCategory.TECHNICAL)
                .title(text)
                .requirementJson(json)
                .reqId(reqId)
                .reqLevel(level)
                .build());
    }

    public OpportunityRequirementItem shallPlatform(String reqId, String text) {
        return line(reqId, RequirementLevel.SHALL, FulfillmentParty.PLATFORM_GENERATED, text);
    }

    /** 평가 항목 factorId 에 걸린 Shall 플랫폼 생성 줄. factorId 가 null 이면 평가 항목이 없는 줄 */
    public OpportunityRequirementItem shallPlatformOf(String reqId, String factorId) {
        OpportunityRequirementItem item = shallPlatform(reqId, "The offeror shall address " + reqId + ".");
        item.getRequirementJson().put(RequirementSyncService.KEY_FACTOR_ID, factorId);
        return item;
    }

    public BidDocument document(DocumentType type) {
        return TestHelper.withId(BidDocument.builder().bidRequest(bidRequest).documentType(type).build());
    }

    public Notice notice(String factorTreeJson) {
        return Notice.builder().opportunity(opportunity).generationStatus(NoticeGenerationStatus.COMPLETED)
                .factorTreeJsonRaw(factorTreeJson).priceItemsJsonRaw("{\"items\":[{\"clin\":\"0001\"}]}").build();
    }

    public ProposalSection section(String title, List<String> refs) {
        return TestHelper.withId(ProposalSection.builder()
                .chapterId(chapter.getId()).title(title).requirementRefs(refs).build());
    }

    public UUID opportunityId() {
        return opportunity.getId();
    }
}
