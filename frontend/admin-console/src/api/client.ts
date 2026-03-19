import axios from 'axios'

const BASE_URL = 'http://14.63.25.49:8088/api'

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
