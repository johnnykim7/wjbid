// CR-169 고객 서류 읽기 응답 타입과 표시 이름 (BE DocumentReadingDto 대응)

export type ReadingStatus = 'WAITING' | 'READING' | 'DONE' | 'FAILED' | 'UNSUPPORTED'
export type ReadingOrigin = 'BID_DOCUMENT' | 'VAULT'

export interface ReadingFulfill {
  requirementTitle: string | null
  reqId: string | null
}

export interface ReadingItem {
  readingId: string | null
  fileName: string
  origin: ReadingOrigin
  documentId: string
  category: string | null
  fulfills: ReadingFulfill[]
  status: ReadingStatus | null
  readAs: string | null
  factCount: number
  visionUsed: boolean
  failureReason: string | null
  readAt: string | null
}

export interface ReadingOverview {
  readingStarted: boolean
  inProgress: boolean
  items: ReadingItem[]
}

export interface ReadingFact {
  category?: string
  key?: string
  value?: string
  page?: string | number | null
  anchor?: string | null
  confidence?: string | null
  readMode?: 'TEXT' | 'VISION' | null
}

export interface ReadingDetail {
  readingId: string
  fileName: string
  status: ReadingStatus
  readAs: string | null
  visionUsed: boolean
  failureReason: string | null
  readAt: string | null
  facts: ReadingFact[]
}

export interface SourceDocument {
  readingId: string
  fileName: string
  status: ReadingStatus
  factCount: number
}

const STATUS_LABEL: Record<ReadingStatus, string> = {
  WAITING: '읽는 중',
  READING: '읽는 중',
  DONE: '읽음',
  FAILED: '읽지 못함',
  UNSUPPORTED: '읽을 수 없는 형식',
}

export function readingStatusLabel(status: ReadingStatus | null, visionUsed = false): string {
  if (!status) return ''
  if (status === 'DONE' && visionUsed) return '읽음(글자 인식)'
  return STATUS_LABEL[status]
}

export function isReadingInFlight(status: ReadingStatus | null): boolean {
  return status === 'WAITING' || status === 'READING'
}

export const ORIGIN_LABEL: Record<ReadingOrigin, string> = {
  BID_DOCUMENT: '공고별 서류',
  VAULT: '공통 서류함',
}

export const FACT_CATEGORY_LABEL: Record<string, string> = {
  COMPANY: '회사 기본',
  LICENSE: '인허가와 자격',
  PAST_PERFORMANCE: '수행 실적',
  PERSONNEL: '인력',
  EQUIPMENT: '장비와 시설',
  FINANCIAL: '재무',
  INSURANCE: '보험',
  CERTIFICATION: '인증',
  OTHER: '그 밖',
}
