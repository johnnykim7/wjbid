export interface Opportunity {
  id: string
  solicitationNumber: string
  title: string
  agencyName: string
  naicsCode?: string
  setAside?: string
  responseDeadline: string
  /** 마감 시각(서울 시각 표시 포함, 예 2026-11-03T13:00:00+09:00) */
  responseDeadlineAt?: string
  postedDate?: string
  placeOfPerformance?: string
  description?: string
  status?: 'active' | 'closed' | 'updated'
  type?: string
  uiLink?: string
  resourceLinks?: string[]
  analysis?: AnalysisResult
}

export interface AnalysisResult {
  analysisStatus: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  analyzedAt?: string
  summary?: AnalysisSummary
  requiredDocuments?: AnalysisRequiredDocuments
  documentFormats?: AnalysisDocumentFormats
  /** CR-021: TipTap JSON 본문 (PDF 양식 풍부도). 있으면 NoticeDocumentView로 렌더. */
  contentJson?: Record<string, unknown>
}

export interface AnalysisSummary {
  overview?: string
  scope?: string
  eligibility?: string
  evaluationCriteria?: string
  keyDates?: { label: string; date: string | null; note?: string | null }[]
  budgetInfo?: string
  specialNotes?: string[]
  contractPeriod?: string
}

export interface AnalysisRequiredDocuments {
  documents?: {
    name: string
    description?: string
    mandatory: boolean
    format?: string
    pageLimit?: string
    notes?: string
  }[]
  /** 참가 자격. 공고 상세는 있는지만 보고 적격성 진단 단추를 띄운다(CR-137) */
  eligibility?: { title?: string }[]
}

export interface AnalysisDocumentFormats {
  generalInstructions?: string
  formats?: {
    section: string
    description?: string
    pageLimit?: string
    fileFormat?: string
    fontRequirements?: string
    otherRequirements?: string[]
  }[]
  submissionMethod?: string
}

export interface BidRequest {
  id: string
  opportunityId: string
  /** CR-024: 노티 ID (공고문 본문/필요서류 조회용). 노티가 없으면 null */
  noticeId?: string
  /** opportunity.title (영문 원본) */
  opportunityTitle: string
  /** CR-024: notice.koreanTitle ?? opportunity.title — 화면 표시용 */
  displayTitle?: string
  solicitationNumber?: string
  agencyName?: string
  state: BidRequestState
  stateDisplay?: string
  createdAt: string
  updatedAt?: string
  submittedAt?: string
}

// 백엔드 BidRequestState FSM과 동기화 (11-state, CR-018)
export type BidRequestState =
  | 'CREATED'
  | 'DOCS_PENDING'
  | 'DOCS_RECEIVED'
  | 'ANALYZING'
  | 'GENERATING'
  | 'REVIEW'
  | 'CONFIRMED'
  | 'SUBMITTED'
  | 'AWARDED'
  | 'NOT_AWARDED'
  | 'CLOSED'

export interface StateTransition {
  fromState: BidRequestState
  toState: BidRequestState
  userId?: string
  username?: string
  timestamp: string
  notes?: string
  automatic?: boolean
}

export interface MemberProfile {
  id: string
  email: string
  companyName: string
  businessRegistrationNumber?: string
  contactPerson?: string
  phone?: string
  address?: string
  role: string
}

export interface Notification {
  id: string
  type: string
  subject: string
  referenceId?: string
  referenceType?: string
  sentAt: string
  read: boolean
  readAt?: string
}

export type AiStatus = 'idle' | 'generating' | 'completed'

export const STATE_LABEL: Record<string, string> = {
  CREATED: '신청 접수',
  DOCS_PENDING: '문서 대기',
  DOCS_RECEIVED: '문서 접수 완료',
  ANALYZING: '분석 중',
  GENERATING: '문서 생성 중',
  REVIEW: '관리자 검토',
  CONFIRMED: '확정',
  SUBMITTED: '제출 완료',
  AWARDED: '합격',
  NOT_AWARDED: '불합격',
  CLOSED: '종료',
}

export const STATE_BADGE: Record<string, 'draft' | 'review' | 'active' | 'final' | 'closing' | 'closed'> = {
  CREATED: 'draft',
  DOCS_PENDING: 'review',
  DOCS_RECEIVED: 'review',
  ANALYZING: 'active',
  GENERATING: 'active',
  REVIEW: 'review',
  CONFIRMED: 'active',
  SUBMITTED: 'final',
  AWARDED: 'active',
  NOT_AWARDED: 'closed',
  CLOSED: 'closing',
}

/** 적격성 진단 (CR-137, T3-2 「적격성 진단」). 답은 회원이 고른 것이고 서버는 참여 가능 판정을 하지 않는다(BIZ-032) */
export type EligibilityAnswerValue = 'HAVE' | 'LACK' | 'UNSURE'

export interface EligibilityNoticeRow {
  noticeId: string
  solicitationNumber: string
  koreanTitle: string | null
  responseDeadline: string | null
  open: boolean
  itemCount: number
  answeredCount: number
}

export interface EligibilityNoticeHead {
  noticeId: string
  solicitationNumber: string
  koreanTitle: string | null
  organizationName: string | null
  responseDeadline: string | null
  open: boolean
}

export interface EligibilityVaultDocument {
  id: string
  name: string
  uploadedAt: string
}

export interface EligibilityItem {
  itemNo: number
  title: string
  description: string | null
  mandatory: boolean
  gate: boolean
  sourceRef: string | null
  evidenceBy: string | null
  answer: EligibilityAnswerValue | null
  /** 답한 뒤 공고 분석이 바뀌어 옛 답을 쓰지 않는다 */
  stale: boolean
  answeredAt: string | null
  vaultCategory: string | null
  vaultCategoryName: string | null
  vaultDocuments: EligibilityVaultDocument[]
}

/** 개수는 통과 조건 자격만 센다. allCount 는 전체 자격 수 */
export interface EligibilitySummary {
  gateCount: number
  have: number
  lack: number
  unsure: number
  unanswered: number
  allCount: number
}

export interface EligibilityDetail {
  notice: EligibilityNoticeHead
  items: EligibilityItem[]
  summary: EligibilitySummary
}

export interface EligibilitySaved {
  item: EligibilityItem
  summary: EligibilitySummary
}
