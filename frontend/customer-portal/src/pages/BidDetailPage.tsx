import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Button as DsButton } from '@therecommerce-develop/ds-ui'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { AiProposalModal } from '../components/modals/AiProposalModal'
import { isAxiosError } from 'axios'
import { getOpportunity, getOriginOpportunity, checkBookmark, addBookmark, removeBookmark, getEligibilityDetail } from '../api/client'
import type { EligibilityDetail, Opportunity } from '../types'
import { Icon } from '../components/layout/Icon'
import { VerdictBox } from '../components/verdict/VerdictBox'
import { VERDICT_BOX_TEXT } from '../data/verdictContent'
import NoticeDocumentView from '../components/NoticeDocumentView'
import { ELIGIBILITY_TEXT, eligibilityHref } from '../data/eligibilityContent'

/** origin: 공개 공고문이 없는 모은 공고를 원문으로 보이는 원본 공고 상세 (CR-152) */
export default function BidDetailPage({ origin = false }: { origin?: boolean }) {
  const { id, opportunityId } = useParams<{ id: string; opportunityId: string }>()
  const navigate = useNavigate()
  const [bid, setBid] = useState<Opportunity | null>(null)
  const [loading, setLoading] = useState(true)
  const [isBookmarked, setIsBookmarked] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)
  const [aiModalOpen, setAiModalOpen] = useState(false)
  // 우리 회사 판정 상자(CR-140). 판정할 수 없는 공고(404)는 상자를 두지 않는다
  const [verdict, setVerdict] = useState<{ status: 'ok'; detail: EligibilityDetail } | { status: 'error' } | null>(null)
  const hasEligibility = (bid?.analysis?.requiredDocuments?.eligibility?.length ?? 0) > 0

  useEffect(() => {
    setVerdict(null)
    if (!id || !hasEligibility) return
    getEligibilityDetail(id)
      .then((res) => setVerdict({ status: 'ok', detail: res.data }))
      .catch((e) => setVerdict(isAxiosError(e) && e.response?.status === 404 ? null : { status: 'error' }))
  }, [id, hasEligibility])

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        if (origin) {
          const res = await getOriginOpportunity(opportunityId!)
          // 공고문이 공개되면 원본 주소로 들어와도 공고문 상세로 넘긴다
          if (res.data.publicNoticeId) {
            navigate(`/search/${res.data.publicNoticeId}`, { replace: true })
            return
          }
          setBid(res.data)
          return
        }
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
  }, [id, opportunityId, origin, navigate])

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
    <div className="flex flex-col items-center justify-center h-full text-ink-mute gap-4">
      <i className="fa-solid fa-circle-exclamation text-4xl text-ink-faint" />
      <p className="text-lg font-semibold">공고 정보를 불러올 수 없습니다.</p>
      <DsButton variant="ghost" mode="link" onClick={() => navigate('/search')}>
        목록으로 돌아가기
      </DsButton>
    </div>
  )

  // 마감은 시각이 붙은 값으로 센다. 날짜만 있는 responseDeadline 은 브라우저가 UTC 자정으로 읽어 늘 오전 9시가 된다.
  const deadlineAt = bid.responseDeadlineAt ?? bid.responseDeadline
  const daysLeft = Math.ceil(
    (new Date(deadlineAt).getTime() - Date.now()) / 86400000
  )

  // 마감일 한글 표기 (2026년 6월 17일 오전 11:00, 서울 시각). 파싱 실패 시 원본 그대로.
  const deadlineKo = (() => {
    if (!deadlineAt) return ''
    const d = new Date(deadlineAt)
    return isNaN(d.getTime())
      ? deadlineAt
      : d.toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  })()

  return (
    <div className="flex flex-col h-full">
      {/* Sticky Header */}
      <div className="sticky top-0 bg-white border-b border-line px-6 py-4 flex justify-between items-center shadow-sm z-10">
        <div className="flex items-center gap-4">
          {/* 모은 공고 화면에서도 들어오므로 온 곳으로 돌아간다. 주소로 바로 열었으면 마감 전 공고로 */}
          <DsButton variant="ghost" size="icon" onClick={() => (window.history.state?.idx > 0 ? navigate(-1) : navigate('/search'))}>
            <i className="fa-solid fa-arrow-left text-xl" />
          </DsButton>
          <div>
            <h3 className="text-lg font-bold text-ink">입찰 공고 상세 내역</h3>
          </div>
        </div>
        <div className="flex gap-2">
          {bid.samUrl && (
            <Button variant="outline" onClick={() => window.open(bid.samUrl, '_blank', 'noopener,noreferrer')}>
              <i className="fa-solid fa-arrow-up-right-from-square mr-1" /> SAM.gov 원문 보기
            </Button>
          )}
          {/* 관심 공고와 제안서는 공고문에 붙는다. 원본 공고 상세에는 두지 않는다 */}
          {!origin && (
            <>
              <Button
                variant="outline"
                onClick={toggleBookmark}
                disabled={bookmarkLoading}
                className="hidden md:flex items-center gap-1"
              >
                {bookmarkLoading
                  ? <i className="fa-solid fa-spinner fa-spin" />
                  : <i className={isBookmarked ? 'fa-solid fa-bookmark text-brand' : 'fa-regular fa-bookmark'} />
                }
                {isBookmarked ? '저장됨' : '관심 공고 저장'}
              </Button>
              <Button variant="accent" onClick={() => setAiModalOpen(true)}>
                <i className="fa-solid fa-wand-magic-sparkles mr-1" /> 제안서 작성
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-6 md:p-8">
        <div className="max-w-5xl mx-auto">
          <div className="bg-white rounded-xl shadow-sm border border-line p-8">
            {/* Title & Badges */}
            <div className="mb-6">
              <div className="flex items-center gap-2 mb-3">
                <Badge variant="active">Active</Badge>
                <span className="text-sm font-bold text-ink-mute">
                  Notice ID: {bid.solicitationNumber}
                </span>
              </div>
              <h1 className="text-2xl md:text-3xl font-extrabold text-ink leading-tight mb-4">
                {bid.title}
              </h1>
              {bid.originalTitle && bid.originalTitle !== bid.title && (
                <p className="text-sm text-ink-mute -mt-2 mb-4">{bid.originalTitle}</p>
              )}
              <div className="flex flex-wrap gap-6 text-sm text-ink-sub">
                <div className="flex items-center gap-1">
                  <i className="fa-solid fa-building-columns text-ink-mute w-5" />
                  {bid.agencyName}
                </div>
                <div className="flex items-center gap-1">
                  <i className="fa-solid fa-calendar-days text-ink-mute w-5" />
                  마감일:{' '}
                  <span className="font-bold text-ink ml-1">{deadlineKo}</span>
                  {daysLeft >= 0 && (
                    <span className={`ml-1 font-bold ${daysLeft <= 7 ? 'text-danger' : daysLeft <= 14 ? 'text-warning' : 'text-ink-sub'}`}>
                      (D-{daysLeft})
                    </span>
                  )}
                </div>
                {bid.naicsCode && (
                  <div className="flex items-center gap-1">
                    <i className="fa-solid fa-tag text-ink-mute w-5" />
                    NAICS: <span className="font-bold text-ink ml-1">{bid.naicsCode}</span>
                  </div>
                )}
                {bid.setAside && (
                  <div className="flex items-center gap-1">
                    <i className="fa-solid fa-shield-halved text-ink-mute w-5" />
                    Set-Aside: <span className="font-bold text-ink ml-1">{bid.setAside}</span>
                  </div>
                )}
              </div>
            </div>

            <hr className="border-line my-8" />

            {bid.analysis?.analysisStatus === 'COMPLETED' ? (
              /* ── 정리된 공고문 (분석 완료) ── */
              <div className="space-y-8">
                {/* 분석 배지 */}
                <div className="flex items-center gap-2">
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-success-soft text-success rounded-full text-xs font-semibold">
                    <i className="fa-solid fa-wand-magic-sparkles" /> 분석 완료
                  </span>
                  {bid.analysis.analyzedAt && (
                    <span className="text-xs text-ink-mute">{bid.analysis.analyzedAt} 분석</span>
                  )}
                </div>

                {/* CR-021: 공고문 본문 (PDF 양식 풍부도) — contentJson 있을 때만 */}
                {bid.analysis.contentJson && (
                  <NoticeDocumentView contentJson={bid.analysis.contentJson} />
                )}

                {verdict?.status === 'ok' && <VerdictBox detail={verdict.detail} />}
                {verdict?.status === 'error' && (
                  <div className="wj vd-host">
                    <section className="cbx vd-box">
                      <div className="vd-berr"><Icon name="alert" />{VERDICT_BOX_TEXT.loadError}</div>
                    </section>
                  </div>
                )}

                {/* 공고 요약 */}
                {bid.analysis.summary && (
                  <div>
                    <h3 className="text-lg font-bold text-ink mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-file-lines text-brand" /> 공고 요약
                    </h3>
                    <div className="space-y-4">
                      {bid.analysis.summary.overview && (
                        <div className="bg-brand-soft border border-brand-line rounded-lg p-5">
                          <p className="text-sm text-ink leading-relaxed">{bid.analysis.summary.overview}</p>
                        </div>
                      )}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {bid.analysis.summary.scope && (
                          <div className="bg-surface rounded-lg p-4 border border-line">
                            <div className="text-xs font-semibold text-ink-mute uppercase mb-2">작업 범위</div>
                            <p className="text-sm text-ink-sub leading-relaxed">{bid.analysis.summary.scope}</p>
                          </div>
                        )}
                        {bid.analysis.summary.eligibility && (
                          <div className="bg-surface rounded-lg p-4 border border-line">
                            <div className="flex items-center justify-between gap-2 mb-2">
                              <div className="text-xs font-semibold text-ink-mute uppercase">참여 자격</div>
                              {/* 공고 분석에 참가 자격 목록이 있어야 진단할 수 있다(CR-137, BIZ-033) */}
                              {(bid.analysis.requiredDocuments?.eligibility?.length ?? 0) > 0 && (
                                <DsButton asChild variant="primary" size="sm" className="bg-brand hover:bg-brand-strong">
                                  <Link to={eligibilityHref(bid.id)}>{ELIGIBILITY_TEXT.title}</Link>
                                </DsButton>
                              )}
                            </div>
                            <p className="text-sm text-ink-sub leading-relaxed">{bid.analysis.summary.eligibility}</p>
                          </div>
                        )}
                        {bid.analysis.summary.evaluationCriteria && (
                          <div className="bg-surface rounded-lg p-4 border border-line">
                            <div className="text-xs font-semibold text-ink-mute uppercase mb-2">평가 기준</div>
                            <p className="text-sm text-ink-sub leading-relaxed">{bid.analysis.summary.evaluationCriteria}</p>
                          </div>
                        )}
                        {bid.analysis.summary.budgetInfo && (
                          <div className="bg-surface rounded-lg p-4 border border-line">
                            <div className="text-xs font-semibold text-ink-mute uppercase mb-2">예산 정보</div>
                            <p className="text-sm text-ink-sub leading-relaxed">{bid.analysis.summary.budgetInfo}</p>
                          </div>
                        )}
                      </div>
                      {bid.analysis.summary.specialNotes && bid.analysis.summary.specialNotes.length > 0 && (
                        <div className="bg-warning-soft border border-warning/30 rounded-lg p-4">
                          <div className="text-xs font-semibold text-warning uppercase mb-2">
                            <i className="fa-solid fa-triangle-exclamation mr-1" /> 특이사항
                          </div>
                          <ul className="list-disc list-inside space-y-1">
                            {bid.analysis.summary.specialNotes.map((note, i) => (
                              <li key={i} className="text-sm text-ink-sub">{note}</li>
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
                    <h3 className="text-lg font-bold text-ink mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-calendar-days text-brand" /> 주요 일정
                    </h3>
                    <div className="bg-white border border-line rounded-lg overflow-hidden">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="bg-surface border-b border-line">
                            <th className="text-left px-4 py-2.5 text-xs font-semibold text-ink-mute uppercase">항목</th>
                            <th className="text-left px-4 py-2.5 text-xs font-semibold text-ink-mute uppercase">일자</th>
                            <th className="text-left px-4 py-2.5 text-xs font-semibold text-ink-mute uppercase">비고</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-line-soft">
                          {bid.analysis.summary.keyDates.map((d, i) => (
                            <tr key={i} className="hover:bg-surface">
                              <td className="px-4 py-2.5 font-medium text-ink">{d.label}</td>
                              <td className="px-4 py-2.5 text-ink-sub">{d.date}</td>
                              <td className="px-4 py-2.5 text-ink-mute">{d.note || '-'}</td>
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
                    <h3 className="text-lg font-bold text-ink mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-list-check text-brand" /> 필요 서류 체크리스트
                    </h3>
                    <div className="space-y-3">
                      {bid.analysis.requiredDocuments.documents.map((doc, i) => (
                        <div key={i} className="flex items-start gap-3 p-4 bg-white border border-line rounded-lg">
                          <div className="mt-0.5">
                            <i className={`fa-regular fa-square text-lg ${doc.mandatory ? 'text-danger' : 'text-ink-faint'}`} />
                          </div>
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="font-bold text-sm text-ink">{doc.name}</span>
                              {doc.mandatory ? (
                                <span className="px-1.5 py-0.5 bg-danger-soft text-danger rounded text-[10px] font-semibold uppercase">필수</span>
                              ) : (
                                <span className="px-1.5 py-0.5 bg-surface text-ink-mute rounded text-[10px] font-semibold uppercase">선택</span>
                              )}
                            </div>
                            {doc.description && <p className="text-sm text-ink-sub mb-1">{doc.description}</p>}
                            <div className="flex gap-4 text-xs text-ink-mute">
                              {doc.format && <span><i className="fa-solid fa-file mr-1" />{doc.format}</span>}
                              {doc.pageLimit && <span><i className="fa-solid fa-ruler mr-1" />{doc.pageLimit}</span>}
                            </div>
                            {doc.notes && <p className="text-xs text-ink-mute mt-1 italic">{doc.notes}</p>}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 문서 양식 안내 */}
                {bid.analysis.documentFormats && (
                  <div>
                    <h3 className="text-lg font-bold text-ink mb-4 flex items-center gap-2">
                      <i className="fa-solid fa-file-contract text-brand" /> 문서 양식 안내
                    </h3>
                    {bid.analysis.documentFormats.generalInstructions && (
                      <div className="bg-surface rounded-lg p-4 border border-line mb-4">
                        <p className="text-sm text-ink-sub leading-relaxed">{bid.analysis.documentFormats.generalInstructions}</p>
                      </div>
                    )}
                    {bid.analysis.documentFormats.formats && bid.analysis.documentFormats.formats.length > 0 && (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        {bid.analysis.documentFormats.formats.map((fmt, i) => (
                          <div key={i} className="bg-white border border-line rounded-lg p-4">
                            <div className="font-bold text-sm text-ink mb-2">{fmt.section}</div>
                            {fmt.description && <p className="text-sm text-ink-sub mb-2">{fmt.description}</p>}
                            <div className="flex flex-wrap gap-3 text-xs text-ink-mute">
                              {fmt.pageLimit && <span className="bg-surface px-2 py-0.5 rounded">{fmt.pageLimit}</span>}
                              {fmt.fileFormat && <span className="bg-surface px-2 py-0.5 rounded">{fmt.fileFormat}</span>}
                              {fmt.fontRequirements && <span className="bg-surface px-2 py-0.5 rounded">{fmt.fontRequirements}</span>}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    {bid.analysis.documentFormats.submissionMethod && (
                      <div className="mt-4 flex items-center gap-2 text-sm text-ink-sub">
                        <i className="fa-solid fa-paper-plane text-brand" />
                        <span>제출 방법: <span className="font-medium text-ink">{bid.analysis.documentFormats.submissionMethod}</span></span>
                      </div>
                    )}
                  </div>
                )}

                {/* 첨부 파일 */}
                <div>
                  <h3 className="text-lg font-bold text-ink mb-3 flex items-center gap-2">
                    <i className="fa-solid fa-paperclip text-brand" /> 첨부 파일
                  </h3>
                  {bid.resourceLinks && bid.resourceLinks.length > 0 ? (
                    <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {bid.resourceLinks.map((url) => {
                        const filename = url.split('/').pop()?.split('?')[0] ?? url
                        return (
                          <li key={url}>
                            <a href={url} target="_blank" rel="noopener noreferrer"
                              className="flex items-center p-4 border border-line rounded-lg hover:bg-surface transition">
                              <div className="w-10 h-10 flex items-center justify-center rounded mr-3 bg-danger-soft text-danger">
                                <i className="fa-solid fa-file-pdf text-xl" />
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="text-sm font-bold text-ink truncate">{filename}</div>
                              </div>
                              <i className="fa-solid fa-download text-ink-mute ml-2" />
                            </a>
                          </li>
                        )
                      })}
                    </ul>
                  ) : (
                    <p className="text-sm text-ink-mute">첨부 파일이 없습니다.</p>
                  )}
                </div>

                {/* SAM.gov 원본 (접이식) */}
                <details className="border border-line rounded-lg">
                  <summary className="px-5 py-3 text-sm font-medium text-ink-sub cursor-pointer hover:bg-surface select-none">
                    <i className="fa-solid fa-chevron-right mr-2 text-xs" />
                    SAM.gov 원본 보기
                  </summary>
                  <div className="px-5 pb-4">
                    {bid.description ? (
                      <div className="text-sm text-ink-sub bg-surface p-4 rounded-lg mt-2 whitespace-pre-wrap leading-relaxed">
                        {bid.description}
                      </div>
                    ) : (
                      <p className="text-sm text-ink-mute mt-2">원본 본문이 제공되지 않은 공고입니다.</p>
                    )}
                    {bid.samUrl && (
                      <a href={bid.samUrl} target="_blank" rel="noopener noreferrer"
                        className="inline-flex items-center gap-2 mt-3 px-4 py-2 border border-line-strong rounded-lg text-sm text-ink-sub hover:bg-surface transition">
                        <i className="fa-solid fa-arrow-up-right-from-square" /> SAM.gov 원문 보기
                      </a>
                    )}
                  </div>
                </details>
              </div>
            ) : (
              /* ── 원본 표시 (분석 미완료 / fallback) ── */
              <div className="space-y-6">
                {origin && (
                  <div className="flex items-center gap-2 px-4 py-3 bg-brand-soft border border-brand-line rounded-lg">
                    <i className="fa-solid fa-circle-info text-brand" />
                    <span className="text-sm text-brand font-medium">아직 한글 공고문이 없는 공고라 원문을 보여 드립니다.</span>
                  </div>
                )}
                {bid.analysis?.analysisStatus === 'ANALYZING' && (
                  <div className="flex items-center gap-2 px-4 py-3 bg-brand-soft border border-brand-line rounded-lg">
                    <i className="fa-solid fa-spinner fa-spin text-brand" />
                    <span className="text-sm text-brand font-medium">분석이 진행 중입니다...</span>
                  </div>
                )}

                <div>
                  <h3 className="text-lg font-bold text-ink mb-3">Description (공고 내용)</h3>
                  {bid.description ? (
                    <div className="text-ink-sub leading-relaxed text-sm bg-surface p-5 rounded-lg border border-line whitespace-pre-wrap">
                      {bid.description}
                    </div>
                  ) : (
                    <div className="text-sm bg-surface p-5 rounded-lg border border-line flex items-start gap-3">
                      <i className="fa-solid fa-circle-info text-brand mt-0.5" />
                      <div>
                        <p className="text-ink-sub mb-2">모아 둔 원문 본문이 없습니다. SAM.gov 원문에서 보세요.</p>
                        {bid.samUrl && (
                          <a href={bid.samUrl} target="_blank" rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 text-brand hover:text-brand-strong font-medium">
                            <i className="fa-solid fa-arrow-up-right-from-square text-xs" />
                            SAM.gov에서 원문 보기
                          </a>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                <div>
                  <h3 className="text-lg font-bold text-ink mb-3">Attachments / Links (첨부 파일)</h3>
                  {bid.resourceLinks && bid.resourceLinks.length > 0 ? (
                    <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {bid.resourceLinks.map((url) => {
                        const filename = url.split('/').pop()?.split('?')[0] ?? url
                        const ext = filename.split('.').pop()?.toLowerCase() ?? ''
                        const isPdf = ext === 'pdf'
                        const isDoc = ['doc', 'docx'].includes(ext)
                        const icon = isPdf ? 'fa-solid fa-file-pdf' : isDoc ? 'fa-solid fa-file-word' : 'fa-solid fa-file'
                        return (
                          <li key={url}>
                            <a href={url} target="_blank" rel="noopener noreferrer"
                              className="flex items-center p-4 border border-line rounded-lg hover:bg-surface transition">
                              <div className="w-10 h-10 flex items-center justify-center rounded mr-3 bg-surface text-ink-mute">
                                <i className={`${icon} text-xl`} />
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="text-sm font-bold text-ink truncate">{filename}</div>
                                <div className="text-xs text-ink-mute mt-0.5 truncate">{url}</div>
                              </div>
                              <i className="fa-solid fa-arrow-up-right-from-square text-ink-mute ml-2" />
                            </a>
                          </li>
                        )
                      })}
                    </ul>
                  ) : (
                    <div className="text-sm bg-surface p-5 rounded-lg border border-line flex items-start gap-3">
                      <i className="fa-solid fa-circle-info text-brand mt-0.5" />
                      <div>
                        <p className="text-ink-sub mb-2">첨부 파일 정보가 없습니다.</p>
                        {bid.samUrl && (
                          <a href={bid.samUrl} target="_blank" rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 text-brand hover:text-brand-strong font-medium">
                            <i className="fa-solid fa-arrow-up-right-from-square text-xs" />
                            SAM.gov에서 첨부 파일 확인
                          </a>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                {bid.samUrl && (
                  <div className="pt-2 border-t border-line-soft">
                    <a href={bid.samUrl} target="_blank" rel="noopener noreferrer"
                      className="inline-flex items-center gap-2 px-4 py-2 border border-line-strong rounded-lg text-sm text-ink-sub hover:bg-surface transition">
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

      {!origin && (
        <AiProposalModal
          isOpen={aiModalOpen}
          onClose={() => setAiModalOpen(false)}
          opportunityId={bid.id}
          noticeId={bid.solicitationNumber}
          noticeTitle={bid.title}
        />
      )}
    </div>
  )
}
