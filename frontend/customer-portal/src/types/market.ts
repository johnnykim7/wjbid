import type { Opportunity } from './index'

/** 메뉴 속 화면 2묶음 응답 (CR-135, T3-2 C-2). 날짜는 'YYYY-MM-DD', 마감 시각은 KST 오프셋이 붙은 ISO 문자열 */

export type MarketGroup = 'PRE' | 'BID' | 'AWARD' | 'OTHER'

export interface MarketRow {
  id: string
  noticeId: string | null
  solicitationNumber: string | null
  title: string | null
  titleKo: string | null
  type: string | null
  typeKo: string | null
  group: MarketGroup
  postedDate: string | null
  responseDeadline: string | null
  awardAmount: string | null
  awardeeName: string | null
  publicNoticeId: string | null
  samUrl: string | null
  hasMainNotice: boolean | null
  originalId: string | null
  originalPublicNoticeId: string | null
  originalSamUrl: string | null
}

interface Paged<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

interface DataRange {
  total: number
  dataFrom: string | null
  dataTo: string | null
}

export interface TypeCount {
  type: string
  typeKo: string | null
  count: number
}

export interface CollectedResponse extends Paged<MarketRow>, DataRange {
  typeCounts: TypeCount[]
}

export interface AmendmentGroup {
  solicitationNumber: string
  title: string | null
  titleKo: string | null
  count: number
  firstPostedDate: string | null
  lastPostedDate: string | null
  lastResponseDeadline: string | null
  deadlineChanged: boolean
  hasAward: boolean
  open: boolean
  versions: MarketRow[]
}

export interface AmendmentsResponse extends Paged<AmendmentGroup>, DataRange {
  openCount: number
  versionCount: number
}

export interface SampleResponse {
  notice: Opportunity
  originalTitle: string | null
  samUrl: string | null
  firstPostedDate: string | null
}

export interface AwardSummary {
  total: number
  amountKnownCount: number
  amountSum: number | null
  amountMax: number | null
  awardeeKnownCount: number
}

export interface MonthCount {
  month: string
  count: number
}

export interface AwardsResponse extends Paged<MarketRow>, DataRange {
  summary: AwardSummary
  monthCounts: MonthCount[]
}

export interface MonthStat {
  month: string
  total: number
  bid: number
  pre: number
  award: number
  other: number
}

export interface MonthlyResponse extends DataRange {
  months: MonthStat[]
  emptyMonths: string[]
  currentMonth: string
}

export interface SubCodeCount {
  code: string
  count: number
}

export interface SectorStat {
  code: string
  nameKo: string
  count: number
  subCodes: SubCodeCount[]
}

export interface NaicsResponse extends DataRange {
  sectors: SectorStat[]
  noneCount: number
  codedCount: number
}

export interface CollectedQuery {
  group?: MarketGroup
  type?: string
  month?: string
  sector?: string
  openOnly?: boolean
  latestOnly?: boolean
  page?: number
  size?: number
}

export interface AmendmentsQuery {
  solicitationNumber?: string
  openOnly?: boolean
  page?: number
  size?: number
}

export type AwardSort = 'LATEST' | 'AMOUNT'
