package com.biddingagency.domain.compliance;

import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * DOCS_PENDING → DOCS_RECEIVED 전이 시 BLOCKER 슬롯이 미충족인 경우 (CR-010, BIZ-015).
 * HTTP 409로 매핑된다.
 */
@Getter
public class RequirementSlotsNotFulfilledException extends RuntimeException {

    private final List<Map<String, Object>> unfulfilledSlots;

    public RequirementSlotsNotFulfilledException(List<OpportunityRequirementItem> unfulfilled) {
        super("Required document slots not fulfilled: " + unfulfilled.size());
        this.unfulfilledSlots = unfulfilled.stream()
                .map(r -> Map.<String, Object>of(
                        "requirementItemId", r.getId(),
                        "title", r.getTitle()))
                .toList();
    }

    public String getErrorCode() {
        return "REQUIREMENT_SLOTS_NOT_FULFILLED";
    }
}
