package com.biddingagency.domain.compliance.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
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

    public ProposalSection section(String title, List<String> refs) {
        return TestHelper.withId(ProposalSection.builder()
                .chapterId(chapter.getId()).title(title).requirementRefs(refs).build());
    }

    public UUID opportunityId() {
        return opportunity.getId();
    }
}
