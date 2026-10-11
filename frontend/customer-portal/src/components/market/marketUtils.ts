import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MARKET_COMMON } from '../../data/marketContent'
import type { MarketRow } from '../../types/market'

/** 메뉴 속 화면 2묶음의 날짜, 금액, 이동 (CR-135). 응답 날짜는 KST 다 */

const KST = 'Asia/Seoul'
const DAY_MS = 86_400_000

/** 'YYYY-MM-DD' 또는 KST 오프셋이 붙은 시각을 'YYYY.MM.DD' 로. 응답 날짜는 이미 KST 라 앞 열 자리만 쓴다 */
export function fmtDate(value: string | null | undefined): string {
  return value ? value.slice(0, 10).replaceAll('-', '.') : '-'
}

/** 'YYYY-MM' 의 달 숫자 */
export const monthNo = (ym: string) => Number(ym.slice(5, 7))

/** 달 이름. 자료가 두 해에 걸치면 해를 붙인다 */
export const monthLabel = (ym: string, withYear: boolean) => MARKET_COMMON.month(monthNo(ym), withYear ? ym.slice(0, 4) : undefined)

/** KST 오늘 'YYYY-MM-DD' */
export function todayKst(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: KST, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
}

const dayValue = (ymd: string) => Date.UTC(Number(ymd.slice(0, 4)), Number(ymd.slice(5, 7)) - 1, Number(ymd.slice(8, 10)))

export function money(value: number): string {
  return '$' + Math.round(value).toLocaleString('en-US')
}

/** 낙찰 공지의 금액 글줄("$60,700.00")을 숫자로. 읽을 수 없으면 null */
export function parseAmount(value: string | null): number | null {
  const digits = value?.replace(/[$,\s]/g, '')
  if (!digits) return null
  const n = Number(digits)
  return Number.isFinite(n) ? n : null
}

/** 두 날짜(앞 열 자리 'YYYY-MM-DD') 사이 날 수 */
export const daysBetween = (from: string, to: string) => Math.round((dayValue(to.slice(0, 10)) - dayValue(from.slice(0, 10))) / DAY_MS)

/** 화면을 연 때의 시각과 KST 오늘. 그리는 동안 바뀌지 않게 한 번만 읽는다 */
export function useNow() {
  const [now] = useState(() => ({ ms: Date.now(), today: todayKst() }))
  return now
}

/** 줄을 누르면 갈 화면 주소 */
export type GoTarget = string

/** 공개 공고문이 있으면 공고 상세, 없으면 원본 공고 상세 (CR-152). SAM.gov 원문은 상세 화면의 단추로 연다 */
export function targetOf(publicNoticeId: string | null, opportunityId: string | null): GoTarget | null {
  if (publicNoticeId) return `/search/${publicNoticeId}`
  if (opportunityId) return `/search/origin/${opportunityId}`
  return null
}

export const rowTarget = (row: MarketRow) => targetOf(row.publicNoticeId, row.id)

/** 줄 누름 이동 */
export function useGo() {
  const navigate = useNavigate()
  return useCallback((target: GoTarget | null) => {
    if (target) navigate(target)
  }, [navigate])
}
