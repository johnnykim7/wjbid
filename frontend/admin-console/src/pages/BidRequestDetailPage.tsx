import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { FileText, Paperclip, PencilLine, RotateCw } from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  ConfirmDialog,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  FieldError,
  InfoGrid,
  Label,
  PageHeader,
  RadioGroup,
  RadioGroupItem,
  Spinner,
  Textarea,
  toast,
} from '@therecommerce-develop/ds-ui'
import {
  getAdminBidRequestDetail,
  getBidDocumentsByBidRequest,
  getClientDocuments,
  transitionBidRequest,
  regenerateBidDocument,
} from '../api/client'
import StateBadge from '../components/StateBadge'
import { Loading } from '../components/LoadState'
import { docTypeLabel } from '../utils/documentType'
import DocumentReadingPanel from '../components/DocumentReadingPanel'
import BidAnalysisPanel from '../components/BidAnalysisPanel'

const NEXT_STATES: Record<string, string[]> = {
  CREATED: ['DOCS_PENDING', 'CLOSED'],
  DOCS_PENDING: ['DOCS_RECEIVED', 'CLOSED'],
  DOCS_RECEIVED: ['ANALYZING', 'DOCS_PENDING', 'CLOSED'],
  ANALYZING: ['GENERATING', 'CLOSED'],
  GENERATING: ['REVIEW'],
  REVIEW: ['CONFIRMED', 'GENERATING'],
  CONFIRMED: ['SUBMITTED', 'REVIEW'],
  SUBMITTED: ['AWARDED', 'NOT_AWARDED'],
}

const DECISION_REQUIRED_MESSAGE = 'Bid/No-Bid 결정을 Go 나 조건부 Go 로 정해야 문서 작성으로 넘어갑니다'

const DOC_STATUS_LABELS: Record<string, string> = {
  DRAFT: '초안',
  LOCKED: '잠김',
  ARCHIVED: '보관',
}

// ServiceLevel 이름(T3-1)
const SERVICE_LEVEL_LABELS: Record<string, string> = {
  MONITORING: '모니터링',
  ANALYSIS: '분석',
  AI_PROPOSAL: 'AI 제안서',
  FULL_PACKAGE: '풀패키지',
}

// GET /admin/bid-requests/{id} 는 공고와 회원을 펼친 칸으로 준다(T3-2, BL-003)
interface BidRequest {
  id: string
  state: string
  opportunityTitle?: string
  solicitationNumber?: string
  agencyName?: string
  memberEmail?: string
  memberCompanyName?: string
  noticeId?: string
  serviceLevel?: string
  createdAt?: string
}

interface BidDocument {
  id: string
  documentType: string
  status: string
  currentVersionNo: number
  createdAt?: string
}

interface ClientDocument {
  id: string
  fileName: string
  fileSize: number
  contentType: string
  createdAt?: string
}

export default function BidRequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [bidRequest, setBidRequest] = useState<BidRequest | null>(null)
  const [documents, setDocuments] = useState<BidDocument[]>([])
  const [clientDocs, setClientDocs] = useState<ClientDocument[]>([])
  const [clientDocsFailed, setClientDocsFailed] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Transition state
  const [showTransition, setShowTransition] = useState(false)
  const [selectedState, setSelectedState] = useState('')
  const [notes, setNotes] = useState('')
  const [transitionError, setTransitionError] = useState('')
  const [notesError, setNotesError] = useState('')
  const [transitioning, setTransitioning] = useState(false)
  const [regenerateType, setRegenerateType] = useState<string | null>(null)

  // CR-169: 고객 서류를 읽는 중이면 문서 작성 중 전환을 막는다(BIZ-087, 서버도 409 로 막는다)
  const [readingInProgress, setReadingInProgress] = useState(false)
  const [readingReloadKey, setReadingReloadKey] = useState(0)
  const [analysisStarted, setAnalysisStarted] = useState(false)

  // CR-170: 관리자 결정이 Go 나 조건부 Go 가 아니면 문서 작성 중 전환을 막는다(BIZ-092, 서버도 409 로 막는다).
  // 입찰 분석을 불러오지 못하면 null 이고 화면에서는 막지 않는다
  const [decisionAllowsGeneration, setDecisionAllowsGeneration] = useState<boolean | null>(null)
  const [analysisReloadKey, setAnalysisReloadKey] = useState(0)

  const loadData = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getAdminBidRequestDetail(id)
      setBidRequest(data.data ?? data)

      try {
        const docRes = await getBidDocumentsByBidRequest(id)
        setDocuments(docRes.data.data ?? docRes.data.content ?? docRes.data ?? [])
      } catch { /* no documents yet */ }

      // 실패를 빈 목록으로 보이면 서류가 없는 것처럼 보여 따로 띠로 알린다(BL-003)
      try {
        const clientRes = await getClientDocuments(id)
        setClientDocs(clientRes.data ?? [])
        setClientDocsFailed(false)
      } catch {
        setClientDocsFailed(true)
      }
    } catch {
      setError('입찰 요청을 불러올 수 없습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [id])

  // DOCS_RECEIVED 에서 DOCS_PENDING 으로 가는 것은 "첨부 반려"(고객 재업로드 요청)라 사유가 필요하다
  const isRejection = bidRequest?.state === 'DOCS_RECEIVED' && selectedState === 'DOCS_PENDING'
  const blockedByReading = bidRequest?.state === 'ANALYZING' && selectedState === 'GENERATING' && readingInProgress
  const blockedByDecision = bidRequest?.state === 'ANALYZING' && selectedState === 'GENERATING'
    && !readingInProgress && decisionAllowsGeneration === false

  const openTransition = (first: string) => {
    setSelectedState(first)
    setNotes('')
    setNotesError('')
    setTransitionError('')
    setShowTransition(true)
  }

  const handleTransition = async () => {
    if (!id || !selectedState) return
    if (isRejection && !notes.trim()) {
      setNotesError('반려 사유를 입력하세요. 고객에게 전달됩니다.')
      return
    }
    setTransitioning(true)
    setTransitionError('')
    try {
      await transitionBidRequest(id, selectedState, notes)
      setShowTransition(false)
      setNotes('')
      loadData()
    } catch (err: unknown) {
      const res = (err as { response?: { status?: number; data?: { error?: string } } })?.response
      if (res?.status === 409 && res.data?.error === 'DOCUMENT_READING_IN_PROGRESS') {
        toast.error('고객 서류를 읽는 중입니다. 다 읽은 뒤 전환하세요')
        setReadingReloadKey((k) => k + 1)
      } else if (res?.status === 409 && res.data?.error === 'BID_DECISION_REQUIRED') {
        toast.error(DECISION_REQUIRED_MESSAGE)
        setAnalysisReloadKey((k) => k + 1)
      } else {
        setTransitionError('상태 전환에 실패했습니다.')
      }
    } finally {
      setTransitioning(false)
    }
  }

  const handleRegenerate = async () => {
    if (!id || !regenerateType) return
    await regenerateBidDocument(id, regenerateType)
    toast.success(`${docTypeLabel(regenerateType)} 문서 재생성을 시작했습니다. 완료까지 잠시 걸립니다.`)
    setRegenerateType(null)
  }

  if (loading) return <Loading className="h-64" />

  if (error && !bidRequest) {
    return (
      <div className="space-y-4">
        <PageHeader variant="detail" back={{ onClick: () => navigate('/bid-requests') }} />
        <Alert variant="destructive" appearance="light">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      </div>
    )
  }

  if (!bidRequest) return null

  const nextStates = NEXT_STATES[bidRequest.state] ?? []

  return (
    <div className="space-y-4">
      <PageHeader
        variant="detail"
        back={{ onClick: () => navigate('/bid-requests') }}
        title={bidRequest.opportunityTitle ?? '-'}
        badge={<StateBadge state={bidRequest.state} />}
        actions={nextStates.length > 0 && (
          <Button variant="primary" onClick={() => openTransition(nextStates[0])}>
            상태 전환
          </Button>
        )}
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>공고 정보</CardTitle>
          </CardHeader>
          <CardContent>
            <InfoGrid
              cols={1}
              items={[
                { label: '제목', value: bidRequest.opportunityTitle ?? '-' },
                { label: '기관', value: bidRequest.agencyName ?? '-' },
                { label: '공고번호', value: bidRequest.solicitationNumber ?? '-' },
              ]}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>고객 정보</CardTitle>
          </CardHeader>
          <CardContent>
            <InfoGrid
              cols={1}
              items={[
                { label: '회사명', value: bidRequest.memberCompanyName ?? '-' },
                { label: '이메일', value: bidRequest.memberEmail ?? '-' },
              ]}
            />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>생성 문서</CardTitle>
        </CardHeader>
        <CardContent>
          {documents.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">아직 생성된 문서가 없습니다.</p>
          ) : (
            <div className="divide-y divide-border">
              {documents.map(doc => (
                <div key={doc.id} className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <FileText className="size-4 text-muted-foreground" />
                    <div>
                      <p className="text-sm font-medium text-foreground">{docTypeLabel(doc.documentType)}</p>
                      <p className="text-xs text-muted-foreground">v{doc.currentVersionNo} | {DOC_STATUS_LABELS[doc.status] ?? doc.status}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {bidRequest.state === 'REVIEW' && doc.status !== 'LOCKED' && (
                      <Button size="sm" variant="outline" onClick={() => setRegenerateType(doc.documentType)}>
                        <RotateCw />
                        재생성
                      </Button>
                    )}
                    <Button size="sm" variant="outline" asChild>
                      <Link to={`/documents/${doc.id}/edit`}>
                        <PencilLine />
                        편집
                      </Link>
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>고객 제출 문서</CardTitle>
        </CardHeader>
        <CardContent>
          {clientDocsFailed ? (
            <Alert variant="destructive" appearance="light">
              <AlertDescription>고객 제출 문서를 불러오지 못했습니다</AlertDescription>
            </Alert>
          ) : clientDocs.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">고객이 업로드한 문서가 없습니다.</p>
          ) : (
            <div className="divide-y divide-border">
              {clientDocs.map(doc => (
                <div key={doc.id} className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <Paperclip className="size-4 text-muted-foreground" />
                    <div>
                      <p className="text-sm font-medium text-foreground">{doc.fileName}</p>
                      <p className="text-xs text-muted-foreground">
                        {(doc.fileSize / 1024).toFixed(1)} KB | {doc.contentType}
                      </p>
                    </div>
                  </div>
                  {doc.createdAt && (
                    <span className="text-xs text-muted-foreground">
                      {new Date(doc.createdAt).toLocaleDateString('ko-KR')}
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <DocumentReadingPanel
        bidRequestId={bidRequest.id}
        bidState={bidRequest.state}
        reloadKey={readingReloadKey}
        onInProgressChange={setReadingInProgress}
        onStartedChange={setAnalysisStarted}
      />

      {analysisStarted && (
        <BidAnalysisPanel
          bidRequestId={bidRequest.id}
          bidState={bidRequest.state}
          noticeId={bidRequest.noticeId}
          reloadKey={analysisReloadKey}
          onDecisionChange={setDecisionAllowsGeneration}
        />
      )}

      <Card>
        <CardHeader>
          <CardTitle>기타 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <InfoGrid
            items={[
              {
                label: '서비스 레벨',
                value: bidRequest.serviceLevel ? (SERVICE_LEVEL_LABELS[bidRequest.serviceLevel] ?? bidRequest.serviceLevel) : '-',
              },
              { label: '생성일', value: bidRequest.createdAt ? new Date(bidRequest.createdAt).toLocaleDateString('ko-KR') : '-' },
            ]}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={regenerateType !== null}
        onOpenChange={(open) => { if (!open) setRegenerateType(null) }}
        title={`${regenerateType ? docTypeLabel(regenerateType) : ''} 문서 재생성`}
        description="새 버전으로 추가되고 완료되면 알림이 발송됩니다."
        confirmLabel="재생성"
        onConfirm={handleRegenerate}
        errorText={() => '문서 재생성에 실패했습니다. REVIEW 상태에서만 가능하며, 잠긴 문서는 재생성할 수 없습니다.'}
      />

      <Dialog open={showTransition} onOpenChange={setShowTransition}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>상태 전환</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-4">
            {transitionError && (
              <Alert variant="destructive" appearance="light">
                <AlertDescription>{transitionError}</AlertDescription>
              </Alert>
            )}
            <div className="flex items-center gap-2 text-sm">
              <StateBadge state={bidRequest.state} />
              <span className="text-xs text-muted-foreground">다음 상태 선택</span>
            </div>
            <RadioGroup value={selectedState} onValueChange={setSelectedState} className="space-y-2">
              {nextStates.map(s => (
                <div key={s} className="flex items-center gap-3">
                  <RadioGroupItem value={s} id={`detail-next-${s}`} />
                  <Label htmlFor={`detail-next-${s}`} className="cursor-pointer">
                    <StateBadge
                      state={s}
                      label={bidRequest.state === 'DOCS_RECEIVED' && s === 'DOCS_PENDING' ? '문서 대기, 첨부 반려' : undefined}
                    />
                  </Label>
                </div>
              ))}
            </RadioGroup>
            {blockedByReading && (
              <Alert variant="warning" appearance="light">
                <AlertDescription>고객 서류를 읽는 중입니다</AlertDescription>
              </Alert>
            )}
            {blockedByDecision && (
              <Alert variant="warning" appearance="light">
                <AlertDescription>{DECISION_REQUIRED_MESSAGE}</AlertDescription>
              </Alert>
            )}
            <div>
              <Textarea
                value={notes}
                onChange={e => { setNotes(e.target.value); setNotesError('') }}
                rows={3}
                placeholder={isRejection ? '반려 사유. 필수이며 고객에게 전달됩니다' : '전환 사유 (선택)'}
                aria-invalid={!!notesError}
                aria-describedby="transition-notes-error"
                className="resize-none"
              />
              <FieldError id="transition-notes-error">{notesError}</FieldError>
            </div>
          </DialogBody>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowTransition(false)}>취소</Button>
            <Button
              variant="primary"
              onClick={handleTransition}
              disabled={!selectedState || transitioning || blockedByReading || blockedByDecision}
            >
              {transitioning && <Spinner />}
              전환
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
