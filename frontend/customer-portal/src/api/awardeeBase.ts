import { api } from './client'
import type { MarketRow } from '../types/market'
import type { AwardeeSort, AwardeesResponse, BaseStats } from '../types/awardeeBase'

/** 낙찰 업체와 기지별 집계 (CR-136, T3-2 C-3). 모두 로그인 없이 부른다 */
export const awardeeBaseApi = {
  awardees: (q: { q?: string; sort?: AwardeeSort }) =>
    api.get<AwardeesResponse>('/opportunities/awardees', { params: q }).then((r) => r.data),

  baseStats: () => api.get<BaseStats>('/opportunities/stats/bases').then((r) => r.data),

  baseOpportunities: (baseId: string, size: number) =>
    api.get<MarketRow[]>(`/opportunities/stats/bases/${baseId}/opportunities`, { params: { size } }).then((r) => r.data),
}
