/** 미 연방 계약 자료로 여는 화면 (CR-142, T3-2 C-4, C-5). 날짜는 'YYYY-MM-DD', 시각은 +09:00 이 붙은 ISO */

export type FederalAwardType = 'PURCHASE_ORDER' | 'DEFINITIVE_CONTRACT' | 'DELIVERY_ORDER' | 'BPA_CALL'
export type RecompeteCategory = 'RECOMPETE' | 'EXTENDABLE' | 'IN_PARENT' | 'UNCONFIRMED'
export type ContractDateKind = 'CURRENT_END' | 'POTENTIAL_END'
export type RecompeteSort = 'END' | 'AMOUNT'
export type RangeGroup = 'STANDALONE' | 'ORDER'
export type MemberContractSource = 'PUBLIC' | 'MANUAL'

export interface FederalSource {
  totalCount: number
  /** 한 번도 받지 않았으면 null */
  lastSyncedAt: string | null
  lagDays: number | null
  dataFrom: string
}

export interface ContractRow {
  id: string
  piid: string
  parentPiid: string | null
  awardType: FederalAwardType
  description: string | null
  recipientName: string | null
  amountUsd: number | null
  naicsCode: string | null
  naicsName: string | null
  startDate: string | null
  endDate: string | null
  potentialEndDate: string | null
  solicitationNumber: string | null
  /** 원래 공고번호가 공개된 한글 공고문에 있으면 그 공고문 id */
  publicNoticeId: string | null
  category: RecompeteCategory | null
  basisDateKind: ContractDateKind | null
  basisDate: string | null
  oneTimeConstruction: boolean
  usaspendingUrl: string
}

export interface RecompeteResponse {
  month: string
  counts: Record<RecompeteCategory, number>
  items: ContractRow[]
  page: number
  size: number
  totalElements: number
  source: FederalSource
}

export interface CalendarMonth {
  month: string
  RECOMPETE: number
  EXTENDABLE: number
  IN_PARENT: number
  UNCONFIRMED: number
}

export interface CalendarResponse {
  months: CalendarMonth[]
  source: FederalSource
}

export interface RangeRow {
  group: RangeGroup
  count: number
  excludedCount: number
  minCount: number
  /** count 가 minCount 보다 적으면 다섯 값 모두 null */
  min: number | null
  p25: number | null
  p50: number | null
  p75: number | null
  max: number | null
}

export interface AwardRangeResponse {
  naicsCode: string
  naicsName: string | null
  noticeNumber: string | null
  noticeTitle: string | null
  rows: RangeRow[]
  evidence: ContractRow[]
  source: FederalSource
}

export interface NaicsOption {
  code: string
  name: string | null
  count: number
}

export interface NaicsResponse {
  items: NaicsOption[]
  source: FederalSource
}

export interface LookupResponse {
  found: boolean
  contract: ContractRow | null
  alreadyRegistered: boolean
}

export interface MemberContractRow {
  id: string
  piid: string
  description: string | null
  source: MemberContractSource
  endDate: string
  potentialEndDate: string | null
  endDaysLeft: number | null
  potentialDaysLeft: number | null
  nextAlertOn: string | null
  nextAlertKind: ContractDateKind | null
  datesChangedOn: string | null
  lastAlertedAt: string | null
  federalContract: ContractRow | null
}

export interface MemberContractsResponse {
  /** 설정이 꺼져 있으면 빈 배열 */
  alertDays: number[]
  today: string
  contracts: MemberContractRow[]
  source: FederalSource
}

export interface MemberContractInput {
  piid?: string
  endDate?: string | null
  potentialEndDate?: string | null
  description?: string | null
}

/** 서버가 돌려준 오류 본문 */
export interface FederalError {
  error: string
  message: string
}
