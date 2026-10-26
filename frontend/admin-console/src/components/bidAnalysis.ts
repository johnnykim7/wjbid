// CR-170 입찰 분석 응답 타입과 표시 이름 (BE BidAnalysisDto 대응)

import { createContext } from 'react'

export type BidAnalysisStatus = 'RUNNING' | 'DONE' | 'FAILED' | 'DECISION_ONLY'
export type BidVerdict = 'STRONG_GO' | 'CONDITIONAL_GO' | 'WEAK' | 'NO_GO'
export type BidDecision = 'GO' | 'CONDITIONAL_GO' | 'NO_GO'

// 산출물 본문은 워크플로우가 쓴 JSON 이라 칸이 빠져 있을 수 있어 느슨하게 받는다
export type Json = Record<string, unknown>

export interface Person {
  id: string
  name: string | null
}

export interface BidQuestion {
  target?: 'CUSTOMER' | 'ADMIN'
  question?: string
  reason?: string
  field?: string
  part?: string
}

export interface CompanyScore {
  company: string
  percent: number | null
}

export interface ThemeGap {
  theme: string
  our: number
  bestCompetitor: number
  diff: number
}

export interface RankedIssue {
  issue: string
  score: number
  memberIssues: { id: string; stakeholder?: string; issue: string; influence: number; importance: number; score: number }[]
}

export interface BidAnalysis {
  analysisId: string
  status: BidAnalysisStatus
  startedAt: string | null
  finishedAt: string | null
  failureReason: string | null
  report: Json | null
  capturePlan: Json | null
  decision: Json | null
  questions: BidQuestion[]
  pwinPercent: number | null
  aiVerdict: BidVerdict | null
  verdictReason: string | null
  scores: CompanyScore[] | null
  strengths: ThemeGap[] | null
  weaknesses: ThemeGap[] | null
  issueRanking: RankedIssue[] | null
  adminDecision: BidDecision | null
  adminNote: string | null
  decidedBy: Person | null
  decidedAt: string | null
  startedBy: Person | null
}

export interface BidAnalysisOverview {
  analysis: BidAnalysis | null
  readingInProgress: boolean
}

export const OUR_COMPANY = 'OUR_COMPANY'

export const STATUS_LABEL: Record<BidAnalysisStatus, string> = {
  RUNNING: '분석 중',
  DONE: '분석 완료',
  FAILED: '분석 실패',
  DECISION_ONLY: '결정만',
}

export const VERDICT_LABEL: Record<BidVerdict, string> = {
  STRONG_GO: 'Strong Go',
  CONDITIONAL_GO: '조건부 Go',
  WEAK: 'Weak',
  NO_GO: 'No-Go',
}

export const DECISION_LABEL: Record<BidDecision, string> = {
  GO: 'Go',
  CONDITIONAL_GO: '조건부 Go',
  NO_GO: 'No-Go',
}

export const DECISION_NOTE_MAX = 2000

/** BIZ-092: 조건부 Go 는 조건, No-Go 는 사유가 있어야 한다 */
export const DECISION_NOTE_LABEL: Partial<Record<BidDecision, string>> = {
  CONDITIONAL_GO: '조건',
  NO_GO: '사유',
}

export const DECISION_NOTE_MISSING: Partial<Record<BidDecision, string>> = {
  CONDITIONAL_GO: '조건을 기재하세요',
  NO_GO: '사유를 기재하세요',
}

export function allowsGeneration(decision: BidDecision | null | undefined): boolean {
  return decision === 'GO' || decision === 'CONDITIONAL_GO'
}

export const CONFIDENCE_LABEL: Record<string, string> = {
  CONFIRMED: '확정',
  ESTIMATED: '추정',
  UNKNOWN: '미상',
}

export const ORDER_VERDICT_LABEL: Record<string, string> = {
  NEW: '신규',
  RECURRING: '반복',
  FOLLOW_ON: '후속',
  UNKNOWN: '불명',
}

export const AWARD_SOURCE_LABEL: Record<string, string> = {
  FEDERAL_CONTRACT: '미 연방 계약',
  SAM_AWARD_NOTICE: '낙찰 공고',
}

// 발주 이력 계약 구분. 미 연방 계약은 FederalAwardType 이름, 낙찰 공고는 SAM 공고 종류가 그대로 온다
export const CONTRACT_TYPE_LABEL: Record<string, string> = {
  BPA_CALL: 'BPA 호출',
  PURCHASE_ORDER: '구매 주문',
  DELIVERY_ORDER: '납품 지시',
  DEFINITIVE_CONTRACT: '확정 계약',
  'Award Notice': '낙찰 공고',
}

export const ELIGIBILITY_LABEL: Record<string, string> = {
  MET: '충족',
  NOT_MET: '미충족',
  CONDITIONAL: '조건부',
  UNKNOWN: '미상',
}

export const LIKELIHOOD_LABEL: Record<string, string> = {
  HIGH: '높음',
  MEDIUM: '보통',
  LOW: '낮음',
}

export const INDICATOR_VALUE_LABEL: Record<string, string> = {
  YES: '예',
  NO: '아니오',
  UNKNOWN: '미상',
}

export const FAIRNESS_LABEL: Record<string, string> = {
  GOOD: '양호',
  CAUTION: '주의',
  RISK: '위험',
}

// 스킬 sam-proposal 01 STEP 7 의 징후, 순번 고정
export const FAIRNESS_INDICATORS: Record<number, string> = {
  1: '공고 기간이 법정 최소치(FAR 5.203)에 가까움',
  2: '사전 공지(Sources Sought, RFI, Draft RFP) 의견이 묵살됨',
  3: '특정사 제품이나 기술 스펙 명시',
  4: '기존 유사 사업 수행사 존재(연속 사업)',
  5: '과업 범위가 특정사 레퍼런스와 정확히 일치',
  6: '예산 대비 과업량 비정상(기존사만 소화 가능)',
  7: '자격 요건이 과도하게 좁음',
  8: '평가 배점이 특정 구도에 유리(정성 편중 등)',
  9: '작성 기간이 신규 진입자에게 불리하게 짧음',
  10: '발주처와 자사 사전 접점 전무(경쟁사는 있다고 추정)',
}

export const COMPETITIVENESS_AXES: { key: string; label: string }[] = [
  { key: 'overall', label: '종합' },
  { key: 'sales', label: '영업' },
  { key: 'technical', label: '기술' },
  { key: 'price', label: '가격' },
]

export const COMPETITIVENESS_LABEL: Record<string, string> = {
  ADVANTAGE: '우위',
  EQUAL: '대등',
  DISADVANTAGE: '열위',
}

// T1-4 POL-041 의 일곱 팩터, 정책 가중치 순서
export const FACTORS: { key: string; label: string }[] = [
  { key: 'CUSTOMER_RELATION', label: '고객 관계' },
  { key: 'SOLUTION_FIT', label: '솔루션 적합도' },
  { key: 'COMPETITION', label: '경쟁 환경' },
  { key: 'PRICE', label: '가격 경쟁력' },
  { key: 'PAST_PERFORMANCE', label: '과거 실적' },
  { key: 'PERSONNEL', label: '참여 인력' },
  { key: 'ENVIRONMENT', label: '환경 요인' },
]

export const RISK_AREA_LABEL: Record<string, string> = {
  FINANCIAL: '재무',
  TECHNICAL: '기술',
  CONTRACT: '계약',
}

export const QUESTION_TARGET_LABEL: Record<string, string> = {
  CUSTOMER: '고객에게 물을 것',
  ADMIN: '관리자가 볼 것',
}

const QUESTION_PART_LABEL: Record<string, string> = {
  REPORT: '리포트',
  CAPTURE_PLAN: '캡처플랜',
  DECISION: '결정',
}

// 확인 필요의 field 는 산출물 칸 경로(eligibility[0].status 등)라 맨 앞 칸 이름만 주제로 바꿔 보인다
const QUESTION_TOPIC_LABEL: Record<string, Record<string, string>> = {
  REPORT: {
    overview: '과업 개요', agency: '발주처', orderHistory: '발주 이력', requirements: '요구 테마와 평가 항목',
    eligibility: '자격', competitors: '경쟁사', fairness: '진입 공정성', hiddenIssues: '수면 아래 이슈',
    insights: '핵심 시사점',
  },
  CAPTURE_PLAN: { business: '사업', issues: '이슈', competitors: '경쟁사', company: '자사', other: '기타' },
  DECISION: {
    scorecard: '스코어카드', sensitivity: '민감도', qualification: '자격 해결', profitability: '수익성',
    risks: '리스크', conditions: '조건', advice: '결정 조언',
  },
}

// 산출물 칸이 아니라 입력 자료를 가리키는 field(어느 단계에서나 올 수 있다)
const QUESTION_INPUT_TOPIC_LABEL: Record<string, string> = { opportunity: '공고', company: '자사' }

export function questionTopic(q: BidQuestion): string {
  const head = (q.field ?? '').split(/[.[]/)[0]
  const part = q.part ?? ''
  const topic = QUESTION_TOPIC_LABEL[part]?.[head] ?? QUESTION_INPUT_TOPIC_LABEL[head]
  return [QUESTION_PART_LABEL[part], topic].filter(Boolean).join(' · ')
}

export function companyLabel(name: string): string {
  return name === OUR_COMPANY ? '자사' : name
}

/** 공고 컴플라이언스 매트릭스 줄에서 REQ ID 별 요구 문장(한글이 있으면 한글) */
export function requirementTexts(matrix: unknown): Record<string, string> {
  const out: Record<string, string> = {}
  for (const raw of arr(obj(matrix).lines)) {
    const line = obj(raw)
    const text = str(line.translation) || str(line.text)
    if (str(line.reqId) && text) out[str(line.reqId)] = text
  }
  return out
}

/** 달러 금액을 $1,234 꼴로. 숫자가 아니면 받은 그대로 */
export function formatUsd(v: unknown): string {
  const s = str(v)
  const n = Number(s)
  return s === '' || Number.isNaN(n) ? s : `$${n.toLocaleString('en-US', { maximumFractionDigits: 0 })}`
}

/** 본문의 칸을 꺼낼 때 쓰는 좁히기. 꼴이 다르면 비운다 */
export function obj(v: unknown): Json {
  return v !== null && typeof v === 'object' && !Array.isArray(v) ? (v as Json) : {}
}

export function arr(v: unknown): unknown[] {
  return Array.isArray(v) ? v : []
}

export function str(v: unknown): string {
  if (v === null || v === undefined) return ''
  if (typeof v === 'string') return v
  if (typeof v === 'number' || typeof v === 'boolean') return String(v)
  return JSON.stringify(v)
}

/** 서버 시각은 시간대 없는 한국 시각이라 브라우저 시각으로 읽어 그대로 보인다 */
export function formatDateTime(local: string | null | undefined): string {
  if (!local) return ''
  const d = new Date(local)
  return Number.isNaN(d.getTime()) ? local : d.toLocaleString('ko-KR')
}

/** 인쇄용 문서 안에서는 접히는 내용을 펼친 채 그린다 (CR-181) */
export const PrintMode = createContext(false)

/** 종합 분석 리포트 과업 개요의 사업명. 근거 값이면 그 값 */
export function projectNameOf(report: unknown): string {
  const name = obj(obj(report).overview).projectName
  const e = obj(name)
  return str('value' in e ? e.value : name).trim()
}

const FILE_NAME_FORBIDDEN = /[\\/:*?"<>|]/g
const FILE_NAME_PROJECT_MAX = 60

/** 인쇄 창이 PDF 파일 이름 기본값으로 쓰는 문서 제목. 입찰분석_사업명_회차시작일 */
export function printFileName(projectName: string, startedAt: string | null): string {
  const name = projectName.replace(FILE_NAME_FORBIDDEN, '_').slice(0, FILE_NAME_PROJECT_MAX)
  const day = (startedAt ?? '').slice(0, 10).replaceAll('-', '')
  return ['입찰분석', name, day].filter(Boolean).join('_')
}
