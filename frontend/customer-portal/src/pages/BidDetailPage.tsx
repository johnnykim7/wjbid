import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { AiProposalModal } from '../components/modals/AiProposalModal'
import { getOpportunity, checkBookmark, addBookmark, removeBookmark } from '../api/client'
import type { Opportunity } from '../types'
import NoticeDocumentView from '../components/NoticeDocumentView'

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
            <i className="fa-solid fa-wand-magic-sparkles mr-1" /> 제안서 작성
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

            {bid.analysis?.analysisStatus === 'COMPLETED' ? (
              /* ── 정리된 공고문 (분석 완료) ── */
              <div className="space-y-8">
                {/* 분석 배지 */}
                <div className="flex items-center gap-2">
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-green-50 text-green-700 rounded-full text-xs font-semibold">
                    <i className="fa-solid fa-wand-magic-sparkles" /> 분석 완료
                  </span>
                  {bid.analysis.analyzedAt && (
                    <span className="text-xs text-gray-400">{bid.analysis.analyzedAt} 분석</span>
                  )}
                </div>

                {/* CR-021: 공고문 본문 (PDF 양식 풍부도) — contentJson 있을 때만 */}
                {bid.analysis.contentJson && (
                  <NoticeDocumentView contentJson={bid.analysis.contentJson} />
                )}

                {/* 공고 요약 */}
                {bid.analysis.summary && (
                  <div>
                    <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-file-lines text-secondary" /> 공고 요약
                    </h3>
                    <div className="space-y-4">
                      {bid.analysis.summary.overview && (
                        <div className="bg-blue-50 border border-blue-100 rounded-lg p-5">
                          <p className="text-sm text-gray-800 leading-relaxed">{bid.analysis.summary.overview}</p>
                        </div>
                      )}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {bid.analysis.summary.scope && (
                          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                            <div className="text-xs font-semibold text-gray-500 uppercase mb-2">작업 범위</div>
                            <p className="text-sm text-gray-700 leading-relaxed">{bid.analysis.summary.scope}</p>
                          </div>
                        )}
                        {bid.analysis.summary.eligibility && (
                          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                            <div className="text-xs font-semibold text-gray-500 uppercase mb-2">참여 자격</div>
                            <p className="text-sm text-gray-700 leading-relaxed">{bid.analysis.summary.eligibility}</p>
                          </div>
                        )}
                        {bid.analysis.summary.evaluationCriteria && (
                          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                            <div className="text-xs font-semibold text-gray-500 uppercase mb-2">평가 기준</div>
                            <p className="text-sm text-gray-700 leading-relaxed">{bid.analysis.summary.evaluationCriteria}</p>
                          </div>
                        )}
                        {bid.analysis.summary.budgetInfo && (
                          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                            <div className="text-xs font-semibold text-gray-500 uppercase mb-2">예산 정보</div>
                            <p className="text-sm text-gray-700 leading-relaxed">{bid.analysis.summary.budgetInfo}</p>
                          </div>
                        )}
                      </div>
                      {bid.analysis.summary.specialNotes && bid.analysis.summary.specialNotes.length > 0 && (
                        <div className="bg-yellow-50 border border-yellow-100 rounded-lg p-4">
                          <div className="text-xs font-semibold text-yellow-700 uppercase mb-2">
                            <i className="fa-solid fa-triangle-exclamation mr-1" /> 특이사항
                          </div>
                          <ul className="list-disc list-inside space-y-1">
                            {bid.analysis.summary.specialNotes.map((note, i) => (
                              <li key={i} className="text-sm text-gray-700">{note}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* 주요 일정 */}
                {bid.analysis.summary?.keyDates && bid.analysis.summary.keyDates.length > 0 && (
                  <div>
                    <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-calendar-days text-secondary" /> 주요 일정
                    </h3>
                    <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="bg-gray-50 border-b border-gray-200">
                            <th className="text-left px-4 py-2.5 text-xs font-semibold text-gray-500 uppercase">항목</th>
                            <th className="text-left px-4 py-2.5 text-xs font-semibold text-gray-500 uppercase">일자</th>
                            <th className="text-left px-4 py-2.5 text-xs font-semibold text-gray-500 uppercase">비고</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                          {bid.analysis.summary.keyDates.map((d, i) => (
                            <tr key={i} className="hover:bg-gray-50">
                              <td className="px-4 py-2.5 font-medium text-gray-800">{d.label}</td>
                              <td className="px-4 py-2.5 text-gray-600">{d.date}</td>
                              <td className="px-4 py-2.5 text-gray-500">{d.note || '-'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {/* 필요 서류 체크리스트 */}
                {bid.analysis.requiredDocuments?.documents && bid.analysis.requiredDocuments.documents.length > 0 && (
                  <div>
                    <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-list-check text-secondary" /> 필요 서류 체크리스트
                    </h3>
                    <div className="space-y-3">
                      {bid.analysis.requiredDocuments.documents.map((doc, i) => (
                        <div key={i} className="flex items-start gap-3 p-4 bg-white border border-gray-200 rounded-lg">
                          <div className="mt-0.5">
                            <i className={`fa-regular fa-square text-lg ${doc.mandatory ? 'text-red-400' : 'text-gray-300'}`} />
                          </div>
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="font-bold text-sm text-gray-900">{doc.name}</span>
                              {doc.mandatory ? (
                                <span className="px-1.5 py-0.5 bg-red-50 text-red-600 rounded text-[10px] font-semibold uppercase">필수</span>
                              ) : (
                                <span className="px-1.5 py-0.5 bg-gray-100 text-gray-500 rounded text-[10px] font-semibold uppercase">선택</span>
                              )}
                            </div>
                            {doc.description && <p className="text-sm text-gray-600 mb-1">{doc.description}</p>}
                            <div className="flex gap-4 text-xs text-gray-400">
                              {doc.format && <span><i className="fa-solid fa-file mr-1" />{doc.format}</span>}
                              {doc.pageLimit && <span><i className="fa-solid fa-ruler mr-1" />{doc.pageLimit}</span>}
                            </div>
                            {doc.notes && <p className="text-xs text-gray-400 mt-1 italic">{doc.notes}</p>}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 문서 양식 안내 */}
                {bid.analysis.documentFormats && (
                  <div>
                    <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-file-contract text-secondary" /> 문서 양식 안내
                    </h3>
                    {bid.analysis.documentFormats.generalInstructions && (
                      <div className="bg-gray-50 rounded-lg p-4 border border-gray-200 mb-4">
                        <p className="text-sm text-gray-700 leading-relaxed">{bid.analysis.documentFormats.generalInstructions}</p>
                      </div>
                    )}
                    {bid.analysis.documentFormats.formats && bid.analysis.documentFormats.formats.length > 0 && (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        {bid.analysis.documentFormats.formats.map((fmt, i) => (
                          <div key={i} className="bg-white border border-gray-200 rounded-lg p-4">
                            <div className="font-bold text-sm text-gray-900 mb-2">{fmt.section}</div>
                            {fmt.description && <p className="text-sm text-gray-600 mb-2">{fmt.description}</p>}
                            <div className="flex flex-wrap gap-3 text-xs text-gray-500">
                              {fmt.pageLimit && <span className="bg-gray-100 px-2 py-0.5 rounded">{fmt.pageLimit}</span>}
                              {fmt.fileFormat && <span className="bg-gray-100 px-2 py-0.5 rounded">{fmt.fileFormat}</span>}
                              {fmt.fontRequirements && <span className="bg-gray-100 px-2 py-0.5 rounded">{fmt.fontRequirements}</span>}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    {bid.analysis.documentFormats.submissionMethod && (
                      <div className="mt-4 flex items-center gap-2 text-sm text-gray-600">
                        <i className="fa-solid fa-paper-plane text-secondary" />
                        <span>제출 방법: <span className="font-medium text-gray-800">{bid.analysis.documentFormats.submissionMethod}</span></span>
                      </div>
                    )}
                  </div>
                )}

                {/* 첨부 파일 */}
                <div>
                  <h3 className="text-lg font-bold text-gray-900 mb-3 flex items-center gap-2">
                    <i className="fa-solid fa-paperclip text-secondary" /> 첨부 파일
                  </h3>
                  {bid.resourceLinks && bid.resourceLinks.length > 0 ? (
                    <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {bid.resourceLinks.map((url) => {
                        const filename = url.split('/').pop()?.split('?')[0] ?? url
                        return (
                          <li key={url}>
                            <a href={url} target="_blank" rel="noopener noreferrer"
                              className="flex items-center p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition">
                              <div className="w-10 h-10 flex items-center justify-center rounded mr-3 bg-red-50 text-red-500">
                                <i className="fa-solid fa-file-pdf text-xl" />
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="text-sm font-bold text-gray-800 truncate">{filename}</div>
                              </div>
                              <i className="fa-solid fa-download text-gray-400 ml-2" />
                            </a>
                          </li>
                        )
                      })}
                    </ul>
                  ) : (
                    <p className="text-sm text-gray-500">첨부 파일이 없습니다.</p>
                  )}
                </div>

                {/* SAM.gov 원본 (접이식) */}
                <details className="border border-gray-200 rounded-lg">
                  <summary className="px-5 py-3 text-sm font-medium text-gray-600 cursor-pointer hover:bg-gray-50 select-none">
                    <i className="fa-solid fa-chevron-right mr-2 text-xs" />
                    SAM.gov 원본 보기
                  </summary>
                  <div className="px-5 pb-4">
                    {bid.description ? (
                      <div className="text-sm text-gray-700 bg-gray-50 p-4 rounded-lg mt-2 whitespace-pre-wrap leading-relaxed">
                        {bid.description}
                      </div>
                    ) : (
                      <p className="text-sm text-gray-500 mt-2">원본 본문이 제공되지 않은 공고입니다.</p>
                    )}
                    {bid.uiLink && (
                      <a href={bid.uiLink} target="_blank" rel="noopener noreferrer"
                        className="inline-flex items-center gap-2 mt-3 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition">
                        <i className="fa-solid fa-arrow-up-right-from-square" /> SAM.gov 원문 보기
                      </a>
                    )}
                  </div>
                </details>
              </div>
            ) : (
              /* ── 원본 표시 (분석 미완료 / fallback) ── */
              <div className="space-y-6">
                {bid.analysis?.analysisStatus === 'ANALYZING' && (
                  <div className="flex items-center gap-2 px-4 py-3 bg-purple-50 border border-purple-100 rounded-lg">
                    <i className="fa-solid fa-spinner fa-spin text-purple-500" />
                    <span className="text-sm text-purple-700 font-medium">분석이 진행 중입니다...</span>
                  </div>
                )}

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
                          <a href={bid.uiLink} target="_blank" rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 text-secondary hover:text-blue-700 font-medium">
                            <i className="fa-solid fa-arrow-up-right-from-square text-xs" />
                            SAM.gov에서 원문 보기
                          </a>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                <div>
                  <h3 className="text-lg font-bold text-gray-900 mb-3">Attachments / Links (첨부 파일)</h3>
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
                            <a href={url} target="_blank" rel="noopener noreferrer"
                              className="flex items-center p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition">
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
                          <a href={bid.uiLink} target="_blank" rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 text-secondary hover:text-blue-700 font-medium">
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
                    <a href={bid.uiLink} target="_blank" rel="noopener noreferrer"
                      className="inline-flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition">
                      <i className="fa-solid fa-arrow-up-right-from-square" />
                      SAM.gov 원문 보기
                    </a>
                  </div>
                )}
              </div>
            )}
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
