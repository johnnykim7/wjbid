import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminOpportunities } from '../api/client'

interface OpportunityAdmin {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  titleKo?: string          // CR-022: 한글 제목 (없으면 영문 fallback)
  typeKo?: string           // CR-022: SAM type 한글 라벨
  translatedAt?: string     // CR-022: 마지막 번역 시각
  organizationName?: string
  postedDate?: string
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
          <table className="w-full text-sm table-fixed">
            <colgroup>
              <col className="w-[44%]" />
              <col className="w-28" />
              <col className="w-28" />
              <col className="w-28" />
              <col className="w-24" />
              <col className="w-24" />
            </colgroup>
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고 (원문)</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">공고유형</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">공고일</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">마감일</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">첨부</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">생성여부</th>
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
                    <div className="font-medium text-gray-900 truncate">{opp.titleKo || opp.title}</div>
                    {opp.titleKo && (
                      <div className="text-xs text-gray-500 truncate">{opp.title}</div>
                    )}
                    <div className="text-xs text-gray-400 font-mono flex items-center gap-2 mt-0.5">
                      <span>{opp.solicitationNumber || opp.noticeId}</span>
                      {!opp.translatedAt && (
                        <span className="inline-flex px-1.5 py-0.5 rounded bg-amber-50 text-amber-600 text-[10px]" title="제목 한글화 미완료 — 영문 표시">미번역</span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    <div className="truncate" title={opp.typeKo || ''}>{opp.typeKo || '-'}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                    {opp.postedDate ? new Date(opp.postedDate).toLocaleDateString('ko') : '-'}
                  </td>
                  <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                    {opp.responseDeadline ? new Date(opp.responseDeadline).toLocaleDateString('ko') : '-'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    {opp.attachmentCount > 0 ? (
                      <span className="inline-flex items-center gap-1 text-gray-600 text-xs" title={`첨부 ${opp.attachmentCount}건`}>
                        <i className="fa-solid fa-paperclip" />{opp.attachmentCount}
                      </span>
                    ) : (
                      <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-600" title="첨부파일 없음">
                        없음
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      opp.noticeCount > 0 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {opp.noticeCount > 0 ? '생성' : '미생성'}
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
