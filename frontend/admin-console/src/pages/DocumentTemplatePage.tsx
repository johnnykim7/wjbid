import { useEffect, useState } from 'react'
import {
  getDocumentTemplates,
  createDocumentTemplate,
  updateDocumentTemplate,
  deactivateDocumentTemplate,
  activateDocumentTemplate,
} from '../api/client'

const DOCUMENT_TYPES = [
  { value: 'NOTICE_VIEW',        label: '공고문 표시 양식 (Notice View) — TipTap JSON 골격' },
  { value: 'COVER_LETTER',       label: '커버레터 (Cover Letter)' },
  { value: 'TECHNICAL_PROPOSAL', label: '기술 제안서 (Technical Proposal)' },
  { value: 'PAST_PERFORMANCE',   label: '실적 증명 (Past Performance)' },
  { value: 'COMPANY_PROFILE',    label: '회사 소개서 (Company Profile)' },
  { value: 'COMPLIANCE_MATRIX',  label: '요건 준수표 (Compliance Matrix)' },
  { value: 'PRICING_SUMMARY',    label: '가격 제안서 (Pricing Summary)' },
  { value: 'OTHER',              label: '기타 (Other)' },
]

const DEFAULT_CONTENT_JSON = JSON.stringify(
  {
    type: 'doc',
    content: [
      {
        type: 'heading',
        attrs: { level: 1 },
        content: [{ type: 'text', text: '제목을 입력하세요' }],
      },
      {
        type: 'paragraph',
        content: [{ type: 'text', text: '내용을 입력하세요.' }],
      },
    ],
  },
  null,
  2
)

interface Template {
  id: string
  templateName: string
  documentType: string
  templateVersion: number
  active: boolean
  createdAt: string
  // BE Boolean(대문자) 직렬화 호환
}

export default function DocumentTemplatePage() {
  const [templates, setTemplates] = useState<Template[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [jsonError, setJsonError] = useState('')
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)

  // 폼 상태
  const [form, setForm] = useState({
    templateName: '',
    documentType: 'COVER_LETTER',
    contentJsonStr: DEFAULT_CONTENT_JSON,
  })

  const load = async () => {
    setLoading(true)
    try {
      const { data } = await getDocumentTemplates(includeInactive)
      setTemplates(data)
    } catch {
      setError('템플릿 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [includeInactive])

  const openModal = () => {
    setForm({ templateName: '', documentType: 'COVER_LETTER', contentJsonStr: DEFAULT_CONTENT_JSON })
    setJsonError('')
    setError('')
    setEditingId(null)
    setShowModal(true)
  }

  const openEditModal = async (t: Template) => {
    // 목록 API에는 contentJson이 안 들어있을 수 있어 서버에서 다시 받아오는 게 정석이지만,
    // 현 BE는 목록에 contentJson을 그대로 실어 보냄. 일단 t에서 직접 사용.
    const raw = (t as unknown as { contentJson?: Record<string, unknown> }).contentJson
    const jsonStr = raw ? JSON.stringify(raw, null, 2) : DEFAULT_CONTENT_JSON
    setForm({
      templateName: t.templateName,
      documentType: t.documentType,
      contentJsonStr: jsonStr,
    })
    setJsonError('')
    setError('')
    setEditingId(t.id)
    setShowModal(true)
  }

  const handleSave = async () => {
    if (!form.templateName.trim()) {
      setError('템플릿 이름을 입력하세요.')
      return
    }

    let contentJson: Record<string, unknown>
    try {
      contentJson = JSON.parse(form.contentJsonStr)
    } catch {
      setJsonError('올바른 JSON 형식이 아닙니다.')
      return
    }

    setSaving(true)
    setError('')
    try {
      if (editingId) {
        await updateDocumentTemplate(editingId, {
          templateName: form.templateName,
          contentJson,
        })
      } else {
        await createDocumentTemplate({
          templateName: form.templateName,
          documentType: form.documentType,
          contentJson,
        })
      }
      setShowModal(false)
      setEditingId(null)
      await load()
    } catch {
      setError('저장에 실패했습니다. 다시 시도해주세요.')
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (id: string, name: string) => {
    if (!confirm(`"${name}" 템플릿을 비활성화하시겠습니까?`)) return
    try {
      await deactivateDocumentTemplate(id)
      await load()
    } catch {
      setError('비활성화에 실패했습니다.')
    }
  }

  const handleActivate = async (id: string) => {
    try {
      await activateDocumentTemplate(id)
      await load()
    } catch {
      setError('재활성화에 실패했습니다.')
    }
  }

  const docTypeLabel = (value: string) =>
    DOCUMENT_TYPES.find((d) => d.value === value)?.label ?? value

  return (
    <div className="p-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">문서 템플릿 관리</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Claude가 문서 생성 시 사용하는 TipTap JSON 템플릿을 관리합니다.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="inline-flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={includeInactive}
              onChange={(e) => setIncludeInactive(e.target.checked)}
              className="w-4 h-4 accent-secondary"
            />
            비활성 포함
          </label>
          <button
            onClick={openModal}
            className="flex items-center gap-2 px-4 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition"
          >
            <i className="fa-solid fa-plus" />
            새 템플릿 등록
          </button>
        </div>
      </div>

      {error && !showModal && (
        <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
          {error}
        </div>
      )}

      {/* 템플릿 목록 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48 text-gray-400">
            <i className="fa-solid fa-circle-notch fa-spin mr-2" />
            불러오는 중...
          </div>
        ) : templates.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-gray-400">
            <i className="fa-solid fa-file-lines text-3xl mb-3 opacity-30" />
            <p className="text-sm">등록된 템플릿이 없습니다.</p>
            <button
              onClick={openModal}
              className="mt-3 text-sm text-secondary hover:underline"
            >
              첫 번째 템플릿 등록하기
            </button>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  템플릿 이름
                </th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  문서 타입
                </th>
                <th className="text-center px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  버전
                </th>
                <th className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  등록일
                </th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {templates.map((t) => (
                <tr key={t.id} className={`hover:bg-gray-50 transition ${!t.active ? 'opacity-60 bg-gray-50/50' : ''}`}>
                  <td className="px-5 py-3.5 font-medium text-gray-900">
                    <div className="flex items-center gap-2">
                      {t.templateName}
                      {!t.active && (
                        <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-gray-200 text-gray-600 uppercase">
                          비활성
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-5 py-3.5">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                      {docTypeLabel(t.documentType)}
                    </span>
                  </td>
                  <td className="px-5 py-3.5 text-center text-gray-500">
                    v{t.templateVersion}
                  </td>
                  <td className="px-5 py-3.5 text-gray-500">
                    {t.createdAt ? new Date(t.createdAt).toLocaleDateString('ko-KR') : '-'}
                  </td>
                  <td className="px-5 py-3.5 text-right space-x-3">
                    {t.active && (
                      <button
                        onClick={() => openEditModal(t)}
                        className="text-xs text-secondary hover:text-blue-700 transition"
                      >
                        수정
                      </button>
                    )}
                    {t.active ? (
                      <button
                        onClick={() => handleDeactivate(t.id, t.templateName)}
                        className="text-xs text-red-500 hover:text-red-700 transition"
                      >
                        비활성화
                      </button>
                    ) : (
                      <button
                        onClick={() => handleActivate(t.id)}
                        className="text-xs text-green-600 hover:text-green-700 transition font-semibold"
                      >
                        재활성화
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* 등록 모달 */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 flex flex-col max-h-[90vh]">
            {/* 모달 헤더 */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div>
                <h2 className="text-base font-bold text-gray-900">{editingId ? '템플릿 수정' : '새 템플릿 등록'}</h2>
                {editingId && (
                  <p className="text-xs text-amber-600 mt-0.5">
                    저장하면 새 버전이 생성되고 현재 버전은 비활성화됩니다 (버전 불변성).
                  </p>
                )}
              </div>
              <button
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <i className="fa-solid fa-xmark text-lg" />
              </button>
            </div>

            {/* 모달 바디 */}
            <div className="flex-1 overflow-y-auto px-6 py-5 space-y-4">
              {error && (
                <div className="px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
                  {error}
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  템플릿 이름 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={form.templateName}
                  onChange={(e) => setForm({ ...form, templateName: e.target.value })}
                  placeholder="예: 커버레터 기본 템플릿"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  문서 타입 <span className="text-red-500">*</span>
                </label>
                <select
                  value={form.documentType}
                  onChange={(e) => setForm({ ...form, documentType: e.target.value })}
                  disabled={!!editingId}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary bg-white disabled:bg-gray-100 disabled:text-gray-500"
                >
                  {DOCUMENT_TYPES.map((d) => (
                    <option key={d.value} value={d.value}>{d.label}</option>
                  ))}
                </select>
                {editingId && (
                  <p className="text-xs text-gray-400 mt-1">수정 시 문서 타입은 변경할 수 없습니다.</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  TipTap JSON <span className="text-red-500">*</span>
                </label>
                <p className="text-xs text-gray-400 mb-2">
                  TipTap 에디터 형식의 JSON을 입력하세요. Claude가 이 구조를 기반으로 문서를 작성합니다.
                </p>
                <textarea
                  value={form.contentJsonStr}
                  onChange={(e) => {
                    setForm({ ...form, contentJsonStr: e.target.value })
                    setJsonError('')
                  }}
                  rows={14}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-xs font-mono focus:outline-none focus:ring-2 focus:ring-secondary resize-none"
                  spellCheck={false}
                />
                {jsonError && (
                  <p className="mt-1 text-xs text-red-500">
                    <i className="fa-solid fa-triangle-exclamation mr-1" />
                    {jsonError}
                  </p>
                )}
              </div>
            </div>

            {/* 모달 푸터 */}
            <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
              <button
                onClick={() => setShowModal(false)}
                className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition"
              >
                취소
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-5 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition disabled:opacity-50"
              >
                {saving ? (
                  <><i className="fa-solid fa-circle-notch fa-spin mr-2" />저장 중...</>
                ) : '저장'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
