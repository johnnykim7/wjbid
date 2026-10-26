import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    const url: string = err.config?.url ?? ''
    // 인증 실패(401)만 세션 만료로 보고 로그인으로. BE가 인증실패=401, 권한부족=403으로 구분하므로
    // 403(권한 부족·기능 버그)은 로그아웃시키지 않는다. 로그인 API 자체 실패는 제외(무한 리다이렉트 방지).
    const isAuthEndpoint = url.includes('/auth/')
    if (status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('adminToken')
      // admin은 /admin/ 서브경로로 서빙 → 절대경로로 로그인 화면 이동
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = '/admin/login'
      }
    }
    return Promise.reject(err)
  }
)

// Auth
export const login = (email: string, password: string) =>
  api.post('/auth/login', { email, password })

// Dashboard stats
export const getBidRequestStats = () =>
  api.get('/admin/bid-requests/stats')

export const getOpportunityCount = () =>
  api.get('/opportunities', { params: { page: 0, size: 1 } })

// Members
export const getMembers = (page = 0) =>
  api.get('/admin/members', { params: { page, size: 20 } })

/** 회원 기간 한 줄 (CR-157). current 는 오늘을 품은 기간 */
export interface MembershipLine {
  id: string
  startsOn: string
  endsOn: string
  memo: string | null
  createdByEmail: string | null
  createdAt: string
  current: boolean
}

export const getMemberships = (memberId: string) =>
  api.get<MembershipLine[]>(`/admin/members/${memberId}/memberships`)

export const grantMembership = (memberId: string, body: { startsOn: string; endsOn: string; memo: string }) =>
  api.post<MembershipLine>(`/admin/members/${memberId}/memberships`, body)

export const deleteMembership = (memberId: string, membershipId: string) =>
  api.delete(`/admin/members/${memberId}/memberships/${membershipId}`)

// Bid Requests (admin)
export const getAdminBidRequests = (state?: string, page = 0) =>
  api.get('/admin/bid-requests', { params: { state, page, size: 20 } })

export const transitionBidRequest = (id: string, toState: string, notes?: string) =>
  api.post(`/admin/bid-requests/${id}/transition`, { toState, notes })

// CR-017 ②: 개별 문서 재생성 (REVIEW 상태)
export const regenerateBidDocument = (id: string, documentType: string) =>
  api.post(`/admin/bid-requests/${id}/documents/${documentType}/regenerate`)

// Collection
export const triggerCollection = (daysBack = 30) =>
  api.post('/admin/collection/trigger', null, { params: { daysBack } })

export const getCollectionStatus = () =>
  api.get('/admin/collection/status')

// Document Templates
export const getDocumentTemplates = (includeInactive = false) =>
  api.get('/admin/document-templates', { params: { includeInactive } })

export const getDocumentTemplatesByType = (documentType: string) =>
  api.get(`/admin/document-templates/${documentType}`)

export const createDocumentTemplate = (data: {
  templateName: string
  documentType: string
  contentJson: Record<string, unknown>
}) => api.post('/admin/document-templates', data)

export const deactivateDocumentTemplate = (id: string) =>
  api.delete(`/admin/document-templates/${id}`)

export const activateDocumentTemplate = (id: string) =>
  api.post(`/admin/document-templates/${id}/activate`)

export const updateDocumentTemplate = (id: string, data: {
  templateName?: string
  contentJson?: Record<string, unknown>
  description?: string
}) => api.patch(`/admin/document-templates/${id}`, data)

// Bid Documents (editing)
export const getBidDocument = (id: string) =>
  api.get(`/bid-documents/${id}`)

export const getBidDocumentVersions = (id: string) =>
  api.get(`/bid-documents/${id}/versions`)

export const getBidDocumentVersion = (id: string, versionNo: number) =>
  api.get(`/bid-documents/${id}/versions/${versionNo}`)

export const saveDocumentVersion = (id: string, data: {
  contentJson: Record<string, unknown>
  changeSummary?: string
}) => api.post(`/bid-documents/${id}/versions`, data)

export const lockDocument = (id: string) =>
  api.post(`/bid-documents/${id}/lock`)

export const unlockDocument = (id: string) =>
  api.post(`/bid-documents/${id}/unlock`)

export const exportDocumentPdf = (id: string) =>
  api.get(`/bid-documents/${id}/export/pdf`, { responseType: 'blob' })

// CR-030 — Proposal section tree
export const getProposalTree = (documentId: string) =>
  api.get(`/admin/bid-documents/${documentId}/chapters`)

export const getProposalSection = (documentId: string, sectionId: string) =>
  api.get(`/admin/bid-documents/${documentId}/sections/${sectionId}`)

export const regenerateProposalSection = (documentId: string, sectionId: string) =>
  api.post(`/admin/bid-documents/${documentId}/sections/${sectionId}/regenerate`)

export const lockProposalSection = (documentId: string, sectionId: string) =>
  api.post(`/admin/bid-documents/${documentId}/sections/${sectionId}/lock`)

export const unlockProposalSection = (documentId: string, sectionId: string) =>
  api.post(`/admin/bid-documents/${documentId}/sections/${sectionId}/unlock`)

// CR-031 — 충실성·분량 검증
export const getProposalSectionVerification = (documentId: string, sectionId: string) =>
  api.get(`/admin/bid-documents/${documentId}/sections/${sectionId}/verification`)

export const reverifyProposalSection = (documentId: string, sectionId: string) =>
  api.post(`/admin/bid-documents/${documentId}/sections/${sectionId}/reverify`)

// CR-159 커버리지 검증
export const getProposalCoverage = (documentId: string) =>
  api.get(`/admin/bid-documents/${documentId}/coverage`)

// Bid Request Detail (admin)
export const getAdminBidRequestDetail = (id: string) =>
  api.get(`/admin/bid-requests/${id}`)

// Bid Documents for a bid request
export const getBidDocumentsByBidRequest = (bidRequestId: string) =>
  api.get(`/bid-documents`, { params: { bidRequestId } })

// Client Documents
export const getClientDocuments = (bidRequestId: string) =>
  api.get(`/bid-requests/${bidRequestId}/client-documents`)

// CR-169 고객 서류 읽기
export const getDocumentReadings = (bidRequestId: string) =>
  api.get(`/admin/bid-requests/${bidRequestId}/document-readings`)

export const getDocumentReading = (readingId: string) =>
  api.get(`/admin/document-readings/${readingId}`)

export const rerunDocumentReadings = (bidRequestId: string, readingIds?: string[]) =>
  api.post(`/admin/bid-requests/${bidRequestId}/document-readings/run`, { readingIds: readingIds ?? null })

// CR-170 입찰 분석과 Bid/No-Bid 결정
export const getBidAnalysis = (bidRequestId: string) =>
  api.get(`/admin/bid-requests/${bidRequestId}/bid-analysis`)

export const rerunBidAnalysis = (bidRequestId: string) =>
  api.post(`/admin/bid-requests/${bidRequestId}/bid-analysis/run`)

export const saveBidDecision = (bidRequestId: string, decision: string, note: string | null) =>
  api.post(`/admin/bid-requests/${bidRequestId}/bid-analysis/decision`, { decision, note })

// Collection Runs
export const getCollectionRuns = (page = 0) =>
  api.get('/admin/collection/runs', { params: { page, size: 20 } })

// 원본 공고 Admin — 선별 풀 (CR-016)
// CR-118: keyword(제목·본문·공고번호) / type(공고유형) / hasAttachment(첨부유무) / hasNotice(생성여부) 검색·필터
export interface OpportunitySearchParams {
  keyword?: string
  type?: string
  hasAttachment?: boolean
  hasNotice?: boolean
}
export const getAdminOpportunities = (page = 0, filters: OpportunitySearchParams = {}) =>
  api.get('/admin/opportunities', {
    params: {
      page,
      size: 20,
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.type ? { type: filters.type } : {}),
      ...(filters.hasAttachment !== undefined ? { hasAttachment: filters.hasAttachment } : {}),
      ...(filters.hasNotice !== undefined ? { hasNotice: filters.hasNotice } : {}),
    },
  })

// CR-118: 공고유형 셀렉트 옵션
export const getAdminOpportunityTypes = () =>
  api.get<Array<{ type: string; typeKo: string }>>('/admin/opportunities/types')

export const getAdminOpportunityDetail = (id: string) =>
  api.get(`/admin/opportunities/${id}`)

// 게이트①: 공고문 만들기 (원본 선별 → 한글화 트리거)
export const createNotice = (opportunityId: string) =>
  api.post(`/admin/opportunities/${opportunityId}/create-notice`)

// CR-037: 다중 파일 업로드. ZIP은 BE가 자동 해제. files 파라미터로 여러 파일 전송.
export const uploadOpportunityAttachment = (id: string, files: File[]) => {
  const formData = new FormData()
  files.forEach((f) => formData.append('files', f))
  return api.post(`/admin/opportunities/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// CR-019: 원본 공고 첨부 목록 (상태/외부 링크 포함)
export const getOpportunityAttachments = (id: string) =>
  api.get(`/admin/opportunities/${id}/attachments`)

// CR-034: 저장된 첨부 다운로드 (blob)
export const downloadOpportunityAttachment = (id: string, attachmentId: string) =>
  api.get(`/admin/opportunities/${id}/attachments/${attachmentId}/download`, {
    responseType: 'blob',
  })

// CR-034: 수동 업로드 첨부 삭제 (SAM 수집 첨부는 서버가 403 거부)
export const deleteOpportunityAttachment = (id: string, attachmentId: string) =>
  api.delete(`/admin/opportunities/${id}/attachments/${attachmentId}`)

// CR-022 (재구현): 본문 한글 번역 수동 트리거
export const retranslateOpportunityDescription = (id: string) =>
  api.post(`/admin/opportunities/${id}/retranslate-description`)

// 제목만 번역 (리스트 일괄 번역용 — SAM 쿼터 미소진)
export const translateOpportunityTitle = (id: string) =>
  api.post(`/admin/opportunities/${id}/translate-title`)

// CR-042: 원본 공고 소프트 삭제. 연결 공고문이 노출/분석 중이면 서버가 409 거부.
export const deleteOpportunity = (id: string) =>
  api.delete(`/admin/opportunities/${id}`)

// CR-043: PIEE 링크 오류 표식 토글. 관리자가 수동으로 "이 공고 PIEE 링크 오류" 표시/해제.
export const setOpportunityPieeLinkBroken = (id: string, broken: boolean) =>
  api.patch(`/admin/opportunities/${id}/piee-link-broken`, { broken })

// CR-120: 테스트용 수동 공고 등록. SAM 수집 없이 빈 원본 공고 1건 생성.
// noticeId 미입력 시 서버가 TEST-{timestamp} 자동 생성, 중복이면 409.
export const createManualOpportunity = (noticeId: string, title: string) =>
  api.post('/admin/opportunities/manual', { noticeId, title })

// 공고문(Notice) Admin (CR-016)
export const getAdminNotices = (page = 0) =>
  api.get('/admin/notices', { params: { page, size: 20 } })

export const getAdminNoticeDetail = (id: string) =>
  api.get(`/admin/notices/${id}`)

// 공고분석 진행 STEP 조회 — ANALYZING 동안 화면이 주기 폴링
export const getNoticeAnalysisProgress = (id: string) =>
  api.get(`/admin/notices/${id}/progress`)

// CR-159 컴플라이언스 매트릭스
export const getNoticeComplianceMatrix = (id: string) =>
  api.get(`/admin/notices/${id}/compliance-matrix`)

// 게이트②: 검수 후 노출/비노출
export const publishNotice = (id: string) =>
  api.post(`/admin/notices/${id}/publish`)

export const hideNotice = (id: string) =>
  api.post(`/admin/notices/${id}/hide`)

export const regenerateNotice = (id: string) =>
  api.post(`/admin/notices/${id}/regenerate`)

// CR-039: ANALYZING으로 멈춘(stuck) 공고문 강제 중단 → FAILED 전환
export const cancelNotice = (id: string) =>
  api.post(`/admin/notices/${id}/cancel`)

// CR-040: 공고문 삭제(hard). VISIBLE/ANALYZING이면 서버 409
export const deleteNotice = (id: string) =>
  api.delete(`/admin/notices/${id}`)

export const updateNoticeResult = (id: string, data: Record<string, unknown>) =>
  api.patch(`/admin/notices/${id}`, data)

// CR-033: 교정 채팅 위젯 BFF 토큰. ADMIN 인증 통과 시 Aimbase 단기 위젯 토큰(30분)을 대리 발급.
// 응답: { token, expires_at, refresh_after, scopes }
export const issueWidgetToken = () =>
  api.post('/admin/aimbase/widget-token')

// 성공 제안서 패턴 (CR-013)
export const getRfpSamples = (page = 0) =>
  api.get('/admin/rfp-samples', { params: { page, size: 20 } })

export const createRfpSample = (data: {
  opportunityNo: string
  industryType: string
  outcome: string
  company?: string
  agency?: string
  awardAmount?: number
  fiscalYear?: number
  note?: string
}) => api.post('/admin/rfp-samples', data)

export const getRfpSampleDetail = (id: string) =>
  api.get(`/admin/rfp-samples/${id}`)

export const uploadRfpSampleFile = (id: string, file: File, isPws = false) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/admin/rfp-samples/${id}/files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    params: { isPws },
  })
}

export const deleteRfpSample = (id: string) =>
  api.delete(`/admin/rfp-samples/${id}`)

export const deleteRfpSampleFile = (id: string, fileId: string) =>
  api.delete(`/admin/rfp-samples/${id}/files/${fileId}`)

// 패턴 가이드 (CR-013 재설계) — 공고유형 단위
export const getPatternGuides = () =>
  api.get('/admin/pattern-guides')

export const getPatternGuide = (industryType: string) =>
  api.get(`/admin/pattern-guides/${industryType}`)

export const extractPatternGuide = (industryType: string) =>
  api.post(`/admin/pattern-guides/${industryType}/extract`)

export const updatePatternGuide = (industryType: string, data: {
  guideJson?: Record<string, unknown>
  guideMarkdown?: string
}) => api.put(`/admin/pattern-guides/${industryType}`, data)

// 고객센터 1:1 문의 (CR-132)
export type InquiryStatus = 'RECEIVED' | 'ANSWERED'

export interface InquiryListItem {
  id: string
  createdAt: string
  typeCode: string
  typeLabel: string
  title: string
  name: string
  email: string
  member: boolean
  status: InquiryStatus
}

export interface InquiryCounts {
  all: number
  received: number
  answered: number
}

export interface InquiryDetail {
  id: string
  typeCode: string
  typeLabel: string
  title: string
  content: string
  name: string
  email: string
  memberId: string | null
  memberCompanyName: string | null
  status: InquiryStatus
  createdAt: string
  adminNotice: { total: number; success: number; lastSentAt: string | null }
  answer: string | null
  answeredAt: string | null
  answeredByEmail: string | null
}

export const INQUIRY_ALREADY_ANSWERED = 'INQUIRY_ALREADY_ANSWERED'

export const getInquiryTypes = () =>
  api.get<{ code: string; label: string }[]>('/inquiries/types')

export const getAdminInquiries = (params: { status?: InquiryStatus; typeCode?: string; page?: number; size?: number }) =>
  api.get<{ content: InquiryListItem[]; totalElements: number; totalPages: number; number: number; size: number }>(
    '/admin/inquiries', { params })

export const getAdminInquiryCounts = () =>
  api.get<InquiryCounts>('/admin/inquiries/counts')

export const getAdminInquiry = (id: string) =>
  api.get<InquiryDetail>(`/admin/inquiries/${id}`)

export const answerAdminInquiry = (id: string, answer: string) =>
  api.post<InquiryDetail>(`/admin/inquiries/${id}/answer`, { answer })

// ── CR-136: 기지 관리와 지난 낙찰 공지 다시 받기 ──

export interface AdminBase {
  id: string
  nameKo: string
  nameEn: string
  matchTerms: string[]
  region: string | null
  latitude: number | null
  longitude: number | null
  sortOrder: number
  matchedCount: number
}

export interface BaseInput {
  nameKo: string
  nameEn: string
  matchTerms: string[]
  region: string | null
  latitude: number | null
  longitude: number | null
  sortOrder: number | null
}

export interface UnmatchedRow {
  id: string
  noticeId: string
  solicitationNumber: string | null
  title: string
  titleKo: string | null
  postedDate: string | null
}

export interface AwardBackfillResult {
  samCalls: number
  fetched: number
  filled: number
  created: number
  withoutAwardee: number
}

export const getAdminBases = () => api.get<AdminBase[]>('/admin/bases')

export const createAdminBase = (body: BaseInput) => api.post<string>('/admin/bases', body)

export const updateAdminBase = (id: string, body: BaseInput) => api.put(`/admin/bases/${id}`, body)

export const deleteAdminBase = (id: string) => api.delete(`/admin/bases/${id}`)

export const getUnmatchedOpportunities = () =>
  api.get<{ count: number; rows: UnmatchedRow[] }>('/admin/bases/unmatched')

export const runAwardBackfill = (postedFrom: string, postedTo: string) =>
  api.post<AwardBackfillResult>('/admin/opportunities/award-backfill', { postedFrom, postedTo })

// ── CR-153: 고객 화면 사진 ──

/** 올리기 규칙(POL-039). 설정값에서 읽는다 */
export interface SitePhotoRules {
  allowedTypes: string[]
  maxUploadMb: number
  maxMegapixels: number
  minWidth: number
  saveWidth: number
  saveMaxKb: number
}

export interface AdminSitePhoto {
  id: string
  slotKey: string
  url: string
  fileName: string
  width: number
  height: number
  originalWidth: number | null
  originalHeight: number | null
  sizeBytes: number
  bundled: boolean
  visible: boolean
  createdAt: string
}

export interface AdminSitePhotoSlot {
  key: string
  name: string
  location: string
  photos: AdminSitePhoto[]
}

export const getAdminSitePhotos = () =>
  api.get<{ rules: SitePhotoRules; slots: AdminSitePhotoSlot[] }>('/admin/site-photos')

export const uploadAdminSitePhoto = (slotKey: string, file: File) => {
  const formData = new FormData()
  formData.append('slotKey', slotKey)
  formData.append('file', file)
  return api.post<AdminSitePhoto>('/admin/site-photos', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const setAdminSitePhotoVisible = (id: string, visible: boolean) =>
  api.patch<AdminSitePhoto>(`/admin/site-photos/${id}`, { visible })

export const deleteAdminSitePhoto = (id: string) => api.delete(`/admin/site-photos/${id}`)
