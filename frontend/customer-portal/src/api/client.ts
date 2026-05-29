import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${BASE_URL}/auth/refresh?refreshToken=${refreshToken}`)
          localStorage.setItem('accessToken', data.accessToken)
          err.config.headers.Authorization = `Bearer ${data.accessToken}`
          return api.request(err.config)
        } catch {
          localStorage.clear()
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(err)
  }
)

// Auth
export const login = (email: string, password: string) =>
  api.post('/auth/login', { email, password })

export const register = (data: {
  email: string; password: string; companyName: string
  contactPerson: string; phone: string; address: string
}) => api.post('/auth/register', data)

// Opportunities
export const getOpportunities = (page = 0, size = 20) =>
  api.get('/opportunities', { params: { page, size } })

export const getOpportunity = (id: string) =>
  api.get(`/opportunities/${id}`)

export const searchOpportunities = (keyword: string, page = 0) =>
  api.get('/opportunities/search', { params: { keyword, page, size: 20 } })

export const getNearDeadline = (days = 7) =>
  api.get('/opportunities/near-deadline', { params: { days } })

// Bid Requests
export const createBidRequest = (opportunityId: string) =>
  api.post('/bid-requests', { opportunityId })

export const getMyBidRequests = (page = 0) =>
  api.get('/bid-requests/my', { params: { page, size: 20 } })

export const getBidRequest = (id: string) =>
  api.get(`/bid-requests/${id}`)

export const transitionBidRequestState = (id: string, toState: string, notes?: string) =>
  api.patch(`/bid-requests/${id}/state`, { toState, notes })

export const getBidRequestNextStates = (id: string) =>
  api.get(`/bid-requests/${id}/next-states`)

export const getBidRequestHistory = (id: string) =>
  api.get(`/bid-requests/${id}/history`)

export const getBidRequestDocuments = (id: string) =>
  api.get(`/bid-requests/${id}/documents`)

// Bookmarks
export const getMyBookmarks = (page = 0) =>
  api.get('/bookmarks', { params: { page, size: 20 } })

export const checkBookmark = (opportunityId: string) =>
  api.get(`/bookmarks/${opportunityId}/status`)

export const addBookmark = (opportunityId: string) =>
  api.post(`/bookmarks/${opportunityId}`)

export const removeBookmark = (opportunityId: string) =>
  api.delete(`/bookmarks/${opportunityId}`)

// Pricing
export const getPricing = () =>
  api.get('/pricing')

// Members (Profile)
export const getMyProfile = () =>
  api.get('/members/me')

export const updateMyProfile = (data: {
  companyName: string; contactPerson?: string; phone?: string; address?: string
}) => api.patch('/members/me', data)

// Client Documents
export const getClientDocuments = (bidRequestId: string) =>
  api.get(`/client-documents/${bidRequestId}`)

export const uploadClientDocument = (bidRequestId: string, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/client-documents/${bidRequestId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const deleteClientDocument = (id: string) =>
  api.delete(`/client-documents/${id}`)

// Required Document Slots (CR-010: 공고 요구서류 슬롯 매칭)
export const getRequiredDocumentSlots = (bidRequestId: string) =>
  api.get(`/bid-requests/${bidRequestId}/required-document-slots`)

export const uploadToSlot = (bidRequestId: string, requirementItemId: string, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(
    `/bid-requests/${bidRequestId}/required-document-slots/${requirementItemId}/upload`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}

export const unmapSlot = (bidRequestId: string, requirementItemId: string) =>
  api.delete(`/bid-requests/${bidRequestId}/required-document-slots/${requirementItemId}`)

// Notifications (CR-006: 고객 인앱 알림)
export const getNotifications = (page = 0, size = 20) =>
  api.get('/notifications', { params: { page, size } })

export const getUnreadNotificationCount = () =>
  api.get('/notifications/unread-count')

export const markNotificationAsRead = (id: string) =>
  api.patch(`/notifications/${id}/read`)
