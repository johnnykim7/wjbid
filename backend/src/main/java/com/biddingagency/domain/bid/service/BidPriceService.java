package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidPriceInput;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidPriceInputRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.proposal.service.DocumentCompositionService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 가격 표 (CR-194, BIZ-108). 줄은 공고 분석의 가격 항목이고 단가는 고객이 넣는다. 금액, 소계, 합계는 읽을 때 서버가 계산한다.
 */
@Service
@RequiredArgsConstructor
public class BidPriceService {

    public static final String NOT_READY = "NOT_READY";
    public static final String NOT_APPLICABLE = "NOT_APPLICABLE";
    public static final String INCOMPLETE = "INCOMPLETE";
    public static final String COMPLETE = "COMPLETE";

    private static final Set<BidRequestState> LOCKED = EnumSet.of(
            BidRequestState.CONFIRMED, BidRequestState.SUBMITTED,
            BidRequestState.AWARDED, BidRequestState.NOT_AWARDED, BidRequestState.CLOSED);
    private static final int MAX_SCALE = 6;
    private static final int MAX_INTEGER_DIGITS = 14;
    private static final String[] EXPORT_COLUMNS = {"CLIN", "품목", "수량", "단위", "단가", "금액"};

    private final BidRequestService bidRequestService;
    private final BidPriceInputRepository inputRepository;
    private final DocumentCompositionService documentCompositionService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PriceTable customerTable(UUID bidRequestId, Member viewer) {
        BidRequest bidRequest = bidRequestService.findAccessible(bidRequestId, viewer);
        return build(bidRequest, !LOCKED.contains(bidRequest.getState()));
    }

    @Transactional(readOnly = true)
    public PriceTable adminTable(UUID bidRequestId) {
        return build(bidRequestService.findByIdWithDetails(bidRequestId), false);
    }

    @Transactional
    public PriceTable save(UUID bidRequestId, Member viewer, String updatedBy, List<UnitPrice> unitPrices) {
        BidRequest bidRequest = bidRequestService.findAccessible(bidRequestId, viewer);
        if (LOCKED.contains(bidRequest.getState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "확정된 건의 단가는 바꿀 수 없습니다");
        }
        Set<String> known = new java.util.HashSet<>();
        for (Item item : items(bidRequest)) {
            known.add(key(item.clin(), item.period()));
        }
        Map<String, BidPriceInput> existing = new HashMap<>();
        for (BidPriceInput input : inputRepository.findByBidRequestId(bidRequestId)) {
            existing.put(key(input.getClin(), input.getPeriod()), input);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (UnitPrice unitPrice : unitPrices) {
            String period = unitPrice.period() == null ? "" : unitPrice.period();
            String key = key(unitPrice.clin(), period);
            if (!known.contains(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공고 가격 항목에 없는 줄입니다: " + unitPrice.clin());
            }
            BidPriceInput current = existing.get(key);
            if (unitPrice.unitPrice() == null) {
                if (current != null) {
                    inputRepository.delete(current);
                    existing.remove(key);
                }
                continue;
            }
            requireStorable(unitPrice);
            if (current != null) {
                current.changePrice(unitPrice.unitPrice(), updatedBy, now);
            } else {
                existing.put(key, inputRepository.save(BidPriceInput.builder()
                        .bidRequestId(bidRequestId)
                        .clin(unitPrice.clin())
                        .period(period)
                        .unitPrice(unitPrice.unitPrice())
                        .updatedBy(updatedBy)
                        .updatedAt(now)
                        .build()));
            }
        }
        inputRepository.flush();
        return build(bidRequest, true);
    }

    @Transactional(readOnly = true)
    public byte[] customerExport(UUID bidRequestId, Member viewer) {
        return export(customerTable(bidRequestId, viewer));
    }

    @Transactional(readOnly = true)
    public byte[] adminExport(UUID bidRequestId) {
        return export(adminTable(bidRequestId));
    }

    private void requireStorable(UnitPrice unitPrice) {
        BigDecimal value = unitPrice.unitPrice();
        if (value.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단가는 음수일 수 없습니다: " + unitPrice.clin());
        }
        // 저장할 때 잘리면 금액이 달라지므로 받지 않는다
        if (value.stripTrailingZeros().scale() > MAX_SCALE || value.precision() - value.scale() > MAX_INTEGER_DIGITS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단가의 자릿수가 너무 많습니다: " + unitPrice.clin());
        }
    }

    private PriceTable build(BidRequest bidRequest, boolean editable) {
        Map<String, Object> json = priceJson(bidRequest);
        if (json == null || !(json.get("items") instanceof List<?> rawItems)) {
            return new PriceTable(NOT_READY, editable, List.of(), BigDecimal.ZERO);
        }
        if (rawItems.isEmpty()) {
            return new PriceTable(NOT_APPLICABLE, editable, List.of(), BigDecimal.ZERO);
        }
        Map<String, BigDecimal> prices = new HashMap<>();
        for (BidPriceInput input : inputRepository.findByBidRequestId(bidRequest.getId())) {
            prices.put(key(input.getClin(), input.getPeriod()), input.getUnitPrice());
        }
        Map<String, List<Line>> byPeriod = new LinkedHashMap<>();
        boolean complete = true;
        for (Item item : toItems(rawItems)) {
            BigDecimal unitPrice = prices.get(key(item.clin(), item.period()));
            BigDecimal amount = item.quantity() != null && unitPrice != null ? item.quantity().multiply(unitPrice) : null;
            complete &= amount != null;
            byPeriod.computeIfAbsent(item.period(), p -> new ArrayList<>()).add(new Line(
                    item.clin(), item.description(), item.quantity(), item.unit(), item.sourceRef(),
                    plain(unitPrice), plain(amount)));
        }
        List<Period> periods = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, List<Line>> entry : byPeriod.entrySet()) {
            BigDecimal subtotal = entry.getValue().stream()
                    .map(Line::amount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(subtotal);
            periods.add(new Period(entry.getKey(), entry.getValue(), plain(subtotal)));
        }
        return new PriceTable(complete ? COMPLETE : INCOMPLETE, editable, periods, plain(total));
    }

    private Map<String, Object> priceJson(BidRequest bidRequest) {
        return documentCompositionService.designNotice(bidRequest.getOpportunity().getId())
                .map(Notice::getPriceItemsJson)
                .orElse(null);
    }

    private List<Item> items(BidRequest bidRequest) {
        Map<String, Object> json = priceJson(bidRequest);
        return json != null && json.get("items") instanceof List<?> rawItems ? toItems(rawItems) : List.of();
    }

    private static List<Item> toItems(List<?> rawItems) {
        List<Item> items = new ArrayList<>();
        for (Object raw : rawItems) {
            if (raw instanceof Map<?, ?> map) {
                String period = str(map.get("period"));
                items.add(new Item(str(map.get("clin")), str(map.get("description")), decimal(map.get("quantity")),
                        str(map.get("unit")), period == null ? "" : period, str(map.get("sourceRef"))));
            }
        }
        return items;
    }

    private static byte[] export(PriceTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Price");
            int rowNo = 0;
            for (Period period : table.periods()) {
                sheet.createRow(rowNo++).createCell(0).setCellValue(period.period());
                Row header = sheet.createRow(rowNo++);
                for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                    header.createCell(i).setCellValue(EXPORT_COLUMNS[i]);
                }
                for (Line line : period.lines()) {
                    Row row = sheet.createRow(rowNo++);
                    row.createCell(0).setCellValue(line.clin());
                    row.createCell(1).setCellValue(line.description());
                    number(row, 2, line.quantity());
                    row.createCell(3).setCellValue(line.unit());
                    number(row, 4, line.unitPrice());
                    number(row, 5, line.amount());
                }
                Row subtotal = sheet.createRow(rowNo++);
                subtotal.createCell(4).setCellValue("소계");
                number(subtotal, 5, period.subtotal());
                rowNo++;
            }
            Row total = sheet.createRow(rowNo);
            total.createCell(4).setCellValue("합계");
            number(total, 5, table.total());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("가격 표 엑셀을 만들지 못했습니다", e);
        }
    }

    private static void number(Row row, int column, BigDecimal value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        }
    }

    private static String key(String clin, String period) {
        return clin + "\u0001" + period;
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal plain(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    private record Item(String clin, String description, BigDecimal quantity, String unit, String period,
                        String sourceRef) {
    }

    public record UnitPrice(String clin, String period, BigDecimal unitPrice) {
    }

    public record PriceTable(String status, boolean editable, List<Period> periods, BigDecimal total) {

        public long lineCount() {
            return periods.stream().mapToLong(p -> p.lines().size()).sum();
        }

        public long filledCount() {
            return periods.stream().flatMap(p -> p.lines().stream()).filter(l -> l.amount() != null).count();
        }
    }

    public record Period(String period, List<Line> lines, BigDecimal subtotal) {
    }

    public record Line(String clin, String description, BigDecimal quantity, String unit, String sourceRef,
                       BigDecimal unitPrice, BigDecimal amount) {
    }
}
