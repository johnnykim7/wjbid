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
    // 갱신 후 다시 보낸 요청이 또 401 이면 토큰 문제가 아니므로 더 갱신하지 않는다.
    // 횟수 제한이 없으면 갱신과 재요청이 끝없이 돈다(CR-130).
    // 로그인·가입 요청의 401 은 잘못 넣은 비밀번호라서 토큰을 갱신할 일이 아니다.
    // 갱신하러 가면 남은 옛 토큰 때문에 화면이 새로 열려 실패 안내가 사라진다(CR-131).
    if (err.response?.status === 401 && err.config && !err.config._retried && !err.config.url?.startsWith('/auth/')) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${BASE_URL}/auth/refresh?refreshToken=${refreshToken}`)
          localStorage.setItem('accessToken', data.accessToken)
          err.config._retried = true
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

/** 이미 가입된 이메일로 가입하면 서버가 409 와 함께 주는 오류 코드 (CR-131) */
export const EMAIL_ALREADY_EXISTS = 'EMAIL_ALREADY_EXISTS'

export const register = (data: {
  email: string; password: string; companyName: string
  contactPerson: string; phone: string
  businessRegistrationNumber: string
  address?: string
  agreeTerms: boolean; agreePrivacy: boolean; agreeMarketing: boolean
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

// Pricing (CR-129)
export interface PricingInfo {
  /** 회원 월 요금(원, 부가세 별도). 미정이면 null */
  memberMonthlyPrice: number | null
}

export const getPricing = () =>
  api.get<PricingInfo>('/pricing')

// Members (Profile)
export const getMyProfile = () =>
  api.get('/members/me')

export const updateMyProfile = (data: {
  companyName: string; contactPerson?: string; phone?: string; address?: string
}) => api.patch('/members/me', data)

// Client Documents
export const getClientDocuments = (bidRequestId: string) =>
  api.get(`/bid-requests/${bidRequestId}/client-documents`)

export const uploadClientDocument = (bidRequestId: string, file: File, category?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  if (category) formData.append('category', category)
  return api.post(`/bid-requests/${bidRequestId}/client-documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const deleteClientDocument = (bidRequestId: string, docId: string) =>
  api.delete(`/bid-requests/${bidRequestId}/client-documents/${docId}`)

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

// CR-125 2단계: 서류함에서 미충족 슬롯 일괄 자동채움
export const autoFillSlots = (bidRequestId: string) =>
  api.post(`/bid-requests/${bidRequestId}/required-document-slots/auto-fill`)

// Member Documents (CR-125: 회사 공통 서류함)
export const getMemberDocumentVault = () => api.get('/member-documents')

export const uploadMemberDocument = (
  file: File,
  category: string,
  displayName?: string,
  description?: string,
) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  if (displayName) formData.append('displayName', displayName)
  if (description) formData.append('description', description)
  return api.post('/member-documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const replaceMemberDocument = (
  id: string,
  opts: { file?: File; displayName?: string; description?: string },
) => {
  const formData = new FormData()
  if (opts.file) formData.append('file', opts.file)
  if (opts.displayName) formData.append('displayName', opts.displayName)
  if (opts.description !== undefined) formData.append('description', opts.description)
  return api.put(`/member-documents/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const deleteMemberDocument = (id: string) =>
  api.delete(`/member-documents/${id}`)

// Notifications (CR-006: 고객 인앱 알림)
export const getNotifications = (page = 0, size = 20) =>
  api.get('/notifications', { params: { page, size } })

export const getUnreadNotificationCount = () =>
  api.get('/notifications/unread-count')

export const markNotificationAsRead = (id: string) =>
  api.patch(`/notifications/${id}/read`)
