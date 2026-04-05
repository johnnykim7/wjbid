import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Placeholder from '@tiptap/extension-placeholder'
import {
  getBidDocument,
  getBidDocumentVersions,
  getBidDocumentVersion,
  saveDocumentVersion,
  lockDocument,
  unlockDocument,
  exportDocumentPdf,
} from '../api/client'

interface DocumentVersion {
  id: string
  versionNo: number
  versionLabel: string
  editedBy: string
  editedAt: string
  changeSummary: string
  wordCount: number
  contentJson?: Record<string, unknown>
}

interface BidDocumentData {
  id: string
  bidRequestId: string
  documentType: string
  status: string
  currentVersionNo: number
  createdAt: string
  updatedAt: string
  latestVersion?: DocumentVersion
}

const DOC_TYPE_LABELS: Record<string, string> = {
  COVER_LETTER: 'Cover Letter',
  TECHNICAL_PROPOSAL: 'Technical Proposal',
  PAST_PERFORMANCE: 'Past Performance',
  COMPANY_PROFILE: 'Company Profile',
  COMPLIANCE_MATRIX: 'Compliance Matrix',
  PRICING_SUMMARY: 'Pricing Summary',
  OTHER: 'Other',
}

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-yellow-100 text-yellow-800',
  READY: 'bg-blue-100 text-blue-800',
  APPROVED: 'bg-green-100 text-green-800',
  LOCKED: 'bg-red-100 text-red-800',
}

export default function DocumentEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [doc, setDoc] = useState<BidDocumentData | null>(null)
  const [versions, setVersions] = useState<DocumentVersion[]>([])
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [changeSummary, setChangeSummary] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      Placeholder.configure({ placeholder: 'Start writing...' }),
    ],
    editorProps: {
      attributes: {
        class: 'prose prose-sm max-w-none focus:outline-none min-h-[400px] p-4',
      },
    },
  })

  const isLocked = doc?.status === 'LOCKED'

  const loadDocument = useCallback(async () => {
    if (!id) return
    try {
      setLoading(true)
      const [docRes, versionsRes] = await Promise.all([
        getBidDocument(id),
        getBidDocumentVersions(id),
      ])
      setDoc(docRes.data)
      setVersions(versionsRes.data)

      if (docRes.data.latestVersion?.contentJson && editor) {
        editor.commands.setContent(docRes.data.latestVersion.contentJson)
        setSelectedVersion(docRes.data.currentVersionNo)
      }
    } catch (err: unknown) {
      setError('문서를 불러올 수 없습니다.')
    } finally {
      setLoading(false)
    }
  }, [id, editor])

  useEffect(() => {
    if (editor) loadDocument()
  }, [editor, loadDocument])

  useEffect(() => {
    if (editor) editor.setEditable(!isLocked)
  }, [isLocked, editor])

  const handleSave = async () => {
    if (!id || !editor || isLocked) return
    try {
      setSaving(true)
      setError('')
      const contentJson = editor.getJSON()
      await saveDocumentVersion(id, {
        contentJson,
        changeSummary: changeSummary || 'Manual edit',
      })
      setChangeSummary('')
      setSuccess('저장되었습니다.')
      setTimeout(() => setSuccess(''), 3000)
      await loadDocument()
    } catch (err: unknown) {
      setError('저장 실패')
    } finally {
      setSaving(false)
    }
  }

  const handleVersionSelect = async (versionNo: number) => {
    if (!id || !editor) return
    try {
      const res = await getBidDocumentVersion(id, versionNo)
      editor.commands.setContent(res.data.contentJson)
      setSelectedVersion(versionNo)
    } catch {
      setError('버전을 불러올 수 없습니다.')
    }
  }

  const handleLock = async () => {
    if (!id) return
    try {
      await lockDocument(id)
      setSuccess('문서가 잠금되었습니다.')
      setTimeout(() => setSuccess(''), 3000)
      await loadDocument()
    } catch {
      setError('잠금 실패')
    }
  }

  const handleUnlock = async () => {
    if (!id) return
    try {
      await unlockDocument(id)
      setSuccess('잠금이 해제되었습니다.')
      setTimeout(() => setSuccess(''), 3000)
      await loadDocument()
    } catch {
      setError('잠금 해제 실패')
    }
  }

  const handleExportPdf = async () => {
    if (!id) return
    try {
      const res = await exportDocumentPdf(id)
      const blob = new Blob([res.data], { type: 'application/pdf' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${doc?.documentType?.toLowerCase() || 'document'}_v${doc?.currentVersionNo || 1}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('PDF 내보내기 실패')
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-secondary" />
      </div>
    )
  }

  if (!doc) {
    return (
      <div className="p-6">
        <p className="text-red-500">문서를 찾을 수 없습니다.</p>
        <button onClick={() => navigate(-1)} className="mt-4 text-secondary underline">
          뒤로 가기
        </button>
      </div>
    )
  }

  return (
    <div className="flex h-[calc(100vh-64px)]">
      {/* Main editor area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="border-b bg-white px-6 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate(-1)}
              className="text-gray-500 hover:text-gray-700"
            >
              ← 뒤로
            </button>
            <h1 className="text-lg font-semibold">
              {DOC_TYPE_LABELS[doc.documentType] || doc.documentType}
            </h1>
            <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLORS[doc.status] || 'bg-gray-100'}`}>
              {doc.status}
            </span>
          </div>
          <div className="flex items-center gap-2">
            {isLocked ? (
              <button
                onClick={handleUnlock}
                className="px-3 py-1.5 text-sm bg-yellow-500 text-white rounded hover:bg-yellow-600"
              >
                🔓 잠금 해제
              </button>
            ) : (
              <>
                <button
                  onClick={handleSave}
                  disabled={saving}
                  className="px-3 py-1.5 text-sm bg-secondary text-white rounded hover:bg-blue-600 disabled:opacity-50"
                >
                  {saving ? '저장 중...' : '💾 저장'}
                </button>
                <button
                  onClick={handleLock}
                  className="px-3 py-1.5 text-sm bg-red-500 text-white rounded hover:bg-red-600"
                >
                  🔒 잠금
                </button>
              </>
            )}
            <button
              onClick={handleExportPdf}
              className="px-3 py-1.5 text-sm bg-gray-600 text-white rounded hover:bg-gray-700"
            >
              📄 PDF
            </button>
          </div>
        </div>

        {/* Toolbar */}
        {!isLocked && editor && (
          <div className="border-b bg-gray-50 px-6 py-2 flex items-center gap-1 flex-wrap">
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleBold().run()}
              active={editor.isActive('bold')}
              label="B"
              className="font-bold"
            />
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleItalic().run()}
              active={editor.isActive('italic')}
              label="I"
              className="italic"
            />
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleUnderline().run()}
              active={editor.isActive('underline')}
              label="U"
              className="underline"
            />
            <div className="w-px h-5 bg-gray-300 mx-1" />
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
              active={editor.isActive('heading', { level: 1 })}
              label="H1"
            />
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
              active={editor.isActive('heading', { level: 2 })}
              label="H2"
            />
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
              active={editor.isActive('heading', { level: 3 })}
              label="H3"
            />
            <div className="w-px h-5 bg-gray-300 mx-1" />
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleBulletList().run()}
              active={editor.isActive('bulletList')}
              label="• List"
            />
            <ToolbarButton
              onClick={() => editor.chain().focus().toggleOrderedList().run()}
              active={editor.isActive('orderedList')}
              label="1. List"
            />
            <div className="w-px h-5 bg-gray-300 mx-1" />
            <ToolbarButton
              onClick={() => editor.chain().focus().setTextAlign('left').run()}
              active={editor.isActive({ textAlign: 'left' })}
              label="Left"
            />
            <ToolbarButton
              onClick={() => editor.chain().focus().setTextAlign('center').run()}
              active={editor.isActive({ textAlign: 'center' })}
              label="Center"
            />
            <ToolbarButton
              onClick={() => editor.chain().focus().setTextAlign('right').run()}
              active={editor.isActive({ textAlign: 'right' })}
              label="Right"
            />
          </div>
        )}

        {/* Messages */}
        {error && (
          <div className="mx-6 mt-2 p-2 bg-red-50 text-red-700 text-sm rounded">
            {error}
          </div>
        )}
        {success && (
          <div className="mx-6 mt-2 p-2 bg-green-50 text-green-700 text-sm rounded">
            {success}
          </div>
        )}

        {/* Editor */}
        <div className="flex-1 overflow-auto bg-white">
          <EditorContent editor={editor} />
        </div>

        {/* Change summary input */}
        {!isLocked && (
          <div className="border-t bg-gray-50 px-6 py-2 flex items-center gap-2">
            <input
              type="text"
              value={changeSummary}
              onChange={(e) => setChangeSummary(e.target.value)}
              placeholder="변경 사항 요약 (선택)"
              className="flex-1 px-3 py-1.5 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-secondary"
            />
          </div>
        )}
      </div>

      {/* Version sidebar */}
      <div className="w-64 border-l bg-gray-50 flex flex-col overflow-hidden">
        <div className="px-4 py-3 border-b bg-white">
          <h2 className="text-sm font-semibold text-gray-700">버전 이력</h2>
          <p className="text-xs text-gray-500 mt-0.5">
            총 {versions.length}개 버전
          </p>
        </div>
        <div className="flex-1 overflow-auto">
          {versions.map((v) => (
            <button
              key={v.versionNo}
              onClick={() => handleVersionSelect(v.versionNo)}
              className={`w-full text-left px-4 py-3 border-b hover:bg-blue-50 transition-colors ${
                selectedVersion === v.versionNo ? 'bg-blue-50 border-l-2 border-l-secondary' : ''
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-800">
                  {v.versionLabel}
                </span>
                {v.versionNo === doc.currentVersionNo && (
                  <span className="text-[10px] bg-secondary text-white px-1.5 py-0.5 rounded">
                    latest
                  </span>
                )}
              </div>
              {v.changeSummary && (
                <p className="text-xs text-gray-600 mt-1 line-clamp-2">
                  {v.changeSummary}
                </p>
              )}
              <p className="text-[10px] text-gray-400 mt-1">{v.editedAt}</p>
            </button>
          ))}
          {versions.length === 0 && (
            <p className="text-xs text-gray-400 p-4">버전이 없습니다.</p>
          )}
        </div>
      </div>
    </div>
  )
}

function ToolbarButton({
  onClick,
  active,
  label,
  className = '',
}: {
  onClick: () => void
  active: boolean
  label: string
  className?: string
}) {
  return (
    <button
      onClick={onClick}
      className={`px-2 py-1 text-xs rounded transition-colors ${className} ${
        active ? 'bg-secondary text-white' : 'bg-white text-gray-700 hover:bg-gray-200 border'
      }`}
    >
      {label}
    </button>
  )
}
