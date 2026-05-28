import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getRfpSampleDetail, uploadRfpSampleFile, deleteRfpSampleFile, deleteRfpSample,
} from '../api/client'

interface RfpFile {
  id: string
  fileName: string
  fileSize?: number
  contentType?: string
  isPws: boolean
}

interface RfpSampleDetail {
  id: string
  opportunityNo: string
  industryType: string
  outcome: string
  company?: string
  agency?: string
  awardAmount?: number
  fiscalYear?: number
  note?: string
  files: RfpFile[]
}

const OUTCOME_COLORS: Record<string, string> = {
  WON: 'bg-green-100 text-green-700',
  SUBMITTED: 'bg-blue-100 text-blue-700',
  OTHER: 'bg-gray-100 text-gray-600',
}

export default function RfpSampleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<RfpSampleDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [actionBusy, setActionBusy] = useState(false)

  const fetchData = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getRfpSampleDetail(id)
      setDetail(data)
    } catch (err) {
      console.error('상세 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { fetchData() }, [fetchData])

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>, isPws: boolean) => {
    if (!id || !e.target.files?.[0]) return
    setUploading(true)
    try {
      await uploadRfpSampleFile(id, e.target.files[0], isPws)
      fetchData()
    } catch (err) {
      console.error('파일 업로드 실패:', err)
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  const handleDeleteFile = async (fileId: string) => {
    if (!id) return
    setActionBusy(true)
    try {
      await deleteRfpSampleFile(id, fileId)
      fetchData()
    } finally {
      setActionBusy(false)
    }
  }

  const handleDeleteSample = async () => {
    if (!id || !confirm('이 성공 제안서를 삭제하시겠습니까? (파일 모두 삭제)')) return
    await deleteRfpSample(id)
    navigate('/rfp-samples')
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-32">
        <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
      </div>
    )
  }
  if (!detail) {
    return <div className="p-6 text-gray-400 text-sm">제안서를 찾을 수 없습니다.</div>
  }

  return (
    <div className="p-6 space-y-5">
      <button onClick={() => navigate('/rfp-samples')} className="text-sm text-gray-500 hover:text-gray-700">
        <i className="fa-solid fa-arrow-left mr-2" />목록으로
      </button>

      {/* 메타 */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-lg font-bold text-gray-900 font-mono">{detail.opportunityNo}</h1>
              <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${OUTCOME_COLORS[detail.outcome] || ''}`}>
                {detail.outcome}
              </span>
            </div>
            <div className="text-sm text-gray-500 mt-2 space-x-4">
              <span>{detail.industryType}</span>
              {detail.company && <span>업체: {detail.company}</span>}
              {detail.agency && <span>발주처: {detail.agency}</span>}
              {detail.awardAmount != null && <span>낙찰액: {detail.awardAmount.toLocaleString()}원</span>}
            </div>
          </div>
          <button onClick={handleDeleteSample} className="px-3 py-1.5 text-xs rounded-lg bg-red-50 text-red-600 hover:bg-red-100">
            <i className="fa-solid fa-trash mr-1" />삭제
          </button>
        </div>
      </div>

      {/* 원본 파일 (FACTOR별로 나뉜 그대로 보관 — 슬롯 분류 없음) */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <div className="flex items-center justify-between mb-3">
          <div>
            <h2 className="text-sm font-bold text-gray-900">원본 파일</h2>
            <p className="text-xs text-gray-400 mt-0.5">제출 시 나뉜 파일(FACTOR/섹션)을 그대로 업로드하세요. 파일명이 곧 섹션 태그입니다.</p>
          </div>
          <div className="flex gap-2">
            <label className="px-3 py-1.5 text-xs rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 cursor-pointer">
              <i className="fa-solid fa-upload mr-1" />{uploading ? '업로드 중...' : '제안서 파일'}
              <input type="file" className="hidden" disabled={uploading} onChange={(e) => handleUpload(e, false)} />
            </label>
            <label className="px-3 py-1.5 text-xs rounded-lg bg-gray-50 text-gray-700 hover:bg-gray-100 cursor-pointer">
              <i className="fa-solid fa-file-lines mr-1" />PWS 공고문
              <input type="file" className="hidden" disabled={uploading} onChange={(e) => handleUpload(e, true)} />
            </label>
          </div>
        </div>
        {detail.files.length === 0 ? (
          <p className="text-sm text-gray-400">업로드된 파일이 없습니다.</p>
        ) : (
          <div className="space-y-1.5">
            {detail.files.map((f) => (
              <div key={f.id} className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50">
                <div className="flex items-center gap-2 min-w-0">
                  <i className={`fa-solid ${f.isPws ? 'fa-file-contract text-gray-400' : 'fa-file text-blue-500'}`} />
                  <span className="text-sm text-gray-700 truncate">{f.fileName}</span>
                  {f.isPws && <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-200 text-gray-600">PWS</span>}
                </div>
                <button onClick={() => handleDeleteFile(f.id)} disabled={actionBusy}
                  className="text-gray-400 hover:text-red-500 disabled:opacity-40">
                  <i className="fa-solid fa-xmark" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
