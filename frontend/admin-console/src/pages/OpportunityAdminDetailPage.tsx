import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAdminOpportunityDetail,
  createNotice,
  uploadOpportunityAttachment,
  getOpportunityAttachments,
  retranslateOpportunityDescription,
} from '../api/client'
import OpportunityMetaPanel from '../components/OpportunityMetaPanel'

interface OpportunityDetail {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  titleKo?: string                 // CR-022
  type?: string
  typeKo?: string                  // CR-022
  organizationName?: string
  postedDate?: string
  responseDeadline?: string
  active?: boolean
  uiLink?: string
  translatedAt?: string            // CR-022
  descriptionSummaryKo?: string    // CR-022 2차
  awardAmount?: string             // CR-022 2차
  placeOfPerformanceShort?: string // CR-022 2차
  setAsideKo?: string              // CR-022 2차
  naicsLabelKo?: string            // CR-022 2차
  // CR-021 2차 — SAM 원본 메타 (raw_json 기반)
  setAside?: string | null
  naicsCode?: string | null
  classificationCode?: string | null
  placeOfPerformance?: unknown
  description?: string | null
  pointOfContact?: Array<{
    type?: string
    fullName?: string
    email?: string
    phone?: string
    fax?: string
  }> | null
  resourceLinks?: string[] | null
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
  const [translateLoading, setTranslateLoading] = useState(false)

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
      // 기존 공고문이 있으면 BE가 그 noticeId를 반환(중복 생성 안 함) → 해당 상세로 이동
      const res = await createNotice(id)
      const noticeId = res?.data?.noticeId
      navigate(noticeId ? `/notices/${noticeId}` : '/notices')
    } catch (err) {
      console.error('공고문 생성 실패:', err)
    } finally {
      setActionLoading(false)
    }
  }

  const handleRetranslate = async () => {
    if (!id) return
    setTranslateLoading(true)
    try {
      const { data } = await retranslateOpportunityDescription(id)
      if (data?.status === 'FAILED_OR_EMPTY') {
        alert('번역에 실패했거나 원본 본문이 비어있습니다. 잠시 후 다시 시도해 주세요.')
      }
      fetchData()
    } catch (err) {
      console.error('본문 번역 실패:', err)
      alert('본문 번역 요청에 실패했습니다.')
    } finally {
      setTranslateLoading(false)
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
          <h1 className="text-xl font-bold text-gray-900 line-clamp-2">{opp.titleKo || opp.title}</h1>
          {opp.titleKo && (
            <p className="text-sm text-gray-500 line-clamp-2 mt-1">{opp.title}</p>
          )}
          <div className="flex items-center gap-3 mt-2 flex-wrap">
            <span className="text-xs text-gray-500 font-mono">{opp.solicitationNumber || opp.noticeId}</span>
            <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
              opp.noticeCount > 0 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
            }`}>
              {opp.noticeCount > 0 ? `공고문 ${opp.noticeCount}개` : '공고문 미생성'}
            </span>
            {opp.translatedAt ? (
              <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700"
                title={`번역 완료: ${new Date(opp.translatedAt).toLocaleString('ko')}`}>
                <i className="fa-solid fa-language mr-1" />번역됨
              </span>
            ) : (
              <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700"
                title="제목 한글화 미완료 — 영문 fallback">
                미번역 (영문 표시)
              </span>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleRetranslate}
            disabled={translateLoading}
            className="px-3 py-2 text-sm rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-40"
            title="원본 본문을 LLM으로 한글 번역 (자동 실패 시 수동)"
          >
            <i className={`fa-solid ${translateLoading ? 'fa-spinner fa-spin' : 'fa-language'} mr-1.5`} />
            한글 번역하기
          </button>
          <button
            onClick={handleCreateNotice}
            disabled={actionLoading}
            className="px-4 py-2 text-sm rounded-lg bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-40"
          >
            <i className="fa-solid fa-wand-magic-sparkles mr-1.5" />공고문 만들기
          </button>
        </div>
      </div>

      {/* CR-021 2차: SAM.gov 원본 메타 패널 (페이지 헤더가 별도로 있으므로 hideHeader) */}
      <OpportunityMetaPanel
        hideHeader
        data={{
          title: opp.title,
          koreanTitle: opp.titleKo,
          solicitationNumber: opp.solicitationNumber,
          type: opp.typeKo || opp.type,
          organizationName: opp.organizationName,
          postedDate: opp.postedDate,
          responseDeadline: opp.responseDeadline,
          active: opp.active,
          uiLink: opp.uiLink,
          // CR-022 한글화 라벨이 있으면 우선
          setAside: opp.setAsideKo || opp.setAside,
          naicsCode: opp.naicsLabelKo
            ? `${opp.naicsCode || ''} — ${opp.naicsLabelKo}`.trim().replace(/^—\s*/, '')
            : opp.naicsCode,
          classificationCode: opp.classificationCode,
          placeOfPerformance: opp.placeOfPerformanceShort || opp.placeOfPerformance,
          description: opp.descriptionSummaryKo || opp.description,
          pointOfContact: opp.pointOfContact,
          resourceLinks: opp.resourceLinks,
        }}
      />

      <div className="grid grid-cols-1 gap-4">
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
