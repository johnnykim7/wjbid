import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { StatCard } from '../components/ui/stat-card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../components/ui/table'
import { Pagination } from '../components/ui/pagination'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { EmptyState } from '../components/ui/empty-state'
import {
  Button,
  Input,
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@therecommerce-develop/ds-ui'
import {
  getOpportunities,
  searchOpportunities,
  addBookmark,
  removeBookmark,
  getMyBookmarks,
  getMyBidRequests,
} from '../api/client'
import type { Opportunity } from '../types'
import { DdayBadge } from '../utils/dday'

const AGENCIES = [
  '모든 발주처 (Agency)',
  'Dept. of Defense (DoD)',
  'Dept. of Veterans Affairs (VA)',
  'General Services Admin (GSA)',
  'Dept. of the Army',
  'Dept. of the Navy',
  'Dept. of Air Force',
]

export default function SearchPage() {
  const navigate = useNavigate()
  // CR-127 — 홈 검색바가 /search?keyword=... 로 넘겨준 키워드를 초기값으로 사용
  const [searchParams] = useSearchParams()
  const initialKeyword = searchParams.get('keyword') ?? ''
  const [items, setItems] = useState<Opportunity[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [keyword, setKeyword] = useState(initialKeyword)
  const [bookmarkedIds, setBookmarkedIds] = useState<Set<string>>(new Set())
  const [stats, setStats] = useState({ total: 0, bookmarks: 0, proposals: 0 })
  const [loadError, setLoadError] = useState<string | null>(null)

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
          setBookmarkedIds(new Set(content.map((b) => b.id)))
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
    setLoadError(null)
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
    } catch (err) {
      const msg = (err as { response?: { status?: number } })?.response?.status === 401
        ? '로그인이 필요합니다.'
        : '공고문을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
      setLoadError(msg)
      setItems([])
      setTotalPages(1)
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData(0, initialKeyword)
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

  const toggleBookmark = async (bid: Opportunity, e: React.MouseEvent) => {
    e.stopPropagation()
    const isBookmarked = bookmarkedIds.has(bid.id)
    try {
      if (isBookmarked) {
        await removeBookmark(bid.id)
        setBookmarkedIds((prev) => { const s = new Set(prev); s.delete(bid.id); return s })
        setStats((s) => ({ ...s, bookmarks: Math.max(0, s.bookmarks - 1) }))
      } else {
        await addBookmark(bid.id)
        setBookmarkedIds((prev) => new Set(prev).add(bid.id))
        setStats((s) => ({ ...s, bookmarks: s.bookmarks + 1 }))
      }
    } catch { /* ignore */ }
  }

  return (
    <div className="space-y-6">
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
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigate('/proposals')}
              className="text-xs bg-white/20 hover:bg-white/30 text-white"
            >
              보러가기
            </Button>
          }
        />
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm mb-4">
        <form onSubmit={handleSearch} className="flex flex-col md:flex-row gap-4 items-center">
          <div className="flex-1 w-full relative">
            <i className="fa-solid fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm z-10" />
            <Input
              type="text"
              placeholder="공고번호, 키워드(예: IT Support), 기관명 검색..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="w-full pl-10"
            />
          </div>
          <div className="flex gap-2 w-full md:w-auto flex-wrap">
            <Select defaultValue={AGENCIES[0]}>
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder="발주처 선택" />
              </SelectTrigger>
              <SelectContent>
                {AGENCIES.map((a) => (
                  <SelectItem key={a} value={a}>{a}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select defaultValue="active">
              <SelectTrigger className="w-[160px]">
                <SelectValue placeholder="상태 선택" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="active">Active (진행중)</SelectItem>
                <SelectItem value="updated">Updated (수정됨)</SelectItem>
              </SelectContent>
            </Select>
            <Button type="submit">
              <i className="fa-solid fa-search mr-1" /> 검색
            </Button>
          </div>
        </form>
      </div>

      {/* Table */}
      <div className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden flex flex-col">
        {loading ? (
          <LoadingSpinner fullPage />
        ) : loadError ? (
          <EmptyState
            icon="fa-solid fa-triangle-exclamation"
            title="공고문을 불러오지 못했습니다"
            description={loadError}
          />
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
                    <TableHead>공고명 / 발주처</TableHead>
                    <TableHead className="w-28">공고유형</TableHead>
                    <TableHead className="w-28">공고일</TableHead>
                    <TableHead className="w-36">마감일</TableHead>
                    <TableHead className="w-24 text-center">관심</TableHead>
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
                      <TableCell className="align-top text-sm text-gray-600">
                        {bid.type || '-'}
                      </TableCell>
                      <TableCell className="whitespace-nowrap align-top text-sm text-gray-600">
                        {bid.postedDate || '-'}
                      </TableCell>
                      <TableCell className="whitespace-nowrap align-top">
                        <div className="flex items-center gap-1.5">
                          <span className="text-sm text-gray-700">{bid.responseDeadline}</span>
                          <DdayBadge deadline={bid.responseDeadline} />
                        </div>
                      </TableCell>
                      <TableCell className="text-center align-middle">
                        <Button
                          variant="ghost"
                          size="icon"
                          title={bookmarkedIds.has(bid.id) ? '북마크 제거' : '관심 공고 저장'}
                          className={bookmarkedIds.has(bid.id) ? 'text-secondary' : 'text-gray-400 hover:text-secondary'}
                          onClick={(e) => toggleBookmark(bid, e)}
                        >
                          <i className={bookmarkedIds.has(bid.id) ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark'} />
                        </Button>
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
