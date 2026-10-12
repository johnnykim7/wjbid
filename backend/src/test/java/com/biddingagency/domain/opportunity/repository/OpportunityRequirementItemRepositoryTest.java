package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.service.RequirementSyncService;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-COMP-010, TC-COMP-011 (CR-159, BIZ-075): 고객 공고 요구사항과 제출 전 검사가 읽는 조회는 매트릭스 줄을 빼고 제출 서류 줄만 준다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class OpportunityRequirementItemRepositoryTest {

    @Autowired private OpportunityRepository opportunityRepository;
    @Autowired private OpportunityRequirementItemRepository requirementRepository;

    private Opportunity opportunity;

    @BeforeEach
    void setUp() {
        LocalDateTime at = LocalDateTime.of(2026, 10, 1, 9, 0);
        opportunity = opportunityRepository.save(Opportunity.builder()
                .noticeId("N-COMP").solicitationNumber("W1").title("title").type("Solicitation")
                .postedDate(at).active(true).firstSeenAt(at).lastModifiedAt(at).build());
        requirementRepository.save(row("License", FulfillmentParty.CLIENT_UPLOAD, null, 0));
        requirementRepository.save(row("Technical Volume", FulfillmentParty.PLATFORM_GENERATED, null, 1));
        requirementRepository.save(row("Quality control plan", FulfillmentParty.PLATFORM_GENERATED, "REQ-001", 1));
        requirementRepository.save(row("Past performance", FulfillmentParty.PLATFORM_GENERATED, "REQ-002", 2));
        requirementRepository.saveAndFlush(row("Key personnel", FulfillmentParty.PLATFORM_GENERATED, "REQ-003", 3));
    }

    private OpportunityRequirementItem row(String title, FulfillmentParty party, String reqId, int sortOrder) {
        return OpportunityRequirementItem.builder()
                .opportunity(opportunity)
                .category(reqId == null ? RequirementCategory.DOCUMENT : RequirementCategory.TECHNICAL)
                .title(title)
                .requirementJson(Map.of(RequirementSyncService.KEY_FULFILLMENT_PARTY, party.name()))
                .reqId(reqId)
                .reqLevel(reqId == null ? null : RequirementLevel.SHALL)
                .sortOrder(sortOrder)
                .build();
    }

    @Test
    @DisplayName("TC-COMP-010 고객요구사항조회_매트릭스줄제외")
    void 고객요구사항조회_매트릭스줄제외() {
        List<OpportunityRequirementItem> items =
                requirementRepository.findByOpportunityIdAndReqIdIsNullOrderBySortOrderAsc(opportunity.getId());

        assertThat(items).extracting(OpportunityRequirementItem::getTitle).containsExactly("License", "Technical Volume");
    }

    @Test
    @DisplayName("TC-COMP-011 제출전검사_플랫폼항목_매트릭스줄없음")
    void 제출전검사_플랫폼항목_매트릭스줄없음() {
        List<OpportunityRequirementItem> platform = requirementRepository
                .findSubmissionItemsByOpportunityIds(Set.of(opportunity.getId())).stream()
                .filter(ri -> FulfillmentParty.PLATFORM_GENERATED.name()
                        .equals(ri.getRequirementJson().get(RequirementSyncService.KEY_FULFILLMENT_PARTY)))
                .toList();

        assertThat(platform).extracting(OpportunityRequirementItem::getTitle).containsExactly("Technical Volume");
    }
}
