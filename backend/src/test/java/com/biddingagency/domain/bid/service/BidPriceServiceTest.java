package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidPriceInput;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.BidRequestNotFoundException;
import com.biddingagency.domain.bid.repository.BidPriceInputRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.proposal.service.DocumentCompositionService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** TC-DOC-316 ~ 324: 가격 표 (CR-194, BID-DOC-008, BIZ-108) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BidPriceServiceTest {

    private static final String BASE = "Base Period";
    private static final String OPTION = "Option Period 1";

    @Mock private BidRequestService bidRequestService;
    @Mock private BidPriceInputRepository inputRepository;
    @Mock private DocumentCompositionService documentCompositionService;
    @Mock private Notice notice;
    @Mock private Member viewer;

    private BidPriceService service;
    private BidRequest bidRequest;
    private UUID bidRequestId;
    private final List<BidPriceInput> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new BidPriceService(bidRequestService, inputRepository, documentCompositionService, Clock.systemDefaultZone());
        Opportunity opportunity = Opportunity.builder().build();
        ReflectionTestUtils.setField(opportunity, "id", UUID.randomUUID());
        bidRequest = BidRequest.builder().opportunity(opportunity).build();
        bidRequestId = UUID.randomUUID();
        ReflectionTestUtils.setField(bidRequest, "id", bidRequestId);
        ReflectionTestUtils.setField(bidRequest, "state", BidRequestState.REVIEW);
        given(bidRequestService.findAccessible(bidRequestId, viewer)).willReturn(bidRequest);
        given(bidRequestService.findByIdWithDetails(bidRequestId)).willReturn(bidRequest);
        given(documentCompositionService.designNotice(opportunity.getId())).willReturn(Optional.of(notice));
        given(inputRepository.findByBidRequestId(bidRequestId)).willReturn(saved);
    }

    private static Map<String, Object> item(String clin, Object quantity, String period) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("clin", clin);
        item.put("description", "Grass cutting " + clin);
        item.put("quantity", quantity);
        item.put("unit", "Acre");
        item.put("period", period);
        item.put("sourceRef", "p.7");
        return item;
    }

    private void givenItems(Map<?, ?>... items) {
        given(notice.getPriceItemsJson()).willReturn(Map.of("items", List.of(items)));
    }

    private void givenPrice(String clin, String period, String unitPrice) {
        saved.add(BidPriceInput.builder().bidRequestId(bidRequestId).clin(clin).period(period)
                .unitPrice(new BigDecimal(unitPrice)).build());
    }

    private BidPriceService.PriceTable table() {
        return service.customerTable(bidRequestId, viewer);
    }

    @Test
    @DisplayName("TC-DOC-316 가격표_단가입력_금액은수량곱단가")
    void 금액은_수량곱하기단가_반올림없음() {
        givenItems(item("0001AA", 843.5, BASE));
        givenPrice("0001AA", BASE, "97043");

        BigDecimal amount = table().periods().get(0).lines().get(0).amount();

        assertThat(amount).isEqualByComparingTo("81855770.5");
    }

    @Test
    @DisplayName("TC-DOC-317 가격표_연차여럿_소계와합계")
    void 연차별_소계와_합계() {
        givenItems(item("0001", 2, BASE), item("0002", 3, BASE), item("1001", 4, OPTION));
        givenPrice("0001", BASE, "10");
        givenPrice("0002", BASE, "20");
        givenPrice("1001", OPTION, "5");

        BidPriceService.PriceTable table = table();

        assertThat(List.of(table.periods().get(0).subtotal().intValue(), table.periods().get(1).subtotal().intValue(), table.total().intValue()))
                .containsExactly(80, 20, 100);
    }

    @Test
    @DisplayName("TC-DOC-318 가격표_단가빠진줄_미완료")
    void 단가빠진줄_미완료_합계는넣은줄만() {
        givenItems(item("0001", 2, BASE), item("0002", 3, BASE));
        givenPrice("0001", BASE, "10");

        BidPriceService.PriceTable table = table();

        assertThat(table.status()).isEqualTo(BidPriceService.INCOMPLETE);
        assertThat(table.total()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("TC-DOC-319 가격표_항목칸비어있음_확인안됨")
    void 항목칸_비어있음() {
        given(notice.getPriceItemsJson()).willReturn(null);

        assertThat(table().status()).isEqualTo(BidPriceService.NOT_READY);
    }

    @Test
    @DisplayName("TC-DOC-320 가격표_빈목록_해당없음")
    void 빈목록() {
        givenItems();

        assertThat(table().status()).isEqualTo(BidPriceService.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("TC-DOC-321 가격표저장_확정뒤_거절")
    void 확정뒤_저장거절() {
        givenItems(item("0001", 2, BASE));
        ReflectionTestUtils.setField(bidRequest, "state", BidRequestState.CONFIRMED);

        assertThatThrownBy(() -> service.save(bidRequestId, viewer, "a@b.c",
                List.of(new BidPriceService.UnitPrice("0001", BASE, BigDecimal.TEN))))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        verify(inputRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-DOC-322 가격표저장_없는줄_거절")
    void 없는줄_거절() {
        givenItems(item("0001", 2, BASE));

        assertThatThrownBy(() -> service.save(bidRequestId, viewer, "a@b.c",
                List.of(new BidPriceService.UnitPrice("9999", BASE, BigDecimal.TEN))))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("TC-DOC-322 가격표저장_음수_거절")
    void 음수_거절() {
        givenItems(item("0001", 2, BASE));

        assertThatThrownBy(() -> service.save(bidRequestId, viewer, "a@b.c",
                List.of(new BidPriceService.UnitPrice("0001", BASE, new BigDecimal("-1")))))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("TC-DOC-323 가격표_엑셀내보내기_칸과합계")
    void 엑셀_칸과합계() throws Exception {
        givenItems(item("0001", 2, BASE), item("0002", 3, BASE));
        givenPrice("0001", BASE, "10");
        givenPrice("0002", BASE, "20");

        byte[] bytes = service.customerExport(bidRequestId, viewer);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(1);
            List<String> columns = new ArrayList<>();
            header.forEach(cell -> columns.add(cell.getStringCellValue()));
            assertThat(columns).containsExactly("CLIN", "품목", "수량", "단위", "단가", "금액");
            assertThat(sheet.getRow(sheet.getLastRowNum()).getCell(5).getNumericCellValue()).isEqualTo(80d);
        }
    }

    @Test
    @DisplayName("TC-DOC-324 가격표_남의건_404")
    void 남의건_거절() {
        UUID other = UUID.randomUUID();
        given(bidRequestService.findAccessible(other, viewer)).willThrow(new BidRequestNotFoundException());

        assertThatThrownBy(() -> service.customerTable(other, viewer)).isInstanceOf(BidRequestNotFoundException.class);
    }
}
