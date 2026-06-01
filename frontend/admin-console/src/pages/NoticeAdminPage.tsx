import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminNotices } from '../api/client'

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
  PENDING: '대기', ANALYZING: '분석 중', COMPLETED: '완료', FAILED: '실패',
}

export default function NoticeAdminPage() {
  const navigate = useNavigate()
  const [notices, setNotices] = useState<NoticeAdmin[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)

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
          <table className="w-full text-sm table-fixed">
            <colgroup>
              <col className="w-[45%]" />
              <col />
              <col className="w-24" />
              <col className="w-20" />
            </colgroup>
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고문</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">기관</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">진행상태</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">노출</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {notices.map((n) => (
                <tr key={n.id} className="hover:bg-gray-50 cursor-pointer" onClick={() => navigate(`/notices/${n.id}`)}>
                  <td className="px-4 py-3">
                    <div className="font-medium text-gray-900 truncate">{n.koreanTitle || n.originTitle}</div>
                    <div className="text-xs text-gray-400 truncate">{n.originTitle}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    <div className="truncate" title={n.organizationName || ''}>{n.organizationName || '-'}</div>
                  </td>
                  <td className="px-4 py-3 text-center whitespace-nowrap">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${GEN_COLORS[n.generationStatus] || ''}`}>
                      {GEN_LABELS[n.generationStatus] || n.generationStatus}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center whitespace-nowrap">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      n.visibility === 'VISIBLE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                    }`}>
                      {n.visibility === 'VISIBLE' ? '노출' : '비노출'}
                    </span>
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
