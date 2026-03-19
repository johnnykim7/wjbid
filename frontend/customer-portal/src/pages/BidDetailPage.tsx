import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { AiProposalModal } from '../components/modals/AiProposalModal'
import { getOpportunity, checkBookmark, addBookmark, removeBookmark } from '../api/client'
import type { Opportunity } from '../types'

export default function BidDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [bid, setBid] = useState<Opportunity | null>(null)
  const [loading, setLoading] = useState(true)
  const [isBookmarked, setIsBookmarked] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)
  const [aiModalOpen, setAiModalOpen] = useState(false)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const res = await getOpportunity(id!)
        setBid(res.data)
        try {
          const bRes = await checkBookmark(id!)
          setIsBookmarked(bRes.data?.bookmarked ?? false)
        } catch { /* 북마크 확인 실패 시 무시 */ }
      } catch {
        setBid(null)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  const toggleBookmark = async () => {
    if (!bid || bookmarkLoading) return
    setBookmarkLoading(true)
    try {
      if (isBookmarked) {
        await removeBookmark(bid.id)
        setIsBookmarked(false)
      } else {
        await addBookmark(bid.id)
        setIsBookmarked(true)
      }
    } catch { /* ignore */ } finally {
      setBookmarkLoading(false)
    }
  }

  if (loading) return <LoadingSpinner fullPage />
  if (!bid) return (
    <div className="flex flex-col items-center justify-center h-full text-gray-500 gap-4">
      <i className="fa-solid fa-circle-exclamation text-4xl text-gray-300" />
      <p className="text-lg font-semibold">공고 정보를 불러올 수 없습니다.</p>
      <button onClick={() => navigate('/search')} className="text-secondary underline text-sm">
        목록으로 돌아가기
      </button>
    </div>
  )

  const daysLeft = Math.ceil(
    (new Date(bid.responseDeadline).getTime() - Date.now()) / 86400000
  )

  return (
    <div className="flex flex-col h-full">
      {/* Sticky Header */}
      <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center shadow-sm z-10">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/search')}
            className="text-gray-500 hover:text-gray-800 transition"
          >
            <i className="fa-solid fa-arrow-left text-xl" />
          </button>
          <div>
            <h3 className="text-lg font-bold text-gray-800">입찰 공고 상세 내역</h3>
          </div>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={toggleBookmark}
            disabled={bookmarkLoading}
            className="hidden md:flex items-center gap-1"
          >
            {bookmarkLoading
              ? <i className="fa-solid fa-spinner fa-spin" />
              : <i className={isBookmarked ? 'fa-solid fa-bookmark text-secondary' : 'fa-regular fa-bookmark'} />
            }
            {isBookmarked ? '저장됨' : '관심 공고 저장'}
          </Button>
          <Button variant="accent" onClick={() => setAiModalOpen(true)}>
            <i className="fa-solid fa-wand-magic-sparkles mr-1" /> AI 제안서 작성
          </Button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-6 md:p-8">
        <div className="max-w-5xl mx-auto">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
            {/* Title & Badges */}
            <div className="mb-6">
              <div className="flex items-center gap-2 mb-3">
                <Badge variant="active">Active</Badge>
                <span className="text-sm font-bold text-gray-500">
                  Notice ID: {bid.solicitationNumber}
                </span>
              </div>
              <h1 className="text-2xl md:text-3xl font-extrabold text-gray-900 leading-tight mb-4">
                {bid.title}
              </h1>
              <div className="flex flex-wrap gap-6 text-sm text-gray-600">
                <div className="flex items-center gap-1">
                  <i className="fa-solid fa-building-columns text-gray-400 w-5" />
                  {bid.agencyName}
                </div>
                <div className="flex items-center gap-1">
                  <i className="fa-solid fa-calendar-days text-gray-400 w-5" />
                  마감일:{' '}
                  <span className="font-bold text-gray-900 ml-1">{bid.responseDeadline}</span>
                  {daysLeft >= 0 && (
                    <span className={`ml-1 font-bold ${daysLeft <= 7 ? 'text-red-500' : daysLeft <= 14 ? 'text-yellow-500' : 'text-gray-600'}`}>
                      (D-{daysLeft})
                    </span>
                  )}
                </div>
                {bid.naicsCode && (
                  <div className="flex items-center gap-1">
                    <i className="fa-solid fa-tag text-gray-400 w-5" />
                    NAICS: <span className="font-bold text-gray-900 ml-1">{bid.naicsCode}</span>
                  </div>
                )}
                {bid.setAside && (
                  <div className="flex items-center gap-1">
                    <i className="fa-solid fa-shield-halved text-gray-400 w-5" />
                    Set-Aside: <span className="font-bold text-gray-900 ml-1">{bid.setAside}</span>
                  </div>
                )}
              </div>
            </div>

            <hr className="border-gray-200 my-8" />

            <div className="space-y-6">
              {/* Description */}
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-3">Description (공고 내용)</h3>
                {bid.description ? (
                  <div className="text-gray-700 leading-relaxed text-sm bg-gray-50 p-5 rounded-lg border border-gray-200 whitespace-pre-wrap">
                    {bid.description}
                  </div>
                ) : (
                  <div className="text-sm bg-gray-50 p-5 rounded-lg border border-gray-200 flex items-start gap-3">
                    <i className="fa-solid fa-circle-info text-blue-400 mt-0.5" />
                    <div>
                      <p className="text-gray-600 mb-2">SAM.gov 수집 시 공고 본문이 제공되지 않은 공고입니다.</p>
                      {bid.uiLink && (
                        <a
                          href={bid.uiLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1.5 text-secondary hover:text-blue-700 font-medium"
                        >
                          <i className="fa-solid fa-arrow-up-right-from-square text-xs" />
                          SAM.gov에서 원문 보기
                        </a>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* Attachments */}
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-3">
                  Attachments / Links (첨부 파일)
                </h3>
                {bid.resourceLinks && bid.resourceLinks.length > 0 ? (
                  <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {bid.resourceLinks.map((url) => {
                      const filename = url.split('/').pop()?.split('?')[0] ?? url
                      const ext = filename.split('.').pop()?.toLowerCase() ?? ''
                      const isPdf = ext === 'pdf'
                      const isDoc = ['doc', 'docx'].includes(ext)
                      const icon = isPdf ? 'fa-solid fa-file-pdf' : isDoc ? 'fa-solid fa-file-word' : 'fa-solid fa-file'
                      const color = isPdf ? 'bg-red-50 text-red-500' : isDoc ? 'bg-blue-50 text-blue-500' : 'bg-gray-50 text-gray-500'
                      return (
                        <li key={url}>
                          <a
                            href={url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex items-center p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition"
                          >
                            <div className={`w-10 h-10 flex items-center justify-center rounded mr-3 ${color}`}>
                              <i className={`${icon} text-xl`} />
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="text-sm font-bold text-gray-800 truncate">{filename}</div>
                              <div className="text-xs text-gray-400 mt-0.5 truncate">{url}</div>
                            </div>
                            <i className="fa-solid fa-arrow-up-right-from-square text-gray-400 ml-2" />
                          </a>
                        </li>
                      )
                    })}
                  </ul>
                ) : (
                  <div className="text-sm bg-gray-50 p-5 rounded-lg border border-gray-200 flex items-start gap-3">
                    <i className="fa-solid fa-circle-info text-blue-400 mt-0.5" />
                    <div>
                      <p className="text-gray-600 mb-2">첨부 파일 정보가 없습니다.</p>
                      {bid.uiLink && (
                        <a
                          href={bid.uiLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1.5 text-secondary hover:text-blue-700 font-medium"
                        >
                          <i className="fa-solid fa-arrow-up-right-from-square text-xs" />
                          SAM.gov에서 첨부 파일 확인
                        </a>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {bid.uiLink && (
                <div className="pt-2 border-t border-gray-100">
                  <a
                    href={bid.uiLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition"
                  >
                    <i className="fa-solid fa-arrow-up-right-from-square" />
                    SAM.gov 원문 보기
                  </a>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <AiProposalModal
        isOpen={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
        opportunityId={bid.id}
        noticeId={bid.solicitationNumber}
        noticeTitle={bid.title}
      />
    </div>
  )
}
