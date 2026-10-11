package com.biddingagency.domain.event;

import com.biddingagency.domain.federal.entity.ContractDateKind;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 회원 계약의 지금 끝날짜나 옵션 끝날짜가 알림 시점에 닿음 (CR-142, BID-MKT-033).
 * 매일 확인이 회원 계약·날짜 종류·시점마다 한 건씩 낸다. 알림 모듈이 회원 메일로 보낸다.
 */
public class ContractEndApproachingEvent extends DomainEvent {

    private final UUID memberId;
    private final String piid;
    private final String description;
    private final ContractDateKind dateKind;
    private final LocalDate targetDate;
    private final int daysRemaining;

    public ContractEndApproachingEvent(UUID memberContractId, UUID memberId, String piid, String description,
                                       ContractDateKind dateKind, LocalDate targetDate, int daysRemaining) {
        super("ContractEndApproaching", memberContractId, "MemberContract", null);
        this.memberId = memberId;
        this.piid = piid;
        this.description = description;
        this.dateKind = dateKind;
        this.targetDate = targetDate;
        this.daysRemaining = daysRemaining;
    }

    public UUID getMemberId() { return memberId; }
    public String getPiid() { return piid; }
    public String getDescription() { return description; }
    public ContractDateKind getDateKind() { return dateKind; }
    public LocalDate getTargetDate() { return targetDate; }
    public int getDaysRemaining() { return daysRemaining; }
}
