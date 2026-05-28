import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getRfpSampleDetail, uploadRfpSampleFile, deleteRfpSampleFile,
  assignSlot, unassignSlot, deleteRfpSample,
} from '../api/client'

interface RfpFile {
  id: string
  fileName: string
  fileSize?: number
  contentType?: string
  isPws: boolean
}

interface SlotAssignment {
  id: string
  slotCode: string
  slotLabel: string
  sampleFileId?: string
  fileName?: string
  otherLabel?: string
  sectionText?: string
  confirmed: boolean
  autoEstimated: boolean
}

interface SlotStatus {
  slotCode: string
  slotLabel: string
  displayOrder: number
  isOther: boolean
  empty: boolean
  assignments: SlotAssignment[]
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
  slots: SlotStatus[]
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

  const handleAssign = async (slotCode: string, fileId: string) => {
    if (!id) return
    setActionBusy(true)
    try {
      await assignSlot(id, slotCode, { sampleFileId: fileId, confirmed: true })
      fetchData()
    } finally {
      setActionBusy(false)
    }
  }

  const handleConfirm = async (slotCode: string, a: SlotAssignment) => {
    if (!id) return
    setActionBusy(true)
    try {
      await assignSlot(id, slotCode, { sampleFileId: a.sampleFileId, confirmed: true })
      await unassignSlot(id, a.id)
      fetchData()
    } finally {
      setActionBusy(false)
    }
  }

  const handleUnassign = async (assignmentId: string) => {
    if (!id) return
    setActionBusy(true)
    try {
      await unassignSlot(id, assignmentId)
      fetchData()
    } finally {
      setActionBusy(false)
    }
  }

  const handleDeleteSample = async () => {
    if (!id || !confirm('이 성공 제안서를 삭제하시겠습니까? (파일·배치 모두 삭제)')) return
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

  const proposalFiles = detail.files.filter((f) => !f.isPws)
  // 미배치 파일 = 어떤 슬롯에도 안 들어간 파일
  const assignedFileIds = new Set(
    detail.slots.flatMap((s) => s.assignments.map((a) => a.sampleFileId).filter(Boolean))
  )
  const unassignedFiles = proposalFiles.filter((f) => !assignedFileIds.has(f.id))

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

      {/* 파일 업로드 */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-bold text-gray-900">원본 파일</h2>
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

      {/* 7슬롯 배치 */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h2 className="text-sm font-bold text-gray-900 mb-3">7슬롯 배치</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {detail.slots.map((slot) => (
            <div key={slot.slotCode}
              className={`border rounded-lg p-3 ${slot.empty ? 'border-yellow-300 bg-yellow-50' : 'border-gray-200'}`}>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-medium text-gray-800">{slot.slotLabel}</span>
                {slot.empty && (
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-yellow-200 text-yellow-800">
                    <i className="fa-solid fa-triangle-exclamation mr-1" />데이터 넣어줘
                  </span>
                )}
              </div>
              {slot.assignments.length > 0 ? (
                <div className="space-y-1.5">
                  {slot.assignments.map((a) => (
                    <div key={a.id} className="flex items-center justify-between text-xs bg-white border border-gray-100 rounded px-2 py-1">
                      <span className="truncate text-gray-700">{a.fileName || a.otherLabel || '(섹션)'}</span>
                      <div className="flex items-center gap-1.5 shrink-0">
                        {a.autoEstimated && !a.confirmed && (
                          <button onClick={() => handleConfirm(slot.slotCode, a)} disabled={actionBusy}
                            className="text-[10px] px-1.5 py-0.5 rounded bg-purple-50 text-purple-700 hover:bg-purple-100"
                            title="자동추정 — 확인">자동·확인</button>
                        )}
                        {a.confirmed && <i className="fa-solid fa-check text-green-500" title="확인됨" />}
                        <button onClick={() => handleUnassign(a.id)} disabled={actionBusy}
                          className="text-gray-300 hover:text-red-500"><i className="fa-solid fa-xmark" /></button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                unassignedFiles.length > 0 && (
                  <select
                    onChange={(e) => { if (e.target.value) handleAssign(slot.slotCode, e.target.value) }}
                    value=""
                    disabled={actionBusy}
                    className="w-full text-xs px-2 py-1 border border-gray-200 rounded bg-white">
                    <option value="">+ 파일 배치...</option>
                    {unassignedFiles.map((f) => <option key={f.id} value={f.id}>{f.fileName}</option>)}
                  </select>
                )
              )}
            </div>
          ))}
        </div>
        {unassignedFiles.length > 0 && (
          <p className="text-xs text-gray-400 mt-3">미배치 파일 {unassignedFiles.length}건 — 빈 슬롯에서 배치하세요.</p>
        )}
      </div>
    </div>
  )
}
