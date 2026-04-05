import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminBidRequests, transitionBidRequest } from '../api/client'

const ALL_STATES = [
  'CREATED', 'DOCS_PENDING', 'DOCS_RECEIVED',
  'ANALYZING', 'GENERATING', 'REVIEW',
  'CONFIRMED', 'SUBMITTED', 'CLOSED',
]

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

// 상태별 유효한 다음 상태 (FSM)
const NEXT_STATES: Record<string, string[]> = {
  CREATED: ['DOCS_PENDING', 'CLOSED'],
  DOCS_PENDING: ['DOCS_RECEIVED', 'CLOSED'],
  DOCS_RECEIVED: ['ANALYZING', 'CLOSED'],
  ANALYZING: ['GENERATING'],
  GENERATING: ['REVIEW'],
  REVIEW: ['CONFIRMED', 'GENERATING'],
  CONFIRMED: ['SUBMITTED', 'REVIEW'],
}

interface BidRequest {
  id: string
  state: string
  opportunity?: { title: string; organizationName: string }
  member?: { email: string; companyName: string }
  assignedTo?: string
  createdAt?: string
}

interface TransitionModal {
  bidRequestId: string
  currentState: string
  opportunityTitle: string
}

export default function BidRequestAdminPage() {
  const [requests, setRequests] = useState<BidRequest[]>([])
  const [filterState, setFilterState] = useState<string>('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<TransitionModal | null>(null)
  const [selectedNextState, setSelectedNextState] = useState('')
  const [notes, setNotes] = useState('')
  const [transitioning, setTransitioning] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const load = async (state: string, p: number) => {
    setLoading(true)
    try {
      const { data } = await getAdminBidRequests(state || undefined, p)
      setRequests(data.content ?? [])
      setTotalPages(data.totalPages ?? 0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setPage(0)
    load(filterState, 0)
  }, [filterState])

  useEffect(() => {
    load(filterState, page)
  }, [page])

  const openTransition = (br: BidRequest) => {
    setModal({ bidRequestId: br.id, currentState: br.state, opportunityTitle: br.opportunity?.title ?? '-' })
    setSelectedNextState(NEXT_STATES[br.state]?.[0] ?? '')
    setNotes('')
    setError('')
  }

  const handleTransition = async () => {
    if (!modal || !selectedNextState) return
    setTransitioning(true)
    setError('')
    try {
      await transitionBidRequest(modal.bidRequestId, selectedNextState, notes)
      setModal(null)
      load(filterState, page)
    } catch {
      setError('상태 전환에 실패했습니다.')
    } finally {
      setTransitioning(false)
    }
  }

  const nextStates = modal ? (NEXT_STATES[modal.currentState] ?? []) : []

  return (
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">입찰 요청 관리</h1>
        <p className="text-sm text-gray-500 mt-0.5">입찰 요청의 상태를 조회하고 전환합니다.</p>
      </div>

      {/* 상태 필터 탭 */}
      <div className="flex flex-wrap gap-1.5">
        <button
          onClick={() => setFilterState('')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
            filterState === '' ? 'bg-secondary text-white' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
          }`}
        >
          전체
        </button>
        {ALL_STATES.map(s => (
          <button
            key={s}
            onClick={() => setFilterState(s)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
              filterState === s ? 'bg-secondary text-white' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
            }`}
          >
            {STATE_LABELS[s]}
          </button>
        ))}
      </div>

      {/* 테이블 */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48 text-gray-400">
            <i className="fa-solid fa-circle-notch fa-spin mr-2" /> 불러오는 중...
          </div>
        ) : requests.length === 0 ? (
          <div className="flex items-center justify-center h-48 text-sm text-gray-400">
            해당 조건의 입찰 요청이 없습니다.
          </div>
        ) : (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  {['공고 제목', '기관', '회원', '상태', ''].map((h, i) => (
                    <th key={i} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {requests.map((br) => {
                  const canTransition = (NEXT_STATES[br.state]?.length ?? 0) > 0
                  return (
                    <tr key={br.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium text-gray-900 max-w-xs">
                        <div className="truncate cursor-pointer text-secondary hover:underline"
                          onClick={() => navigate(`/bid-requests/${br.id}`)}>
                          {br.opportunity?.title ?? '-'}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-gray-500 text-xs">
                        {br.opportunity?.organizationName ?? '-'}
                      </td>
                      <td className="px-4 py-3 text-gray-500 text-xs">
                        {br.member?.email ?? '-'}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${STATE_COLORS[br.state] ?? 'bg-gray-100 text-gray-600'}`}>
                          {STATE_LABELS[br.state] ?? br.state}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        {canTransition && (
                          <button
                            onClick={() => openTransition(br)}
                            className="text-xs text-secondary hover:underline font-medium"
                          >
                            상태 전환
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 px-5 py-4 border-t border-gray-100">
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                  className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">이전</button>
                <span className="text-xs text-gray-500">{page + 1} / {totalPages}</span>
                <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                  className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">다음</button>
              </div>
            )}
          </>
        )}
      </div>

      {/* 상태 전환 모달 */}
      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h2 className="text-base font-bold text-gray-900">상태 전환</h2>
              <button onClick={() => setModal(null)} className="text-gray-400 hover:text-gray-600">
                <i className="fa-solid fa-xmark text-lg" />
              </button>
            </div>

            <div className="px-6 py-5 space-y-4">
              <div className="text-sm text-gray-600 bg-gray-50 rounded-lg px-4 py-3">
                <div className="font-medium text-gray-900 truncate mb-1">{modal.opportunityTitle}</div>
                <div className="flex items-center gap-2">
                  <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${STATE_COLORS[modal.currentState]}`}>
                    {STATE_LABELS[modal.currentState]}
                  </span>
                  <i className="fa-solid fa-arrow-right text-gray-400 text-xs" />
                  <span className="text-xs text-gray-500">다음 상태 선택</span>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">전환할 상태</label>
                <div className="space-y-2">
                  {nextStates.map(s => (
                    <label key={s} className="flex items-center gap-3 cursor-pointer">
                      <input
                        type="radio"
                        name="nextState"
                        value={s}
                        checked={selectedNextState === s}
                        onChange={() => setSelectedNextState(s)}
                        className="text-secondary"
                      />
                      <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${STATE_COLORS[s] ?? 'bg-gray-100 text-gray-600'}`}>
                        {STATE_LABELS[s]}
                      </span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">메모 (선택)</label>
                <textarea
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                  rows={3}
                  placeholder="전환 사유 또는 메모를 입력하세요."
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary resize-none"
                />
              </div>

              {error && <p className="text-sm text-red-500">{error}</p>}
            </div>

            <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
              <button onClick={() => setModal(null)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">취소</button>
              <button
                onClick={handleTransition}
                disabled={!selectedNextState || transitioning}
                className="px-5 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition disabled:opacity-50"
              >
                {transitioning ? <><i className="fa-solid fa-circle-notch fa-spin mr-2" />처리 중...</> : '전환'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
