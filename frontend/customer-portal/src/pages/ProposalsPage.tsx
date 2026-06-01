import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { EmptyState } from '../components/ui/empty-state'
import { Button } from '../components/ui/button'
import { getMyBidRequests, getBidRequestDocuments } from '../api/client'
import { STATE_BADGE, STATE_LABEL } from '../types'
import type { BidRequest } from '../types'
import { cn } from '../lib/utils'

const DOC_TYPE_LABELS: Record<string, string> = {
  COVER_LETTER: 'Cover Letter',
  TECHNICAL_PROPOSAL: 'Technical Proposal',
  PAST_PERFORMANCE: 'Past Performance',
  COMPANY_PROFILE: 'Company Profile',
  COMPLIANCE_MATRIX: 'Compliance Matrix',
  PRICING_SUMMARY: 'Pricing Summary',
  OTHER: 'Other',
}

const PENDING_STATES = ['CREATED', 'DOCS_PENDING', 'DOCS_RECEIVED', 'ANALYZING']
const DRAFTING_STATES = ['GENERATING', 'REVIEW']
const READY_STATES = ['CONFIRMED', 'SUBMITTED']

interface BidDocSummary {
  id: string
  documentType: string
  status: string
  currentVersionNo: number
  createdAt: string
  contentJson?: Record<string, unknown>
  changeSummary?: string
  editedAt?: string
}

function extractTextFromTipTap(contentJson: Record<string, unknown>): string {
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

function StateGuide({ state }: { state: string }) {
  if (PENDING_STATES.includes(state)) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center p-8">
        <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
          <i className="fa-solid fa-magnifying-glass text-2xl text-secondary" />
        </div>
        <h3 className="text-lg font-bold text-gray-800 mb-2">요구사항 분석 중</h3>
        <p className="text-sm text-gray-500 max-w-xs">
          공고를 분석하고 요구사항을 추출하고 있습니다.<br />
          잠시 후 문서 작성이 시작됩니다.
        </p>
        <div className="mt-4 flex gap-1.5">
          {[0, 1, 2].map((i) => (
            <div key={i} className="w-2 h-2 bg-secondary rounded-full animate-bounce" style={{ animationDelay: `${i * 0.2}s` }} />
          ))}
        </div>
      </div>
    )
  }
  if (DRAFTING_STATES.includes(state)) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center p-8">
        <div className="w-16 h-16 bg-purple-50 rounded-full flex items-center justify-center mb-4">
          <i className="fa-solid fa-pen-to-square text-2xl text-purple-500" />
        </div>
        <h3 className="text-lg font-bold text-gray-800 mb-2">문서 작성 / 검토 중</h3>
        <p className="text-sm text-gray-500 max-w-xs">
          제안서 문서를 작성하거나 내부 검토가 진행 중입니다.<br />
          문서가 생성되면 좌측에서 확인하세요.
        </p>
      </div>
    )
  }
  if (READY_STATES.includes(state)) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center p-8">
        <div className="w-16 h-16 bg-emerald-50 rounded-full flex items-center justify-center mb-4">
          <i className="fa-solid fa-paper-plane text-2xl text-emerald-500" />
        </div>
        <h3 className="text-lg font-bold text-gray-800 mb-2">제출 준비 완료</h3>
        <p className="text-sm text-gray-500 max-w-xs">
          문서가 준비되었습니다. 담당자가 검토 후 제출합니다.
        </p>
      </div>
    )
  }
  return (
    <div className="flex items-center justify-center h-full text-gray-400">
      <p>좌측 목록에서 제안서를 선택하세요.</p>
    </div>
  )
}

export default function ProposalsPage() {
  const navigate = useNavigate()
  const [proposals, setProposals] = useState<BidRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState<BidRequest | null>(null)
  const [documents, setDocuments] = useState<BidDocSummary[]>([])
  const [docLoading, setDocLoading] = useState(false)
  const [activeDocIdx, setActiveDocIdx] = useState(0)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const { data } = await getMyBidRequests()
        const content = data.content ?? []
        setProposals(content)
        if (content.length) loadDocuments(content[0])
      } catch {
        setProposals([])
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const loadDocuments = async (proposal: BidRequest) => {
    setSelected(proposal)
    setDocuments([])
    setActiveDocIdx(0)
    setDocLoading(true)
    try {
      const { data } = await getBidRequestDocuments(proposal.id)
      setDocuments(data ?? [])
    } catch {
      setDocuments([])
    } finally {
      setDocLoading(false)
    }
  }

  const stateLabel = (p: BidRequest) => STATE_LABEL[p.state] ?? p.state

  const activeDoc = documents[activeDocIdx]

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-xl font-bold text-gray-800">내 제안서 관리</h2>
          <p className="text-sm text-gray-500 mt-1">작성된 제안서 초안을 확인하고 수정하세요.</p>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner fullPage />
      ) : proposals.length === 0 ? (
        <EmptyState
          icon="fa-solid fa-folder-open"
          title="작성된 제안서가 없습니다"
          description="입찰 검색 화면에서 제안서 작성 버튼을 눌러 시작하세요."
        />
      ) : (
        <div className="flex gap-6 h-[calc(100vh-220px)]">
          {/* Left: List */}
          <div className="w-80 flex-shrink-0 bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden flex flex-col">
            <div className="px-4 py-3 border-b border-gray-200 bg-gray-50">
              <p className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                제안서 목록 ({proposals.length})
              </p>
            </div>
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
              {proposals.map((p) => (
                <button
                  key={p.id}
                  onClick={() => loadDocuments(p)}
                  className={cn(
                    'w-full text-left px-4 py-4 hover:bg-blue-50/50 transition',
                    selected?.id === p.id && 'bg-blue-50 border-l-2 border-secondary'
                  )}
                >
                  <div className="flex items-start justify-between gap-2 mb-1">
                    <span className="text-xs font-bold text-primary font-mono truncate">
                      {p.solicitationNumber ?? p.opportunityId}
                    </span>
                    <Badge variant={STATE_BADGE[p.state] ?? 'active'} size="sm">
                      {stateLabel(p)}
                    </Badge>
                  </div>
                  <p className="text-sm font-medium text-gray-800 line-clamp-2 leading-snug">
                    {p.opportunityTitle}
                  </p>
                  <div className="flex items-center justify-between mt-1">
                    <p className="text-xs text-gray-400">{p.createdAt?.slice(0, 10)}</p>
                    <button
                      onClick={(e) => { e.stopPropagation(); navigate(`/proposals/${p.id}`) }}
                      className="text-xs text-secondary hover:underline"
                    >
                      상세 &rarr;
                    </button>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Right: Document Viewer */}
          <div className="flex-1 bg-gray-100 rounded-xl border border-gray-200 overflow-hidden flex flex-col">
            {!selected ? (
              <div className="flex-1 flex items-center justify-center text-gray-400">
                <p>좌측 목록에서 제안서를 선택하세요.</p>
              </div>
            ) : docLoading ? (
              <LoadingSpinner fullPage />
            ) : documents.length === 0 ? (
              <StateGuide state={selected.state} />
            ) : (
              <>
                {/* Doc header */}
                <div className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between flex-shrink-0">
                  <div className="flex items-center gap-3">
                    <div>
                      <p className="text-sm font-bold text-gray-800">
                        {selected.solicitationNumber ?? selected.opportunityId}
                      </p>
                      <p className="text-xs text-gray-500">
                        {DOC_TYPE_LABELS[activeDoc?.documentType] ?? activeDoc?.documentType}
                        {activeDoc && ` · v${activeDoc.currentVersionNo}`}
                        {activeDoc?.status && (
                          <span className="ml-2 px-1.5 py-0.5 rounded text-xs bg-gray-100 text-gray-500">
                            {activeDoc.status}
                          </span>
                        )}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => window.print()}>
                      <i className="fa-solid fa-print mr-1" /> 인쇄
                    </Button>
                  </div>
                </div>

                {/* Doc tabs (if multiple) */}
                {documents.length > 1 && (
                  <div className="bg-white border-b border-gray-200 px-6 flex gap-1 flex-shrink-0 overflow-x-auto">
                    {documents.map((doc, idx) => (
                      <button
                        key={doc.id}
                        onClick={() => setActiveDocIdx(idx)}
                        className={cn(
                          'px-3 py-2 text-xs font-medium whitespace-nowrap border-b-2 transition',
                          idx === activeDocIdx
                            ? 'border-secondary text-secondary'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                        )}
                      >
                        {DOC_TYPE_LABELS[doc.documentType] ?? doc.documentType}
                      </button>
                    ))}
                  </div>
                )}

                {/* Doc content */}
                <div className="flex-1 overflow-y-auto p-8">
                  {activeDoc?.contentJson ? (
                    <div className="proposal-doc max-w-3xl mx-auto bg-white rounded-xl shadow-sm p-8">
                      <div className="text-right text-sm text-gray-500 mb-6">
                        Date: {activeDoc.editedAt?.slice(0, 10) ?? selected.createdAt?.slice(0, 10)}<br />
                        Reference: <span>{selected.solicitationNumber ?? selected.opportunityId}</span>
                      </div>
                      <h1 className="text-2xl font-bold text-center uppercase tracking-wider border-b-2 border-primary pb-4 mb-8">
                        {DOC_TYPE_LABELS[activeDoc.documentType] ?? activeDoc.documentType}
                      </h1>
                      <pre className="whitespace-pre-wrap font-serif text-sm text-gray-800 leading-relaxed">
                        {extractTextFromTipTap(activeDoc.contentJson)}
                      </pre>
                      {activeDoc.changeSummary && (
                        <p className="mt-8 pt-4 border-t border-gray-100 text-xs text-gray-400">
                          <i className="fa-solid fa-clock-rotate-left mr-1" />
                          {activeDoc.changeSummary}
                        </p>
                      )}
                    </div>
                  ) : (
                    <div className="flex items-center justify-center h-full text-gray-400 text-sm">
                      문서 내용이 없습니다.
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
