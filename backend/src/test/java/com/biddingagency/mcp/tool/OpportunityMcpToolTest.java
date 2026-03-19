package com.biddingagency.mcp.tool;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.biddingagency.TestHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-MCP-OPP-001 ~ TC-MCP-OPP-004: OpportunityMcpTool 테스트
 */
@ExtendWith(MockitoExtension.class)
class OpportunityMcpToolTest {

    @Mock
    private OpportunityService opportunityService;
    @Mock
    private OpportunityRequirementItemRepository requirementItemRepository;

    @InjectMocks
    private OpportunityMcpTool opportunityMcpTool;

    private Opportunity createTestOpportunity(UUID id) {
        Opportunity opp = Opportunity.builder()
                .noticeId("NOTICE-001")
                .title("Test Opportunity")
                .type("Solicitation")
                .organizationName("US Army")
                .postedDate(LocalDateTime.now().minusDays(5))
                .responseDeadline(LocalDateTime.now().plusDays(30))
                .active(true)
                .uiLink("http://sam.gov/123")
                .descriptionLink("http://sam.gov/123/desc")
                .firstSeenAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();
        return TestHelper.withId(opp, id);
    }

    // TC-MCP-OPP-001: get_opportunity 정상
    @Test
    @DisplayName("유효opportunityId_getOpportunity_공고상세JSON반환")
    void 유효ID_getOpportunity_상세반환() {
        // given
        UUID id = UUID.randomUUID();
        Opportunity opp = createTestOpportunity(id);
        given(opportunityService.findById(id)).willReturn(opp);

        // when
        String result = opportunityMcpTool.getOpportunity(Map.of("opportunityId", id.toString()));

        // then
        assertThat(result).contains("NOTICE-001");
        assertThat(result).contains("Test Opportunity");
        assertThat(result).contains("US Army");
    }

    // TC-MCP-OPP-002: get_opportunity 미존재
    @Test
    @DisplayName("무효UUID_getOpportunity_예외")
    void 무효ID_getOpportunity_예외() {
        // given
        UUID id = UUID.randomUUID();
        given(opportunityService.findById(id)).willThrow(new IllegalArgumentException("Opportunity not found"));

        // when & then
        assertThatThrownBy(() -> opportunityMcpTool.getOpportunity(Map.of("opportunityId", id.toString())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // TC-MCP-OPP-003: search_opportunities
    @Test
    @DisplayName("키워드USFK_limit5_최대5건검색결과")
    void 키워드검색_searchOpportunities_결과반환() {
        // given
        Opportunity opp = createTestOpportunity(UUID.randomUUID());
        given(opportunityService.searchByKeyword(eq("USFK"), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(opp)));

        // when
        String result = opportunityMcpTool.searchOpportunities(Map.of("keyword", "USFK", "limit", 5));

        // then
        assertThat(result).contains("USFK");
        assertThat(result).contains("totalFound");
    }

    // TC-MCP-OPP-004: get_opportunity_requirements
    @Test
    @DisplayName("유효opportunityId_requirements_요구사항목록반환")
    void 유효ID_getRequirements_목록반환() {
        // given
        UUID oppId = UUID.randomUUID();
        OpportunityRequirementItem item = TestHelper.withId(OpportunityRequirementItem.builder()
                .opportunity(createTestOpportunity(oppId))
                .category(RequirementCategory.DOCUMENT)
                .title("Submit proposal")
                .description("Full technical proposal required")
                .isBlocker(true)
                .requirementJson(Map.of())
                .build());
        given(requirementItemRepository.findByOpportunityId(oppId)).willReturn(List.of(item));

        // when
        String result = opportunityMcpTool.getOpportunityRequirements(Map.of("opportunityId", oppId.toString()));

        // then
        assertThat(result).contains("Submit proposal");
        assertThat(result).contains("DOCUMENT");
        assertThat(result).contains("\"count\" : 1");
    }
}
