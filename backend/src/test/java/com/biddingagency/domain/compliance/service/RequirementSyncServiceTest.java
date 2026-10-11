package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TC-PRE-001 ~ TC-PRE-006: 공고 분석의 제출 서류로 요구 항목 맞추기 (CR-141, BIZ-045, BIZ-046)
 */
@ExtendWith(MockitoExtension.class)
class RequirementSyncServiceTest {

    @Mock private OpportunityRequirementItemRepository requirementRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;

    @InjectMocks private RequirementSyncService syncService;

    private final UUID opportunityId = UUID.randomUUID();
    private final Opportunity opportunity = opportunity();

    private Opportunity opportunity() {
        Opportunity opp = Opportunity.builder().build();
        ReflectionTestUtils.setField(opp, "id", opportunityId);
        return opp;
    }

    private Notice notice(String requiredDocumentsJson) {
        Notice notice = Notice.builder().opportunity(opportunity).requiredDocumentsJsonRaw(requiredDocumentsJson).build();
        ReflectionTestUtils.setField(notice, "id", UUID.randomUUID());
        return notice;
    }

    private OpportunityRequirementItem existing(String sourceKey, String description) {
        Map<String, Object> json = new HashMap<>();
        json.put(RequirementSyncService.KEY_SOURCE_KEY, sourceKey);
        OpportunityRequirementItem item = OpportunityRequirementItem.builder()
                .opportunity(opportunity)
                .category(RequirementCategory.DOCUMENT)
                .title(sourceKey)
                .description(description)
                .requirementJson(json)
                .build();
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        return item;
    }

    @SuppressWarnings("unchecked")
    private List<OpportunityRequirementItem> savedItems() {
        ArgumentCaptor<List<OpportunityRequirementItem>> captor = ArgumentCaptor.forClass(List.class);
        then(requirementRepository).should().saveAll(captor.capture());
        return captor.getValue();
    }

    private static final String FACTORS_JSON = """
            {"factors":[
              {"factorId":"F1","factorTitle":"Volume I","subfactors":[
                {"subfactorId":"F1.1","name":"Business License","fulfillmentParty":"CLIENT_UPLOAD","mandatory":true,
                 "format":"PDF","pageLimit":"1 page","sourceRef":"L.4.1"},
                {"subfactorId":"F1.2","name":"Past Performance","fulfillmentParty":"PLATFORM_GENERATED","mandatory":true}
              ]},
              {"factorId":"F2","factorTitle":"Volume II","subfactors":[
                {"subfactorId":"F2.1","name":"Insurance Certificate","fulfillmentParty":"CLIENT_UPLOAD","mandatory":false}
              ]}
            ]}
            """;

    @Test
    @DisplayName("TC-PRE-001 제출물있음_채움_제출물마다한항목")
    void 제출물있음_채움_제출물마다한항목() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());

        syncService.syncFromNotice(notice(FACTORS_JSON));

        assertThat(savedItems()).hasSize(3);
    }

    @Test
    @DisplayName("TC-PRE-001 제출물있음_채움_고객올림필수만막음")
    void 제출물있음_채움_고객올림필수만막음() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());

        syncService.syncFromNotice(notice(FACTORS_JSON));

        assertThat(savedItems()).extracting(OpportunityRequirementItem::getTitle, OpportunityRequirementItem::getIsBlocker)
                .containsExactly(
                        tuple("Business License", true),
                        tuple("Past Performance", false),
                        tuple("Insurance Certificate", false));
    }

    @Test
    @DisplayName("TC-PRE-001 제출물있음_채움_조건과근거를남김")
    void 제출물있음_채움_조건과근거를남김() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());

        syncService.syncFromNotice(notice(FACTORS_JSON));

        assertThat(savedItems().get(0).getRequirementJson())
                .containsEntry(RequirementSyncService.KEY_FULFILLMENT_PARTY, "CLIENT_UPLOAD")
                .containsEntry(RequirementSyncService.KEY_FORMAT, "PDF")
                .containsEntry(RequirementSyncService.KEY_PAGE_LIMIT, "1 page")
                .containsEntry(RequirementSyncService.KEY_SOURCE_REF, "L.4.1");
    }

    @Test
    @DisplayName("TC-PRE-002 제출물없음_채움_평면목록을고객올림으로")
    void 제출물없음_채움_평면목록을고객올림으로() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());

        syncService.syncFromNotice(notice("""
                {"documents":[{"name":"Business License","mandatory":true},{"name":"Resume"}]}
                """));

        assertThat(savedItems()).extracting(ri -> ri.getRequirementJson().get(RequirementSyncService.KEY_FULFILLMENT_PARTY))
                .containsExactly("CLIENT_UPLOAD", "CLIENT_UPLOAD");
    }

    @Test
    @DisplayName("TC-PRE-003 다시맞춤_같은열쇠_고쳐쓰고연결유지")
    void 다시맞춤_같은열쇠_고쳐쓰고연결유지() {
        OpportunityRequirementItem before = existing("F1.1", "old");
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of(before));

        syncService.syncFromNotice(notice("""
                {"factors":[{"factorId":"F1","subfactors":[
                  {"subfactorId":"F1.1","name":"Business License","description":"new","fulfillmentParty":"CLIENT_UPLOAD"}
                ]}]}
                """));

        assertThat(before.getDescription()).isEqualTo("new");
        assertThat(savedItems()).isEmpty();
        then(fulfillmentMapRepository).should(never()).deleteByRequirementItemIds(anyCollection());
    }

    @Test
    @DisplayName("TC-PRE-004 다시맞춤_사라진열쇠_항목과연결지움")
    void 다시맞춤_사라진열쇠_항목과연결지움() {
        OpportunityRequirementItem gone = existing("OLD", "gone");
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of(gone));

        syncService.syncFromNotice(notice("""
                {"documents":[{"name":"Resume"}]}
                """));

        then(fulfillmentMapRepository).should().deleteByRequirementItemIds(List.of(gone.getId()));
        then(requirementRepository).should().deleteAll(List.of(gone));
    }

    @Test
    @DisplayName("TC-PRE-005 열쇠겹침_채움_순번붙여둘다남김")
    void 열쇠겹침_채움_순번붙여둘다남김() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());

        syncService.syncFromNotice(notice("""
                {"documents":[{"name":"Resume"},{"name":"Resume"}]}
                """));

        assertThat(savedItems()).extracting(ri -> ri.getRequirementJson().get(RequirementSyncService.KEY_SOURCE_KEY))
                .containsExactly("Resume", "Resume#2");
    }

    @Test
    @DisplayName("TC-PRE-006 제출서류없음_채움_항목없음")
    void 제출서류없음_채움_항목없음() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());

        assertThatCode(() -> syncService.syncFromNotice(notice(null))).doesNotThrowAnyException();
        assertThat(savedItems()).isEmpty();
    }
}
