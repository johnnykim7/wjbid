import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Check, Circle, CircleStop, FileIcon, Quote, RotateCw, Ruler, Square, Trash2, X } from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  ConfirmDialog,
  PageHeader,
  Spinner,
} from '@therecommerce-develop/ds-ui'
import {
  getAdminNoticeDetail,
  getNoticeAnalysisProgress,
  publishNotice,
  hideNotice,
  regenerateNotice,
  cancelNotice,
  deleteNotice,
} from '../api/client'
import NoticeDocumentView from '../components/NoticeDocumentView'
import type { EligibilityItem, NoticeSummary, RequiredDocumentItem } from '../components/NoticeDocumentView'
import NoticeCorrectionChat from '../components/NoticeCorrectionChat'
import ComplianceMatrixPanel from '../components/ComplianceMatrixPanel'
import { ToneBadge } from '../components/StateBadge'
import { Loading } from '../components/LoadState'
import type { StateTone } from '../utils/bidState'

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
  noticeTypeKo?: string
  naicsLabelKo?: string
  postedDate?: string
  responseDeadline?: string
  koreanTitle?: string
  generationStatus: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  visibility: 'HIDDEN' | 'VISIBLE'
  analyzedAt?: string
  errorMessage?: string
  analysis?: AnalysisResult
}

const GEN_TONE: Record<string, StateTone> = {
  PENDING: 'warning',
  ANALYZING: 'info',
  COMPLETED: 'success',
  FAILED: 'destructive',
}
const GEN_LABELS: Record<string, string> = {
  PENDING: '대기', ANALYZING: '분석 중', COMPLETED: '완료', FAILED: '실패',
}

const PARTY_BADGE: Record<string, { label: string; variant: 'success' | 'primary' | 'info' }> = {
  CLIENT_UPLOAD: { label: '고객 업로드', variant: 'success' },
  PLATFORM_GENERATED: { label: '플랫폼 생성', variant: 'primary' },
  SYSTEM_FORM: { label: '시스템 양식', variant: 'info' },
}

// 공고분석 진행 STEP (BE /progress 응답)
interface StepProgress {
  id: string
  name: string
  status: 'completed' | 'running' | 'pending' | 'failed'
}
interface AnalysisProgress {
  generationStatus: string
  currentStepId?: string | null
  currentStepName?: string | null
  steps: StepProgress[]
}

const STEP_TEXT: Record<StepProgress['status'], string> = {
  completed: 'text-muted-foreground',
  running: 'font-medium text-foreground',
  failed: 'font-medium text-destructive',
  pending: 'text-muted-foreground opacity-60',
}

function StepIcon({ status }: { status: StepProgress['status'] }) {
  if (status === 'completed') {
    return (
      <Badge variant="success" appearance="light" shape="circle" size="sm" className="px-0">
        <Check />
      </Badge>
    )
  }
  if (status === 'failed') {
    return (
      <Badge variant="destructive" appearance="light" shape="circle" size="sm" className="px-0">
        <X />
      </Badge>
    )
  }
  return (
    <span className="flex size-5 items-center justify-center text-muted-foreground">
      {status === 'running' ? <Spinner className="size-3.5" /> : <Circle className="size-3.5" />}
    </span>
  )
}

function MandatoryBadge({ mandatory }: { mandatory: unknown }) {
  return mandatory ? <ToneBadge tone="destructive">필수</ToneBadge> : <ToneBadge tone="gray">선택</ToneBadge>
}

function deleteFailReason(err: unknown) {
  return (err as { response?: { data?: { reason?: string } } })?.response?.data?.reason
}

export default function NoticeAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [notice, setNotice] = useState<NoticeDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [progress, setProgress] = useState<AnalysisProgress | null>(null)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

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

  // 공고분석 진행 폴링. ANALYZING 동안만 4초 간격으로 현재 STEP 을 갱신한다.
  // 종료(COMPLETED/FAILED)로 바뀌면 폴링을 멈추고 fetchData()로 본문을 다시 불러온다.
  // 이전엔 분석이 끝나도 화면이 ANALYZING 스피너에 멈춰 새로고침해야 했다.
  useEffect(() => {
    const stopPolling = () => {
      if (pollRef.current) { clearInterval(pollRef.current); pollRef.current = null }
    }

    if (!id || notice?.generationStatus !== 'ANALYZING') {
      stopPolling()
      if (notice?.generationStatus !== 'ANALYZING') setProgress(null)
      return
    }

    let cancelled = false
    const poll = async () => {
      try {
        const { data } = await getNoticeAnalysisProgress(id)
        if (cancelled) return
        setProgress(data)
        // 서버 기준 상태가 더 이상 ANALYZING이 아니면(종료) 본문을 다시 불러와 자동으로 넘어간다
        if (data.generationStatus && data.generationStatus !== 'ANALYZING') {
          stopPolling()
          fetchData()
        }
      } catch (err) {
        console.error('공고분석 진행 조회 실패:', err)
      }
    }

    poll() // 즉시 1회
    pollRef.current = setInterval(poll, 4000)
    return () => { cancelled = true; stopPolling() }
  }, [id, notice?.generationStatus])

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

  // CR-040: hard delete 라 성공하면 목록으로 간다. 서버는 노출 중, 분석 중이면 409 와 사유를 준다
  const handleDelete = async () => {
    if (!id) return
    await deleteNotice(id)
    setDeleteOpen(false)
    navigate('/notices')
  }

  if (loading) return <Loading className="h-64" />

  if (!notice) {
    return (
      <div className="space-y-4">
        <PageHeader variant="detail" back={{ onClick: () => navigate('/notices') }} />
        <p className="py-12 text-center text-sm text-muted-foreground">공고문을 찾을 수 없습니다.</p>
      </div>
    )
  }

  const a = notice.analysis
  const analyzing = notice.generationStatus === 'ANALYZING'
  const visible = notice.visibility === 'VISIBLE'

  return (
    <div className="space-y-4">
      <PageHeader
        variant="detail"
        back={{ onClick: () => navigate('/notices') }}
        title={notice.koreanTitle || notice.originTitle}
        badge={
          <>
            <ToneBadge tone={GEN_TONE[notice.generationStatus] ?? 'gray'}>
              한글화: {GEN_LABELS[notice.generationStatus] || notice.generationStatus}
            </ToneBadge>
            <ToneBadge tone={visible ? 'success' : 'gray'}>{visible ? '노출' : '비노출'}</ToneBadge>
          </>
        }
        actions={
          <>
            {/* CR-040: 삭제는 노출 중, 분석 중이 아닐 때만 활성(서버 가드와 일치).
                잠긴 단추는 마우스를 받지 않아 잠긴 사유는 감싼 칸에 단다 */}
            <span
              className="inline-flex"
              title={
                visible ? '노출 중인 공고문은 삭제할 수 없습니다 (먼저 숨김 처리)'
                : analyzing ? '분석 중인 공고문은 삭제할 수 없습니다 (먼저 강제 중단)'
                : '공고문 삭제 (복구 불가, 원본은 보존)'
              }
            >
              <Button
                variant="destructive"
                onClick={() => setDeleteOpen(true)}
                disabled={actionLoading || visible || analyzing}
              >
                <Trash2 />
                삭제
              </Button>
            </span>
            {/* CR-039: ANALYZING으로 멈춘 stuck 공고문 강제 중단 (재생성은 ANALYZING 중 disabled이라 화면 잠금 탈출용) */}
            {analyzing && (
              <Button
                variant="outline"
                onClick={() => run(cancelNotice)}
                disabled={actionLoading}
                title="분석이 멈춰있을 때 강제로 중단하고 재생성 가능 상태로 되돌립니다."
              >
                <CircleStop />
                강제 중단
              </Button>
            )}
            <Button variant="outline" onClick={() => run(regenerateNotice)} disabled={actionLoading || analyzing}>
              <RotateCw />
              재생성
            </Button>
            {notice.visibility === 'HIDDEN' ? (
              <span
                className="inline-flex"
                title={notice.generationStatus !== 'COMPLETED' ? '한글화 완료 후 노출 가능' : undefined}
              >
                <Button
                  variant="primary"
                  onClick={() => run(publishNotice)}
                  disabled={actionLoading || notice.generationStatus !== 'COMPLETED'}
                >
                  노출 승인
                </Button>
              </span>
            ) : (
              <Button variant="outline" onClick={() => run(hideNotice)} disabled={actionLoading}>
                숨김 처리
              </Button>
            )}
          </>
        }
      />

      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
        <span className="min-w-0 truncate">{notice.originTitle}</span>
        <span className="font-mono text-xs">{notice.solicitationNumber || '-'}</span>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>한글화 결과</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {notice.generationStatus === 'PENDING' && (
            <p className="py-8 text-center text-sm text-muted-foreground">한글화 대기 중입니다.</p>
          )}
          {analyzing && (
            <div className="py-3">
              <div className="mb-4 flex items-center gap-2 text-sm font-medium text-foreground">
                <Spinner />
                <span>
                  {progress?.currentStepName
                    ? `${progress.currentStepName}…`
                    : '공고를 한글화/분석하고 있습니다…'}
                </span>
              </div>
              <ol className="max-w-md space-y-2">
                {(progress?.steps ?? []).map((s) => (
                  <li key={s.id} className="flex items-center gap-2.5 text-sm">
                    <StepIcon status={s.status} />
                    <span className={STEP_TEXT[s.status]}>{s.name}</span>
                  </li>
                ))}
              </ol>
            </div>
          )}
          {notice.generationStatus === 'FAILED' && (
            <Alert variant="destructive" appearance="light">
              <AlertDescription>한글화에 실패했습니다. {notice.errorMessage || '재생성을 시도해주세요.'}</AlertDescription>
            </Alert>
          )}
          {notice.generationStatus === 'COMPLETED' && (
            <div className="space-y-6">
              {notice.analyzedAt && (
                <div className="text-xs text-muted-foreground">완료: {new Date(notice.analyzedAt).toLocaleString('ko')}</div>
              )}

              {/* CR-021: TipTap JSON 본문, PDF 양식 풍부도.
                  CR-033: 자격요건 정밀추출은 본문 "자격 요건"(§6) 자리에 인라인 렌더(중복·하단분리 방지) */}
              <NoticeDocumentView
                koreanTitle={notice.koreanTitle}
                summary={a?.summary as NoticeSummary | undefined}
                documents={(a?.requiredDocuments as Record<string, unknown> | undefined)?.documents as RequiredDocumentItem[] | undefined}
                solicitationNumber={notice.solicitationNumber}
                organizationName={notice.organizationName}
                noticeTypeKo={notice.noticeTypeKo}
                naicsLabelKo={notice.naicsLabelKo}
                responseDeadline={notice.responseDeadline}
                eligibility={(a?.requiredDocuments as Record<string, unknown> | undefined)?.eligibility as EligibilityItem[] | undefined}
                postedDate={notice.postedDate}
              />

              {/* 필요 서류 체크리스트는 액션 데이터(고객 슬롯 매칭/제출 차단 기준)라 본문과 별도 표시.
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
                            const party = PARTY_BADGE[String(s.fulfillmentParty ?? '')]
                            return (
                              <div key={si} className="flex items-start gap-3 p-3 bg-white border border-slate-200 rounded-md">
                                <Square className={`mt-0.5 size-4 shrink-0 ${s.mandatory ? 'text-destructive' : 'text-slate-300'}`} />
                                <div className="flex-1 text-sm">
                                  <div className="flex items-center gap-2 mb-0.5 flex-wrap">
                                    {s.subfactorId != null && <span className="text-[11px] text-slate-400 font-mono">{String(s.subfactorId)}</span>}
                                    <span className="font-semibold text-slate-900">{String(s.name ?? '')}</span>
                                    <MandatoryBadge mandatory={s.mandatory} />
                                    {party && (
                                      <Badge variant={party.variant} appearance="light" className="whitespace-nowrap">{party.label}</Badge>
                                    )}
                                  </div>
                                  {s.description != null && <p className="text-slate-600 mb-0.5">{String(s.description)}</p>}
                                  <div className="flex gap-3 text-xs text-slate-400">
                                    {s.format != null && <span className="inline-flex items-center gap-1"><FileIcon className="size-3.5" />{String(s.format)}</span>}
                                    {s.pageLimit != null && <span className="inline-flex items-center gap-1"><Ruler className="size-3.5" />{String(s.pageLimit)}</span>}
                                  </div>
                                  {s.sourceRef != null && (
                                    <p className="text-[11px] text-slate-400 mt-1"><Quote className="mr-1 inline size-3" />{String(s.sourceRef)}</p>
                                  )}
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
                        <Square className={`mt-0.5 size-4 shrink-0 ${doc.mandatory ? 'text-destructive' : 'text-slate-300'}`} />
                        <div className="flex-1 text-sm">
                          <div className="flex items-center gap-2 mb-0.5">
                            <span className="font-semibold text-slate-900">{String(doc.name ?? '')}</span>
                            <MandatoryBadge mandatory={doc.mandatory} />
                          </div>
                          {doc.description != null && <p className="text-slate-600 mb-0.5">{String(doc.description)}</p>}
                          <div className="flex gap-3 text-xs text-slate-400">
                            {doc.format != null && <span className="inline-flex items-center gap-1"><FileIcon className="size-3.5" />{String(doc.format)}</span>}
                            {doc.pageLimit != null && <span className="inline-flex items-center gap-1"><Ruler className="size-3.5" />{String(doc.pageLimit)}</span>}
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
        </CardContent>
      </Card>

      {id && (
        <ComplianceMatrixPanel
          noticeId={id}
          analyzing={analyzing}
          reloadKey={notice.analyzedAt}
        />
      )}

      {/* CR-033: 공고문 완료 후 교정 채팅. 분석 결과를 자연어로 부분 수정 */}
      {notice.generationStatus === 'COMPLETED' && id && (
        <NoticeCorrectionChat noticeId={id} />
      )}

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="공고문 삭제"
        description="삭제하면 복구할 수 없습니다. 원본 공고는 보존됩니다."
        confirmLabel="삭제"
        confirmVariant="destructive"
        onConfirm={handleDelete}
        errorText={(err: unknown) => deleteFailReason(err) || '공고문 삭제에 실패했습니다.'}
      />
    </div>
  )
}
