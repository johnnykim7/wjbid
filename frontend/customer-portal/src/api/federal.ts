import { isAxiosError } from 'axios'
import { api } from './client'
import type { AwardRangeResponse, FederalError, NaicsResponse } from '../types/federal'

/** 미 연방 계약 자료로 여는 화면 (CR-142, CR-147, T3-2 C-4, C-5). 모두 로그인해야 부른다 */
export const federalApi = {
  awardRange: (q: { noticeNumber?: string; naics?: string; q?: string }) =>
    api.get<AwardRangeResponse>('/federal-contracts/award-range', { params: q }).then((r) => r.data),

  naics: (q?: string) =>
    api.get<NaicsResponse>('/federal-contracts/naics', { params: { q } }).then((r) => r.data),
}

/** 서버 오류 코드. 서버가 오류 본문을 주지 않았으면 null */
export function federalErrorCode(e: unknown): string | null {
  if (!isAxiosError<FederalError>(e)) return null
  return e.response?.data?.error ?? null
}
