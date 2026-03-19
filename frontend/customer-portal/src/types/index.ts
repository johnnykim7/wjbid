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

// 백엔드 BidRequestState FSM과 동기화 (9-state)
export type BidRequestState =
  | 'CREATED'
  | 'DOCS_PENDING'
  | 'DOCS_RECEIVED'
  | 'ANALYZING'
  | 'GENERATING'
  | 'REVIEW'
  | 'CONFIRMED'
  | 'SUBMITTED'
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
  CLOSED: '종료',
}

export const STATE_BADGE: Record<string, 'draft' | 'review' | 'active' | 'final' | 'closing'> = {
  CREATED: 'draft',
  DOCS_PENDING: 'review',
  DOCS_RECEIVED: 'review',
  ANALYZING: 'active',
  GENERATING: 'active',
  REVIEW: 'review',
  CONFIRMED: 'active',
  SUBMITTED: 'final',
  CLOSED: 'closing',
}
