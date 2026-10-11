/** 서류 만료 상태 (CR-136, T3-2 GET /member-documents/expiry) */
export type ExpiryStatus = 'EXPIRED' | 'SOON' | 'OK' | 'NONE'

export interface ExpiryDocument {
  id: string
  category: string
  categoryLabel: string
  displayName: string | null
  fileName: string
  /** YYYY-MM-DD, 한국 날짜 */
  expiresOn: string | null
  daysLeft: number | null
  status: ExpiryStatus
  lastAlertedAt: string | null
}

export interface ExpiryOverview {
  /** 알림 시점(만료 며칠 전), 큰 날부터. 비었으면 알림 꺼짐 */
  alertDays: number[]
  soonDays: number | null
  today: string
  summary: { expired: number; soon: number; noDate: number; total: number }
  documents: ExpiryDocument[]
}
