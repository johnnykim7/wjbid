import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getRfpSamples, createRfpSample, getPatternGuides, extractPatternGuide,
} from '../api/client'

interface RfpSample {
  id: string
  opportunityNo: string
  industryType: string
  outcome: string
  company?: string
  agency?: string
  awardAmount?: number
  fileCount: number
  assignedSlotCount: number
  createdAt?: string
}

interface PatternGuide {
  slotCode: string
  slotLabel: string
  status: string
  source: string
  sampleCount?: number
}

const INDUSTRY_TYPES = [
  'GROUND_MAINTENANCE', 'CUSTODIAL', 'LAUNDRY', 'HVAC',
  'WASTE', 'PIPELINE', 'SECURITY', 'FACILITY_LEASE',
]
const OUTCOMES = ['WON', 'SUBMITTED', 'OTHER']

const OUTCOME_COLORS: Record<string, string> = {
  WON: 'bg-green-100 text-green-700',
  SUBMITTED: 'bg-blue-100 text-blue-700',
  OTHER: 'bg-gray-100 text-gray-600',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-gray-100 text-gray-500',
  EXTRACTING: 'bg-purple-100 text-purple-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}

const SOURCE_COLORS: Record<string, string> = {
  AI_EXTRACTED: 'bg-blue-50 text-blue-700',
  HUMAN_EDITED: 'bg-green-50 text-green-700',
  HUMAN_ADDED: 'bg-green-50 text-green-700',
}

const EMPTY_FORM = {
  opportunityNo: '', industryType: 'GROUND_MAINTENANCE', outcome: 'WON',
  company: '', agency: '', awardAmount: '', fiscalYear: '',
}

export default function RfpSamplePage() {
  const navigate = useNavigate()
  const [samples, setSamples] = useState<RfpSample[]>([])
  const [guides, setGuides] = useState<PatternGuide[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)
  const [extracting, setExtracting] = useState<string | null>(null)

  const fetchData = async () => {
    setLoading(true)
    try {
      const [sRes, gRes] = await Promise.all([getRfpSamples(0), getPatternGuides()])
      setSamples(sRes.data.content || [])
      setGuides(gRes.data || [])
    } catch (err) {
      console.error('성공 제안서 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  const handleSave = async () => {
    if (!form.opportunityNo.trim()) return
    setSaving(true)
    try {
      await createRfpSample({
        opportunityNo: form.opportunityNo.trim(),
        industryType: form.industryType,
        outcome: form.outcome,
        company: form.company || undefined,
        agency: form.agency || undefined,
        awardAmount: form.awardAmount ? Number(form.awardAmount) : undefined,
        fiscalYear: form.fiscalYear ? Number(form.fiscalYear) : undefined,
      })
      setShowModal(false)
      setForm({ ...EMPTY_FORM })
      fetchData()
    } catch (err) {
      console.error('등록 실패:', err)
    } finally {
      setSaving(false)
    }
  }

  const handleExtract = async (slotCode: string) => {
    setExtracting(slotCode)
    try {
      await extractPatternGuide(slotCode)
      fetchData()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      alert(msg || '패턴 추출 실패 (동일 슬롯 2건 이상 필요)')
    } finally {
      setExtracting(null)
    }
  }

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">성공 제안서 패턴</h1>
          <p className="text-sm text-gray-500 mt-1">과거 성공/낙찰 제안서 등록 → 7슬롯 배치 → 슬롯별 낙찰 패턴 추출</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="px-4 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition"
        >
          <i className="fa-solid fa-plus mr-2" />신규 등록
        </button>
      </div>

      {/* 슬롯별 가이드 상태 */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h2 className="text-sm font-bold text-gray-900 mb-3">슬롯별 패턴 가이드</h2>
        {guides.length === 0 ? (
          <p className="text-sm text-gray-400">아직 추출된 가이드가 없습니다. 제안서를 슬롯에 배치한 뒤 추출하세요.</p>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {guides.map((g) => (
              <div key={g.slotCode + (g.source || '')} className="border border-gray-200 rounded-lg p-3">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-gray-800">{g.slotLabel}</span>
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-[10px] font-medium ${STATUS_COLORS[g.status] || ''}`}>
                    {g.status}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className={`inline-flex px-2 py-0.5 rounded text-[10px] font-medium ${SOURCE_COLORS[g.source] || ''}`}>
                    {g.source === 'AI_EXTRACTED' ? 'AI' : '사람편집'}
                  </span>
                  <button
                    onClick={() => handleExtract(g.slotCode)}
                    disabled={extracting === g.slotCode}
                    className="px-2 py-0.5 text-[10px] rounded bg-purple-50 text-purple-700 hover:bg-purple-100 disabled:opacity-40"
                  >
                    {extracting === g.slotCode ? '추출중...' : '재추출'}
                  </button>
                </div>
                {g.sampleCount != null && (
                  <div className="text-[10px] text-gray-400 mt-1">{g.sampleCount}건 기반</div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 등록 목록 */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
          </div>
        ) : samples.length === 0 ? (
          <div className="text-center py-20 text-gray-400 text-sm">등록된 성공 제안서가 없습니다.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고번호</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">사업유형</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">결과</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">업체</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">파일</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">배치 슬롯</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {samples.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50 cursor-pointer" onClick={() => navigate(`/rfp-samples/${s.id}`)}>
                  <td className="px-4 py-3 font-mono text-gray-900">{s.opportunityNo}</td>
                  <td className="px-4 py-3 text-gray-600">{s.industryType}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${OUTCOME_COLORS[s.outcome] || ''}`}>
                      {s.outcome}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{s.company || '-'}</td>
                  <td className="px-4 py-3 text-center text-gray-600">{s.fileCount}</td>
                  <td className="px-4 py-3 text-center text-gray-600">{s.assignedSlotCount} / 8</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* 등록 모달 */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 flex flex-col max-h-[90vh]">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h2 className="text-base font-bold text-gray-900">성공 제안서 등록</h2>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600">
                <i className="fa-solid fa-xmark text-lg" />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto px-6 py-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">공고번호 <span className="text-red-500">*</span></label>
                <input type="text" value={form.opportunityNo}
                  onChange={(e) => setForm({ ...form, opportunityNo: e.target.value })}
                  placeholder="예: W90VN725RA012"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">사업유형 <span className="text-red-500">*</span></label>
                  <select value={form.industryType}
                    onChange={(e) => setForm({ ...form, industryType: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary bg-white">
                    {INDUSTRY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">결과 <span className="text-red-500">*</span></label>
                  <select value={form.outcome}
                    onChange={(e) => setForm({ ...form, outcome: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary bg-white">
                    {OUTCOMES.map((o) => <option key={o} value={o}>{o}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">업체</label>
                  <input type="text" value={form.company}
                    onChange={(e) => setForm({ ...form, company: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">발주처</label>
                  <input type="text" value={form.agency}
                    onChange={(e) => setForm({ ...form, agency: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">낙찰액 (원)</label>
                  <input type="number" value={form.awardAmount}
                    onChange={(e) => setForm({ ...form, awardAmount: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">회계연도</label>
                  <input type="number" value={form.fiscalYear}
                    onChange={(e) => setForm({ ...form, fiscalYear: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary" />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
              <button onClick={() => setShowModal(false)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition">취소</button>
              <button onClick={handleSave} disabled={saving || !form.opportunityNo.trim()}
                className="px-5 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition disabled:opacity-50">
                {saving ? <><i className="fa-solid fa-circle-notch fa-spin mr-2" />저장 중...</> : '등록'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
