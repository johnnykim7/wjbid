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

// Bid Request Detail (admin)
export const getAdminBidRequestDetail = (id: string) =>
  api.get(`/admin/bid-requests/${id}`)

// Bid Documents for a bid request
export const getBidDocumentsByBidRequest = (bidRequestId: string) =>
  api.get(`/bid-documents`, { params: { bidRequestId } })

// Client Documents
export const getClientDocuments = (bidRequestId: string) =>
  api.get(`/client-documents/${bidRequestId}`)

// Collection Runs
export const getCollectionRuns = (page = 0) =>
  api.get('/admin/collection/runs', { params: { page, size: 20 } })

// 원본 공고 Admin — 선별 풀 (CR-016)
export const getAdminOpportunities = (page = 0, q = '') =>
  api.get('/admin/opportunities', { params: { page, size: 20, ...(q ? { q } : {}) } })

export const getAdminOpportunityDetail = (id: string) =>
  api.get(`/admin/opportunities/${id}`)

// 게이트①: 공고문 만들기 (원본 선별 → 한글화 트리거)
export const createNotice = (opportunityId: string) =>
  api.post(`/admin/opportunities/${opportunityId}/create-notice`)

export const uploadOpportunityAttachment = (id: string, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/admin/opportunities/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// CR-019: 원본 공고 첨부 목록 (상태/외부 링크 포함)
export const getOpportunityAttachments = (id: string) =>
  api.get(`/admin/opportunities/${id}/attachments`)

// CR-022 (재구현): 본문 한글 번역 수동 트리거
export const retranslateOpportunityDescription = (id: string) =>
  api.post(`/admin/opportunities/${id}/retranslate-description`)

// 공고문(Notice) Admin (CR-016)
export const getAdminNotices = (page = 0) =>
  api.get('/admin/notices', { params: { page, size: 20 } })

export const getAdminNoticeDetail = (id: string) =>
  api.get(`/admin/notices/${id}`)

// 게이트②: 검수 후 노출/비노출
export const publishNotice = (id: string) =>
  api.post(`/admin/notices/${id}/publish`)

export const hideNotice = (id: string) =>
  api.post(`/admin/notices/${id}/hide`)

export const regenerateNotice = (id: string) =>
  api.post(`/admin/notices/${id}/regenerate`)

export const updateNoticeResult = (id: string, data: Record<string, unknown>) =>
  api.patch(`/admin/notices/${id}`, data)

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
