import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAdminOpportunityDetail,
  createNotice,
  uploadOpportunityAttachment,
  getOpportunityAttachments,
  downloadOpportunityAttachment,
  deleteOpportunityAttachment,
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
  fileSize?: number
  contentType?: string
  sourceUrl?: string
  downloadStatus: 'SUCCESS' | 'FAILED' | 'LINK_ONLY' | 'MANUAL_FETCH_REQUIRED'
  manualFetchRequired: boolean
  downloadedAt?: string
}

/** 바이트를 사람이 읽기 좋은 단위로. null/0/미상은 빈 문자열 반환(표시 생략). */
function formatFileSize(bytes?: number): string {
  if (bytes == null || bytes <= 0) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let v = bytes
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v < 10 && i > 0 ? v.toFixed(1) : Math.round(v)} ${units[i]}`
}

export default function OpportunityAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [opp, setOpp] = useState<OpportunityDetail | null>(null)
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [uploadLoading, setUploadLoading] = useState(false)
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

  // PIEE에서 받은 zip은 "공고번호.zip"(예: W91QVN26QA030.zip, W91QVN26QA022 (1).zip) 형태로 떨어진다.
  // 다운로드 폴더에 섞인 '다른 공고'의 zip을 무의식적으로 잘못 올리는 사고를 막기 위해,
  // 파일명이 공고번호 패턴인데 현재 공고번호와 다르면 경고(업로드 자체는 막지 않음, 확인 시 진행).
  // 일반 파일명(report.zip 등)은 PIEE 산출물이 아니므로 경고하지 않는다.
  const SOLNO_PATTERN = /^[A-Z0-9]{10,}$/
  const fileBaseAsSolNo = (name: string): string | null => {
    const base = name
      .replace(/\.zip$/i, '')      // 확장자 제거
      .replace(/\s*\(\d+\)\s*$/, '') // 브라우저 중복 다운로드 표식 "(1)" 제거
      .trim()
      .toUpperCase()
    return SOLNO_PATTERN.test(base) ? base : null
  }

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!id || !e.target.files?.length) return
    const files = Array.from(e.target.files)

    // 공고번호 패턴 zip인데 현재 공고번호와 불일치하는 파일 검출
    const currentSolNo = opp?.solicitationNumber?.trim().toUpperCase()
    if (currentSolNo) {
      const mismatched = files
        .map((f) => ({ name: f.name, solNo: fileBaseAsSolNo(f.name) }))
        .filter((f) => f.solNo && f.solNo !== currentSolNo)
      if (mismatched.length > 0) {
        const list = mismatched.map((f) => `· ${f.name} → ${f.solNo}`).join('\n')
        const ok = window.confirm(
          `다음 파일명이 현재 공고번호(${opp?.solicitationNumber})와 다릅니다.\n` +
          `다른 공고의 입찰서류를 잘못 올린 것은 아닌지 확인하세요.\n\n${list}\n\n그대로 업로드할까요?`
        )
        if (!ok) { e.target.value = ''; return }
      }
    }

    setUploadLoading(true)
    try {
      await uploadOpportunityAttachment(id, files)
      fetchData()
    } catch (err) {
      console.error('파일 업로드 실패:', err)
    } finally {
      setUploadLoading(false)
      e.target.value = '' // 같은 파일 재선택 허용
    }
  }

  // CR-034: 저장된 첨부 다운로드 (blob 응답을 받아 브라우저 저장)
  const handleDownload = async (a: Attachment) => {
    if (!id) return
    try {
      const res = await downloadOpportunityAttachment(id, a.id)
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.download = a.fileName || 'attachment'
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      console.error('첨부 다운로드 실패:', err)
      alert('다운로드에 실패했습니다.')
    }
  }

  // CR-034: 수동 업로드 첨부 삭제 (SAM 수집 첨부는 버튼 자체가 없음)
  const handleDelete = async (a: Attachment) => {
    if (!id) return
    if (!window.confirm(`"${a.fileName || '이 첨부'}"를 삭제할까요?`)) return
    setActionLoading(true)
    try {
      await deleteOpportunityAttachment(id, a.id)
      fetchData()
    } catch (err) {
      console.error('첨부 삭제 실패:', err)
      alert('삭제에 실패했습니다. (SAM 수집 첨부는 삭제할 수 없습니다)')
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
            <label
              className={`px-3 py-1.5 text-xs rounded-lg bg-blue-50 text-blue-700 ${
                uploadLoading ? 'opacity-60 cursor-not-allowed' : 'hover:bg-blue-100 cursor-pointer'
              }`}
            >
              {uploadLoading ? (
                <>
                  <i className="fa-solid fa-spinner fa-spin mr-1" />업로드 중...
                </>
              ) : (
                <>
                  <i className="fa-solid fa-upload mr-1" />수동 업로드
                </>
              )}
              <input
                type="file"
                multiple
                className="hidden"
                onChange={handleFileUpload}
                disabled={uploadLoading}
              />
            </label>
          </div>
          {/* CR-034: PIEE 입찰서류 안내 — 첨부 유무와 무관하게 항상 노출. 입찰서류 정본/추가본이 PIEE에 있을 수 있음 */}
          {opp.solicitationNumber && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 px-3 py-2.5">
              <p className="text-xs text-amber-800 mb-2 leading-relaxed">
                입찰서류 정본(본 공고서·수정본·추가서류)은 PIEE에도 게시될 수 있습니다.
                아래에서 공고번호 <span className="font-semibold">{opp.solicitationNumber}</span>로 확인 후,
                필요한 파일을 받아 "수동 업로드" 하세요.
              </p>
              <a
                href={`https://piee.eb.mil/sol/xhtml/unauth/search/oppMgmtLink.xhtml?solNo=${encodeURIComponent(opp.solicitationNumber)}`}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 rounded-md bg-amber-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-amber-700"
              >
                <i className="fa-solid fa-up-right-from-square" />
                PIEE 입찰서류 보기
              </a>
            </div>
          )}
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
                    <div className="font-medium text-gray-800 truncate">
                      {a.fileName || '(이름 없음)'}
                      {formatFileSize(a.fileSize) && (
                        <span className="ml-2 font-normal text-xs text-gray-400">{formatFileSize(a.fileSize)}</span>
                      )}
                    </div>
                    {a.sourceUrl && a.sourceUrl !== 'admin-upload' && (
                      <a href={a.sourceUrl} target="_blank" rel="noreferrer" className="text-xs text-blue-600 hover:underline truncate inline-block max-w-full">
                        <i className="fa-solid fa-external-link mr-1" />외부 원본 링크
                      </a>
                    )}
                  </div>
                  <div className="shrink-0 flex items-center gap-1.5">
                    {a.manualFetchRequired ? (
                      <span className="inline-flex px-2 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-700">가져와야 함</span>
                    ) : a.downloadStatus === 'SUCCESS' ? (
                      <span className="inline-flex px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">수집됨</span>
                    ) : (
                      <span className="inline-flex px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-500">{a.downloadStatus}</span>
                    )}
                    {/* CR-034: 저장된 파일(수집됨)은 다운로드 가능 */}
                    {a.downloadStatus === 'SUCCESS' && (
                      <button
                        onClick={() => handleDownload(a)}
                        className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700 hover:bg-blue-100"
                        title="다운로드"
                      >
                        <i className="fa-solid fa-download" />
                      </button>
                    )}
                    {/* CR-034: 관리자 수동 업로드분만 삭제 가능. SAM 수집 첨부는 삭제 버튼 없음 */}
                    {a.sourceUrl === 'admin-upload' && (
                      <button
                        onClick={() => handleDelete(a)}
                        disabled={actionLoading}
                        className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-50 text-red-600 hover:bg-red-100 disabled:opacity-50"
                        title="삭제 (수동 업로드분)"
                      >
                        <i className="fa-solid fa-trash" />
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
