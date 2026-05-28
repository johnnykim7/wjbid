package com.biddingagency.domain.rfp.dto;

import com.biddingagency.domain.rfp.entity.SlotDefinition;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 한 슬롯의 배치 현황 (CR-013). 빈 슬롯이면 assignments가 비고 empty=true →
 * 시스템이 "이 데이터 어디 있어? 넣어줘" 역요구.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SlotStatusDto(
        String slotCode,
        String slotLabel,
        int displayOrder,
        boolean isOther,
        boolean empty,
        List<SlotAssignmentDto> assignments
) {
    public static SlotStatusDto of(SlotDefinition slot, List<SlotAssignmentDto> assignments) {
        return new SlotStatusDto(
                slot.getSlotCode(),
                slot.getLabelKo(),
                slot.getDisplayOrder(),
                slot.isOther(),
                assignments.isEmpty(),
                assignments
        );
    }
}
