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
    if (err.response?.status === 401) {
      localStorage.removeItem('adminToken')
      window.location.href = '/login'
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

// Collection
export const triggerCollection = (daysBack = 30) =>
  api.post('/admin/collection/trigger', null, { params: { daysBack } })

export const getCollectionStatus = () =>
  api.get('/admin/collection/status')

// Document Templates
export const getDocumentTemplates = () =>
  api.get('/admin/document-templates')

export const getDocumentTemplatesByType = (documentType: string) =>
  api.get(`/admin/document-templates/${documentType}`)

export const createDocumentTemplate = (data: {
  templateName: string
  documentType: string
  contentJson: Record<string, unknown>
}) => api.post('/admin/document-templates', data)

export const deactivateDocumentTemplate = (id: string) =>
  api.delete(`/admin/document-templates/${id}`)

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

// Opportunity Admin (CR-003)
export const getAdminOpportunities = (page = 0) =>
  api.get('/admin/opportunities', { params: { page, size: 20 } })

export const getAdminOpportunityDetail = (id: string) =>
  api.get(`/admin/opportunities/${id}`)

export const getOpportunityAnalysis = (id: string) =>
  api.get(`/admin/opportunities/${id}/analysis`)

export const triggerOpportunityAnalysis = (id: string) =>
  api.post(`/admin/opportunities/${id}/analyze`)

export const approveOpportunity = (id: string) =>
  api.post(`/admin/opportunities/${id}/approve`)

export const hideOpportunity = (id: string) =>
  api.post(`/admin/opportunities/${id}/hide`)

export const updateOpportunityAnalysis = (id: string, data: Record<string, unknown>) =>
  api.patch(`/admin/opportunities/${id}/analysis`, data)

export const uploadOpportunityAttachment = (id: string, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/admin/opportunities/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

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

export const getRfpSlots = (id: string) =>
  api.get(`/admin/rfp-samples/${id}/slots`)

export const assignSlot = (id: string, slotCode: string, data: {
  sampleFileId?: string
  sectionText?: string
  otherLabel?: string
  confirmed?: boolean
}) => api.put(`/admin/rfp-samples/${id}/slots/${slotCode}`, data)

export const unassignSlot = (id: string, assignmentId: string) =>
  api.delete(`/admin/rfp-samples/${id}/slots/${assignmentId}`)

// 패턴 가이드 (CR-013)
export const getPatternGuides = () =>
  api.get('/admin/pattern-guides')

export const getPatternGuide = (slotCode: string) =>
  api.get(`/admin/pattern-guides/${slotCode}`)

export const extractPatternGuide = (slotCode: string, industryType?: string) =>
  api.post(`/admin/pattern-guides/${slotCode}/extract`, null, {
    params: industryType ? { industryType } : {},
  })

export const updatePatternGuide = (slotCode: string, data: {
  guideJson?: Record<string, unknown>
  guideMarkdown?: string
}) => api.put(`/admin/pattern-guides/${slotCode}`, data)
