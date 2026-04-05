import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import {
  getBidRequest,
  getBidRequestDocuments,
  getBidRequestHistory,
  getClientDocuments,
  uploadClientDocument,
  deleteClientDocument,
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

interface ClientDoc {
  id: string
  originalFileName: string
  fileSize: number
  mimeType: string
  uploadedAt?: string
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
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [proposal, setProposal] = useState<BidRequest | null>(null)
  const [documents, setDocuments] = useState<BidDocSummary[]>([])
  const [history, setHistory] = useState<StateTransition[]>([])
  const [clientDocs, setClientDocs] = useState<ClientDoc[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<'documents' | 'uploads' | 'history'>('documents')
  const [activeDocIdx, setActiveDocIdx] = useState(0)
  const [uploading, setUploading] = useState(false)

  const loadAll = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getBidRequest(id)
      setProposal(data.data ?? data)

      const [docRes, histRes, clientRes] = await Promise.allSettled([
        getBidRequestDocuments(id),
        getBidRequestHistory(id),
        getClientDocuments(id),
      ])
      if (docRes.status === 'fulfilled') setDocuments(docRes.value.data ?? [])
      if (histRes.status === 'fulfilled') setHistory(histRes.value.data ?? [])
      if (clientRes.status === 'fulfilled') setClientDocs(clientRes.value.data?.data ?? clientRes.value.data ?? [])
    } catch {
      setProposal(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadAll() }, [id])

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !id) return
    setUploading(true)
    try {
      await uploadClientDocument(id, file)
      const res = await getClientDocuments(id)
      setClientDocs(res.data?.data ?? res.data ?? [])
    } catch { /* ignore */ }
    finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const handleDelete = async (docId: string) => {
    if (!id) return
    try {
      await deleteClientDocument(docId)
      setClientDocs(prev => prev.filter(d => d.id !== docId))
    } catch { /* ignore */ }
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
  const canUpload = ['CREATED', 'DOCS_PENDING'].includes(proposal.state)

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
          { key: 'uploads', label: '제출 서류', icon: 'fa-solid fa-cloud-arrow-up', count: clientDocs.length },
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
                AI가 요구사항을 분석하고 문서를 생성하면 여기에 표시됩니다.
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

        {/* Uploads Tab */}
        {activeTab === 'uploads' && (
          <div className="p-6 max-w-3xl mx-auto space-y-4">
            {canUpload && (
              <div className="bg-blue-50 border border-blue-200 rounded-xl p-6 text-center">
                <i className="fa-solid fa-cloud-arrow-up text-3xl text-secondary mb-3" />
                <p className="text-sm text-gray-700 mb-3">입찰에 필요한 서류를 업로드하세요.</p>
                <input
                  ref={fileInputRef}
                  type="file"
                  onChange={handleUpload}
                  className="hidden"
                  accept=".pdf,.doc,.docx,.xls,.xlsx,.zip,.jpg,.png"
                />
                <Button
                  variant="accent"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading}
                >
                  {uploading ? (
                    <><i className="fa-solid fa-spinner fa-spin mr-2" /> 업로드 중...</>
                  ) : (
                    <><i className="fa-solid fa-plus mr-2" /> 파일 선택</>
                  )}
                </Button>
              </div>
            )}

            {clientDocs.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <i className="fa-solid fa-folder-open text-4xl mb-3" />
                <p className="text-sm">업로드한 서류가 없습니다.</p>
              </div>
            ) : (
              <div className="bg-white rounded-xl border border-gray-200 divide-y divide-gray-100">
                {clientDocs.map(doc => (
                  <div key={doc.id} className="flex items-center justify-between px-5 py-4">
                    <div className="flex items-center gap-3 min-w-0">
                      <i className="fa-solid fa-paperclip text-gray-300 text-lg" />
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-800 truncate">{doc.originalFileName}</p>
                        <p className="text-xs text-gray-400">
                          {(doc.fileSize / 1024).toFixed(1)} KB
                          {doc.uploadedAt && ` | ${new Date(doc.uploadedAt).toLocaleDateString('ko-KR')}`}
                        </p>
                      </div>
                    </div>
                    {canUpload && (
                      <button
                        onClick={() => handleDelete(doc.id)}
                        className="text-gray-400 hover:text-red-500 transition ml-2"
                        title="삭제"
                      >
                        <i className="fa-solid fa-trash-can" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
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
