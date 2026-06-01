import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { StatCard } from '../components/ui/stat-card'
import { Badge } from '../components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../components/ui/table'
import { Pagination } from '../components/ui/pagination'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { EmptyState } from '../components/ui/empty-state'
import {
  getOpportunities,
  searchOpportunities,
  getMyBookmarks,
  getMyBidRequests,
} from '../api/client'
import { MOCK_BIDS } from '../data/mockData'
import type { Opportunity } from '../types'

const AGENCIES = [
  '모든 발주처 (Agency)',
  'Dept. of Defense (DoD)',
  'Dept. of Veterans Affairs (VA)',
  'General Services Admin (GSA)',
  'Dept. of the Army',
  'Dept. of the Navy',
  'Dept. of Air Force',
]

function getDeadlineBadgeVariant(deadline: string): 'closing' | 'active' {
  const days = Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000)
  return days <= 14 ? 'closing' : 'active'
}

export default function SearchPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Opportunity[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [stats, setStats] = useState({ total: 0, bookmarks: 0, proposals: 0 })

  // 초기 북마크 & 통계 로드
  useEffect(() => {
    const loadSideData = async () => {
      try {
        const [bookmarksRes, proposalsRes] = await Promise.allSettled([
          getMyBookmarks(),
          getMyBidRequests(),
        ])
        if (bookmarksRes.status === 'fulfilled') {
          const content: Opportunity[] = bookmarksRes.value.data.content ?? []
          setStats((s) => ({ ...s, bookmarks: bookmarksRes.value.data.totalElements ?? content.length }))
        }
        if (proposalsRes.status === 'fulfilled') {
          setStats((s) => ({ ...s, proposals: proposalsRes.value.data.totalElements ?? 0 }))
        }
      } catch { /* ignore */ }
    }
    loadSideData()
  }, [])

  const fetchData = async (p = 0, kw = keyword) => {
    setLoading(true)
    try {
      let data
      if (kw.trim()) {
        const res = await searchOpportunities(kw, p)
        data = res.data
      } else {
        const res = await getOpportunities(p)
        data = res.data
      }
      setItems(data.content ?? [])
      setTotalPages(data.totalPages ?? 1)
      setTotalElements(data.totalElements ?? 0)
      setStats((s) => ({ ...s, total: data.totalElements ?? 0 }))
    } catch {
      setItems(MOCK_BIDS)
      setTotalPages(1)
      setTotalElements(MOCK_BIDS.length)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData(0)
  }, [])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    fetchData(0, keyword)
  }

  const handlePageChange = (p: number) => {
    setPage(p)
    fetchData(p)
  }

  return (
    <div className="p-6">
      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <StatCard
          label="SAM.gov 활성 공고"
          value={stats.total > 0 ? stats.total.toLocaleString() : '-'}
          unit="건"
        />
        <StatCard
          label="관심 공고 (북마크)"
          value={stats.bookmarks}
          unit="건"
          iconColor="text-secondary"
        />
        <StatCard
          label="나의 입찰 요청"
          value={stats.proposals}
          unit="건"
          iconColor="text-accent"
        />
        <StatCard
          dark
          label="제안서 작성"
          value="→"
          unit="내 제안서"
          action={
            <button
              onClick={() => navigate('/proposals')}
              className="text-xs bg-white/20 hover:bg-white/30 px-2 py-1 rounded transition text-white"
            >
              보러가기
            </button>
          }
        />
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm mb-4">
        <form onSubmit={handleSearch} className="flex flex-col md:flex-row gap-4 items-center">
          <div className="flex-1 w-full relative">
            <i className="fa-solid fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm" />
            <input
              type="text"
              placeholder="공고번호, 키워드(예: IT Support), 기관명 검색..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary focus:bg-white transition text-sm"
            />
          </div>
          <div className="flex gap-2 w-full md:w-auto flex-wrap">
            <select
              className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-secondary"
            >
              {AGENCIES.map((a) => (
                <option key={a}>{a}</option>
              ))}
            </select>
            <select className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-secondary">
              <option>Active (진행중)</option>
              <option>Updated (수정됨)</option>
            </select>
            <button
              type="submit"
              className="px-4 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition"
            >
              <i className="fa-solid fa-search mr-1" /> 검색
            </button>
          </div>
        </form>
      </div>

      {/* Table */}
      <div className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden flex flex-col">
        {loading ? (
          <LoadingSpinner fullPage />
        ) : items.length === 0 ? (
          <EmptyState
            icon="fa-solid fa-magnifying-glass"
            title="검색 결과가 없습니다"
            description="다른 키워드로 검색해보세요."
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <tr>
                    <TableHead className="w-48">공고번호 / NAICS</TableHead>
                    <TableHead>제목 / 기관</TableHead>
                    <TableHead className="w-32">공고유형</TableHead>
                    <TableHead className="w-32">공고일</TableHead>
                    <TableHead className="w-32">마감일</TableHead>
                  </tr>
                </TableHeader>
                <TableBody>
                  {items.map((bid) => (
                    <TableRow
                      key={bid.id}
                      className="cursor-pointer"
                      onClick={() => navigate(`/search/${bid.id}`)}
                    >
                      <TableCell className="whitespace-nowrap align-top">
                        <div className="font-bold text-primary text-xs">{bid.solicitationNumber}</div>
                        {bid.naicsCode && (
                          <div className="text-xs text-gray-400 mt-1">NAICS: {bid.naicsCode}</div>
                        )}
                      </TableCell>
                      <TableCell className="align-top">
                        <div className="font-semibold text-gray-900 text-sm mb-1 line-clamp-2">
                          {bid.title}
                        </div>
                        <div className="text-xs text-gray-500 flex items-center gap-1">
                          <i className="fa-solid fa-building-columns text-gray-400" />
                          {bid.agencyName}
                        </div>
                      </TableCell>
                      <TableCell className="whitespace-nowrap align-top text-sm text-gray-600">
                        {bid.type || '-'}
                      </TableCell>
                      <TableCell className="whitespace-nowrap align-top text-sm text-gray-600">
                        {bid.postedDate || '-'}
                      </TableCell>
                      <TableCell className="whitespace-nowrap align-top">
                        <Badge variant={getDeadlineBadgeVariant(bid.responseDeadline)}>
                          {bid.responseDeadline}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            <Pagination
              page={page}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={handlePageChange}
            />
          </>
        )}
      </div>
    </div>
  )
}
