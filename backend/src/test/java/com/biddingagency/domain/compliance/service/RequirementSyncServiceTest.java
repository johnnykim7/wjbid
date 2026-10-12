package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.entity.RequirementLevel;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
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
import java.util.Optional;
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
 * TC-COMP-001 ~ TC-COMP-008: 컴플라이언스 매트릭스 줄 맞추기 (CR-159, BIZ-045, BIZ-074, BIZ-075)
 */
@ExtendWith(MockitoExtension.class)
class RequirementSyncServiceTest {

    @Mock private OpportunityRequirementItemRepository requirementRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private OpportunityRepository opportunityRepository;

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

    // ===== CR-159 컴플라이언스 매트릭스 =====

    private static final String QC = "The offeror shall describe its quality control plan.";
    private static final String PP = "The offeror shall provide three past performance references.";
    private static final String PAGES = "Proposals should not exceed 20 pages.";
    private static final String STAFF = "The offeror shall demonstrate key personnel qualifications.";

    private static String line(String clause, String text, String level, String category, String party) {
        return """
                {"text":"%s","clause":"%s","level":"%s","verb":"describe","category":"%s","fulfillmentParty":"%s"}
                """.formatted(text, clause, level, category, party);
    }

    private static String matrix(String... lines) {
        return "{\"lines\":[" + String.join(",", lines) + "]}";
    }

    private static final String MATRIX_3 = matrix(
            line("L.4.1", QC, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"),
            line("L.4.2", PP, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"),
            line("L.3", PAGES, "SHOULD", "FORMAT", "PLATFORM_GENERATED"));

    private Notice matrixNotice(String requiredDocumentsJson, String matrixJson) {
        Notice notice = Notice.builder().opportunity(opportunity)
                .requiredDocumentsJsonRaw(requiredDocumentsJson).complianceMatrixJsonRaw(matrixJson).build();
        ReflectionTestUtils.setField(notice, "id", UUID.randomUUID());
        return notice;
    }

    private OpportunityRequirementItem matrixRow(String reqId, String clause, String text) {
        Map<String, Object> json = new HashMap<>();
        json.put(RequirementSyncService.KEY_SOURCE_KEY, RequirementSyncService.matrixKey(clause, text));
        json.put(RequirementSyncService.KEY_TEXT, text);
        OpportunityRequirementItem item = OpportunityRequirementItem.builder()
                .opportunity(opportunity)
                .category(RequirementCategory.TECHNICAL)
                .title(text)
                .requirementJson(json)
                .reqId(reqId)
                .reqLevel(RequirementLevel.SHALL)
                .sortOrder(Integer.parseInt(reqId.substring(4)))
                .build();
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        return item;
    }

    private void givenNumbering(int lastReqNo) {
        ReflectionTestUtils.setField(opportunity, "lastReqNo", lastReqNo);
        given(opportunityRepository.findByIdForUpdate(opportunityId)).willReturn(Optional.of(opportunity));
    }

    @Test
    @DisplayName("TC-COMP-001 첫분석_매트릭스3줄_REQ001부터")
    void 첫분석_매트릭스3줄_REQ001부터() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());
        givenNumbering(0);

        syncService.syncFromNotice(matrixNotice(null, MATRIX_3));

        assertThat(savedItems()).extracting(OpportunityRequirementItem::getReqId)
                .containsExactly("REQ-001", "REQ-002", "REQ-003");
        assertThat(opportunity.getLastReqNo()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-COMP-002 다시분석_같은원문_번호와매핑유지")
    void 다시분석_같은원문_번호와매핑유지() {
        List<OpportunityRequirementItem> before = List.of(
                matrixRow("REQ-001", "L.4.1", QC), matrixRow("REQ-002", "L.4.2", PP), matrixRow("REQ-003", "L.3", PAGES));
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(before);

        syncService.syncFromNotice(matrixNotice(null, MATRIX_3));

        assertThat(savedItems()).isEmpty();
        then(fulfillmentMapRepository).should(never()).deleteByRequirementItemIds(anyCollection());
        assertThat(before).extracting(OpportunityRequirementItem::getReqId).containsExactly("REQ-001", "REQ-002", "REQ-003");
    }

    @Test
    @DisplayName("TC-COMP-003 다시분석_새줄_마지막번호다음")
    void 다시분석_새줄_마지막번호다음() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of(
                matrixRow("REQ-001", "L.4.1", QC), matrixRow("REQ-002", "L.4.2", PP), matrixRow("REQ-003", "L.3", PAGES)));
        givenNumbering(3);

        syncService.syncFromNotice(matrixNotice(null, matrix(
                line("L.4.1", QC, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"),
                line("L.4.2", PP, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"),
                line("L.3", PAGES, "SHOULD", "FORMAT", "PLATFORM_GENERATED"),
                line("L.4.3", STAFF, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"))));

        assertThat(savedItems()).extracting(OpportunityRequirementItem::getReqId).containsExactly("REQ-004");
        assertThat(opportunity.getLastReqNo()).isEqualTo(4);
    }

    @Test
    @DisplayName("TC-COMP-004 사라진줄_매핑과지움_번호다시안씀")
    void 사라진줄_매핑과지움_번호다시안씀() {
        OpportunityRequirementItem gone = matrixRow("REQ-003", "L.3", PAGES);
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of(
                matrixRow("REQ-001", "L.4.1", QC), matrixRow("REQ-002", "L.4.2", PP), gone));
        givenNumbering(3);

        syncService.syncFromNotice(matrixNotice(null, matrix(
                line("L.4.1", QC, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"),
                line("L.4.2", PP, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"),
                line("L.4.3", STAFF, "SHALL", "TECHNICAL", "PLATFORM_GENERATED"))));

        then(fulfillmentMapRepository).should().deleteByRequirementItemIds(List.of(gone.getId()));
        assertThat(savedItems()).extracting(OpportunityRequirementItem::getReqId).containsExactly("REQ-004");
    }

    @Test
    @DisplayName("TC-COMP-005 매트릭스줄_Shall고객업로드_막는항목아님")
    void 매트릭스줄_Shall고객업로드_막는항목아님() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());
        givenNumbering(0);

        syncService.syncFromNotice(matrixNotice(null, matrix(
                line("L.5", "The offeror shall submit a current business license.", "SHALL", "ELIGIBILITY", "CLIENT_UPLOAD"))));

        assertThat(savedItems()).extracting(OpportunityRequirementItem::getIsBlocker).containsExactly(false);
    }

    @Test
    @DisplayName("TC-COMP-006 매트릭스줄_분류DOCUMENT_OTHER로")
    void 매트릭스줄_분류DOCUMENT_OTHER로() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());
        givenNumbering(0);

        syncService.syncFromNotice(matrixNotice(null, matrix(
                line("L.5", "The offeror shall submit a current business license.", "SHALL", "DOCUMENT", "CLIENT_UPLOAD"),
                line("L.6", "The offeror shall be registered in SAM.", "SHALL", "UNKNOWN", "CLIENT_UPLOAD"))));

        assertThat(savedItems()).extracting(OpportunityRequirementItem::getCategory)
                .containsExactly(RequirementCategory.OTHER, RequirementCategory.OTHER);
    }

    @Test
    @DisplayName("TC-COMP-007 제출서류와매트릭스_같이맞춤_서로안지움")
    void 제출서류와매트릭스_같이맞춤_서로안지움() {
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of());
        givenNumbering(0);

        syncService.syncFromNotice(matrixNotice("""
                {"documents":[{"name":"Business License","mandatory":true},{"name":"Insurance","mandatory":false}]}
                """, MATRIX_3));

        assertThat(savedItems()).extracting(OpportunityRequirementItem::getReqId)
                .containsExactly(null, null, "REQ-001", "REQ-002", "REQ-003");
        then(fulfillmentMapRepository).should(never()).deleteByRequirementItemIds(anyCollection());
    }

    @Test
    @DisplayName("TC-COMP-008 매트릭스없는분석_제출서류줄만")
    void 매트릭스없는분석_제출서류줄만() {
        OpportunityRequirementItem license = existing("Business License", null);
        OpportunityRequirementItem req1 = matrixRow("REQ-001", "L.4.1", QC);
        OpportunityRequirementItem req2 = matrixRow("REQ-002", "L.4.2", PP);
        given(requirementRepository.findByOpportunityIdOrderBySortOrderAsc(opportunityId)).willReturn(List.of(license, req1, req2));

        syncService.syncFromNotice(matrixNotice("""
                {"documents":[{"name":"Business License","mandatory":true}]}
                """, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<UUID>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        then(fulfillmentMapRepository).should().deleteByRequirementItemIds(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(req1.getId(), req2.getId());
    }
}
