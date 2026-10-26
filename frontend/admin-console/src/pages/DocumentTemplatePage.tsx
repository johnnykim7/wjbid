import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Badge,
  Button,
  Card,
  CardHeader,
  CardTable,
  CardToolbar,
  Checkbox,
  ConfirmDialog,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  FieldError,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Spinner,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Textarea,
} from '@therecommerce-develop/ds-ui'
import {
  getDocumentTemplates,
  createDocumentTemplate,
  updateDocumentTemplate,
  deactivateDocumentTemplate,
  activateDocumentTemplate,
} from '../api/client'
import { ToneBadge } from '../components/StateBadge'
import { TableMessageRow } from '../components/LoadState'

const DOCUMENT_TYPES = [
  { value: 'NOTICE_VIEW',        label: '공고문 표시 양식 (Notice View), TipTap JSON 골격' },
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
  const [nameError, setNameError] = useState('')
  const [jsonError, setJsonError] = useState('')
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [deactivateTarget, setDeactivateTarget] = useState<Template | null>(null)

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
    setNameError('')
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
    setNameError('')
    setJsonError('')
    setError('')
    setEditingId(t.id)
    setShowModal(true)
  }

  const handleSave = async () => {
    if (!form.templateName.trim()) {
      setNameError('템플릿 이름을 입력하세요.')
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

  const handleDeactivate = async () => {
    if (!deactivateTarget) return
    await deactivateDocumentTemplate(deactivateTarget.id)
    setDeactivateTarget(null)
    load()
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
    <div className="space-y-4">
      {error && !showModal && (
        <Alert variant="destructive" appearance="light">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* 템플릿 목록 */}
      <Card>
        <CardHeader className="py-2.5">
          <CardToolbar className="ms-auto">
            <div className="flex items-center gap-2">
              <Checkbox
                id="template-include-inactive"
                checked={includeInactive}
                onCheckedChange={(v) => setIncludeInactive(v === true)}
              />
              <Label htmlFor="template-include-inactive" className="cursor-pointer font-normal">
                비활성 포함
              </Label>
            </div>
            <Button variant="primary" onClick={openModal}>
              <Plus />
              새 템플릿 등록
            </Button>
          </CardToolbar>
        </CardHeader>
        <CardTable>
          <Table>
            <TableHeader className="bg-muted">
              <TableRow>
                <TableHead>템플릿 이름</TableHead>
                <TableHead>문서 타입</TableHead>
                <TableHead className="text-center">버전</TableHead>
                <TableHead>등록일</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableMessageRow colSpan={5} loading />
              ) : templates.length === 0 ? (
                <TableMessageRow colSpan={5}>
                  등록된 템플릿이 없습니다.
                  <Button mode="link" variant="primary" className="ms-2 text-secondary" onClick={openModal}>
                    첫 번째 템플릿 등록하기
                  </Button>
                </TableMessageRow>
              ) : (
                templates.map((t) => (
                  <TableRow key={t.id} className={!t.active ? 'bg-muted/50 opacity-60' : undefined}>
                    <TableCell className="font-medium text-foreground">
                      <div className="flex items-center gap-2">
                        {t.templateName}
                        {!t.active && <ToneBadge tone="gray">비활성</ToneBadge>}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="info" appearance="light" className="whitespace-nowrap">
                        {docTypeLabel(t.documentType)}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-center text-muted-foreground">
                      v{t.templateVersion}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {t.createdAt ? new Date(t.createdAt).toLocaleDateString('ko-KR') : '-'}
                    </TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-2">
                        {t.active && (
                          <Button size="sm" variant="outline" onClick={() => openEditModal(t)}>
                            수정
                          </Button>
                        )}
                        {t.active ? (
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-destructive"
                            onClick={() => setDeactivateTarget(t)}
                          >
                            비활성화
                          </Button>
                        ) : (
                          <Button size="sm" variant="outline" onClick={() => handleActivate(t.id)}>
                            재활성화
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardTable>
      </Card>

      <ConfirmDialog
        open={deactivateTarget !== null}
        onOpenChange={(open) => { if (!open) setDeactivateTarget(null) }}
        title="템플릿 비활성화"
        description={`"${deactivateTarget?.templateName ?? ''}" 템플릿을 비활성화합니다.`}
        confirmLabel="비활성화"
        confirmVariant="destructive"
        onConfirm={handleDeactivate}
        errorText={() => '비활성화에 실패했습니다.'}
      />

      {/* 등록 모달 */}
      <Dialog open={showModal} onOpenChange={setShowModal}>
        {/* 바깥을 눌러도 닫지 않는다. 고쳐 쓰던 JSON 을 잃지 않게 */}
        <DialogContent className="max-h-[90vh] max-w-2xl" onInteractOutside={(e) => e.preventDefault()}>
          <DialogHeader>
            <DialogTitle>{editingId ? '템플릿 수정' : '새 템플릿 등록'}</DialogTitle>
          </DialogHeader>

          <DialogBody className="min-h-0 space-y-4 overflow-y-auto">
            {error && (
              <Alert variant="destructive" appearance="light">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            {editingId && (
              <Alert variant="warning" appearance="light">
                <AlertDescription>
                  저장하면 새 버전이 생성되고 현재 버전은 비활성화됩니다 (버전 불변성).
                </AlertDescription>
              </Alert>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="template-name">
                템플릿 이름 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="template-name"
                value={form.templateName}
                onChange={(e) => {
                  setForm({ ...form, templateName: e.target.value })
                  setNameError('')
                }}
                placeholder="예: 커버레터 기본 템플릿"
                aria-invalid={!!nameError}
                aria-describedby="template-name-error"
              />
              <FieldError id="template-name-error">{nameError}</FieldError>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="template-type">
                문서 타입 <span className="text-destructive">*</span>
              </Label>
              <Select
                value={form.documentType}
                onValueChange={(v) => setForm({ ...form, documentType: v })}
                disabled={!!editingId}
              >
                <SelectTrigger id="template-type" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {DOCUMENT_TYPES.map((d) => (
                    <SelectItem key={d.value} value={d.value}>{d.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {editingId && (
                <p className="text-xs text-muted-foreground">수정 시 문서 타입은 변경할 수 없습니다.</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="template-json">
                TipTap JSON <span className="text-destructive">*</span>
              </Label>
              <p className="text-xs text-muted-foreground">
                TipTap 에디터 형식의 JSON을 입력하세요. Claude가 이 구조를 기반으로 문서를 작성합니다.
              </p>
              <Textarea
                id="template-json"
                value={form.contentJsonStr}
                onChange={(e) => {
                  setForm({ ...form, contentJsonStr: e.target.value })
                  setJsonError('')
                }}
                rows={14}
                className="resize-none font-mono text-xs"
                spellCheck={false}
                aria-invalid={!!jsonError}
                aria-describedby="template-json-error"
              />
              <FieldError id="template-json-error">{jsonError}</FieldError>
            </div>
          </DialogBody>

          <DialogFooter>
            <Button variant="outline" onClick={() => setShowModal(false)}>
              취소
            </Button>
            <Button variant="primary" onClick={handleSave} disabled={saving}>
              {saving && <Spinner />}
              {saving ? '저장 중...' : '저장'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
