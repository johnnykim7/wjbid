import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge } from '../components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../components/ui/table'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { AiProposalModal } from '../components/modals/AiProposalModal'
import { getMyBookmarks, removeBookmark as removeBookmarkApi } from '../api/client'
import type { Opportunity } from '../types'

function getDeadlineBadgeVariant(deadline: string): 'closing' | 'active' {
  const days = Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000)
  return days <= 14 ? 'closing' : 'active'
}

export default function BookmarksPage() {
  const navigate = useNavigate()
  const [bookmarks, setBookmarks] = useState<Opportunity[]>([])
  const [loading, setLoading] = useState(true)
  const [removingId, setRemovingId] = useState<string | null>(null)
  const [aiModal, setAiModal] = useState<{ open: boolean; opportunityId: string; noticeId: string; title: string }>({
    open: false, opportunityId: '', noticeId: '', title: '',
  })

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getMyBookmarks()
        setBookmarks(res.data.content ?? [])
      } catch {
        setBookmarks([])
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const handleRemove = async (bid: Opportunity, e: React.MouseEvent) => {
    e.stopPropagation()
    setRemovingId(bid.id)
    try {
      await removeBookmarkApi(bid.id)
      setBookmarks((prev) => prev.filter((b) => b.id !== bid.id))
    } finally {
      setRemovingId(null)
    }
  }

  const openAiModal = (bid: Opportunity, e: React.MouseEvent) => {
    e.stopPropagation()
    setAiModal({ open: true, opportunityId: bid.id, noticeId: bid.solicitationNumber, title: bid.title })
  }

  if (loading) return <LoadingSpinner fullPage />

  return (
    <div className="p-6">
      <h2 className="text-xl font-bold text-gray-800 mb-6">
        관심 공고 (북마크)
        {bookmarks.length > 0 && (
          <span className="ml-2 text-sm font-normal text-gray-500">{bookmarks.length}건</span>
        )}
      </h2>

      {bookmarks.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-16 text-center text-gray-400 shadow-sm">
          <i className="fa-regular fa-bookmark text-4xl mb-3 block" />
          <p className="font-medium">저장된 관심 공고가 없습니다.</p>
          <p className="text-sm mt-1">입찰 검색에서 관심 공고를 저장해보세요.</p>
          <button
            onClick={() => navigate('/search')}
            className="mt-4 px-4 py-2 bg-secondary text-white rounded-lg text-sm hover:bg-blue-600 transition"
          >
            공고 검색하기
          </button>
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">
          <Table>
            <TableHeader>
              <tr>
                <TableHead className="w-48">Notice ID</TableHead>
                <TableHead>Title / Agency</TableHead>
                <TableHead className="w-32">Deadline</TableHead>
                <TableHead className="w-32 text-center">동작</TableHead>
              </tr>
            </TableHeader>
            <TableBody>
              {bookmarks.map((bid) => (
                <TableRow
                  key={bid.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/search/${bid.id}`)}
                >
                  <TableCell className="whitespace-nowrap align-top">
                    <div className="font-bold text-primary text-xs">{bid.solicitationNumber}</div>
                    <div className="mt-2">
                      <Badge variant="active" size="sm">Active</Badge>
                    </div>
                  </TableCell>
                  <TableCell className="align-top">
                    <div className="font-semibold text-gray-900 text-sm mb-1 line-clamp-2">{bid.title}</div>
                    <div className="text-xs text-gray-500 flex items-center gap-1">
                      <i className="fa-solid fa-building-columns text-gray-400" />
                      {bid.agencyName}
                    </div>
                  </TableCell>
                  <TableCell className="whitespace-nowrap align-top">
                    <Badge variant={getDeadlineBadgeVariant(bid.responseDeadline)}>
                      {bid.responseDeadline}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-center align-middle">
                    <button
                      title="북마크 제거"
                      className="text-secondary hover:text-red-500 transition mr-3 disabled:opacity-40"
                      disabled={removingId === bid.id}
                      onClick={(e) => handleRemove(bid, e)}
                    >
                      {removingId === bid.id
                        ? <i className="fa-solid fa-spinner fa-spin text-lg" />
                        : <i className="fa-solid fa-bookmark text-lg" />}
                    </button>
                    <button
                      title="제안서 작성"
                      className="text-secondary hover:text-blue-700 transition"
                      onClick={(e) => openAiModal(bid, e)}
                    >
                      <i className="fa-solid fa-wand-magic-sparkles text-lg" />
                    </button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <AiProposalModal
        isOpen={aiModal.open}
        onClose={() => setAiModal((m) => ({ ...m, open: false }))}
        opportunityId={aiModal.opportunityId}
        noticeId={aiModal.noticeId}
        noticeTitle={aiModal.title}
      />
    </div>
  )
}
