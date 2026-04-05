import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAdminOpportunityDetail,
  triggerOpportunityAnalysis,
  approveOpportunity,
  hideOpportunity,
  uploadOpportunityAttachment,
} from '../api/client'

interface OpportunityDetail {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  type?: string
  organizationName?: string
  postedDate?: string
  responseDeadline?: string
  visibility: 'HIDDEN' | 'VISIBLE'
  uiLink?: string
  attachmentCount: number
  analysisStatus?: string | null
  analyzedAt?: string | null
  summaryJson?: Record<string, unknown> | null
  documentFormatsJson?: Record<string, unknown> | null
  requiredDocumentsJson?: Record<string, unknown> | null
}

const ANALYSIS_LABELS: Record<string, string> = {
  PENDING: '대기',
  ANALYZING: '분석 중',
  COMPLETED: '완료',
  FAILED: '실패',
}

const ANALYSIS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  ANALYZING: 'bg-purple-100 text-purple-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}

export default function OpportunityAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [opp, setOpp] = useState<OpportunityDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)

  const fetchData = async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getAdminOpportunityDetail(id)
      setOpp(data)
    } catch (err) {
      console.error('공고 상세 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [id])

  const handleAnalyze = async () => {
    if (!id) return
    setActionLoading(true)
    try {
      await triggerOpportunityAnalysis(id)
      fetchData()
    } catch (err) {
      console.error('분석 트리거 실패:', err)
    } finally {
      setActionLoading(false)
    }
  }

  const handleToggleVisibility = async () => {
    if (!id || !opp) return
    setActionLoading(true)
    try {
      if (opp.visibility === 'HIDDEN') await approveOpportunity(id)
      else await hideOpportunity(id)
      fetchData()
    } catch (err) {
      console.error('노출 상태 변경 실패:', err)
    } finally {
      setActionLoading(false)
    }
  }

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!id || !e.target.files?.[0]) return
    setActionLoading(true)
    try {
      await uploadOpportunityAttachment(id, e.target.files[0])
      fetchData()
    } catch (err) {
      console.error('파일 업로드 실패:', err)
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

  if (!opp) {
    return <div className="p-6 text-center text-gray-400">공고를 찾을 수 없습니다.</div>
  }

  return (
    <div className="p-6 space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <button onClick={() => navigate('/opportunities')} className="text-sm text-gray-500 hover:text-gray-700 mb-2">
            <i className="fa-solid fa-arrow-left mr-1" />공고 관리
          </button>
          <h1 className="text-xl font-bold text-gray-900 line-clamp-2">{opp.title}</h1>
          <div className="flex items-center gap-3 mt-2">
            <span className="text-xs text-gray-500 font-mono">{opp.solicitationNumber || opp.noticeId}</span>
            <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
              opp.visibility === 'VISIBLE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
            }`}>
              {opp.visibility === 'VISIBLE' ? '노출' : '비노출'}
            </span>
            {opp.analysisStatus && (
              <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${ANALYSIS_COLORS[opp.analysisStatus] || ''}`}>
                분석: {ANALYSIS_LABELS[opp.analysisStatus] || opp.analysisStatus}
              </span>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleAnalyze}
            disabled={actionLoading || opp.analysisStatus === 'ANALYZING'}
            className="px-4 py-2 text-sm rounded-lg bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-40"
          >
            <i className="fa-solid fa-brain mr-1.5" />
            {opp.analysisStatus === 'ANALYZING' ? '분석 중...' : '사전 분석'}
          </button>
          <button
            onClick={handleToggleVisibility}
            disabled={actionLoading}
            className={`px-4 py-2 text-sm rounded-lg ${
              opp.visibility === 'HIDDEN'
                ? 'bg-green-600 text-white hover:bg-green-700'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            } disabled:opacity-40`}
          >
            {opp.visibility === 'HIDDEN' ? '노출 승인' : '숨김 처리'}
          </button>
        </div>
      </div>

      {/* 공고 기본 정보 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
          <h3 className="text-sm font-semibold text-gray-700">공고 정보</h3>
          <div className="space-y-2 text-sm">
            <div><span className="text-gray-500 w-20 inline-block">기관</span>{opp.organizationName || '-'}</div>
            <div><span className="text-gray-500 w-20 inline-block">유형</span>{opp.type || '-'}</div>
            <div><span className="text-gray-500 w-20 inline-block">게시일</span>{opp.postedDate ? new Date(opp.postedDate).toLocaleDateString('ko') : '-'}</div>
            <div><span className="text-gray-500 w-20 inline-block">마감일</span>{opp.responseDeadline ? new Date(opp.responseDeadline).toLocaleDateString('ko') : '-'}</div>
            {opp.uiLink && (
              <div>
                <a href={opp.uiLink} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline text-xs">
                  <i className="fa-solid fa-external-link mr-1" />SAM.gov 원본
                </a>
              </div>
            )}
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-700">첨부파일</h3>
            <label className="px-3 py-1.5 text-xs rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 cursor-pointer">
              <i className="fa-solid fa-upload mr-1" />수동 업로드
              <input type="file" className="hidden" onChange={handleFileUpload} />
            </label>
          </div>
          <div className="text-sm text-gray-600">
            {opp.attachmentCount > 0 ? (
              <span>{opp.attachmentCount}개 첨부파일</span>
            ) : (
              <span className="text-gray-400">첨부파일 없음 — 수동으로 업로드하세요</span>
            )}
          </div>
        </div>
      </div>

      {/* 사전 분석 결과 */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
        <h3 className="text-sm font-semibold text-gray-700">사전 분석 결과</h3>

        {!opp.analysisStatus && (
          <div className="text-sm text-gray-400 py-8 text-center">
            아직 분석이 실행되지 않았습니다. "사전 분석" 버튼을 눌러주세요.
          </div>
        )}

        {opp.analysisStatus === 'ANALYZING' && (
          <div className="flex items-center gap-3 py-8 justify-center text-purple-600">
            <div className="w-5 h-5 border-2 border-purple-200 border-t-purple-600 rounded-full animate-spin" />
            <span className="text-sm">LLM이 공고를 분석하고 있습니다...</span>
          </div>
        )}

        {opp.analysisStatus === 'FAILED' && (
          <div className="bg-red-50 rounded-lg p-4 text-sm text-red-700">
            분석에 실패했습니다. 다시 시도해주세요.
          </div>
        )}

        {opp.analysisStatus === 'COMPLETED' && (
          <div className="space-y-4">
            {opp.analyzedAt && (
              <div className="text-xs text-gray-400">분석 완료: {new Date(opp.analyzedAt).toLocaleString('ko')}</div>
            )}

            {opp.summaryJson && (
              <div>
                <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">공고 요약</h4>
                <pre className="bg-gray-50 rounded-lg p-4 text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap max-h-60">
                  {JSON.stringify(opp.summaryJson, null, 2)}
                </pre>
              </div>
            )}

            {opp.requiredDocumentsJson && (
              <div>
                <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">필요 서류 목록</h4>
                <pre className="bg-gray-50 rounded-lg p-4 text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap max-h-60">
                  {JSON.stringify(opp.requiredDocumentsJson, null, 2)}
                </pre>
              </div>
            )}

            {opp.documentFormatsJson && (
              <div>
                <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">문서 양식</h4>
                <pre className="bg-gray-50 rounded-lg p-4 text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap max-h-60">
                  {JSON.stringify(opp.documentFormatsJson, null, 2)}
                </pre>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
