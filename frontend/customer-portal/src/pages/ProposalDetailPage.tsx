import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { Button as DsButton } from '@therecommerce-develop/ds-ui'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { PreCheckBoard } from '../components/precheck/PreCheckBoard'
import {
  getBidRequest,
  getBidRequestDocuments,
  getBidRequestHistory,
  getPreCheck,
} from '../api/client'
import { PRE_CHECK_DETAIL_TEXT, PRE_CHECK_TEXT, preCheckHref } from '../data/preCheckContent'
import { STATE_LABEL, STATE_BADGE } from '../types'
import type { BidRequest, StateTransition } from '../types'
import type { PreCheckDetail } from '../types/preCheck'

const DOC_TYPE_LABELS: Record<string, string> = {
  COVER_LETTER: 'Cover Letter',
  TECHNICAL_PROPOSAL: 'Technical Proposal',
  PAST_PERFORMANCE: 'Past Performance',
  COMPANY_PROFILE: 'Company Profile',
  COMPLIANCE_MATRIX: 'Compliance Matrix',
  PRICING_SUMMARY: 'Pricing Summary',
  OTHER: 'Other',
}

interface BidDocSummary {
  id: string
  documentType: string
  status: string
  currentVersionNo: number
  contentJson?: Record<string, unknown>
  changeSummary?: string
  editedAt?: string
}

function extractText(contentJson: Record<string, unknown>): string {
  if (!contentJson) return ''
  const traverse = (node: unknown): string => {
    if (!node || typeof node !== 'object') return ''
    const n = node as Record<string, unknown>
    if (n.type === 'text') return (n.text as string) ?? ''
    if (Array.isArray(n.content)) {
      return (n.content as unknown[]).map(traverse).join(
        n.type === 'paragraph' || n.type === 'heading' ? '\n' : ''
      )
    }
    return ''
  }
  return traverse(contentJson).trim()
}

export default function ProposalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [proposal, setProposal] = useState<BidRequest | null>(null)
  const [documents, setDocuments] = useState<BidDocSummary[]>([])
  const [history, setHistory] = useState<StateTransition[]>([])
  const [preCheck, setPreCheck] = useState<PreCheckDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [searchParams] = useSearchParams()
  // 기본 탭은 '제출 서류'(uploads). ?tab= 으로 딥링크 시 해당 탭으로 진입
  const tabParam = searchParams.get('tab')
  const initialTab: 'uploads' | 'documents' | 'history' =
    tabParam === 'documents' || tabParam === 'history' ? tabParam : 'uploads'
  const [activeTab, setActiveTab] = useState<'uploads' | 'documents' | 'history'>(initialTab)
  const [activeDocIdx, setActiveDocIdx] = useState(0)

  const loadAll = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getBidRequest(id)
      const p: BidRequest = data.data ?? data
      setProposal(p)

      // CR-141: 제출 서류 탭은 공고 분석의 요구 서류에 파일을 잇는 제출 전 검사 결과를 쓴다
      const [docRes, histRes, preCheckRes] = await Promise.allSettled([
        getBidRequestDocuments(id),
        getBidRequestHistory(id),
        getPreCheck(id),
      ])
      if (docRes.status === 'fulfilled') setDocuments((docRes.value as { data: BidDocSummary[] }).data ?? [])
      if (histRes.status === 'fulfilled') setHistory((histRes.value as { data: StateTransition[] }).data ?? [])
      setPreCheck(preCheckRes.status === 'fulfilled' ? preCheckRes.value.data : null)
    } catch {
      setProposal(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadAll() }, [id])

  const reloadPreCheck = async () => {
    if (!id) return
    try {
      const { data } = await getPreCheck(id)
      setPreCheck(data)
    } catch { /* 다시 읽지 못하면 앞의 결과를 그대로 둔다 */ }
  }

  if (loading) return <LoadingSpinner fullPage />
  if (!proposal) return (
    <div className="flex flex-col items-center justify-center h-full text-ink-mute gap-4 p-8">
      <i className="fa-solid fa-circle-exclamation text-4xl text-ink-faint" />
      <p>제안서를 찾을 수 없습니다.</p>
      <DsButton variant="ghost" mode="link" onClick={() => navigate('/proposals')}>목록으로</DsButton>
    </div>
  )

  const activeDoc = documents[activeDocIdx]

  // 관리자 첨부 반려: 가장 최근 DOCS_RECEIVED → DOCS_PENDING 전이의 사유 (현재 DOCS_PENDING일 때만 노출)
  const rejectionReason = proposal.state === 'DOCS_PENDING'
    ? history
        .filter(h => h.fromState === 'DOCS_RECEIVED' && h.toState === 'DOCS_PENDING')
        .sort((a, b) => (b.timestamp ?? '').localeCompare(a.timestamp ?? ''))[0]?.notes
    : undefined

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="sticky top-0 bg-white border-b border-line px-6 py-4 flex justify-between items-center shadow-sm z-10">
        <div className="flex items-center gap-4">
          <DsButton variant="ghost" size="icon" onClick={() => navigate('/proposals')}>
            <i className="fa-solid fa-arrow-left text-xl" />
          </DsButton>
          <div>
            <h3 className="text-lg font-bold text-ink line-clamp-1">{proposal.displayTitle || proposal.opportunityTitle}</h3>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="text-xs text-ink-mute">{proposal.solicitationNumber ?? proposal.opportunityId}</span>
              <Badge variant={STATE_BADGE[proposal.state] ?? 'active'} size="sm">
                {STATE_LABEL[proposal.state] ?? proposal.state}
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white border-b border-line px-6 flex gap-1">
        {([
          { key: 'uploads', label: '제출 서류', icon: 'fa-solid fa-cloud-arrow-up', count: preCheck?.clientItems.length ?? 0 },
          { key: 'documents', label: '생성 문서', icon: 'fa-solid fa-file-lines', count: documents.length },
          { key: 'history', label: '진행 이력', icon: 'fa-solid fa-clock-rotate-left', count: history.length },
        ] as const).map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-3 text-sm font-medium border-b-2 transition ${
              activeTab === tab.key
                ? 'border-brand text-brand'
                : 'border-transparent text-ink-mute hover:text-ink-sub'
            }`}
          >
            <i className={`${tab.icon} mr-1.5`} />
            {tab.label}
            {tab.count > 0 && (
              <span className="ml-1.5 px-1.5 py-0.5 rounded-full text-xs bg-surface text-ink-mute">{tab.count}</span>
            )}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        {/* Documents Tab */}
        {activeTab === 'documents' && (
          documents.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-center p-8">
              <div className="w-16 h-16 bg-brand-soft rounded-full flex items-center justify-center mb-4">
                <i className="fa-solid fa-file-circle-plus text-2xl text-brand" />
              </div>
              <h3 className="text-lg font-bold text-ink mb-2">아직 생성된 문서가 없습니다</h3>
              <p className="text-sm text-ink-mute max-w-xs">
                요구사항 분석과 문서 생성이 끝나면 여기에 표시됩니다.
              </p>
            </div>
          ) : (
            <div className="flex h-full">
              {/* Doc list sidebar */}
              <div className="w-64 border-r border-line bg-surface flex-shrink-0 overflow-y-auto">
                {documents.map((doc, idx) => (
                  <button
                    key={doc.id}
                    onClick={() => setActiveDocIdx(idx)}
                    className={`w-full text-left px-4 py-4 border-b border-line-soft hover:bg-white transition ${
                      idx === activeDocIdx ? 'bg-white border-l-2 border-l-brand' : ''
                    }`}
                  >
                    <p className="text-sm font-medium text-ink">
                      {DOC_TYPE_LABELS[doc.documentType] ?? doc.documentType}
                    </p>
                    <p className="text-xs text-ink-mute mt-0.5">
                      v{doc.currentVersionNo} | {doc.status}
                    </p>
                  </button>
                ))}
              </div>
              {/* Doc viewer */}
              <div className="flex-1 p-8 overflow-y-auto">
                {activeDoc?.contentJson ? (
                  <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-sm border border-line p-8">
                    <h1 className="text-xl font-bold text-center uppercase tracking-wider border-b-2 border-ink pb-4 mb-6">
                      {DOC_TYPE_LABELS[activeDoc.documentType] ?? activeDoc.documentType}
                    </h1>
                    <pre className="whitespace-pre-wrap font-serif text-sm text-ink leading-relaxed">
                      {extractText(activeDoc.contentJson)}
                    </pre>
                    {activeDoc.changeSummary && (
                      <p className="mt-6 pt-4 border-t border-line-soft text-xs text-ink-mute">
                        <i className="fa-solid fa-clock-rotate-left mr-1" /> {activeDoc.changeSummary}
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="flex items-center justify-center h-full text-sm text-ink-mute">
                    문서 내용이 없습니다.
                  </div>
                )}
              </div>
            </div>
          )
        )}

        {/* 제출 서류 탭: 제출 전 검사와 같은 요구 서류 목록 (CR-141) */}
        {activeTab === 'uploads' && (
          <div className="p-6 max-w-5xl mx-auto space-y-4">
            {/* 관리자 첨부 반려 사유 — 재업로드 안내 */}
            {rejectionReason && (
              <div className="bg-warning-soft border border-warning/30 rounded-xl p-4">
                <div className="flex items-start gap-2">
                  <i className="fa-solid fa-triangle-exclamation text-warning mt-0.5" />
                  <div>
                    <p className="text-sm font-semibold text-warning">제출 서류가 반려되었습니다</p>
                    <p className="text-xs text-warning mt-1 whitespace-pre-wrap">{rejectionReason}</p>
                    <p className="text-xs text-warning mt-2">아래에서 서류를 다시 업로드한 뒤 ‘문서 제출 완료’를 눌러주세요.</p>
                  </div>
                </div>
              </div>
            )}
            {preCheck ? (
              <div className="wj">
                <p className="pc-tabnote">
                  {PRE_CHECK_DETAIL_TEXT.tabNote}
                  <Link to={preCheckHref(preCheck.bidRequestId)}>{PRE_CHECK_DETAIL_TEXT.tabLink}</Link>
                </p>
                <PreCheckBoard detail={preCheck} reload={reloadPreCheck} onSubmitted={loadAll} />
              </div>
            ) : (
              <p className="text-sm text-ink-mute text-center py-12">{PRE_CHECK_TEXT.loadError}</p>
            )}
          </div>
        )}

        {/* History Tab */}
        {activeTab === 'history' && (
          <div className="p-6 max-w-3xl mx-auto">
            {history.length === 0 ? (
              <div className="text-center py-12 text-ink-mute">
                <i className="fa-solid fa-clock-rotate-left text-4xl mb-3" />
                <p className="text-sm">진행 이력이 없습니다.</p>
              </div>
            ) : (
              <div className="relative pl-6">
                <div className="absolute left-2.5 top-2 bottom-2 w-0.5 bg-line" />
                {history.map((h, idx) => (
                  <div key={idx} className="relative mb-6 last:mb-0">
                    <div className="absolute -left-3.5 top-1.5 w-3 h-3 rounded-full bg-white border-2 border-brand" />
                    <div className="bg-white rounded-lg border border-line p-4 ml-4">
                      <div className="flex items-center gap-2 mb-1">
                        <Badge variant={STATE_BADGE[h.toState] ?? 'active'} size="sm">
                          {STATE_LABEL[h.toState] ?? h.toState}
                        </Badge>
                        {h.fromState && (
                          <span className="text-xs text-ink-mute">
                            {STATE_LABEL[h.fromState] ?? h.fromState} &rarr;
                          </span>
                        )}
                      </div>
                      {h.notes && <p className="text-sm text-ink-sub mt-1">{h.notes}</p>}
                      <p className="text-xs text-ink-mute mt-2">
                        {h.timestamp ? new Date(h.timestamp).toLocaleString('ko-KR') : ''}
                        {h.username && ` | ${h.username}`}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
