import { isAxiosError } from 'axios'
import { api } from './client'
import type {
  AwardRangeResponse,
  CalendarResponse,
  FederalError,
  LookupResponse,
  MemberContractInput,
  MemberContractRow,
  MemberContractsResponse,
  NaicsResponse,
  RecompeteCategory,
  RecompeteResponse,
  RecompeteSort,
} from '../types/federal'

/** 미 연방 계약 자료로 여는 화면 (CR-142, T3-2 C-4, C-5). 모두 로그인해야 부른다 */
export const federalApi = {
  recompete: (q: { month?: string; category?: RecompeteCategory; naics?: string; sort?: RecompeteSort; page?: number; size?: number }) =>
    api.get<RecompeteResponse>('/federal-contracts/recompete', { params: q }).then((r) => r.data),

  calendar: (naics?: string) =>
    api.get<CalendarResponse>('/federal-contracts/recompete/calendar', { params: { naics } }).then((r) => r.data),

  awardRange: (q: { noticeNumber?: string; naics?: string; q?: string }) =>
    api.get<AwardRangeResponse>('/federal-contracts/award-range', { params: q }).then((r) => r.data),

  naics: (q?: string) =>
    api.get<NaicsResponse>('/federal-contracts/naics', { params: { q } }).then((r) => r.data),
}

export const memberContractApi = {
  list: () => api.get<MemberContractsResponse>('/member-contracts').then((r) => r.data),

  lookup: (piid: string) =>
    api.get<LookupResponse>('/member-contracts/lookup', { params: { piid } }).then((r) => r.data),

  register: (body: MemberContractInput) =>
    api.post<MemberContractRow>('/member-contracts', body).then((r) => r.data),

  update: (id: string, body: MemberContractInput) =>
    api.patch<MemberContractRow>(`/member-contracts/${id}`, body).then((r) => r.data),

  remove: (id: string) => api.delete(`/member-contracts/${id}`).then(() => undefined),
}

/** 서버 오류 코드. 서버가 오류 본문을 주지 않았으면 null */
export function federalErrorCode(e: unknown): string | null {
  if (!isAxiosError<FederalError>(e)) return null
  return e.response?.data?.error ?? null
}
