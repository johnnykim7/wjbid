import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminOpportunities, triggerOpportunityAnalysis, approveOpportunity, hideOpportunity } from '../api/client'

interface OpportunityAdmin {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  organizationName?: string
  responseDeadline?: string
  visibility: 'HIDDEN' | 'VISIBLE'
  attachmentCount: number
  analysisStatus?: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED' | null
}

const VISIBILITY_COLORS: Record<string, string> = {
  HIDDEN: 'bg-gray-100 text-gray-600',
  VISIBLE: 'bg-green-100 text-green-700',
}

const ANALYSIS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  ANALYZING: 'bg-purple-100 text-purple-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}

const ANALYSIS_LABELS: Record<string, string> = {
  PENDING: '대기',
  ANALYZING: '분석 중',
  COMPLETED: '완료',
  FAILED: '실패',
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
      console.error('공고 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [page])

  const handleAnalyze = async (id: string) => {
    setActionLoading(id)
    try {
      await triggerOpportunityAnalysis(id)
      fetchData()
    } catch (err) {
      console.error('분석 트리거 실패:', err)
    } finally {
      setActionLoading(null)
    }
  }

  const handleToggleVisibility = async (id: string, current: string) => {
    setActionLoading(id)
    try {
      if (current === 'HIDDEN') await approveOpportunity(id)
      else await hideOpportunity(id)
      fetchData()
    } catch (err) {
      console.error('노출 상태 변경 실패:', err)
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">공고 관리</h1>
          <p className="text-sm text-gray-500 mt-1">사전 분석 및 노출 승인</p>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
          </div>
        ) : opportunities.length === 0 ? (
          <div className="text-center py-20 text-gray-400 text-sm">공고가 없습니다.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">기관</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">마감일</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">첨부</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">분석</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">노출</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {opportunities.map((opp) => (
                <tr
                  key={opp.id}
                  className="hover:bg-gray-50 cursor-pointer"
                  onClick={() => navigate(`/opportunities/${opp.id}`)}
                >
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
                  </td>
                  <td className="px-4 py-3 text-center">
                    {opp.analysisStatus ? (
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${ANALYSIS_COLORS[opp.analysisStatus] || ''}`}>
                        {ANALYSIS_LABELS[opp.analysisStatus] || opp.analysisStatus}
                      </span>
                    ) : (
                      <span className="text-xs text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${VISIBILITY_COLORS[opp.visibility]}`}>
                      {opp.visibility === 'VISIBLE' ? '노출' : '비노출'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center" onClick={(e) => e.stopPropagation()}>
                    <div className="flex items-center justify-center gap-1.5">
                      <button
                        onClick={() => handleAnalyze(opp.id)}
                        disabled={actionLoading === opp.id || opp.analysisStatus === 'ANALYZING'}
                        className="px-2.5 py-1 text-xs rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-100 disabled:opacity-40"
                        title="분석 실행"
                      >
                        <i className="fa-solid fa-brain mr-1" />분석
                      </button>
                      <button
                        onClick={() => handleToggleVisibility(opp.id, opp.visibility)}
                        disabled={actionLoading === opp.id}
                        className={`px-2.5 py-1 text-xs rounded-lg ${
                          opp.visibility === 'HIDDEN'
                            ? 'bg-green-50 text-green-700 hover:bg-green-100'
                            : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
                        } disabled:opacity-40`}
                        title={opp.visibility === 'HIDDEN' ? '승인 (노출)' : '숨김'}
                      >
                        {opp.visibility === 'HIDDEN' ? '승인' : '숨김'}
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
