import { useState, useEffect } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import {
  getBidRequest,
  getBidRequestDocuments,
  getBidRequestHistory,
  getOpportunity,
  getClientDocuments,
  uploadClientDocument,
  deleteClientDocument,
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

interface RequiredDoc {
  name: string
  description?: string
  mandatory?: boolean
  format?: string
  pageLimit?: string
  notes?: string
}

interface ClientDocument {
  id: string
  fileName: string
  fileSize: number
  contentType?: string
  documentCategory?: string
  createdAt?: string
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
  const [requiredDocs, setRequiredDocs] = useState<RequiredDoc[]>([])
  const [clientDocs, setClientDocs] = useState<ClientDocument[]>([])
  const [loading, setLoading] = useState(true)
  const [searchParams] = useSearchParams()
  // 기본 탭은 '제출 서류'(uploads). ?tab= 으로 딥링크 시 해당 탭으로 진입
  const tabParam = searchParams.get('tab')
  const initialTab: 'uploads' | 'documents' | 'history' =
    tabParam === 'documents' || tabParam === 'history' ? tabParam : 'uploads'
  const [activeTab, setActiveTab] = useState<'uploads' | 'documents' | 'history'>(initialTab)
  const [activeDocIdx, setActiveDocIdx] = useState(0)
  const [uploadingDoc, setUploadingDoc] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const loadClientDocs = async (bidId: string) => {
    const res = await getClientDocuments(bidId)
    setClientDocs(res.data ?? [])
  }

  const loadAll = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getBidRequest(id)
      const p: BidRequest = data.data ?? data
      setProposal(p)

      // CR-024: noticeId(공고문 ID)로 GET /opportunities/{noticeId} → analysis.requiredDocuments 획득
      // (고객 엔드포인트는 noticeId만 받음. opportunityId(원본 UUID)는 404)
      const calls: Promise<unknown>[] = [
        getBidRequestDocuments(id),
        getBidRequestHistory(id),
        getClientDocuments(id),
      ]
      if (p.noticeId) calls.push(getOpportunity(p.noticeId))
      const settled = await Promise.allSettled(calls)
      const [docRes, histRes, clientRes, oppRes] = settled
      if (docRes.status === 'fulfilled') setDocuments((docRes.value as { data: BidDocSummary[] }).data ?? [])
      if (histRes.status === 'fulfilled') setHistory((histRes.value as { data: StateTransition[] }).data ?? [])
      if (clientRes.status === 'fulfilled') setClientDocs((clientRes.value as { data: ClientDocument[] }).data ?? [])
      if (oppRes && oppRes.status === 'fulfilled') {
        const data = (oppRes.value as { data: { analysis?: { requiredDocuments?: { documents?: RequiredDoc[] } } } }).data
        const docs = data?.analysis?.requiredDocuments?.documents ?? []
        setRequiredDocs(docs)
      }
    } catch {
      setProposal(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadAll() }, [id])

  // 공고 서류명 ↔ 업로드된 ClientDocument 매핑 (documentCategory로)
  const docByCategory = (name: string) =>
    clientDocs.find((c) => c.documentCategory === name)

  const handleUpload = async (docName: string, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !id) return
    setUploadingDoc(docName)
    try {
      // 기존 동일 category 업로드 있으면 먼저 삭제(=변경 효과)
      const existing = docByCategory(docName)
      if (existing) await deleteClientDocument(id, existing.id)
      await uploadClientDocument(id, file, docName)
      await loadClientDocs(id)
    } catch {
      /* ignore — TODO: 인라인 에러 표시 */
    } finally {
      setUploadingDoc(null)
      e.target.value = ''
    }
  }

  const handleRemove = async (docId: string) => {
    if (!id) return
    try {
      await deleteClientDocument(id, docId)
      await loadClientDocs(id)
    } catch { /* ignore */ }
  }

  const mandatoryDocs = requiredDocs.filter((d) => d.mandatory)
  const fulfilledMandatory = mandatoryDocs.filter((d) => docByCategory(d.name)).length
  const canSubmitDocs = mandatoryDocs.length === 0 || fulfilledMandatory === mandatoryDocs.length

  const handleSubmitDocuments = async () => {
    if (!id) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      await transitionBidRequestState(id, 'DOCS_RECEIVED', '고객 서류 제출 완료')
      await loadAll()
    } catch {
      setSubmitError('제출에 실패했습니다. 잠시 후 다시 시도해주세요.')
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

  // 관리자 첨부 반려: 가장 최근 DOCS_RECEIVED → DOCS_PENDING 전이의 사유 (현재 DOCS_PENDING일 때만 노출)
  const rejectionReason = proposal.state === 'DOCS_PENDING'
    ? history
        .filter(h => h.fromState === 'DOCS_RECEIVED' && h.toState === 'DOCS_PENDING')
        .sort((a, b) => (b.timestamp ?? '').localeCompare(a.timestamp ?? ''))[0]?.notes
    : undefined

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center shadow-sm z-10">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/proposals')} className="text-gray-500 hover:text-gray-800">
            <i className="fa-solid fa-arrow-left text-xl" />
          </button>
          <div>
            <h3 className="text-lg font-bold text-gray-800 line-clamp-1">{proposal.displayTitle || proposal.opportunityTitle}</h3>
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
          { key: 'uploads', label: '제출 서류', icon: 'fa-solid fa-cloud-arrow-up', count: requiredDocs.length },
          { key: 'documents', label: '생성 문서', icon: 'fa-solid fa-file-lines', count: documents.length },
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
                요구사항 분석과 문서 생성이 끝나면 여기에 표시됩니다.
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

        {/* Uploads Tab — 공고문 필요서류 + ClientDocument 업로드 (변경/삭제 자유) */}
        {activeTab === 'uploads' && (
          <div className="p-6 max-w-3xl mx-auto space-y-4">
            {/* 관리자 첨부 반려 사유 — 재업로드 안내 */}
            {rejectionReason && (
              <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
                <div className="flex items-start gap-2">
                  <i className="fa-solid fa-triangle-exclamation text-amber-500 mt-0.5" />
                  <div>
                    <p className="text-sm font-semibold text-amber-800">제출 서류가 반려되었습니다</p>
                    <p className="text-xs text-amber-700 mt-1 whitespace-pre-wrap">{rejectionReason}</p>
                    <p className="text-xs text-amber-600 mt-2">아래에서 서류를 다시 업로드한 뒤 ‘문서 제출 완료’를 눌러주세요.</p>
                  </div>
                </div>
              </div>
            )}
            {requiredDocs.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <i className="fa-solid fa-folder-open text-4xl mb-3" />
                <p className="text-sm">이 공고에는 명시된 필요 서류가 없습니다.</p>
                <p className="text-xs mt-1">바로 제출 가능합니다.</p>
              </div>
            ) : (
              <>
                {/* 충족 요약 */}
                <div className="bg-white border border-gray-200 rounded-xl p-4 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">필수 서류 충족 현황</p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {fulfilledMandatory} / {mandatoryDocs.length} 건 업로드 완료
                    </p>
                  </div>
                  <div className="w-32 h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-secondary transition-all"
                      style={{
                        width: mandatoryDocs.length > 0
                          ? `${(fulfilledMandatory / mandatoryDocs.length) * 100}%`
                          : '100%',
                      }}
                    />
                  </div>
                </div>

                {/* 서류 목록 */}
                <div className="space-y-3">
                  {requiredDocs.map((doc) => {
                    const uploaded = docByCategory(doc.name)
                    const isUploading = uploadingDoc === doc.name
                    return (
                      <div
                        key={doc.name}
                        className={`bg-white border rounded-xl p-4 ${
                          uploaded ? 'border-green-200' : 'border-gray-200'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2 flex-wrap">
                              <i className={`fa-solid ${
                                uploaded ? 'fa-circle-check text-green-500' : 'fa-circle-exclamation text-amber-400'
                              }`} />
                              <p className="text-sm font-semibold text-gray-800">{doc.name}</p>
                              {doc.mandatory ? (
                                <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-red-50 text-red-500">필수</span>
                              ) : (
                                <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-gray-100 text-gray-500">선택</span>
                              )}
                            </div>
                            {doc.description && (
                              <p className="text-xs text-gray-500 mt-1 ml-6">{doc.description}</p>
                            )}
                            <div className="flex gap-3 mt-1 ml-6 text-xs text-gray-400">
                              {doc.format && <span><i className="fa-solid fa-file mr-1" />{doc.format}</span>}
                              {doc.pageLimit && <span><i className="fa-solid fa-ruler mr-1" />{doc.pageLimit}</span>}
                            </div>
                            {uploaded && (
                              <div className="flex items-center gap-2 mt-2 ml-6 text-xs text-gray-700">
                                <i className="fa-solid fa-paperclip text-gray-400" />
                                <span className="truncate">{uploaded.fileName}</span>
                                <span className="text-gray-400 flex-shrink-0">
                                  ({(uploaded.fileSize / 1024).toFixed(1)} KB)
                                </span>
                              </div>
                            )}
                          </div>
                          <div className="flex-shrink-0 flex items-center gap-1">
                            {uploaded && (
                              <button
                                onClick={() => handleRemove(uploaded.id)}
                                disabled={isUploading}
                                className="text-xs text-gray-400 hover:text-red-500 px-1.5 py-1"
                                title="삭제"
                              >
                                <i className="fa-solid fa-xmark" />
                              </button>
                            )}
                            <label className={`inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium cursor-pointer transition ${
                              isUploading
                                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                : uploaded
                                  ? 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
                                  : 'bg-secondary text-white hover:opacity-90'
                            }`}>
                              {isUploading ? (
                                <><i className="fa-solid fa-spinner fa-spin mr-1.5" /> 업로드 중</>
                              ) : uploaded ? (
                                <><i className="fa-solid fa-rotate-left mr-1.5" /> 변경</>
                              ) : (
                                <><i className="fa-solid fa-arrow-up-from-bracket mr-1.5" /> 파일 선택</>
                              )}
                              <input
                                type="file"
                                className="hidden"
                                disabled={isUploading}
                                onChange={(e) => handleUpload(doc.name, e)}
                                accept=".pdf,.doc,.docx,.xls,.xlsx,.zip,.jpg,.png"
                              />
                            </label>
                          </div>
                        </div>
                      </div>
                    )
                  })}
                </div>

                {/* 분류 외 추가 첨부(category 미지정) — 있으면 표시 */}
                {clientDocs.filter((c) => !c.documentCategory).length > 0 && (
                  <div className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                    <p className="text-xs font-medium text-gray-500 mb-2">기타 첨부</p>
                    <div className="space-y-1.5">
                      {clientDocs.filter((c) => !c.documentCategory).map((c) => (
                        <div key={c.id} className="flex items-center justify-between text-xs">
                          <span className="text-gray-700 truncate">
                            <i className="fa-solid fa-paperclip text-gray-400 mr-1" />
                            {c.fileName}
                          </span>
                          <button
                            onClick={() => handleRemove(c.id)}
                            className="text-gray-400 hover:text-red-500 px-1.5"
                            title="삭제"
                          >
                            <i className="fa-solid fa-xmark" />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 제출 버튼 — CREATED 또는 DOCS_PENDING에서 노출 */}
                {(proposal.state === 'CREATED' || proposal.state === 'DOCS_PENDING') && (
                  <div className="pt-2">
                    {submitError && (
                      <p className="text-xs text-red-500 mb-2 text-center">{submitError}</p>
                    )}
                    <Button
                      variant="primary"
                      className="w-full"
                      disabled={submitting || !canSubmitDocs}
                      onClick={handleSubmitDocuments}
                    >
                      {submitting ? (
                        <><i className="fa-solid fa-spinner fa-spin mr-2" /> 제출 중...</>
                      ) : (
                        <><i className="fa-solid fa-paper-plane mr-2" /> 서류 제출 완료</>
                      )}
                    </Button>
                    {!canSubmitDocs && (
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
