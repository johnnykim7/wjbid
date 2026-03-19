import { useEffect, useState } from 'react'
import { getBidRequestStats, getAdminBidRequests } from '../api/client'

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
  ANALYZING: 'bg-purple-100 text-purple-700',
  GENERATING: 'bg-blue-100 text-blue-700',
  REVIEW: 'bg-yellow-100 text-yellow-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  SUBMITTED: 'bg-emerald-100 text-emerald-700',
  CLOSED: 'bg-gray-100 text-gray-600',
}

interface BidRequest {
  id: string
  state: string
  opportunity?: { title: string; organizationName: string }
  member?: { email: string; companyName: string }
  createdAt?: string
}

export default function DashboardPage() {
  const [stats, setStats] = useState<Record<string, number>>({})
  const [recent, setRecent] = useState<BidRequest[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getBidRequestStats().then(r => setStats(r.data)),
      getAdminBidRequests(undefined, 0).then(r => setRecent(r.data.content ?? [])),
    ]).finally(() => setLoading(false))
  }, [])

  const total = stats.total ?? Object.values(stats).reduce((a, b) => a + b, 0)
  const active = (stats.ANALYZING ?? 0) + (stats.GENERATING ?? 0) +
    (stats.REVIEW ?? 0) + (stats.CONFIRMED ?? 0)
  const submitted = stats.SUBMITTED ?? 0

  const STAT_CARDS = [
    { label: '전체 입찰 요청', value: total, icon: 'fa-solid fa-file-contract', color: 'text-blue-600', bg: 'bg-blue-50' },
    { label: '진행 중', value: active, icon: 'fa-solid fa-spinner', color: 'text-purple-600', bg: 'bg-purple-50' },
    { label: '제출 완료', value: submitted, icon: 'fa-solid fa-circle-check', color: 'text-emerald-600', bg: 'bg-emerald-50' },
  ]

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-400">
        <i className="fa-solid fa-circle-notch fa-spin mr-2" /> 불러오는 중...
      </div>
    )
  }

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-xl font-bold text-gray-900">대시보드</h1>
        <p className="text-sm text-gray-500 mt-0.5">입찰 요청 현황을 한눈에 확인합니다.</p>
      </div>

      {/* 통계 카드 */}
      <div className="grid grid-cols-3 gap-4">
        {STAT_CARDS.map(({ label, value, icon, color, bg }) => (
          <div key={label} className="bg-white rounded-xl border border-gray-200 p-5">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm text-gray-500">{label}</span>
              <div className={`w-9 h-9 rounded-lg ${bg} flex items-center justify-center`}>
                <i className={`${icon} ${color} text-sm`} />
              </div>
            </div>
            <div className="text-3xl font-bold text-gray-900">{value}</div>
          </div>
        ))}
      </div>

      {/* 최근 입찰 요청 */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-900">최근 입찰 요청</h2>
        </div>
        {recent.length === 0 ? (
          <div className="flex items-center justify-center h-32 text-sm text-gray-400">
            입찰 요청이 없습니다.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase">공고 제목</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase">회원</th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase">상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {recent.slice(0, 8).map((br) => (
                <tr key={br.id} className="hover:bg-gray-50">
                  <td className="px-5 py-3 text-gray-900 max-w-xs truncate">
                    {br.opportunity?.title ?? '-'}
                  </td>
                  <td className="px-5 py-3 text-gray-500">
                    {br.member?.email ?? '-'}
                  </td>
                  <td className="px-5 py-3">
                    <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${STATE_COLORS[br.state] ?? 'bg-gray-100 text-gray-600'}`}>
                      {STATE_LABELS[br.state] ?? br.state}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
