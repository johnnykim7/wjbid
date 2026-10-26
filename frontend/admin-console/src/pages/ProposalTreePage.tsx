import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { FilePenLine, Lock, LockOpen, RotateCw, ShieldCheck } from 'lucide-react'
import {
  getProposalTree,
  getProposalSection,
  regenerateProposalSection,
  lockProposalSection,
  unlockProposalSection,
  getProposalSectionVerification,
  reverifyProposalSection,
  getProposalCoverage,
} from '../api/client'
import {
  Alert,
  AlertContent,
  AlertDescription,
  AlertTitle,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardToolbar,
  ConfirmDialog,
  InfoGrid,
  PageHeader,
  toast,
} from '@therecommerce-develop/ds-ui'
import CoverageBand from '../components/CoverageBand'
import DocumentReadingFacts from '../components/DocumentReadingFacts'
import { ToneBadge } from '../components/StateBadge'
import { Loading } from '../components/LoadState'
import { readingStatusLabel } from '../components/documentReading'
import type { SourceDocument } from '../components/documentReading'
import type { Coverage } from '../components/CoverageBand'
import type { StateTone } from '../utils/bidState'

// ── 타입 (BE ProposalTreeDto 대응) ──
interface SectionNode {
  id: string
  chapterId: string
  subfactorLabel: string | null
  title: string | null
  status: SectionStatus
  locked: boolean
  minWords: number | null
  requirementRefs: string[] | null
  orderNo: number
}
interface ChapterNode {
  id: string
  factorLabel: string | null
  factorTitle: string | null
  sourceSection: string | null
  orderNo: number
  sections: SectionNode[]
}
interface BlockNode {
  id: string
  blockType: string | null
  contentJson: Record<string, unknown> | null
  sourceEvidence: Record<string, unknown> | null
  orderNo: number
}
interface SectionDetail {
  id: string
  subfactorLabel: string | null
  title: string | null
  scope: string | null
  status: SectionStatus
  locked: boolean
  minWords: number | null
  requirementRefs: string[] | null
  blocks: BlockNode[]
  sourceDocuments: SourceDocument[] | null
}

type SectionStatus = 'PENDING' | 'DRAFTING' | 'DRAFTED' | 'VERIFIED' | 'NEEDS_REGEN' | 'LOCKED'

// CR-031 검증 결과
interface VerificationView {
  available: boolean
  method?: 'RULE' | 'LLM'
  verdict?: 'PASS' | 'FAIL'
  hallucinations?: { sentence?: string; reason?: string }[]
  hallucinationCount?: number
  missingFromSource?: { source_quote?: string; reason?: string }[]
  ruleFindings?: string[]
  wordCount?: number | null
  attempt?: number
  verifiedAt?: string
  message?: string
}

const STATUS_TONE: Record<SectionStatus, StateTone> = {
  PENDING: 'gray',
  DRAFTING: 'info',
  DRAFTED: 'info',
  VERIFIED: 'success',
  NEEDS_REGEN: 'destructive',
  LOCKED: 'gray',
}

const STATUS_LABEL: Record<SectionStatus, string> = {
  PENDING: '대기',
  DRAFTING: '작성중',
  DRAFTED: '초안',
  VERIFIED: '검증완료',
  NEEDS_REGEN: '재생성필요',
  LOCKED: '잠김',
}

/** TipTap JSON node 에서 평문 텍스트 추출 (읽기 전용 미리보기용). */
function extractText(node: unknown): string {
  if (!node || typeof node !== 'object') return ''
  const n = node as Record<string, unknown>
  if (typeof n.text === 'string') return n.text
  const content = n.content
  if (Array.isArray(content)) return content.map(extractText).join('')
  return ''
}

export default function ProposalTreePage() {
  const { id: documentId } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [chapters, setChapters] = useState<ChapterNode[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<SectionDetail | null>(null)
  const [verification, setVerification] = useState<VerificationView | null>(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [acting, setActing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [coverage, setCoverage] = useState<Coverage | null>(null)
  const [coverageFailed, setCoverageFailed] = useState(false)
  const [openSourceId, setOpenSourceId] = useState<string | null>(null)
  const [regenerateId, setRegenerateId] = useState<string | null>(null)

  const loadTree = useCallback(async () => {
    if (!documentId) return
    setLoading(true)
    try {
      const res = await getProposalTree(documentId)
      setChapters(res.data.chapters || [])
    } catch {
      setError('트리를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [documentId])

  // CR-159 커버리지는 트리와 따로 불러 실패해도 트리는 그대로 둔다
  const loadCoverage = useCallback(async () => {
    if (!documentId) return
    setCoverageFailed(false)
    try {
      const res = await getProposalCoverage(documentId)
      setCoverage(res.data)
    } catch {
      setCoverageFailed(true)
    }
  }, [documentId])

  useEffect(() => {
    loadTree()
    loadCoverage()
  }, [loadTree, loadCoverage])

  const sectionTitle = (sectionId: string) => {
    for (const ch of chapters) {
      const s = ch.sections.find((x) => x.id === sectionId)
      if (s) return `${s.subfactorLabel ? `${s.subfactorLabel} ` : ''}${s.title || '(무제)'}`
    }
    return sectionId
  }
  const refsOf = (sectionId: string) =>
    coverage?.matrixReady && coverage.evaluatedDocument
      ? coverage.sections.find((s) => s.sectionId === sectionId)?.refs ?? []
      : null
  const unknownIn = (sectionId: string) =>
    new Set((coverage?.unknownIds ?? []).filter((u) => u.sectionId === sectionId).map((u) => u.reqId))

  const loadDetail = useCallback(async (sectionId: string) => {
    if (!documentId) return
    setSelectedId(sectionId)
    setDetailLoading(true)
    setVerification(null)
    setOpenSourceId(null)
    try {
      const res = await getProposalSection(documentId, sectionId)
      setDetail(res.data)
      // CR-031 검증 결과 병행 로드 (실패해도 상세는 표시)
      try {
        const v = await getProposalSectionVerification(documentId, sectionId)
        setVerification(v.data)
      } catch {
        setVerification(null)
      }
    } catch {
      setError('section 상세를 불러오지 못했습니다.')
    } finally {
      setDetailLoading(false)
    }
  }, [documentId])

  const handleReverify = async () => {
    if (!documentId || !detail) return
    setActing(true)
    setError(null)
    try {
      await reverifyProposalSection(documentId, detail.id)
      setError(null)
      await loadCoverage()
      toast.success('재검증을 시작했습니다. 잠시 후 새로고침하면 결과가 갱신됩니다.')
    } catch {
      setError('재검증 요청에 실패했습니다.')
    } finally {
      setActing(false)
    }
  }

  const handleRegenerate = async (sectionId: string) => {
    if (!documentId) return
    setActing(true)
    setError(null)
    try {
      await regenerateProposalSection(documentId, sectionId)
      await loadTree()
      await loadDetail(sectionId)
      await loadCoverage()
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || '재생성 요청에 실패했습니다.')
    } finally {
      setActing(false)
    }
  }

  const handleToggleLock = async () => {
    if (!documentId || !detail) return
    setActing(true)
    setError(null)
    try {
      if (detail.locked) {
        await unlockProposalSection(documentId, detail.id)
      } else {
        await lockProposalSection(documentId, detail.id)
      }
      await loadTree()
      await loadDetail(detail.id)
    } catch {
      setError('잠금 상태 변경에 실패했습니다.')
    } finally {
      setActing(false)
    }
  }

  const reqRefsValue = (sectionDetail: SectionDetail) => {
    const refs = refsOf(sectionDetail.id)
    if (!refs) {
      return sectionDetail.requirementRefs && sectionDetail.requirementRefs.length > 0
        ? sectionDetail.requirementRefs.join(', ')
        : null
    }
    if (refs.length === 0) return null
    const unknown = unknownIn(sectionDetail.id)
    const known = refs.filter((r) => !unknown.has(r.reqId))
    return (
      <>
        {known.map((r) => (
          <Badge
            key={r.reqId}
            variant={r.fulfilled ? 'success' : 'warning'}
            appearance="light"
            size="sm"
            className="whitespace-nowrap"
          >
            {r.reqId} {r.fulfilled ? '충족' : '누락'}
          </Badge>
        ))}
        {unknown.size > 0 && (
          <span className="text-xs text-destructive">
            매트릭스에 없는 ID: {[...unknown].join(', ')}
          </span>
        )}
      </>
    )
  }

  const sourcesValue = (sectionDetail: SectionDetail) =>
    (sectionDetail.sourceDocuments ?? []).length === 0 ? (
      <span className="text-muted-foreground">근거 서류 없음</span>
    ) : (
      (sectionDetail.sourceDocuments ?? []).map((d) => (
        <Button
          key={d.readingId}
          variant={openSourceId === d.readingId ? 'secondary' : 'outline'}
          size="sm"
          onClick={() => setOpenSourceId(openSourceId === d.readingId ? null : d.readingId)}
        >
          {d.fileName} 사실 {d.factCount}
          {d.status !== 'DONE' && ` (${readingStatusLabel(d.status)})`}
        </Button>
      ))
    )

  const showDetail = detail !== null && !detailLoading

  return (
    <div className="space-y-4">
      <PageHeader
        variant="detail"
        back={{ onClick: () => navigate(-1) }}
        title="제안서 Section 관리"
        actions={
          <Button variant="outline" onClick={() => documentId && navigate(`/documents/${documentId}/edit`)}>
            <FilePenLine />
            통합 편집기
          </Button>
        }
      />

      {error && (
        <Alert variant="destructive" appearance="light">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <CoverageBand
        coverage={coverage}
        failed={coverageFailed}
        onRetry={loadCoverage}
        onSelectSection={loadDetail}
        sectionTitle={sectionTitle}
      />

      <div className="grid grid-cols-12 gap-4">
        {/* ── 좌: 트리 ── */}
        <Card className="col-span-4 max-h-[75vh] overflow-hidden">
          <CardContent className="min-h-0 overflow-y-auto p-3">
            {loading ? (
              <Loading className="h-32" />
            ) : chapters.length === 0 ? (
              <p className="p-2 text-sm text-muted-foreground">생성된 section 이 없습니다.</p>
            ) : (
              chapters.map((ch) => (
                <div key={ch.id} className="mb-3">
                  <div className="px-1 py-1 text-sm font-semibold text-foreground">
                    {ch.factorLabel ? `${ch.factorLabel}. ` : ''}
                    {ch.factorTitle || '(제목 없음)'}
                  </div>
                  <ul className="ml-2">
                    {ch.sections.map((s) => {
                      const refs = refsOf(s.id)
                      const done = refs ? refs.filter((r) => r.fulfilled).length : 0
                      return (
                        <li key={s.id}>
                          <Button
                            variant="ghost"
                            onClick={() => loadDetail(s.id)}
                            className={`h-auto w-full justify-between gap-2 px-2 py-1.5 text-left text-sm font-normal ${
                              selectedId === s.id ? 'bg-accent ring-1 ring-input' : ''
                            }`}
                          >
                            <span className="truncate">
                              {s.subfactorLabel ? `${s.subfactorLabel} ` : ''}
                              {s.title || '(무제)'}
                            </span>
                            <span className="flex shrink-0 items-center gap-1">
                              {refs && refs.length > 0 && (
                                done < refs.length ? (
                                  <ToneBadge tone="warning">{done}/{refs.length}</ToneBadge>
                                ) : (
                                  <span className="text-[11px] text-muted-foreground">{done}/{refs.length}</span>
                                )
                              )}
                              <ToneBadge tone={STATUS_TONE[s.status]}>{STATUS_LABEL[s.status]}</ToneBadge>
                            </span>
                          </Button>
                        </li>
                      )
                    })}
                  </ul>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        {/* ── 우: 상세 ── */}
        <Card className="col-span-8 max-h-[75vh] overflow-hidden">
          {showDetail && (
            <CardHeader>
              <div className="flex min-w-0 items-center gap-2">
                <CardTitle className="truncate text-base">
                  {detail.subfactorLabel ? `${detail.subfactorLabel} ` : ''}
                  {detail.title || '(무제)'}
                </CardTitle>
                <ToneBadge tone={STATUS_TONE[detail.status]}>{STATUS_LABEL[detail.status]}</ToneBadge>
              </div>
              <CardToolbar>
                {/* 잠긴 section 의 안내를 보이려고 감싼다. 끈 단추는 마우스를 받지 않는다 */}
                <span title={detail.locked ? '잠긴 section 은 재생성할 수 없습니다' : ''}>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => setRegenerateId(detail.id)}
                    disabled={acting || detail.locked}
                  >
                    <RotateCw />
                    재생성
                  </Button>
                </span>
                <Button variant="outline" size="sm" onClick={handleToggleLock} disabled={acting}>
                  {detail.locked ? <LockOpen /> : <Lock />}
                  {detail.locked ? '잠금 해제' : '잠금'}
                </Button>
              </CardToolbar>
            </CardHeader>
          )}
          <CardContent className="min-h-0 overflow-y-auto">
            {!detail ? (
              <p className="text-sm text-muted-foreground">좌측에서 section 을 선택하세요.</p>
            ) : detailLoading ? (
              <Loading className="h-32" />
            ) : (
              <div className="space-y-4">
                <InfoGrid
                  items={[
                    { label: '최소 분량', value: detail.minWords != null ? `${detail.minWords} words` : null },
                    { label: '대응 REQ ID', value: reqRefsValue(detail) },
                    // CR-169 근거 서류
                    { label: '근거 서류', value: sourcesValue(detail), span: 2 },
                    ...(detail.scope ? [{ label: '작성 지침', value: detail.scope, span: 2 }] : []),
                  ]}
                />
                {openSourceId && (
                  <div className="rounded-md bg-muted p-2">
                    <DocumentReadingFacts readingId={openSourceId} />
                  </div>
                )}

                {/* 검증 결과 (CR-031) */}
                <div>
                  <div className="mb-1 flex items-center justify-between">
                    <span className="text-sm font-medium text-foreground">충실성·분량 검증</span>
                    <Button variant="outline" size="sm" onClick={handleReverify} disabled={acting}>
                      <ShieldCheck />
                      재검증
                    </Button>
                  </div>
                  {!verification || !verification.available ? (
                    <div className="rounded border border-dashed border-border p-2 text-xs text-muted-foreground">
                      {verification?.message || '아직 검증되지 않았습니다.'}
                    </div>
                  ) : (
                    <div className="space-y-2 rounded border border-border p-2 text-sm">
                      <div className="flex items-center gap-2">
                        <ToneBadge tone={verification.verdict === 'PASS' ? 'success' : 'destructive'}>
                          {verification.verdict === 'PASS' ? '검증 통과' : '검증 실패'}
                        </ToneBadge>
                        <span className="text-xs text-muted-foreground">
                          {verification.method === 'RULE' ? '정형 룰' : 'LLM 충실성'}
                          {verification.attempt && verification.attempt > 1
                            ? ` · 재시도 ${verification.attempt - 1}회`
                            : ''}
                          {verification.wordCount != null ? ` · ${verification.wordCount} words` : ''}
                        </span>
                      </div>

                      {/* 환각 문장 */}
                      {verification.hallucinations && verification.hallucinations.length > 0 && (
                        <Alert variant="destructive" appearance="light" size="sm">
                          <AlertContent>
                            <AlertTitle className="text-destructive">
                              근거 없는 문장 (환각) {verification.hallucinations.length}건
                            </AlertTitle>
                            <AlertDescription>
                              <ul className="space-y-1">
                                {verification.hallucinations.map((h, i) => (
                                  <li key={i} className="text-xs">
                                    <span className="text-foreground">“{h.sentence}”</span>
                                    {h.reason && <span className="text-muted-foreground">, {h.reason}</span>}
                                  </li>
                                ))}
                              </ul>
                            </AlertDescription>
                          </AlertContent>
                        </Alert>
                      )}

                      {/* 누락 */}
                      {verification.missingFromSource && verification.missingFromSource.length > 0 && (
                        <Alert variant="warning" appearance="light" size="sm">
                          <AlertContent>
                            <AlertTitle>원문 누락 {verification.missingFromSource.length}건</AlertTitle>
                            <AlertDescription>
                              <ul className="space-y-1">
                                {verification.missingFromSource.map((m, i) => (
                                  <li key={i} className="text-xs">
                                    <span className="text-foreground">“{m.source_quote}”</span>
                                    {m.reason && <span className="text-muted-foreground">, {m.reason}</span>}
                                  </li>
                                ))}
                              </ul>
                            </AlertDescription>
                          </AlertContent>
                        </Alert>
                      )}

                      {/* 정형 룰 위반 */}
                      {verification.ruleFindings && verification.ruleFindings.length > 0 && (
                        <Alert variant="destructive" appearance="light" size="sm">
                          <AlertContent>
                            <AlertTitle className="text-destructive">정형 룰 위반</AlertTitle>
                            <AlertDescription>
                              <ul className="ml-4 list-disc space-y-0.5">
                                {verification.ruleFindings.map((f, i) => (
                                  <li key={i} className="text-xs text-foreground">
                                    {f}
                                  </li>
                                ))}
                              </ul>
                            </AlertDescription>
                          </AlertContent>
                        </Alert>
                      )}
                    </div>
                  )}
                </div>

                {/* 본문 (읽기 전용 미리보기) */}
                <div className="border-t border-border pt-3">
                  <div className="mb-2 text-sm font-medium text-foreground">
                    본문 ({detail.blocks.length} blocks)
                  </div>
                  {detail.blocks.length === 0 ? (
                    <p className="text-sm text-muted-foreground">작성된 본문이 없습니다.</p>
                  ) : (
                    <div className="space-y-2">
                      {detail.blocks.map((b) => {
                        const text = extractText(b.contentJson)
                        return (
                          <p key={b.id} className="whitespace-pre-wrap text-sm text-foreground">
                            {text || <span className="text-muted-foreground">(빈 block)</span>}
                          </p>
                        )
                      })}
                    </div>
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <ConfirmDialog
        open={regenerateId !== null}
        onOpenChange={(open) => { if (!open) setRegenerateId(null) }}
        title="section 본문 재생성"
        description="기존 본문은 새 본문으로 바뀝니다."
        confirmLabel="재생성"
        onConfirm={() => {
          const sectionId = regenerateId
          setRegenerateId(null)
          if (sectionId) handleRegenerate(sectionId)
        }}
      />
    </div>
  )
}
