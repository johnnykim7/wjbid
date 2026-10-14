import { api } from './client'
import type {
  AmendmentsQuery,
  AmendmentsResponse,
  AwardSort,
  AwardsResponse,
  CollectedQuery,
  CollectedResponse,
  MonthlyResponse,
  NaicsResponse,
  SampleResponse,
  SiteStats,
} from '../types/market'

/** 메뉴 속 화면 2묶음 (CR-135, T3-2 C-2). 모두 로그인 없이 부른다 */
export const marketApi = {
  collected: (q: CollectedQuery) =>
    api.get<CollectedResponse>('/opportunities/collected', { params: q }).then((r) => r.data),

  amendments: (q: AmendmentsQuery) =>
    api.get<AmendmentsResponse>('/opportunities/amendments', { params: q }).then((r) => r.data),

  /** 예시로 고른 공고문이 없거나 숨김이면 null */
  sample: () =>
    api.get<SampleResponse>('/opportunities/sample').then((r) => (r.status === 204 ? null : r.data)),

  awards: (q: { month?: string; sort?: AwardSort; page?: number; size?: number }) =>
    api.get<AwardsResponse>('/opportunities/awards', { params: q }).then((r) => r.data),

  monthly: () => api.get<MonthlyResponse>('/opportunities/stats/monthly').then((r) => r.data),

  naics: () => api.get<NaicsResponse>('/opportunities/stats/naics').then((r) => r.data),

  siteStats: () => api.get<SiteStats>('/opportunities/stats/site').then((r) => r.data),
}
