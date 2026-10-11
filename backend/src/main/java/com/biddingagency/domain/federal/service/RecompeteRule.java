package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.federal.entity.ContractDateKind;
import com.biddingagency.domain.federal.entity.FederalContract;
import com.biddingagency.domain.federal.entity.RecompeteCategory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 계약 구분 규칙 (CR-142, BIZ-050, BIZ-051). 화면이 밝히는 규칙과 같다.
 * 처음 몇 달은 실제 공고와 대조해 규칙을 고치므로 규칙은 이 클래스 한 곳에만 둔다.
 */
public final class RecompeteRule {

    /** 고른 달에 걸린 구분과 근거 날짜 */
    public record Verdict(RecompeteCategory category, ContractDateKind basisDateKind, LocalDate basisDate) {
    }

    private RecompeteRule() {
    }

    /**
     * 고른 달 기준 구분. 위에서부터 처음 맞는 것이다. 그 달에 걸리지 않으면 비어 있다.
     * ① 주문·BPA 주문이고 지금 끝날짜가 그 달이면 상위 계약 안 주문
     * ② 구매 주문·단독 계약인데 옵션 끝날짜가 없고 지금 끝날짜가 그 달이면 옵션 끝날짜 확인 전
     * ③ 옵션 끝날짜가 그 달이면 재발주 후보
     * ④ 지금 끝날짜가 그 달이고 옵션 끝날짜가 그 달보다 뒤면 연장 가능
     * 상세를 다시 받으라고 표시된 계약도 저장된 옵션 끝날짜가 있으면 그 값으로 낸다.
     */
    public static Optional<Verdict> classify(FederalContract c, YearMonth month) {
        LocalDate end = c.getEndDate();
        LocalDate potential = c.getPotentialEndDate();
        boolean endInMonth = end != null && YearMonth.from(end).equals(month);
        if (c.getAwardType().isOrder()) {
            return endInMonth
                    ? Optional.of(new Verdict(RecompeteCategory.IN_PARENT, ContractDateKind.CURRENT_END, end))
                    : Optional.empty();
        }
        if (potential == null) {
            return endInMonth
                    ? Optional.of(new Verdict(RecompeteCategory.UNCONFIRMED, ContractDateKind.CURRENT_END, end))
                    : Optional.empty();
        }
        if (YearMonth.from(potential).equals(month)) {
            return Optional.of(new Verdict(RecompeteCategory.RECOMPETE, ContractDateKind.POTENTIAL_END, potential));
        }
        if (endInMonth && YearMonth.from(potential).isAfter(month)) {
            return Optional.of(new Verdict(RecompeteCategory.EXTENDABLE, ContractDateKind.CURRENT_END, end));
        }
        return Optional.empty();
    }

    /** 재발주 후보의 업종 코드가 앞자리 중 하나로 시작하면 한 번짜리 공사 꼬리표(BIZ-051) */
    public static boolean oneTimeConstruction(RecompeteCategory category, String naicsCode, List<String> prefixes) {
        if (category != RecompeteCategory.RECOMPETE || naicsCode == null) return false;
        return prefixes.stream().anyMatch(naicsCode::startsWith);
    }
}
