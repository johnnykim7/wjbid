package com.biddingagency.domain.eligibility.service;

import com.biddingagency.domain.eligibility.entity.EligibilityAnswerValue;
import com.biddingagency.domain.eligibility.entity.EligibilityBlockerProblem;
import com.biddingagency.domain.eligibility.entity.EligibilityVerdict;
import com.biddingagency.dto.EligibilityResponses.AttachedDocument;
import com.biddingagency.dto.EligibilityResponses.Blocker;
import com.biddingagency.dto.EligibilityResponses.Item;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 공고별 판정 (CR-140, BIZ-040, BIZ-044).
 *
 * 통과 조건 자격만 보고 위에서부터 처음 맞는 것으로 정한다. 없음 하나라도 있으면 어려움,
 * 확인 필요, 답 안 함, 다시 답해야 함이 있으면 판정 전, 모두 있음인데 증빙 서류가 없거나 만료면 서류 부족.
 */
final class EligibilityVerdictJudge {

    record Judgment(EligibilityVerdict verdict, List<Blocker> blockers) {
    }

    private EligibilityVerdictJudge() {
    }

    static Judgment judge(List<Item> items) {
        List<Item> gates = items.stream().filter(Item::gate).toList();
        if (gates.isEmpty()) {
            return new Judgment(EligibilityVerdict.UNDECIDED, List.of());
        }
        List<Blocker> lack = blockers(gates, i -> i.answer() == EligibilityAnswerValue.LACK
                ? blocker(i, EligibilityBlockerProblem.LACK) : null);
        if (!lack.isEmpty()) {
            return new Judgment(EligibilityVerdict.DIFFICULT, lack);
        }
        List<Blocker> open = blockers(gates, EligibilityVerdictJudge::openProblem);
        if (!open.isEmpty()) {
            return new Judgment(EligibilityVerdict.UNDECIDED, open);
        }
        List<Blocker> docs = blockers(gates, EligibilityVerdictJudge::documentProblem);
        if (!docs.isEmpty()) {
            return new Judgment(EligibilityVerdict.DOCS_MISSING, docs);
        }
        return new Judgment(EligibilityVerdict.ELIGIBLE, List.of());
    }

    private static Blocker openProblem(Item i) {
        if (i.stale()) {
            return blocker(i, EligibilityBlockerProblem.STALE);
        }
        if (i.answer() == null) {
            return blocker(i, EligibilityBlockerProblem.UNANSWERED);
        }
        return i.answer() == EligibilityAnswerValue.UNSURE ? blocker(i, EligibilityBlockerProblem.UNSURE) : null;
    }

    private static Blocker documentProblem(Item i) {
        AttachedDocument d = i.attachedDocument();
        if (d == null) {
            return blocker(i, EligibilityBlockerProblem.NO_DOCUMENT);
        }
        return d.expired() ? new Blocker(i.itemNo(), i.title(), i.sourceRef(), EligibilityBlockerProblem.EXPIRED,
                d.name(), d.expiresOn()) : null;
    }

    private static List<Blocker> blockers(List<Item> gates, Function<Item, Blocker> problem) {
        return gates.stream().map(problem).filter(Objects::nonNull).toList();
    }

    private static Blocker blocker(Item i, EligibilityBlockerProblem problem) {
        return new Blocker(i.itemNo(), i.title(), i.sourceRef(), problem, null, null);
    }
}
