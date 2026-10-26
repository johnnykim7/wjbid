// 마감일 D-day 뱃지 헬퍼, 공고 테이블 공통.
// 마감 카운트다운(D-N) 기준: 오늘 마감=D-DAY, 미래=D-N, 지난 마감=마감.
// 지난 마감은 회색, 임박(7일 이하)은 빨강, 그 외 파랑.

import { ToneBadge } from '../components/StateBadge'

/** 마감일까지 남은 일수(자정 기준). null이면 마감일 없음. */
export function daysUntil(deadline?: string | null): number | null {
  if (!deadline) return null
  const d = new Date(deadline)
  if (Number.isNaN(d.getTime())) return null
  const today = new Date()
  // 날짜만 비교(시각 절삭)
  const dDay = Date.UTC(d.getFullYear(), d.getMonth(), d.getDate())
  const t0 = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate())
  return Math.round((dDay - t0) / 86400000)
}

/** D-day 라벨: D-DAY / D-3 / 마감 */
export function ddayLabel(days: number | null): string | null {
  if (days === null) return null
  if (days < 0) return '마감'
  if (days === 0) return 'D-DAY'
  return `D-${days}`
}

/** 마감일 옆에 붙이는 작은 D-day 뱃지. 마감일이 없으면 null. */
export function DdayBadge({ deadline }: { deadline?: string | null }) {
  const days = daysUntil(deadline)
  const label = ddayLabel(days)
  if (days === null || label === null) return null

  return <ToneBadge size="sm" tone={days < 0 ? 'gray' : days <= 7 ? 'destructive' : 'info'}>{label}</ToneBadge>
}
