import type { MarketRow } from './market'

/** 낙찰 업체와 기지별 집계 응답 (CR-136, T3-2 C-3). 날짜는 'YYYY-MM-DD' */

export type AwardeeSort = 'COUNT' | 'AMOUNT'

export interface AwardeeAward extends MarketRow {
  /** 낙찰 금액(달러, 숫자). 공개되지 않았으면 null */
  awardAmountUsd: number | null
}

export interface Awardee {
  /** UEI:값 또는 NAME:정규화한 이름 (BIZ-029) */
  key: string
  /** 고른 정렬로 전체 업체 중 몇 번째인지. 이름으로 찾아도 바뀌지 않는다 */
  rank: number
  name: string
  uei: string | null
  awardCount: number
  /** 금액이 공개된 공지만 더한 합계. 더한 공지가 없으면 null */
  amountSum: number | null
  amountCount: number
  lastPostedDate: string | null
  awards: AwardeeAward[]
}

export interface AwardeesResponse {
  dataFrom: string | null
  dataTo: string | null
  totalAwards: number
  awardsWithAwardee: number
  awardees: Awardee[]
}

export interface BaseStat {
  id: string
  nameKo: string
  nameEn: string
  region: string | null
  latitude: number | null
  longitude: number | null
  opportunityCount: number
  awardCount: number
  openCount: number
}

export interface BaseStats {
  dataFrom: string | null
  dataTo: string | null
  totalNumbers: number
  unmatchedNumbers: number
  bases: BaseStat[]
}
