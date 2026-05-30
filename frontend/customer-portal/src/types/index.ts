export interface Opportunity {
  id: string
  solicitationNumber: string
  title: string
  agencyName: string
  naicsCode?: string
  setAside?: string
  responseDeadline: string
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
  keyDates?: { label: string; date: string; note?: string }[]
  budgetInfo?: string
  specialNotes?: string[]
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
  opportunityTitle: string
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
