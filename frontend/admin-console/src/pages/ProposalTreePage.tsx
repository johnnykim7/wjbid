import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getProposalTree,
  getProposalSection,
  regenerateProposalSection,
  lockProposalSection,
  unlockProposalSection,
} from '../api/client'

// ── 타입 (BE ProposalTreeDto 대응) ──
interface SectionNode {
  id: string
  chapterId: string
  subfactorLabel: string | null
  title: string | null
  status: SectionStatus
  locked: boolean
  minWords: number | null
  requirementRefs: string[] | null
  orderNo: number
}
interface ChapterNode {
  id: string
  factorLabel: string | null
  factorTitle: string | null
  sourceSection: string | null
  orderNo: number
  sections: SectionNode[]
}
interface BlockNode {
  id: string
  blockType: string | null
  contentJson: Record<string, unknown> | null
  sourceEvidence: Record<string, unknown> | null
  orderNo: number
}
interface SectionDetail {
  id: string
  subfactorLabel: string | null
  title: string | null
  scope: string | null
  status: SectionStatus
  locked: boolean
  minWords: number | null
  requirementRefs: string[] | null
  blocks: BlockNode[]
}

type SectionStatus = 'PENDING' | 'DRAFTING' | 'DRAFTED' | 'VERIFIED' | 'NEEDS_REGEN' | 'LOCKED'

const STATUS_STYLE: Record<SectionStatus, string> = {
  PENDING: 'bg-gray-100 text-gray-600',
  DRAFTING: 'bg-blue-100 text-blue-700',
  DRAFTED: 'bg-indigo-100 text-indigo-700',
  VERIFIED: 'bg-green-100 text-green-700',
  NEEDS_REGEN: 'bg-red-100 text-red-700',
  LOCKED: 'bg-gray-300 text-gray-700',
}

const STATUS_LABEL: Record<SectionStatus, string> = {
  PENDING: '대기',
  DRAFTING: '작성중',
  DRAFTED: '초안',
  VERIFIED: '검증완료',
  NEEDS_REGEN: '재생성필요',
  LOCKED: '잠김',
}

/** TipTap JSON node 에서 평문 텍스트 추출 (읽기 전용 미리보기용). */
function extractText(node: unknown): string {
  if (!node || typeof node !== 'object') return ''
  const n = node as Record<string, unknown>
  if (typeof n.text === 'string') return n.text
  const content = n.content
  if (Array.isArray(content)) return content.map(extractText).join('')
  return ''
}

export default function ProposalTreePage() {
  const { id: documentId } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [chapters, setChapters] = useState<ChapterNode[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<SectionDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [acting, setActing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadTree = useCallback(async () => {
    if (!documentId) return
    setLoading(true)
    try {
      const res = await getProposalTree(documentId)
      setChapters(res.data.chapters || [])
    } catch {
      setError('트리를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [documentId])

  useEffect(() => {
    loadTree()
  }, [loadTree])

  const loadDetail = useCallback(async (sectionId: string) => {
    if (!documentId) return
    setSelectedId(sectionId)
    setDetailLoading(true)
    try {
      const res = await getProposalSection(documentId, sectionId)
      setDetail(res.data)
    } catch {
      setError('section 상세를 불러오지 못했습니다.')
    } finally {
      setDetailLoading(false)
    }
  }, [documentId])

  const handleRegenerate = async () => {
    if (!documentId || !detail) return
    if (!confirm('이 section 본문을 재생성합니다. 기존 본문은 교체됩니다. 진행할까요?')) return
    setActing(true)
    setError(null)
    try {
      await regenerateProposalSection(documentId, detail.id)
      await loadTree()
      await loadDetail(detail.id)
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || '재생성 요청에 실패했습니다.')
    } finally {
      setActing(false)
    }
  }

  const handleToggleLock = async () => {
    if (!documentId || !detail) return
    setActing(true)
    setError(null)
    try {
      if (detail.locked) {
        await unlockProposalSection(documentId, detail.id)
      } else {
        await lockProposalSection(documentId, detail.id)
      }
      await loadTree()
      await loadDetail(detail.id)
    } catch {
      setError('잠금 상태 변경에 실패했습니다.')
    } finally {
      setActing(false)
    }
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="text-sm text-gray-500 hover:text-gray-700 mb-1"
          >
            ← 뒤로
          </button>
          <h1 className="text-xl font-bold">제안서 Section 관리</h1>
          <p className="text-sm text-gray-500">문서 {documentId}</p>
        </div>
        <button
          onClick={() => documentId && navigate(`/documents/${documentId}/edit`)}
          className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50"
        >
          통합 편집기
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded">{error}</div>
      )}

      <div className="grid grid-cols-12 gap-4">
        {/* ── 좌: 트리 ── */}
        <div className="col-span-4 bg-white border rounded-lg p-3 max-h-[75vh] overflow-y-auto">
          {loading ? (
            <p className="text-sm text-gray-400 p-2">불러오는 중…</p>
          ) : chapters.length === 0 ? (
            <p className="text-sm text-gray-400 p-2">생성된 section 이 없습니다.</p>
          ) : (
            chapters.map((ch) => (
              <div key={ch.id} className="mb-3">
                <div className="font-semibold text-sm text-gray-800 px-1 py-1">
                  {ch.factorLabel ? `${ch.factorLabel}. ` : ''}
                  {ch.factorTitle || '(제목 없음)'}
                </div>
                <ul className="ml-2">
                  {ch.sections.map((s) => (
                    <li key={s.id}>
                      <button
                        onClick={() => loadDetail(s.id)}
                        className={`w-full text-left px-2 py-1.5 rounded text-sm flex items-center justify-between gap-2 ${
                          selectedId === s.id ? 'bg-blue-50 ring-1 ring-blue-200' : 'hover:bg-gray-50'
                        }`}
                      >
                        <span className="truncate">
                          {s.subfactorLabel ? `${s.subfactorLabel} ` : ''}
                          {s.title || '(무제)'}
                        </span>
                        <span
                          className={`shrink-0 text-[11px] px-1.5 py-0.5 rounded ${STATUS_STYLE[s.status]}`}
                        >
                          {STATUS_LABEL[s.status]}
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ))
          )}
        </div>

        {/* ── 우: 상세 ── */}
        <div className="col-span-8 bg-white border rounded-lg p-4 max-h-[75vh] overflow-y-auto">
          {!detail ? (
            <p className="text-sm text-gray-400">좌측에서 section 을 선택하세요.</p>
          ) : detailLoading ? (
            <p className="text-sm text-gray-400">불러오는 중…</p>
          ) : (
            <>
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h2 className="font-bold text-lg">
                    {detail.subfactorLabel ? `${detail.subfactorLabel} ` : ''}
                    {detail.title || '(무제)'}
                  </h2>
                  <span
                    className={`inline-block mt-1 text-xs px-2 py-0.5 rounded ${STATUS_STYLE[detail.status]}`}
                  >
                    {STATUS_LABEL[detail.status]}
                  </span>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleRegenerate}
                    disabled={acting || detail.locked}
                    className="px-3 py-1.5 text-sm rounded bg-blue-600 text-white disabled:opacity-40 hover:bg-blue-700"
                    title={detail.locked ? '잠긴 section 은 재생성할 수 없습니다' : ''}
                  >
                    재생성
                  </button>
                  <button
                    onClick={handleToggleLock}
                    disabled={acting}
                    className="px-3 py-1.5 text-sm rounded border disabled:opacity-40 hover:bg-gray-50"
                  >
                    {detail.locked ? '잠금 해제' : '잠금'}
                  </button>
                </div>
              </div>

              {/* 메타 */}
              <div className="mb-4 text-sm grid grid-cols-2 gap-2">
                <div>
                  <span className="text-gray-500">최소 분량</span>{' '}
                  {detail.minWords != null ? `${detail.minWords} words` : '—'}
                </div>
                <div>
                  <span className="text-gray-500">요구사항</span>{' '}
                  {detail.requirementRefs && detail.requirementRefs.length > 0
                    ? detail.requirementRefs.join(', ')
                    : '—'}
                </div>
              </div>

              {detail.scope && (
                <div className="mb-4 p-2 bg-gray-50 rounded text-sm text-gray-600">
                  <span className="font-medium text-gray-700">작성 지침: </span>
                  {detail.scope}
                </div>
              )}

              {/* 검증 결과 — CR-031 도입 후 연결 (placeholder) */}
              <div className="mb-4 p-2 border border-dashed rounded text-xs text-gray-400">
                검증 결과(환각/분량)는 검증 파이프라인(CR-031) 도입 후 표시됩니다.
              </div>

              {/* 본문 (읽기 전용 미리보기) */}
              <div className="border-t pt-3">
                <div className="text-sm font-medium text-gray-700 mb-2">
                  본문 ({detail.blocks.length} blocks)
                </div>
                {detail.blocks.length === 0 ? (
                  <p className="text-sm text-gray-400">작성된 본문이 없습니다.</p>
                ) : (
                  <div className="space-y-2">
                    {detail.blocks.map((b) => {
                      const text = extractText(b.contentJson)
                      return (
                        <p key={b.id} className="text-sm text-gray-800 whitespace-pre-wrap">
                          {text || <span className="text-gray-300">(빈 block)</span>}
                        </p>
                      )
                    })}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
