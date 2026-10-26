/**
 * CR-132: 1:1 문의 목록과 상세가 함께 쓰는 상태, 유형, 구분 표시.
 */
import { Check, Clock } from 'lucide-react'
import type { InquiryStatus } from '../api/client'
import { ToneBadge } from './StateBadge'

export function InquiryStatusPill({ status }: { status: InquiryStatus }) {
  return status === 'ANSWERED' ? (
    <ToneBadge tone="success">
      <Check className="size-3" />
      답변완료
    </ToneBadge>
  ) : (
    <ToneBadge tone="warning">
      <Clock className="size-3" />
      미답변
    </ToneBadge>
  )
}

export function InquiryTypePill({ label }: { label: string }) {
  return <ToneBadge tone="gray">{label}</ToneBadge>
}

export function InquiryMemberPill({ member }: { member: boolean }) {
  return member ? <ToneBadge tone="info">회원</ToneBadge> : <ToneBadge tone="gray">비회원</ToneBadge>
}
