import { useState, useEffect, useCallback } from 'react'
import type { ReactNode } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Placeholder from '@tiptap/extension-placeholder'
import {
  Bold,
  FileDown,
  Heading1,
  Heading2,
  Heading3,
  Italic,
  List,
  ListOrdered,
  ListTree,
  Lock,
  LockOpen,
  Save,
  TextAlignCenter,
  TextAlignEnd,
  TextAlignStart,
  Underline as UnderlineIcon,
} from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Badge,
  Button,
  Card,
  CardFooter,
  CardHeader,
  CardTitle,
  Input,
  PageHeader,
  Separator,
  Spinner,
  Toggle,
} from '@therecommerce-develop/ds-ui'
import {
  getBidDocument,
  getBidDocumentVersions,
  getBidDocumentVersion,
  saveDocumentVersion,
  lockDocument,
  unlockDocument,
  exportDocumentPdf,
} from '../api/client'
import { ToneBadge } from '../components/StateBadge'
import { Loading } from '../components/LoadState'
import { docTypeLabel } from '../utils/documentType'
import type { StateTone } from '../utils/bidState'

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

const STATUS_TONE: Record<string, StateTone> = {
  DRAFT: 'warning',
  READY: 'info',
  APPROVED: 'success',
  LOCKED: 'destructive',
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
    } catch {
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
    } catch {
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

  if (loading) return <Loading className="h-64" />

  if (!doc) {
    return (
      <div className="space-y-4">
        <PageHeader variant="detail" back={{ onClick: () => navigate(-1) }} />
        <Alert variant="destructive" appearance="light">
          <AlertDescription>문서를 찾을 수 없습니다.</AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <PageHeader
        variant="detail"
        back={{ onClick: () => navigate(-1) }}
        title={docTypeLabel(doc.documentType)}
        badge={<ToneBadge tone={STATUS_TONE[doc.status] ?? 'gray'}>{doc.status}</ToneBadge>}
        actions={
          <>
            {isLocked ? (
              <Button variant="outline" onClick={handleUnlock}>
                <LockOpen />
                잠금 해제
              </Button>
            ) : (
              <Button variant="outline" onClick={handleLock}>
                <Lock />
                잠금
              </Button>
            )}
            <Button variant="outline" onClick={() => navigate(`/documents/${id}/sections`)}>
              <ListTree />
              Section 관리
            </Button>
            <Button variant="outline" onClick={handleExportPdf}>
              <FileDown />
              PDF
            </Button>
            {!isLocked && (
              <Button variant="primary" onClick={handleSave} disabled={saving}>
                {saving ? <Spinner /> : <Save />}
                {saving ? '저장 중...' : '저장'}
              </Button>
            )}
          </>
        }
      />

      {/* 머리줄, 본문 여백, PageHeader 를 뺀 높이로 두어 편집기와 버전 이력이 각자 안에서만 스크롤한다 */}
      <div className="flex h-[calc(100vh-9rem)] min-h-[480px] gap-4">
        {/* Main editor area */}
        <Card className="min-w-0 flex-1 overflow-hidden">
          {/* Toolbar */}
          {!isLocked && editor && (
            <CardHeader className="justify-start gap-1 py-1.5">
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleBold().run()}
                active={editor.isActive('bold')}
                label="Bold"
              >
                <Bold />
              </ToolbarButton>
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleItalic().run()}
                active={editor.isActive('italic')}
                label="Italic"
              >
                <Italic />
              </ToolbarButton>
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleUnderline().run()}
                active={editor.isActive('underline')}
                label="Underline"
              >
                <UnderlineIcon />
              </ToolbarButton>
              <Separator orientation="vertical" className="mx-1 h-5" />
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
                active={editor.isActive('heading', { level: 1 })}
                label="Heading 1"
              >
                <Heading1 />
              </ToolbarButton>
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
                active={editor.isActive('heading', { level: 2 })}
                label="Heading 2"
              >
                <Heading2 />
              </ToolbarButton>
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
                active={editor.isActive('heading', { level: 3 })}
                label="Heading 3"
              >
                <Heading3 />
              </ToolbarButton>
              <Separator orientation="vertical" className="mx-1 h-5" />
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleBulletList().run()}
                active={editor.isActive('bulletList')}
                label="Bullet List"
              >
                <List />
              </ToolbarButton>
              <ToolbarButton
                onClick={() => editor.chain().focus().toggleOrderedList().run()}
                active={editor.isActive('orderedList')}
                label="Numbered List"
              >
                <ListOrdered />
              </ToolbarButton>
              <Separator orientation="vertical" className="mx-1 h-5" />
              <ToolbarButton
                onClick={() => editor.chain().focus().setTextAlign('left').run()}
                active={editor.isActive({ textAlign: 'left' })}
                label="Left"
              >
                <TextAlignStart />
              </ToolbarButton>
              <ToolbarButton
                onClick={() => editor.chain().focus().setTextAlign('center').run()}
                active={editor.isActive({ textAlign: 'center' })}
                label="Center"
              >
                <TextAlignCenter />
              </ToolbarButton>
              <ToolbarButton
                onClick={() => editor.chain().focus().setTextAlign('right').run()}
                active={editor.isActive({ textAlign: 'right' })}
                label="Right"
              >
                <TextAlignEnd />
              </ToolbarButton>
            </CardHeader>
          )}

          {/* Messages */}
          {(error || success) && (
            <div className="space-y-2 px-4 pt-3">
              {error && (
                <Alert variant="destructive" appearance="light" size="sm">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
              {success && (
                <Alert variant="success" appearance="light" size="sm">
                  <AlertDescription>{success}</AlertDescription>
                </Alert>
              )}
            </div>
          )}

          {/* Editor */}
          <div className="min-h-0 flex-1 overflow-auto">
            <EditorContent editor={editor} />
          </div>

          {/* Change summary input */}
          {!isLocked && (
            <CardFooter className="py-2">
              <Input
                value={changeSummary}
                onChange={(e) => setChangeSummary(e.target.value)}
                placeholder="변경 사항 요약 (선택)"
              />
            </CardFooter>
          )}
        </Card>

        {/* Version sidebar */}
        <Card className="w-64 shrink-0 overflow-hidden">
          <CardHeader className="flex-col items-start justify-center gap-1 py-2.5">
            <CardTitle>버전 이력</CardTitle>
            <p className="text-xs text-muted-foreground">총 {versions.length}개 버전</p>
          </CardHeader>
          <div className="min-h-0 flex-1 overflow-auto">
            {versions.map((v) => (
              <Button
                key={v.versionNo}
                variant="ghost"
                onClick={() => handleVersionSelect(v.versionNo)}
                className={`h-auto w-full flex-col items-stretch gap-0 whitespace-normal rounded-none border-b border-border px-4 py-3 text-left font-normal ${
                  selectedVersion === v.versionNo ? 'border-l-2 border-l-secondary bg-accent' : ''
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-foreground">
                    {v.versionLabel}
                  </span>
                  {v.versionNo === doc.currentVersionNo && (
                    <Badge variant="secondary" appearance="light" size="xs" className="whitespace-nowrap">
                      latest
                    </Badge>
                  )}
                </div>
                {v.changeSummary && (
                  <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                    {v.changeSummary}
                  </p>
                )}
                <p className="mt-1 text-[10px] text-muted-foreground">{v.editedAt}</p>
              </Button>
            ))}
            {versions.length === 0 && (
              <p className="p-4 text-xs text-muted-foreground">버전이 없습니다.</p>
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}

function ToolbarButton({
  onClick,
  active,
  label,
  children,
}: {
  onClick: () => void
  active: boolean
  label: string
  children: ReactNode
}) {
  return (
    <Toggle size="sm" pressed={active} onPressedChange={onClick} aria-label={label} title={label}>
      {children}
    </Toggle>
  )
}
