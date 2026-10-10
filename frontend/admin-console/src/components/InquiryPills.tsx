/**
 * CR-132 — 1:1 문의 목록과 상세가 함께 쓰는 상태·유형·구분 표시.
 */
import { Badge } from '@therecommerce-develop/ds-ui'
import type { InquiryStatus } from '../api/client'

const PILL = 'rounded-full h-auto px-2.5 py-0.5 text-xs gap-1.5'

export function InquiryStatusPill({ status }: { status: InquiryStatus }) {
  return status === 'ANSWERED' ? (
    <Badge className={`${PILL} bg-emerald-100 text-emerald-700 font-semibold`}>
      <i className="fa-solid fa-check" />
      답변완료
    </Badge>
  ) : (
    <Badge className={`${PILL} bg-amber-100 text-amber-700 font-semibold`}>
      <i className="fa-regular fa-clock" />
      미답변
    </Badge>
  )
}

export function InquiryTypePill({ label }: { label: string }) {
  return <Badge className={`${PILL} bg-gray-100 text-gray-700 font-medium`}>{label}</Badge>
}

export function InquiryMemberPill({ member }: { member: boolean }) {
  return member ? (
    <Badge className={`${PILL} bg-blue-100 text-blue-700 font-medium`}>회원</Badge>
  ) : (
    <Badge className={`${PILL} bg-gray-100 text-gray-500 font-medium`}>비회원</Badge>
  )
}
