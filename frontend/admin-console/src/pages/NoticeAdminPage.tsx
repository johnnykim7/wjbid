import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminNotices, publishNotice, hideNotice, regenerateNotice } from '../api/client'

interface NoticeAdmin {
  id: string
  opportunityId: string
  originTitle: string
  solicitationNumber?: string
  organizationName?: string
  responseDeadline?: string
  koreanTitle?: string
  generationStatus: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  visibility: 'HIDDEN' | 'VISIBLE'
  analyzedAt?: string
}

const GEN_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  ANALYZING: 'bg-purple-100 text-purple-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}
const GEN_LABELS: Record<string, string> = {
  PENDING: '대기', ANALYZING: '한글화 중', COMPLETED: '완료', FAILED: '실패',
}

export default function NoticeAdminPage() {
  const navigate = useNavigate()
  const [notices, setNotices] = useState<NoticeAdmin[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const fetchData = async () => {
    setLoading(true)
    try {
      const { data } = await getAdminNotices(page)
      setNotices(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('공고문 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [page])

  const runAction = async (id: string, fn: (id: string) => Promise<unknown>) => {
    setActionLoading(id)
    try {
      await fn(id)
      fetchData()
    } catch (err) {
      console.error('공고문 작업 실패:', err)
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">공고문 (한글화 산출물)</h1>
          <p className="text-sm text-gray-500 mt-1">검수 후 노출. 한글화 완료 시 노출 가능</p>
        </div>
        <button
          onClick={() => navigate('/opportunities')}
          className="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
        >
          ← 원본 공고
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
          </div>
        ) : notices.length === 0 ? (
          <div className="text-center py-20 text-gray-400 text-sm">공고문이 없습니다. 원본 공고에서 "공고문 만들기"로 생성하세요.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고문</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">기관</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">한글화</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">노출</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {notices.map((n) => (
                <tr key={n.id} className="hover:bg-gray-50 cursor-pointer" onClick={() => navigate(`/notices/${n.id}`)}>
                  <td className="px-4 py-3">
                    <div className="font-medium text-gray-900 line-clamp-1">{n.koreanTitle || n.originTitle}</div>
                    <div className="text-xs text-gray-400 line-clamp-1">{n.originTitle}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{n.organizationName || '-'}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${GEN_COLORS[n.generationStatus] || ''}`}>
                      {GEN_LABELS[n.generationStatus] || n.generationStatus}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      n.visibility === 'VISIBLE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                    }`}>
                      {n.visibility === 'VISIBLE' ? '노출' : '비노출'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center" onClick={(e) => e.stopPropagation()}>
                    <div className="flex items-center justify-center gap-1.5">
                      {n.visibility === 'HIDDEN' ? (
                        <button
                          onClick={() => runAction(n.id, publishNotice)}
                          disabled={actionLoading === n.id || n.generationStatus !== 'COMPLETED'}
                          className="px-2.5 py-1 text-xs rounded-lg bg-green-50 text-green-700 hover:bg-green-100 disabled:opacity-40"
                          title={n.generationStatus !== 'COMPLETED' ? '한글화 완료 후 노출 가능' : '노출'}
                        >
                          노출
                        </button>
                      ) : (
                        <button
                          onClick={() => runAction(n.id, hideNotice)}
                          disabled={actionLoading === n.id}
                          className="px-2.5 py-1 text-xs rounded-lg bg-gray-50 text-gray-600 hover:bg-gray-100 disabled:opacity-40"
                        >
                          숨김
                        </button>
                      )}
                      <button
                        onClick={() => runAction(n.id, regenerateNotice)}
                        disabled={actionLoading === n.id || n.generationStatus === 'ANALYZING'}
                        className="px-2.5 py-1 text-xs rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-100 disabled:opacity-40"
                        title="한글화 재생성"
                      >
                        <i className="fa-solid fa-rotate" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 px-5 py-4 border-t border-gray-100">
            <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
              className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">
              이전
            </button>
            <span className="text-xs text-gray-500">{page + 1} / {totalPages}</span>
            <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
              className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">
              다음
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
