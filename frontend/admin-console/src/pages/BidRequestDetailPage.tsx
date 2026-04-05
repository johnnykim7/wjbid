import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import {
  getAdminBidRequestDetail,
  getBidDocumentsByBidRequest,
  getClientDocuments,
  transitionBidRequest,
} from '../api/client'

const STATE_LABELS: Record<string, string> = {
  CREATED: '신청 접수',
  DOCS_PENDING: '문서 대기',
  DOCS_RECEIVED: '문서 접수',
  ANALYZING: '분석 중',
  GENERATING: '문서 생성',
  REVIEW: '관리자 검토',
  CONFIRMED: '확정',
  SUBMITTED: '제출 완료',
  CLOSED: '종료',
}

const STATE_COLORS: Record<string, string> = {
  CREATED: 'bg-gray-100 text-gray-600',
  DOCS_PENDING: 'bg-yellow-100 text-yellow-700',
  DOCS_RECEIVED: 'bg-blue-100 text-blue-700',
  ANALYZING: 'bg-purple-100 text-purple-700',
  GENERATING: 'bg-indigo-100 text-indigo-700',
  REVIEW: 'bg-orange-100 text-orange-700',
  CONFIRMED: 'bg-teal-100 text-teal-700',
  SUBMITTED: 'bg-emerald-100 text-emerald-700',
  CLOSED: 'bg-gray-100 text-gray-400',
}

const NEXT_STATES: Record<string, string[]> = {
  CREATED: ['DOCS_PENDING', 'CLOSED'],
  DOCS_PENDING: ['DOCS_RECEIVED', 'CLOSED'],
  DOCS_RECEIVED: ['ANALYZING', 'CLOSED'],
  ANALYZING: ['GENERATING'],
  GENERATING: ['REVIEW'],
  REVIEW: ['CONFIRMED', 'GENERATING'],
  CONFIRMED: ['SUBMITTED', 'REVIEW'],
}

const DOC_STATUS_LABELS: Record<string, string> = {
  DRAFT: '초안',
  LOCKED: '잠김',
  ARCHIVED: '보관',
}

interface BidRequest {
  id: string
  state: string
  serviceLevel?: string
  closeReason?: string
  closedAt?: string
  assignedTo?: string
  createdAt?: string
  updatedAt?: string
  opportunity?: {
    id: string
    title: string
    organizationName: string
    noticeId: string
    responseDeadline?: string
    samUrl?: string
  }
  member?: {
    id: string
    email: string
    companyName: string
    contactPerson?: string
    phone?: string
  }
}

interface BidDocument {
  id: string
  documentType: string
  status: string
  currentVersionNo: number
  createdAt?: string
}

interface ClientDocument {
  id: string
  originalFileName: string
  fileSize: number
  mimeType: string
  uploadedAt?: string
}

export default function BidRequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [bidRequest, setBidRequest] = useState<BidRequest | null>(null)
  const [documents, setDocuments] = useState<BidDocument[]>([])
  const [clientDocs, setClientDocs] = useState<ClientDocument[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Transition state
  const [showTransition, setShowTransition] = useState(false)
  const [selectedState, setSelectedState] = useState('')
  const [notes, setNotes] = useState('')
  const [transitioning, setTransitioning] = useState(false)

  const loadData = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getAdminBidRequestDetail(id)
      setBidRequest(data.data ?? data)

      try {
        const docRes = await getBidDocumentsByBidRequest(id)
        setDocuments(docRes.data.data ?? docRes.data.content ?? docRes.data ?? [])
      } catch { /* no documents yet */ }

      try {
        const clientRes = await getClientDocuments(id)
        setClientDocs(clientRes.data.data ?? clientRes.data ?? [])
      } catch { /* no client docs */ }
    } catch {
      setError('입찰 요청을 불러올 수 없습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [id])

  const handleTransition = async () => {
    if (!id || !selectedState) return
    setTransitioning(true)
    try {
      await transitionBidRequest(id, selectedState, notes)
      setShowTransition(false)
      setNotes('')
      loadData()
    } catch {
      setError('상태 전환에 실패했습니다.')
    } finally {
      setTransitioning(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-400">
        <i className="fa-solid fa-circle-notch fa-spin mr-2" /> 불러오는 중...
      </div>
    )
  }

  if (error && !bidRequest) {
    return (
      <div className="p-6">
        <p className="text-red-500">{error}</p>
        <button onClick={() => navigate('/bid-requests')} className="mt-4 text-sm text-secondary hover:underline">
          목록으로 돌아가기
        </button>
      </div>
    )
  }

  if (!bidRequest) return null

  const nextStates = NEXT_STATES[bidRequest.state] ?? []
  const opp = bidRequest.opportunity
  const member = bidRequest.member

  return (
    <div className="p-6 space-y-6 max-w-5xl">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/bid-requests')} className="text-gray-400 hover:text-gray-600">
            <i className="fa-solid fa-arrow-left" />
          </button>
          <div>
            <h1 className="text-lg font-bold text-gray-900">입찰 요청 상세</h1>
            <p className="text-xs text-gray-400 mt-0.5">ID: {bidRequest.id}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className={`inline-flex px-3 py-1 rounded-full text-sm font-medium ${STATE_COLORS[bidRequest.state] ?? 'bg-gray-100'}`}>
            {STATE_LABELS[bidRequest.state] ?? bidRequest.state}
          </span>
          {nextStates.length > 0 && (
            <button
              onClick={() => { setShowTransition(true); setSelectedState(nextStates[0]) }}
              className="px-4 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition"
            >
              상태 전환
            </button>
          )}
        </div>
      </div>

      {error && <p className="text-sm text-red-500 bg-red-50 px-4 py-2 rounded-lg">{error}</p>}

      {/* Info Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Opportunity Info */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
          <h2 className="text-sm font-semibold text-gray-900 flex items-center gap-2">
            <i className="fa-solid fa-file-lines text-secondary" /> 공고 정보
          </h2>
          <div className="space-y-2 text-sm">
            <div>
              <span className="text-gray-500">제목:</span>
              <p className="font-medium text-gray-900">{opp?.title ?? '-'}</p>
            </div>
            <div className="flex gap-6">
              <div>
                <span className="text-gray-500">기관:</span>
                <p className="text-gray-700">{opp?.organizationName ?? '-'}</p>
              </div>
              <div>
                <span className="text-gray-500">공고번호:</span>
                <p className="text-gray-700">{opp?.noticeId ?? '-'}</p>
              </div>
            </div>
            {opp?.responseDeadline && (
              <div>
                <span className="text-gray-500">마감일:</span>
                <p className="text-gray-700">{new Date(opp.responseDeadline).toLocaleDateString('ko-KR')}</p>
              </div>
            )}
            {opp?.samUrl && (
              <a href={opp.samUrl} target="_blank" rel="noopener noreferrer"
                className="text-xs text-secondary hover:underline">
                SAM.gov 원문 보기 <i className="fa-solid fa-arrow-up-right-from-square ml-1" />
              </a>
            )}
          </div>
        </div>

        {/* Member Info */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
          <h2 className="text-sm font-semibold text-gray-900 flex items-center gap-2">
            <i className="fa-solid fa-user text-secondary" /> 고객 정보
          </h2>
          <div className="space-y-2 text-sm">
            <div>
              <span className="text-gray-500">회사명:</span>
              <p className="font-medium text-gray-700">{member?.companyName ?? '-'}</p>
            </div>
            <div>
              <span className="text-gray-500">이메일:</span>
              <p className="text-gray-700">{member?.email ?? '-'}</p>
            </div>
            {member?.contactPerson && (
              <div>
                <span className="text-gray-500">담당자:</span>
                <p className="text-gray-700">{member.contactPerson}</p>
              </div>
            )}
            {member?.phone && (
              <div>
                <span className="text-gray-500">연락처:</span>
                <p className="text-gray-700">{member.phone}</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bid Documents */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
        <h2 className="text-sm font-semibold text-gray-900 flex items-center gap-2">
          <i className="fa-solid fa-file-word text-secondary" /> 생성 문서
        </h2>
        {documents.length === 0 ? (
          <p className="text-sm text-gray-400 py-4 text-center">아직 생성된 문서가 없습니다.</p>
        ) : (
          <div className="divide-y divide-gray-100">
            {documents.map(doc => (
              <div key={doc.id} className="flex items-center justify-between py-3">
                <div className="flex items-center gap-3">
                  <i className="fa-solid fa-file-alt text-gray-300" />
                  <div>
                    <p className="text-sm font-medium text-gray-900">{doc.documentType}</p>
                    <p className="text-xs text-gray-400">v{doc.currentVersionNo} | {DOC_STATUS_LABELS[doc.status] ?? doc.status}</p>
                  </div>
                </div>
                <Link
                  to={`/documents/${doc.id}/edit`}
                  className="text-xs text-secondary hover:underline font-medium"
                >
                  편집 <i className="fa-solid fa-pen-to-square ml-1" />
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Client Documents */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
        <h2 className="text-sm font-semibold text-gray-900 flex items-center gap-2">
          <i className="fa-solid fa-cloud-arrow-up text-secondary" /> 고객 제출 문서
        </h2>
        {clientDocs.length === 0 ? (
          <p className="text-sm text-gray-400 py-4 text-center">고객이 업로드한 문서가 없습니다.</p>
        ) : (
          <div className="divide-y divide-gray-100">
            {clientDocs.map(doc => (
              <div key={doc.id} className="flex items-center justify-between py-3">
                <div className="flex items-center gap-3">
                  <i className="fa-solid fa-paperclip text-gray-300" />
                  <div>
                    <p className="text-sm font-medium text-gray-700">{doc.originalFileName}</p>
                    <p className="text-xs text-gray-400">
                      {(doc.fileSize / 1024).toFixed(1)} KB | {doc.mimeType}
                    </p>
                  </div>
                </div>
                {doc.uploadedAt && (
                  <span className="text-xs text-gray-400">
                    {new Date(doc.uploadedAt).toLocaleDateString('ko-KR')}
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Meta Info */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h2 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <i className="fa-solid fa-circle-info text-secondary" /> 기타 정보
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <span className="text-gray-500">서비스 레벨</span>
            <p className="text-gray-700 font-medium">{bidRequest.serviceLevel ?? '-'}</p>
          </div>
          <div>
            <span className="text-gray-500">생성일</span>
            <p className="text-gray-700">{bidRequest.createdAt ? new Date(bidRequest.createdAt).toLocaleDateString('ko-KR') : '-'}</p>
          </div>
          {bidRequest.closedAt && (
            <div>
              <span className="text-gray-500">종료일</span>
              <p className="text-gray-700">{new Date(bidRequest.closedAt).toLocaleDateString('ko-KR')}</p>
            </div>
          )}
          {bidRequest.closeReason && (
            <div>
              <span className="text-gray-500">종료 사유</span>
              <p className="text-gray-700">{bidRequest.closeReason}</p>
            </div>
          )}
        </div>
      </div>

      {/* Transition Modal */}
      {showTransition && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h2 className="text-base font-bold text-gray-900">상태 전환</h2>
              <button onClick={() => setShowTransition(false)} className="text-gray-400 hover:text-gray-600">
                <i className="fa-solid fa-xmark text-lg" />
              </button>
            </div>
            <div className="px-6 py-5 space-y-4">
              <div className="flex items-center gap-2 text-sm">
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${STATE_COLORS[bidRequest.state]}`}>
                  {STATE_LABELS[bidRequest.state]}
                </span>
                <i className="fa-solid fa-arrow-right text-gray-400 text-xs" />
              </div>
              <div className="space-y-2">
                {nextStates.map(s => (
                  <label key={s} className="flex items-center gap-3 cursor-pointer">
                    <input type="radio" name="nextState" value={s}
                      checked={selectedState === s}
                      onChange={() => setSelectedState(s)}
                      className="text-secondary" />
                    <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${STATE_COLORS[s]}`}>
                      {STATE_LABELS[s]}
                    </span>
                  </label>
                ))}
              </div>
              <textarea
                value={notes}
                onChange={e => setNotes(e.target.value)}
                rows={3}
                placeholder="전환 사유 (선택)"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary resize-none"
              />
            </div>
            <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
              <button onClick={() => setShowTransition(false)} className="px-4 py-2 text-sm text-gray-600">취소</button>
              <button
                onClick={handleTransition}
                disabled={!selectedState || transitioning}
                className="px-5 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 disabled:opacity-50"
              >
                {transitioning ? '처리 중...' : '전환'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
