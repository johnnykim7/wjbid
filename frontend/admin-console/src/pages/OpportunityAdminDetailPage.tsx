import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAdminOpportunityDetail,
  createNotice,
  uploadOpportunityAttachment,
  getOpportunityAttachments,
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
  uiLink?: string
  attachmentCount: number
  manualFetchRequiredCount: number
  noticeCount: number
}

interface Attachment {
  id: string
  fileName?: string
  contentType?: string
  sourceUrl?: string
  downloadStatus: 'SUCCESS' | 'FAILED' | 'LINK_ONLY' | 'MANUAL_FETCH_REQUIRED'
  manualFetchRequired: boolean
  downloadedAt?: string
}

export default function OpportunityAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [opp, setOpp] = useState<OpportunityDetail | null>(null)
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)

  const fetchData = async () => {
    if (!id) return
    setLoading(true)
    try {
      const [detailRes, attachRes] = await Promise.all([
        getAdminOpportunityDetail(id),
        getOpportunityAttachments(id),
      ])
      setOpp(detailRes.data)
      setAttachments(attachRes.data || [])
    } catch (err) {
      console.error('원본 공고 상세 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [id])

  const handleCreateNotice = async () => {
    if (!id) return
    setActionLoading(true)
    try {
      await createNotice(id)
      navigate('/notices')
    } catch (err) {
      console.error('공고문 생성 실패:', err)
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
    return <div className="p-6 text-center text-gray-400">원본 공고를 찾을 수 없습니다.</div>
  }

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-start justify-between">
        <div>
          <button onClick={() => navigate('/opportunities')} className="text-sm text-gray-500 hover:text-gray-700 mb-2">
            <i className="fa-solid fa-arrow-left mr-1" />원본 공고
          </button>
          <h1 className="text-xl font-bold text-gray-900 line-clamp-2">{opp.title}</h1>
          <div className="flex items-center gap-3 mt-2">
            <span className="text-xs text-gray-500 font-mono">{opp.solicitationNumber || opp.noticeId}</span>
            <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
              opp.noticeCount > 0 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
            }`}>
              {opp.noticeCount > 0 ? `공고문 ${opp.noticeCount}개` : '공고문 미생성'}
            </span>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleCreateNotice}
            disabled={actionLoading}
            className="px-4 py-2 text-sm rounded-lg bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-40"
          >
            <i className="fa-solid fa-wand-magic-sparkles mr-1.5" />공고문 만들기
          </button>
          {opp.noticeCount > 0 && (
            <button
              onClick={() => navigate('/notices')}
              className="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
            >
              공고문 리스트 →
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
          <h3 className="text-sm font-semibold text-gray-700">공고 정보 (원문)</h3>
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
              <input type="file" className="hidden" onChange={handleFileUpload} disabled={actionLoading} />
            </label>
          </div>
          {opp.manualFetchRequiredCount > 0 && (
            <div className="text-xs bg-amber-50 text-amber-700 rounded-lg px-3 py-2">
              <i className="fa-solid fa-triangle-exclamation mr-1" />
              외부 사이트 첨부 {opp.manualFetchRequiredCount}건 — 아래 링크에서 직접 받아 "수동 업로드" 하세요.
            </div>
          )}
          {attachments.length === 0 ? (
            <div className="text-sm text-gray-400">첨부파일 없음</div>
          ) : (
            <ul className="space-y-2">
              {attachments.map((a) => (
                <li key={a.id} className="flex items-center justify-between gap-2 text-sm border border-gray-100 rounded-lg px-3 py-2">
                  <div className="min-w-0">
                    <div className="font-medium text-gray-800 truncate">{a.fileName || '(이름 없음)'}</div>
                    {a.sourceUrl && a.sourceUrl !== 'admin-upload' && (
                      <a href={a.sourceUrl} target="_blank" rel="noreferrer" className="text-xs text-blue-600 hover:underline truncate inline-block max-w-full">
                        <i className="fa-solid fa-external-link mr-1" />외부 원본 링크
                      </a>
                    )}
                  </div>
                  {a.manualFetchRequired ? (
                    <span className="shrink-0 inline-flex px-2 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-700">가져와야 함</span>
                  ) : a.downloadStatus === 'SUCCESS' ? (
                    <span className="shrink-0 inline-flex px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">수집됨</span>
                  ) : (
                    <span className="shrink-0 inline-flex px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-500">{a.downloadStatus}</span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
