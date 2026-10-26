import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Badge,
  Button,
  ConfirmDialog,
  FieldError,
  Label,
  RadioGroup,
  RadioGroupItem,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Textarea,
  toast,
} from '@therecommerce-develop/ds-ui'
import { getBidAnalysis, getNoticeComplianceMatrix, rerunBidAnalysis, saveBidDecision } from '../api/client'
import { CapturePlanView, DecisionView, ReportView } from './BidAnalysisSections'
import {
  DECISION_LABEL,
  DECISION_NOTE_LABEL,
  DECISION_NOTE_MAX,
  DECISION_NOTE_MISSING,
  QUESTION_TARGET_LABEL,
  STATUS_LABEL,
  VERDICT_LABEL,
  allowsGeneration,
  formatDateTime,
  questionTopic,
  requirementTexts,
} from './bidAnalysis'
import type { BidAnalysis, BidAnalysisOverview, BidDecision, BidQuestion, Json } from './bidAnalysis'

const POLL_INTERVAL_MS = 10_000
const NOT_ALLOWED_MESSAGE = '지금은 할 수 없습니다'

const STATUS_VARIANT: Record<BidAnalysis['status'], 'info' | 'success' | 'destructive' | 'secondary'> = {
  RUNNING: 'info',
  DONE: 'success',
  FAILED: 'destructive',
  DECISION_ONLY: 'secondary',
}

type ApiError = { response?: { status?: number; data?: { error?: string; message?: string } } }

function errorOf(err: unknown) {
  return (err as ApiError)?.response
}

const QUESTION_TARGETS = ['CUSTOMER', 'ADMIN'] as const

function QuestionList({ list }: { list: BidQuestion[] }) {
  if (list.length === 0) return <p className="py-3 text-sm text-gray-400">없음</p>
  return (
    <ol className="divide-y divide-gray-100">
      {list.map((q, i) => {
        const topic = questionTopic(q)
        return (
          <li key={i} className="flex gap-3 py-3.5">
            <span className="mt-0.5 flex size-5.5 flex-none items-center justify-center rounded-full bg-indigo-50 text-xs font-semibold text-indigo-700">
              {i + 1}
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium leading-relaxed text-gray-900">{q.question}</p>
              {(topic || q.reason) && (
                <div className="mt-1 flex items-baseline gap-2">
                  {topic && <span className="flex-none rounded bg-gray-100 px-1.5 text-[11px] leading-[18px] text-gray-600">{topic}</span>}
                  {q.reason && <span className="text-xs leading-normal text-gray-500">{q.reason}</span>}
                </div>
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}

function Questions({ questions }: { questions: BidQuestion[] }) {
  const [target, setTarget] = useState<string>('CUSTOMER')
  if (questions.length === 0) return null
  const of = (t: string) => questions.filter((q) => (q.target ?? 'ADMIN') === t)
  return (
    <div className="space-y-2">
      <h3 className="text-sm font-semibold text-gray-900">
        확인 필요 <span className="ml-1 font-normal text-gray-500">{questions.length}건</span>
      </h3>
      <Tabs value={target} onValueChange={setTarget}>
        <TabsList variant="line">
          {QUESTION_TARGETS.map((t) => (
            <TabsTrigger key={t} value={t}>{QUESTION_TARGET_LABEL[t]} {of(t).length}</TabsTrigger>
          ))}
        </TabsList>
        {QUESTION_TARGETS.map((t) => (
          <TabsContent key={t} value={t}>
            <QuestionList list={of(t)} />
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}

function NotSaved() {
  return <p className="text-sm text-gray-400">아직 저장되지 않았습니다.</p>
}

/**
 * CR-170 입찰 상세 관리의 입찰 분석 영역.
 * 분석 중이거나 서류를 읽는 중이면 10초마다 다시 부르고, 관리자 결정이 문서 작성 중 전환을 허용하는지 상태 패널에 알린다.
 * 불러오지 못하면 알 수 없음(null)으로 알려 전환을 막지 않는다. 서버가 409 로 막는다.
 */
export default function BidAnalysisPanel({ bidRequestId, bidState, noticeId, reloadKey, onDecisionChange }: {
  bidRequestId: string
  bidState: string
  /** REQ ID 옆에 요구 문장을 붙일 공고. 없으면 REQ ID 만 보인다 */
  noticeId?: string
  reloadKey?: number
  onDecisionChange?: (allowsGeneration: boolean | null) => void
}) {
  const [overview, setOverview] = useState<BidAnalysisOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [tab, setTab] = useState('report')
  const [choice, setChoice] = useState<BidDecision | ''>('')
  const [note, setNote] = useState('')
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const [confirmRerun, setConfirmRerun] = useState(false)
  const [rerunning, setRerunning] = useState(false)
  const notify = useRef(onDecisionChange)
  notify.current = onDecisionChange
  const formKey = useRef<string | null>(null)

  const apply = (data: BidAnalysisOverview) => {
    setOverview(data)
    const a = data.analysis
    notify.current?.(allowsGeneration(a?.adminDecision))
    // 다시 부를 때마다 폼을 덮으면 고치던 글이 사라지므로 회차나 결정이 바뀐 때만 서버 값으로 맞춘다
    const key = `${a?.analysisId ?? ''}|${a?.decidedAt ?? ''}`
    if (formKey.current !== key) {
      formKey.current = key
      setChoice(a?.adminDecision ?? '')
      setNote(a?.adminNote ?? '')
      setFormError('')
    }
  }

  const load = useCallback(async (quiet = false) => {
    if (!quiet) setLoading(true)
    setFailed(false)
    try {
      const { data } = await getBidAnalysis(bidRequestId)
      apply(data)
    } catch {
      setFailed(true)
      notify.current?.(null)
    } finally {
      if (!quiet) setLoading(false)
    }
  }, [bidRequestId])

  useEffect(() => { load() }, [load, reloadKey, bidState])

  const analysis = overview?.analysis ?? null
  const readingInProgress = overview?.readingInProgress ?? false
  const running = analysis?.status === 'RUNNING'
  const analyzing = bidState === 'ANALYZING'
  // 읽기 줄이 다 읽음이 된 뒤 서버가 읽기 회차를 마무리하고 분석을 시작하기까지 틈이 있어, 분석 단계에 회차가 아직 없으면 계속 부른다
  const awaitingAnalysis = analyzing && overview !== null && analysis === null

  useEffect(() => {
    if (!running && !readingInProgress && !awaitingAnalysis) return
    const timer = window.setInterval(() => load(true), POLL_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [running, readingInProgress, awaitingAnalysis, load])

  // 요구 문장은 공고마다 정해져 있어 10초마다 다시 부를 때 함께 부르지 않는다
  const [reqTexts, setReqTexts] = useState<Record<string, string>>({})
  const matrixOf = useRef<string | null>(null)
  const hasAnalysis = analysis !== null
  useEffect(() => {
    if (!noticeId || !hasAnalysis || matrixOf.current === noticeId) return
    matrixOf.current = noticeId
    getNoticeComplianceMatrix(noticeId)
      .then(({ data }) => setReqTexts(requirementTexts(data)))
      .catch(() => setReqTexts({}))
  }, [noticeId, hasAnalysis])

  const handleNotAllowed = (err: unknown, fallback: string) => {
    const res = errorOf(err)
    if (res?.status === 409 && res.data?.error === 'BID_ANALYSIS_NOT_ALLOWED') {
      toast.error(NOT_ALLOWED_MESSAGE)
      load(true)
      return true
    }
    if (res?.status !== 400) toast.error(fallback)
    return false
  }

  const rerun = async () => {
    setRerunning(true)
    try {
      const { data } = await rerunBidAnalysis(bidRequestId)
      apply(data)
      setTab('report')
    } catch (err) {
      handleNotAllowed(err, '다시 분석을 걸지 못했습니다.')
    } finally {
      setRerunning(false)
      setConfirmRerun(false)
    }
  }

  const noteLabel = choice ? DECISION_NOTE_LABEL[choice] : undefined

  const save = async () => {
    if (!choice) {
      setFormError('결정을 선택하세요')
      return
    }
    const noteMissing = choice ? DECISION_NOTE_MISSING[choice] : undefined
    if (noteMissing && !note.trim()) {
      setFormError(noteMissing)
      return
    }
    setFormError('')
    setSaving(true)
    try {
      const { data } = await saveBidDecision(bidRequestId, choice, noteLabel ? note.trim() : null)
      formKey.current = null
      apply(data)
    } catch (err) {
      const res = errorOf(err)
      if (!handleNotAllowed(err, '결정을 저장하지 못했습니다.') && res?.status === 400) {
        setFormError(res.data?.message || '결정을 저장하지 못했습니다.')
      }
    } finally {
      setSaving(false)
    }
  }

  const canRerun = analyzing && !running && !readingInProgress
  const canDecide = analyzing && !running
  const showTabs = analysis !== null && analysis.status !== 'DECISION_ONLY'

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <h2 className="text-sm font-semibold text-gray-900 flex items-center gap-2 mr-2">
          <i className="fa-solid fa-scale-balanced text-secondary" /> 입찰 분석
        </h2>
        {analysis && <Badge variant={STATUS_VARIANT[analysis.status]} size="sm">{STATUS_LABEL[analysis.status]}</Badge>}
        {analyzing && !loading && !failed && (
          <Button variant="outline" size="sm" className="ml-auto" disabled={!canRerun || rerunning}
            onClick={() => setConfirmRerun(true)}>
            다시 분석
          </Button>
        )}
      </div>

      {loading ? (
        <p className="text-sm text-gray-400">불러오는 중</p>
      ) : failed ? (
        <div className="flex items-center gap-3">
          <p className="text-sm text-red-600">불러오지 못했습니다.</p>
          <Button variant="outline" size="sm" onClick={() => load()}>다시 시도</Button>
        </div>
      ) : (
        <>
          {analysis === null ? (
            <p className="text-sm text-gray-500">
              {readingInProgress ? '고객 서류를 다 읽으면 입찰 분석을 시작합니다' : '입찰 분석이 없습니다'}
            </p>
          ) : (
            <div className="space-y-1.5 text-sm">
              {analysis.status === 'FAILED' && analysis.failureReason && (
                <p className="text-red-700">{analysis.failureReason}</p>
              )}
              {analysis.pwinPercent !== null && (
                <p className="text-gray-800">
                  Pwin <b>{analysis.pwinPercent}%</b>
                  {analysis.aiVerdict && <> · 판정 <b>{VERDICT_LABEL[analysis.aiVerdict]}</b></>}
                </p>
              )}
              {analysis.verdictReason && <p className="text-gray-600">{analysis.verdictReason}</p>}
              {analysis.adminDecision && (
                <p className="text-gray-800">
                  관리자 결정 <b>{DECISION_LABEL[analysis.adminDecision]}</b>
                  <span className="text-gray-500"> · {analysis.decidedBy?.name ?? '-'} · {formatDateTime(analysis.decidedAt)}</span>
                </p>
              )}
              {analysis.adminDecision && analysis.adminNote && (
                <p className="text-gray-600 whitespace-pre-line">{[DECISION_NOTE_LABEL[analysis.adminDecision], analysis.adminNote].filter(Boolean).join(' ')}</p>
              )}
            </div>
          )}

          {showTabs && analysis && (
            <Tabs value={tab} onValueChange={setTab}>
              <TabsList variant="line">
                <TabsTrigger value="report">종합 분석 리포트</TabsTrigger>
                <TabsTrigger value="capture">캡처플랜</TabsTrigger>
                <TabsTrigger value="decision">Bid/No-Bid 결정</TabsTrigger>
              </TabsList>
              <TabsContent value="report" className="pt-4">
                {analysis.report ? <ReportView report={analysis.report as Json} reqTexts={reqTexts} /> : <NotSaved />}
              </TabsContent>
              <TabsContent value="capture" className="pt-4">
                {analysis.capturePlan
                  ? <CapturePlanView plan={analysis.capturePlan as Json} issueRanking={analysis.issueRanking} />
                  : <NotSaved />}
              </TabsContent>
              <TabsContent value="decision" className="pt-4">
                {analysis.decision
                  ? <DecisionView decision={analysis.decision as Json} scores={analysis.scores}
                      strengths={analysis.strengths} weaknesses={analysis.weaknesses} />
                  : <NotSaved />}
              </TabsContent>
            </Tabs>
          )}

          {analysis && <Questions questions={analysis.questions ?? []} />}

          {analyzing && (
            <div className="space-y-3 border-t border-gray-100 pt-4">
              <h3 className="text-sm font-semibold text-gray-900">관리자 결정</h3>
              <RadioGroup value={choice} onValueChange={(v) => { setChoice(v as BidDecision); setFormError('') }}
                className="flex flex-wrap gap-5" disabled={!canDecide || saving}>
                {(Object.keys(DECISION_LABEL) as BidDecision[]).map((d) => (
                  <div key={d} className="flex items-center gap-2">
                    <RadioGroupItem value={d} id={`bid-decision-${d}`} />
                    <Label htmlFor={`bid-decision-${d}`}>{DECISION_LABEL[d]}</Label>
                  </div>
                ))}
              </RadioGroup>
              {noteLabel && (
                <div className="space-y-1">
                  <Label htmlFor="bid-decision-note">{noteLabel}</Label>
                  <Textarea id="bid-decision-note" rows={3} value={note} maxLength={DECISION_NOTE_MAX}
                    disabled={!canDecide || saving}
                    onChange={(e) => { setNote(e.target.value); setFormError('') }}
                    aria-describedby="bid-decision-error" />
                  <p className="text-xs text-gray-400 text-right">{note.length}/{DECISION_NOTE_MAX}</p>
                </div>
              )}
              <div>
                <Button size="sm" disabled={!canDecide || saving} onClick={save}>결정 저장</Button>
                <FieldError id="bid-decision-error">{formError}</FieldError>
              </div>
            </div>
          )}
        </>
      )}

      <ConfirmDialog
        open={confirmRerun}
        onOpenChange={setConfirmRerun}
        title="다시 분석"
        description="다시 분석하면 지금 결정은 이어지지 않습니다"
        confirmLabel="다시 분석"
        onConfirm={rerun}
        isLoading={rerunning}
      />
    </div>
  )
}
