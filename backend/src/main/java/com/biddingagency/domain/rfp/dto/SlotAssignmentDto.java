package com.biddingagency.domain.rfp.dto;

import com.biddingagency.domain.rfp.entity.SlotAssignment;
import com.fasterxml.jackson.annotation.JsonInclude;

/** 슬롯 배치 DTO (CR-013) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SlotAssignmentDto(
        String id,
        String slotCode,
        String slotLabel,
        String sampleFileId,
        String fileName,
        String otherLabel,
        String sectionText,
        boolean confirmed,
        boolean autoEstimated
) {
    public static SlotAssignmentDto from(SlotAssignment a) {
        return new SlotAssignmentDto(
                a.getId().toString(),
                a.getSlotDefinition().getSlotCode(),
                a.getSlotDefinition().getLabelKo(),
                a.getSampleFile() != null ? a.getSampleFile().getId().toString() : null,
                a.getSampleFile() != null ? a.getSampleFile().getFileName() : null,
                a.getOtherLabel(),
                a.getSectionText(),
                a.isConfirmed(),
                a.isAutoEstimated()
        );
    }
}
