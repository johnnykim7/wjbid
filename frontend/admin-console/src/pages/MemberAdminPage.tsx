import { useEffect, useState } from 'react'
import { getMembers } from '../api/client'

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '관리자',
  CUSTOMER: '고객',
}

interface Member {
  id: string
  email: string
  companyName: string
  contactPerson: string
  phone: string
  role: string
  createdAt: string
  lastLoginAt: string
}

export default function MemberAdminPage() {
  const [members, setMembers] = useState<Member[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = async (p: number) => {
    setLoading(true)
    try {
      const { data } = await getMembers(p)
      setMembers(data.content ?? [])
      setTotalPages(data.totalPages ?? 0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load(page) }, [page])

  return (
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">회원 관리</h1>
        <p className="text-sm text-gray-500 mt-0.5">가입된 회원 목록을 조회합니다.</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48 text-gray-400">
            <i className="fa-solid fa-circle-notch fa-spin mr-2" /> 불러오는 중...
          </div>
        ) : members.length === 0 ? (
          <div className="flex items-center justify-center h-48 text-sm text-gray-400">
            회원이 없습니다.
          </div>
        ) : (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  {['이메일', '회사명', '담당자', '역할', '가입일', '최근 로그인'].map(h => (
                    <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {members.map((m) => (
                  <tr key={m.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-900">{m.email}</td>
                    <td className="px-4 py-3 text-gray-600">{m.companyName ?? '-'}</td>
                    <td className="px-4 py-3 text-gray-600">{m.contactPerson ?? '-'}</td>
                    <td className="px-4 py-3">
                      <span className="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                        {ROLE_LABELS[m.role] ?? m.role}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">
                      {m.createdAt ? new Date(m.createdAt).toLocaleDateString('ko-KR') : '-'}
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">
                      {m.lastLoginAt ? new Date(m.lastLoginAt).toLocaleDateString('ko-KR') : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* 페이지네이션 */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 px-5 py-4 border-t border-gray-100">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50"
                >
                  이전
                </button>
                <span className="text-xs text-gray-500">{page + 1} / {totalPages}</span>
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50"
                >
                  다음
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
