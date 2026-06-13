import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAdminNoticeDetail,
  publishNotice,
  hideNotice,
  regenerateNotice,
  cancelNotice,
} from '../api/client'
import NoticeDocumentView from '../components/NoticeDocumentView'
import type { EligibilityItem } from '../components/NoticeDocumentView'
import NoticeCorrectionChat from '../components/NoticeCorrectionChat'

interface AnalysisResult {
  analysisStatus?: string
  analyzedAt?: string
  summary?: Record<string, unknown>
  requiredDocuments?: Record<string, unknown>
  documentFormats?: Record<string, unknown>
  contentJson?: Record<string, unknown>
}

interface NoticeDetail {
  id: string
  opportunityId: string
  originTitle: string
  solicitationNumber?: string
  organizationName?: string
  postedDate?: string
  responseDeadline?: string
  koreanTitle?: string
  generationStatus: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  visibility: 'HIDDEN' | 'VISIBLE'
  analyzedAt?: string
  errorMessage?: string
  analysis?: AnalysisResult
}

const GEN_LABELS: Record<string, string> = {
  PENDING: '대기', ANALYZING: '분석 중', COMPLETED: '완료', FAILED: '실패',
}

export default function NoticeAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [notice, setNotice] = useState<NoticeDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)

  const fetchData = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getAdminNoticeDetail(id)
      setNotice(data)
    } catch (err) {
      console.error('공고문 상세 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [id])

  const run = async (fn: (id: string) => Promise<unknown>) => {
    if (!id) return
    setActionLoading(true)
    try {
      await fn(id)
      fetchData()
    } catch (err) {
      console.error('공고문 작업 실패:', err)
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
      </div>
    )
  }

  if (!notice) {
    return <div className="p-6 text-center text-gray-400">공고문을 찾을 수 없습니다.</div>
  }

  const a = notice.analysis

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-start justify-between">
        <div>
          <button onClick={() => navigate('/notices')} className="text-sm text-gray-500 hover:text-gray-700 mb-2">
            <i className="fa-solid fa-arrow-left mr-1" />공고문 관리
          </button>
          <h1 className="text-xl font-bold text-gray-900 line-clamp-2">{notice.koreanTitle || notice.originTitle}</h1>
          <div className="text-sm text-gray-400 mt-1 line-clamp-1">{notice.originTitle}</div>
          <div className="flex items-center gap-3 mt-2">
            <span className="text-xs text-gray-500 font-mono">{notice.solicitationNumber || '-'}</span>
            <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
              한글화: {GEN_LABELS[notice.generationStatus] || notice.generationStatus}
            </span>
            <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
              notice.visibility === 'VISIBLE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
            }`}>
              {notice.visibility === 'VISIBLE' ? '노출' : '비노출'}
            </span>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => run(regenerateNotice)}
            disabled={actionLoading || notice.generationStatus === 'ANALYZING'}
            className="px-4 py-2 text-sm rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-100 disabled:opacity-40"
          >
            <i className="fa-solid fa-rotate mr-1.5" />재생성
          </button>
          {/* CR-039: ANALYZING으로 멈춘 stuck 공고문 강제 중단 (재생성은 ANALYZING 중 disabled이라 화면 잠금 탈출용) */}
          {notice.generationStatus === 'ANALYZING' && (
            <button
              onClick={() => run(cancelNotice)}
              disabled={actionLoading}
              className="px-4 py-2 text-sm rounded-lg bg-red-50 text-red-700 hover:bg-red-100 disabled:opacity-40"
              title="분석이 멈춰있을 때 강제로 중단하고 재생성 가능 상태로 되돌립니다."
            >
              <i className="fa-solid fa-stop mr-1.5" />강제 중단
            </button>
          )}
          {notice.visibility === 'HIDDEN' ? (
            <button
              onClick={() => run(publishNotice)}
              disabled={actionLoading || notice.generationStatus !== 'COMPLETED'}
              className="px-4 py-2 text-sm rounded-lg bg-green-600 text-white hover:bg-green-700 disabled:opacity-40"
              title={notice.generationStatus !== 'COMPLETED' ? '한글화 완료 후 노출 가능' : ''}
            >
              노출 승인
            </button>
          ) : (
            <button
              onClick={() => run(hideNotice)}
              disabled={actionLoading}
              className="px-4 py-2 text-sm rounded-lg bg-gray-200 text-gray-700 hover:bg-gray-300 disabled:opacity-40"
            >
              숨김 처리
            </button>
          )}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
        <h3 className="text-sm font-semibold text-gray-700">한글화 결과</h3>

        {notice.generationStatus === 'PENDING' && (
          <div className="text-sm text-gray-400 py-8 text-center">한글화 대기 중입니다.</div>
        )}
        {notice.generationStatus === 'ANALYZING' && (
          <div className="flex items-center gap-3 py-8 justify-center text-purple-600">
            <div className="w-5 h-5 border-2 border-purple-200 border-t-purple-600 rounded-full animate-spin" />
            <span className="text-sm">LLM이 공고를 한글화/요약하고 있습니다...</span>
          </div>
        )}
        {notice.generationStatus === 'FAILED' && (
          <div className="bg-red-50 rounded-lg p-4 text-sm text-red-700">
            한글화에 실패했습니다. {notice.errorMessage || '재생성을 시도해주세요.'}
          </div>
        )}
        {notice.generationStatus === 'COMPLETED' && (
          <div className="space-y-6">
            {notice.analyzedAt && (
              <div className="text-xs text-gray-400">완료: {new Date(notice.analyzedAt).toLocaleString('ko')}</div>
            )}

            {/* CR-021: TipTap JSON 본문 — PDF 양식 풍부도.
                CR-033: 자격요건 정밀추출은 본문 "자격 요건"(§6) 자리에 인라인 렌더(중복·하단분리 방지) */}
            <NoticeDocumentView
              contentJson={a?.contentJson}
              eligibility={a?.requiredDocuments?.eligibility as EligibilityItem[] | undefined}
              postedDate={notice.postedDate}
            />

            {/* 필요 서류 체크리스트 — 액션 데이터(고객 슬롯 매칭/제출 차단 기준)이므로 본문과 별도 표시.
                CR-033: factors[](FACTOR>Subfactor 정밀추출) 있으면 트리로 전체 노출(충족주체 배지 포함), 없으면 평면 폴백 */}
            {Array.isArray(a?.requiredDocuments?.factors) && (a.requiredDocuments.factors as unknown[]).length > 0 ? (
              <section>
                <h3 className="text-base font-bold text-slate-900 mb-3 pb-1.5 border-b-2 border-slate-800">
                  필요 서류·제출물 (FACTOR 정밀추출)
                </h3>
                <div className="space-y-5">
                  {(a.requiredDocuments.factors as Array<Record<string, unknown>>).map((f, fi) => (
                    <div key={fi}>
                      <div className="flex items-center gap-2 mb-2">
                        <span className="px-2 py-0.5 rounded bg-slate-800 text-white text-[11px] font-semibold tracking-wide">
                          FACTOR {String(f.factorId ?? '')}
                        </span>
                        <span className="text-sm font-semibold text-slate-700">{String(f.factorTitle ?? '')}</span>
                      </div>
                      <div className="space-y-2 pl-1">
                        {(Array.isArray(f.subfactors) ? f.subfactors as Array<Record<string, unknown>> : []).map((s, si) => {
                          const party = String(s.fulfillmentParty ?? '')
                          const partyLabel =
                            party === 'CLIENT_UPLOAD' ? { t: '고객 업로드', c: 'bg-emerald-50 text-emerald-600' }
                            : party === 'PLATFORM_GENERATED' ? { t: '플랫폼 생성', c: 'bg-blue-50 text-blue-600' }
                            : party === 'SYSTEM_FORM' ? { t: '시스템 양식', c: 'bg-purple-50 text-purple-600' }
                            : null
                          return (
                            <div key={si} className="flex items-start gap-3 p-3 bg-white border border-slate-200 rounded-md">
                              <i className={`fa-regular fa-square text-lg mt-0.5 ${s.mandatory ? 'text-red-400' : 'text-slate-300'}`} />
                              <div className="flex-1 text-sm">
                                <div className="flex items-center gap-2 mb-0.5 flex-wrap">
                                  {s.subfactorId != null && <span className="text-[11px] text-slate-400 font-mono">{String(s.subfactorId)}</span>}
                                  <span className="font-semibold text-slate-900">{String(s.name ?? '')}</span>
                                  {s.mandatory ? (
                                    <span className="px-1.5 py-0.5 bg-red-50 text-red-600 rounded text-[10px] font-semibold uppercase">필수</span>
                                  ) : (
                                    <span className="px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded text-[10px] font-semibold uppercase">선택</span>
                                  )}
                                  {partyLabel && (
                                    <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${partyLabel.c}`}>{partyLabel.t}</span>
                                  )}
                                </div>
                                {s.description != null && <p className="text-slate-600 mb-0.5">{String(s.description)}</p>}
                                <div className="flex gap-3 text-xs text-slate-400">
                                  {s.format != null && <span><i className="fa-solid fa-file mr-1" />{String(s.format)}</span>}
                                  {s.pageLimit != null && <span><i className="fa-solid fa-ruler mr-1" />{String(s.pageLimit)}</span>}
                                </div>
                                {s.sourceRef != null && <p className="text-[11px] text-slate-400 mt-1"><i className="fa-solid fa-quote-left mr-1" />{String(s.sourceRef)}</p>}
                                {s.notes != null && <p className="text-xs text-slate-400 mt-1 italic">{String(s.notes)}</p>}
                              </div>
                            </div>
                          )
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            ) : Array.isArray(a?.requiredDocuments?.documents) && a.requiredDocuments.documents.length > 0 ? (
              <section>
                <h3 className="text-base font-bold text-slate-900 mb-3 pb-1.5 border-b-2 border-slate-800">
                  필요 서류 체크리스트
                </h3>
                <div className="space-y-2">
                  {(a.requiredDocuments.documents as Array<Record<string, unknown>>).map((doc, i) => (
                    <div key={i} className="flex items-start gap-3 p-3 bg-white border border-slate-200 rounded-md">
                      <i className={`fa-regular fa-square text-lg mt-0.5 ${doc.mandatory ? 'text-red-400' : 'text-slate-300'}`} />
                      <div className="flex-1 text-sm">
                        <div className="flex items-center gap-2 mb-0.5">
                          <span className="font-semibold text-slate-900">{String(doc.name ?? '')}</span>
                          {doc.mandatory ? (
                            <span className="px-1.5 py-0.5 bg-red-50 text-red-600 rounded text-[10px] font-semibold uppercase">필수</span>
                          ) : (
                            <span className="px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded text-[10px] font-semibold uppercase">선택</span>
                          )}
                        </div>
                        {doc.description != null && <p className="text-slate-600 mb-0.5">{String(doc.description)}</p>}
                        <div className="flex gap-3 text-xs text-slate-400">
                          {doc.format != null && <span><i className="fa-solid fa-file mr-1" />{String(doc.format)}</span>}
                          {doc.pageLimit != null && <span><i className="fa-solid fa-ruler mr-1" />{String(doc.pageLimit)}</span>}
                        </div>
                        {doc.notes != null && <p className="text-xs text-slate-400 mt-1 italic">{String(doc.notes)}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            ) : null}

            {/* CR-033: 자격요건은 본문 §6 자리에 인라인 렌더됨(NoticeDocumentView eligibility prop). 하단 별도 섹션 제거. */}
          </div>
        )}
      </div>

      {/* CR-033: 공고문 완료 후 교정 채팅 — 분석 결과를 자연어로 부분 수정 */}
      {notice.generationStatus === 'COMPLETED' && id && (
        <NoticeCorrectionChat noticeId={id} />
      )}
    </div>
  )
}
