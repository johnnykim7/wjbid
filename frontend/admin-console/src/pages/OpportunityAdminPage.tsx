import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminOpportunities, createNotice } from '../api/client'

interface OpportunityAdmin {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  organizationName?: string
  responseDeadline?: string
  attachmentCount: number
  manualFetchRequiredCount: number
  noticeCount: number
}

export default function OpportunityAdminPage() {
  const navigate = useNavigate()
  const [opportunities, setOpportunities] = useState<OpportunityAdmin[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const fetchData = async () => {
    setLoading(true)
    try {
      const { data } = await getAdminOpportunities(page)
      setOpportunities(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('원본 공고 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [page])

  const handleCreateNotice = async (id: string) => {
    setActionLoading(id)
    try {
      await createNotice(id)
      fetchData()
    } catch (err) {
      console.error('공고문 생성 실패:', err)
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">원본 공고 (선별 풀)</h1>
          <p className="text-sm text-gray-500 mt-1">SAM.gov 수집 원본. "공고문 만들기"로 선별 → 한글화</p>
        </div>
        <button
          onClick={() => navigate('/notices')}
          className="px-4 py-2 text-sm rounded-lg bg-secondary text-white hover:bg-blue-600"
        >
          공고문 리스트 →
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
          </div>
        ) : opportunities.length === 0 ? (
          <div className="text-center py-20 text-gray-400 text-sm">원본 공고가 없습니다.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고 (원문)</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">기관</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">마감일</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">첨부</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고문</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {opportunities.map((opp) => (
                <tr key={opp.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="font-medium text-gray-900 line-clamp-1">{opp.title}</div>
                    <div className="text-xs text-gray-400 font-mono">{opp.solicitationNumber || opp.noticeId}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{opp.organizationName || '-'}</td>
                  <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                    {opp.responseDeadline ? new Date(opp.responseDeadline).toLocaleDateString('ko') : '-'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${
                      opp.attachmentCount > 0 ? 'bg-blue-50 text-blue-700' : 'bg-gray-50 text-gray-400'
                    }`}>
                      {opp.attachmentCount}건
                    </span>
                    {opp.manualFetchRequiredCount > 0 && (
                      <span
                        className="ml-1 inline-flex px-2 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-700"
                        title="외부 사이트 첨부 — 직접 가져와 업로드해야 함"
                      >
                        가져와야 함 {opp.manualFetchRequiredCount}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      opp.noticeCount > 0 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {opp.noticeCount > 0 ? `${opp.noticeCount}개 생성됨` : '미생성'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => handleCreateNotice(opp.id)}
                      disabled={actionLoading === opp.id}
                      className="px-2.5 py-1 text-xs rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-100 disabled:opacity-40"
                      title="공고문 만들기 (한글화)"
                    >
                      <i className="fa-solid fa-wand-magic-sparkles mr-1" />공고문 만들기
                    </button>
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
