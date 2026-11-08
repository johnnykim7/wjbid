import type { StateTone } from '../utils/bidState'

export type LineStatus =
  | 'MET'
  | 'NOT_MET'
  | 'UNVERIFIED'
  | 'NOT_CHECKED'
  | 'NOT_ANSWERED'
  | 'EQUIVALENCE_CHECK'
  | 'EQUIVALENT'
  | 'APPLICABILITY_UNCONFIRMED'

export type LineDisposition = 'NOT_ANSWERED' | 'EQUIVALENCE_CHECK' | 'EQUIVALENT' | 'APPLICABILITY_UNCONFIRMED'

export interface NoteView {
  disposition: string
  reason: string | null
  sourceRef: string | null
  decidedBy: string | null
  decidedAt: string | null
}

export interface SectionRow {
  documentId: string
  sectionId: string
  title: string
  verdict: 'PASS' | 'FAIL' | 'UNVERIFIED'
  inputChanged: boolean
}

export interface LineRow {
  reqId: string
  text: string | null
  translation: string | null
  fulfillmentParty: string | null
  status: LineStatus
  evidenceQuote: string | null
  reason: string | null
  note: NoteView | null
}

export interface ConditionRow {
  key: string
  text: string
  resolved: boolean
  note: NoteView | null
}

export type PriceStatus = 'NOT_READY' | 'NOT_APPLICABLE' | 'INCOMPLETE' | 'COMPLETE'

export interface PriceSummary {
  status: PriceStatus
  lineCount: number
  filledCount: number
  total: number | null
}

export interface SubmissionCheck {
  submittable: boolean
  blockers: { code: string; count: number }[]
  sections: SectionRow[]
  lines: LineRow[]
  conditions: ConditionRow[]
  price: PriceSummary
}

export interface PriceLine {
  clin: string
  description: string | null
  quantity: number | null
  unit: string | null
  sourceRef: string | null
  unitPrice: number | null
  amount: number | null
}

export interface PriceTable {
  status: PriceStatus
  editable: boolean
  periods: { period: string; lines: PriceLine[]; subtotal: number | null }[]
  total: number | null
}

export const LINE_STATUS: Record<LineStatus, { label: string; tone: StateTone }> = {
  MET: { label: '충족', tone: 'success' },
  NOT_MET: { label: '미충족', tone: 'destructive' },
  UNVERIFIED: { label: '검증 못 함', tone: 'warning' },
  NOT_CHECKED: { label: '확인 안 됨', tone: 'gray' },
  NOT_ANSWERED: { label: '답하지 않는 줄', tone: 'gray' },
  EQUIVALENCE_CHECK: { label: '동등 여부 확인', tone: 'warning' },
  EQUIVALENT: { label: '동등 인정', tone: 'success' },
  APPLICABILITY_UNCONFIRMED: { label: '적용 미확인', tone: 'warning' },
}

export const LINE_DISPOSITIONS: LineDisposition[] = [
  'NOT_ANSWERED',
  'EQUIVALENCE_CHECK',
  'EQUIVALENT',
  'APPLICABILITY_UNCONFIRMED',
]

export const VERDICT: Record<SectionRow['verdict'], { label: string; tone: StateTone }> = {
  PASS: { label: '통과', tone: 'success' },
  FAIL: { label: '실패', tone: 'destructive' },
  UNVERIFIED: { label: '검증 못 함', tone: 'warning' },
}

export const PRICE_STATUS: Record<PriceStatus, { label: string; tone: StateTone }> = {
  NOT_READY: { label: '확인 안 됨', tone: 'gray' },
  NOT_APPLICABLE: { label: '가격 표 없음', tone: 'gray' },
  INCOMPLETE: { label: '미완료', tone: 'warning' },
  COMPLETE: { label: '완료', tone: 'success' },
}

const BLOCKER_LABEL: Record<string, string> = {
  NOT_READY: '공고 분석 전',
  SECTION_NOT_PASSED: '사실 검증을 지나지 못한 절',
  INPUT_CHANGED: '검증 뒤 입력이 바뀐 절',
  CONDITION_UNRESOLVED: '해소하지 않은 결정 조건',
  PRICE_INCOMPLETE: '단가가 없는 가격 표 줄',
}

const LINE_BLOCKER_PREFIX = 'LINE_'

export function blockerLabel(code: string): string {
  if (code.startsWith(LINE_BLOCKER_PREFIX)) {
    const status = LINE_STATUS[code.slice(LINE_BLOCKER_PREFIX.length) as LineStatus]
    if (status) return `요구 줄: ${status.label}`
  }
  return BLOCKER_LABEL[code] ?? code
}

const LINE_REASON: Record<string, string> = {
  SECTION_FAILED: '이 줄을 답한 절의 사실 검증이 실패했습니다',
  SECTION_UNVERIFIED: '이 줄을 답한 절을 검증하지 못했습니다',
  INPUT_CHANGED: '검증 뒤 입력이 바뀌었습니다',
}

export function lineReason(reason: string | null): string | null {
  if (!reason) return null
  return LINE_REASON[reason] ?? reason
}

export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-'
  const [whole, fraction] = String(value).split('.')
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return fraction ? `${grouped}.${fraction}` : grouped
}
