import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import {
  getBidRequest,
  getBidRequestDocuments,
  getBidRequestHistory,
  getRequiredDocumentSlots,
  uploadToSlot,
  unmapSlot,
  transitionBidRequestState,
} from '../api/client'
import { STATE_LABEL, STATE_BADGE } from '../types'
import type { BidRequest, StateTransition } from '../types'

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

interface MappedDocument {
  id: string
  fileName: string
  fileSize: number
  uploadedAt?: string
}

interface RequiredSlot {
  requirementItemId: string
  title: string
  description?: string
  isBlocker: boolean
  category: string
  status: 'FULFILLED' | 'PENDING' | 'MISSING'
  fulfillmentType?: string | null
  mappedClientDocument?: MappedDocument | null
}

interface SlotSummary {
  totalBlocker: number
  fulfilledBlocker: number
  canTransitionToDocsReceived: boolean
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
  const [slots, setSlots] = useState<RequiredSlot[]>([])
  const [slotSummary, setSlotSummary] = useState<SlotSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<'documents' | 'uploads' | 'history'>('documents')
  const [activeDocIdx, setActiveDocIdx] = useState(0)
  const [uploadingSlot, setUploadingSlot] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const loadSlots = async (bidId: string) => {
    const res = await getRequiredDocumentSlots(bidId)
    setSlots(res.data?.data ?? [])
    setSlotSummary(res.data?.summary ?? null)
  }

  const loadAll = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getBidRequest(id)
      setProposal(data.data ?? data)

      const [docRes, histRes, slotRes] = await Promise.allSettled([
        getBidRequestDocuments(id),
        getBidRequestHistory(id),
        getRequiredDocumentSlots(id),
      ])
      if (docRes.status === 'fulfilled') setDocuments(docRes.value.data ?? [])
      if (histRes.status === 'fulfilled') setHistory(histRes.value.data ?? [])
      if (slotRes.status === 'fulfilled') {
        setSlots(slotRes.value.data?.data ?? [])
        setSlotSummary(slotRes.value.data?.summary ?? null)
      }
    } catch {
      setProposal(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadAll() }, [id])

  const handleSlotUpload = async (requirementItemId: string, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !id) return
    setUploadingSlot(requirementItemId)
    try {
      await uploadToSlot(id, requirementItemId, file)
      await loadSlots(id)
    } catch { /* ignore */ }
    finally {
      setUploadingSlot(null)
      e.target.value = ''
    }
  }

  const handleSlotUnmap = async (requirementItemId: string) => {
    if (!id) return
    try {
      await unmapSlot(id, requirementItemId)
      await loadSlots(id)
    } catch { /* ignore */ }
  }

  const handleSubmitDocuments = async () => {
    if (!id) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      await transitionBidRequestState(id, 'DOCS_RECEIVED', '고객 서류 제출 완료')
      await loadAll()
    } catch (err: unknown) {
      const e = err as { response?: { status?: number; data?: { error?: string } } }
      setSubmitError(
        e.response?.data?.error === 'REQUIREMENT_SLOTS_NOT_FULFILLED'
          ? '필수 서류가 모두 업로드되지 않았습니다.'
          : '제출에 실패했습니다. 잠시 후 다시 시도해주세요.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <LoadingSpinner fullPage />
  if (!proposal) return (
    <div className="flex flex-col items-center justify-center h-full text-gray-500 gap-4 p-8">
      <i className="fa-solid fa-circle-exclamation text-4xl text-gray-300" />
      <p>제안서를 찾을 수 없습니다.</p>
      <button onClick={() => navigate('/proposals')} className="text-secondary underline text-sm">목록으로</button>
    </div>
  )

  const activeDoc = documents[activeDocIdx]

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center shadow-sm z-10">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/proposals')} className="text-gray-500 hover:text-gray-800">
            <i className="fa-solid fa-arrow-left text-xl" />
          </button>
          <div>
            <h3 className="text-lg font-bold text-gray-800 line-clamp-1">{proposal.opportunityTitle}</h3>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="text-xs text-gray-500">{proposal.solicitationNumber ?? proposal.opportunityId}</span>
              <Badge variant={STATE_BADGE[proposal.state] ?? 'active'} size="sm">
                {STATE_LABEL[proposal.state] ?? proposal.state}
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white border-b border-gray-200 px-6 flex gap-1">
        {([
          { key: 'documents', label: '생성 문서', icon: 'fa-solid fa-file-lines', count: documents.length },
          { key: 'uploads', label: '제출 서류', icon: 'fa-solid fa-cloud-arrow-up', count: slots.length },
          { key: 'history', label: '진행 이력', icon: 'fa-solid fa-clock-rotate-left', count: history.length },
        ] as const).map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-3 text-sm font-medium border-b-2 transition ${
              activeTab === tab.key
                ? 'border-secondary text-secondary'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <i className={`${tab.icon} mr-1.5`} />
            {tab.label}
            {tab.count > 0 && (
              <span className="ml-1.5 px-1.5 py-0.5 rounded-full text-xs bg-gray-100 text-gray-500">{tab.count}</span>
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
              <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
                <i className="fa-solid fa-file-circle-plus text-2xl text-secondary" />
              </div>
              <h3 className="text-lg font-bold text-gray-800 mb-2">아직 생성된 문서가 없습니다</h3>
              <p className="text-sm text-gray-500 max-w-xs">
                요구사항을 분석하고 문서를 작성하면 여기에 표시됩니다.
              </p>
            </div>
          ) : (
            <div className="flex h-full">
              {/* Doc list sidebar */}
              <div className="w-64 border-r border-gray-200 bg-gray-50 flex-shrink-0 overflow-y-auto">
                {documents.map((doc, idx) => (
                  <button
                    key={doc.id}
                    onClick={() => setActiveDocIdx(idx)}
                    className={`w-full text-left px-4 py-4 border-b border-gray-100 hover:bg-white transition ${
                      idx === activeDocIdx ? 'bg-white border-l-2 border-l-secondary' : ''
                    }`}
                  >
                    <p className="text-sm font-medium text-gray-800">
                      {DOC_TYPE_LABELS[doc.documentType] ?? doc.documentType}
                    </p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      v{doc.currentVersionNo} | {doc.status}
                    </p>
                  </button>
                ))}
              </div>
              {/* Doc viewer */}
              <div className="flex-1 p-8 overflow-y-auto">
                {activeDoc?.contentJson ? (
                  <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-sm border border-gray-200 p-8">
                    <h1 className="text-xl font-bold text-center uppercase tracking-wider border-b-2 border-gray-800 pb-4 mb-6">
                      {DOC_TYPE_LABELS[activeDoc.documentType] ?? activeDoc.documentType}
                    </h1>
                    <pre className="whitespace-pre-wrap font-serif text-sm text-gray-800 leading-relaxed">
                      {extractText(activeDoc.contentJson)}
                    </pre>
                    {activeDoc.changeSummary && (
                      <p className="mt-6 pt-4 border-t border-gray-100 text-xs text-gray-400">
                        <i className="fa-solid fa-clock-rotate-left mr-1" /> {activeDoc.changeSummary}
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="flex items-center justify-center h-full text-sm text-gray-400">
                    문서 내용이 없습니다.
                  </div>
                )}
              </div>
            </div>
          )
        )}

        {/* Uploads Tab — CR-010 요구사항 슬롯 */}
        {activeTab === 'uploads' && (
          <div className="p-6 max-w-3xl mx-auto space-y-4">
            {slots.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <i className="fa-solid fa-folder-open text-4xl mb-3" />
                <p className="text-sm">이 공고에는 등록된 필수 서류 요구사항이 없습니다.</p>
              </div>
            ) : (
              <>
                {/* 충족 요약 */}
                {slotSummary && (
                  <div className="bg-white border border-gray-200 rounded-xl p-4 flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-800">필수 서류 충족 현황</p>
                      <p className="text-xs text-gray-500 mt-0.5">
                        {slotSummary.fulfilledBlocker} / {slotSummary.totalBlocker} 건 업로드 완료
                      </p>
                    </div>
                    <div className="w-32 h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-secondary transition-all"
                        style={{
                          width: slotSummary.totalBlocker > 0
                            ? `${(slotSummary.fulfilledBlocker / slotSummary.totalBlocker) * 100}%`
                            : '100%',
                        }}
                      />
                    </div>
                  </div>
                )}

                {/* 슬롯 목록 */}
                <div className="space-y-3">
                  {slots.map(slot => {
                    const fulfilled = slot.status === 'FULFILLED'
                    const isUploading = uploadingSlot === slot.requirementItemId
                    return (
                      <div
                        key={slot.requirementItemId}
                        className={`bg-white border rounded-xl p-4 ${
                          fulfilled ? 'border-green-200' : 'border-gray-200'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <i className={`fa-solid ${
                                fulfilled ? 'fa-circle-check text-green-500' : 'fa-circle-exclamation text-amber-400'
                              }`} />
                              <p className="text-sm font-semibold text-gray-800">{slot.title}</p>
                              {slot.isBlocker && (
                                <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-red-50 text-red-500">필수</span>
                              )}
                            </div>
                            {slot.description && (
                              <p className="text-xs text-gray-500 mt-1 ml-6">{slot.description}</p>
                            )}
                            {fulfilled && slot.mappedClientDocument && (
                              <div className="flex items-center gap-2 mt-2 ml-6 text-xs text-gray-600">
                                <i className="fa-solid fa-paperclip text-gray-300" />
                                <span className="truncate">{slot.mappedClientDocument.fileName}</span>
                                <span className="text-gray-400">
                                  ({(slot.mappedClientDocument.fileSize / 1024).toFixed(1)} KB)
                                </span>
                              </div>
                            )}
                          </div>
                          <div className="flex-shrink-0">
                            {fulfilled ? (
                              <button
                                onClick={() => handleSlotUnmap(slot.requirementItemId)}
                                className="text-xs text-gray-400 hover:text-red-500 transition"
                                title="다시 업로드하려면 해제"
                              >
                                <i className="fa-solid fa-rotate-left mr-1" /> 변경
                              </button>
                            ) : (
                              <label className={`inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium cursor-pointer transition ${
                                isUploading
                                  ? 'bg-gray-100 text-gray-400'
                                  : 'bg-secondary text-white hover:opacity-90'
                              }`}>
                                {isUploading ? (
                                  <><i className="fa-solid fa-spinner fa-spin mr-1.5" /> 업로드 중</>
                                ) : (
                                  <><i className="fa-solid fa-arrow-up-from-bracket mr-1.5" /> 업로드</>
                                )}
                                <input
                                  type="file"
                                  className="hidden"
                                  disabled={isUploading}
                                  onChange={(e) => handleSlotUpload(slot.requirementItemId, e)}
                                  accept=".pdf,.doc,.docx,.xls,.xlsx,.zip,.jpg,.png"
                                />
                              </label>
                            )}
                          </div>
                        </div>
                      </div>
                    )
                  })}
                </div>

                {/* 문서 제출 완료 (DOCS_PENDING에서만, 전부 충족 시 활성) */}
                {proposal.state === 'DOCS_PENDING' && (
                  <div className="pt-2">
                    {submitError && (
                      <p className="text-xs text-red-500 mb-2 text-center">{submitError}</p>
                    )}
                    <Button
                      variant="accent"
                      className="w-full"
                      disabled={submitting || !(slotSummary?.canTransitionToDocsReceived ?? false)}
                      onClick={handleSubmitDocuments}
                    >
                      {submitting ? (
                        <><i className="fa-solid fa-spinner fa-spin mr-2" /> 제출 중...</>
                      ) : (
                        <><i className="fa-solid fa-paper-plane mr-2" /> 문서 제출 완료</>
                      )}
                    </Button>
                    {!(slotSummary?.canTransitionToDocsReceived ?? false) && (
                      <p className="text-xs text-gray-400 mt-2 text-center">
                        모든 필수 서류를 업로드하면 제출할 수 있습니다.
                      </p>
                    )}
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* History Tab */}
        {activeTab === 'history' && (
          <div className="p-6 max-w-3xl mx-auto">
            {history.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <i className="fa-solid fa-clock-rotate-left text-4xl mb-3" />
                <p className="text-sm">진행 이력이 없습니다.</p>
              </div>
            ) : (
              <div className="relative pl-6">
                <div className="absolute left-2.5 top-2 bottom-2 w-0.5 bg-gray-200" />
                {history.map((h, idx) => (
                  <div key={idx} className="relative mb-6 last:mb-0">
                    <div className="absolute -left-3.5 top-1.5 w-3 h-3 rounded-full bg-white border-2 border-secondary" />
                    <div className="bg-white rounded-lg border border-gray-200 p-4 ml-4">
                      <div className="flex items-center gap-2 mb-1">
                        <Badge variant={STATE_BADGE[h.toState] ?? 'active'} size="sm">
                          {STATE_LABEL[h.toState] ?? h.toState}
                        </Badge>
                        {h.fromState && (
                          <span className="text-xs text-gray-400">
                            {STATE_LABEL[h.fromState] ?? h.fromState} &rarr;
                          </span>
                        )}
                      </div>
                      {h.notes && <p className="text-sm text-gray-600 mt-1">{h.notes}</p>}
                      <p className="text-xs text-gray-400 mt-2">
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
