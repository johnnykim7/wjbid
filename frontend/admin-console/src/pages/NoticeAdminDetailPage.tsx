import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAdminNoticeDetail,
  publishNotice,
  hideNotice,
  regenerateNotice,
} from '../api/client'

interface AnalysisResult {
  analysisStatus?: string
  analyzedAt?: string
  summary?: Record<string, unknown>
  requiredDocuments?: Record<string, unknown>
  documentFormats?: Record<string, unknown>
}

interface NoticeDetail {
  id: string
  opportunityId: string
  originTitle: string
  solicitationNumber?: string
  organizationName?: string
  responseDeadline?: string
  koreanTitle?: string
  generationStatus: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  visibility: 'HIDDEN' | 'VISIBLE'
  analyzedAt?: string
  errorMessage?: string
  analysis?: AnalysisResult
}

const GEN_LABELS: Record<string, string> = {
  PENDING: '대기', ANALYZING: '한글화 중', COMPLETED: '완료', FAILED: '실패',
}

export default function NoticeAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [notice, setNotice] = useState<NoticeDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)

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

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
      </div>
    )
  }

  if (!notice) {
    return <div className="p-6 text-center text-gray-400">공고문을 찾을 수 없습니다.</div>
  }

  const a = notice.analysis

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-start justify-between">
        <div>
          <button onClick={() => navigate('/notices')} className="text-sm text-gray-500 hover:text-gray-700 mb-2">
            <i className="fa-solid fa-arrow-left mr-1" />공고문 관리
          </button>
          <h1 className="text-xl font-bold text-gray-900 line-clamp-2">{notice.koreanTitle || notice.originTitle}</h1>
          <div className="text-sm text-gray-400 mt-1 line-clamp-1">{notice.originTitle}</div>
          <div className="flex items-center gap-3 mt-2">
            <span className="text-xs text-gray-500 font-mono">{notice.solicitationNumber || '-'}</span>
            <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
              한글화: {GEN_LABELS[notice.generationStatus] || notice.generationStatus}
            </span>
            <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
              notice.visibility === 'VISIBLE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
            }`}>
              {notice.visibility === 'VISIBLE' ? '노출' : '비노출'}
            </span>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => run(regenerateNotice)}
            disabled={actionLoading || notice.generationStatus === 'ANALYZING'}
            className="px-4 py-2 text-sm rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-100 disabled:opacity-40"
          >
            <i className="fa-solid fa-rotate mr-1.5" />재생성
          </button>
          {notice.visibility === 'HIDDEN' ? (
            <button
              onClick={() => run(publishNotice)}
              disabled={actionLoading || notice.generationStatus !== 'COMPLETED'}
              className="px-4 py-2 text-sm rounded-lg bg-green-600 text-white hover:bg-green-700 disabled:opacity-40"
              title={notice.generationStatus !== 'COMPLETED' ? '한글화 완료 후 노출 가능' : ''}
            >
              노출 승인
            </button>
          ) : (
            <button
              onClick={() => run(hideNotice)}
              disabled={actionLoading}
              className="px-4 py-2 text-sm rounded-lg bg-gray-200 text-gray-700 hover:bg-gray-300 disabled:opacity-40"
            >
              숨김 처리
            </button>
          )}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
        <h3 className="text-sm font-semibold text-gray-700">한글화 결과</h3>

        {notice.generationStatus === 'PENDING' && (
          <div className="text-sm text-gray-400 py-8 text-center">한글화 대기 중입니다.</div>
        )}
        {notice.generationStatus === 'ANALYZING' && (
          <div className="flex items-center gap-3 py-8 justify-center text-purple-600">
            <div className="w-5 h-5 border-2 border-purple-200 border-t-purple-600 rounded-full animate-spin" />
            <span className="text-sm">LLM이 공고를 한글화/요약하고 있습니다...</span>
          </div>
        )}
        {notice.generationStatus === 'FAILED' && (
          <div className="bg-red-50 rounded-lg p-4 text-sm text-red-700">
            한글화에 실패했습니다. {notice.errorMessage || '재생성을 시도해주세요.'}
          </div>
        )}
        {notice.generationStatus === 'COMPLETED' && (
          <div className="space-y-4">
            {notice.analyzedAt && (
              <div className="text-xs text-gray-400">완료: {new Date(notice.analyzedAt).toLocaleString('ko')}</div>
            )}
            {a?.summary && (
              <div>
                <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">공고 요약</h4>
                <pre className="bg-gray-50 rounded-lg p-4 text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap max-h-60">
                  {JSON.stringify(a.summary, null, 2)}
                </pre>
              </div>
            )}
            {a?.requiredDocuments && (
              <div>
                <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">필요 서류</h4>
                <pre className="bg-gray-50 rounded-lg p-4 text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap max-h-60">
                  {JSON.stringify(a.requiredDocuments, null, 2)}
                </pre>
              </div>
            )}
            {a?.documentFormats && (
              <div>
                <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">문서 양식</h4>
                <pre className="bg-gray-50 rounded-lg p-4 text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap max-h-60">
                  {JSON.stringify(a.documentFormats, null, 2)}
                </pre>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
