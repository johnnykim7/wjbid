import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogBody,
  DialogFooter,
} from '../components/ui/dialog'
import { Card } from '../components/ui/card'
import { Badge } from '../components/ui/badge'
import { StatCard } from '../components/ui/stat-card'
import {
  getMemberDocumentVault,
  uploadMemberDocument,
  deleteMemberDocument,
} from '../api/client'
import { DatePick } from '../components/expiry/DatePick'
import { daysLeftKst } from '../utils/kstDate'
import { EXPIRY_TEXT, VAULT_EXPIRY_TEXT as XT } from '../data/expiryContent'
import { VAULT_TEXT as VT } from '../data/vaultContent'

const EXPIRY_PATH = '/documents/expiry'

// ── CR-125: 회사 공통 서류함 (member 종속, BE 실연동) ──────────────
interface MemberDocumentDto {
  id: string
  category: string
  categoryLabel: string
  fixed: boolean
  fileName: string
  fileSize: number
  contentType: string
  storageUrl: string
  displayName?: string
  description?: string
  /** CR-136: YYYY-MM-DD */
  expiresOn?: string
  createdAt: string
  /** CR-147: 국문·영문 칸만 값이 있다 */
  language?: Language | null
}

type Language = 'KO' | 'EN'

/** 고정 칸 한 줄. 국문·영문 칸은 국문, 영문 두 줄이다 (CR-147, BIZ-065) */
interface SlotFile {
  language: Language | null
  document: MemberDocumentDto | null
}

interface FixedSlot {
  category: string
  categoryLabel: string
  files: SlotFile[]
}

interface VaultResponse {
  fixedSlots: FixedSlot[]
  others: MemberDocumentDto[]
}

/** 만료일 한 줄 (CR-136). 남은 날은 한국 날짜 기준 */
function ExpiryLine({ doc, withLink }: { doc: MemberDocumentDto; withLink?: boolean }) {
  if (!doc.expiresOn) {
    return (
      <p className="text-[11px] text-ink-mute">
        {XT.none}
        {withLink && <> · <Link to={EXPIRY_PATH} className="text-brand font-medium">{XT.setOnExpiryPage}</Link></>}
      </p>
    )
  }
  const left = daysLeftKst(doc.expiresOn)
  const label = left < 0 ? EXPIRY_TEXT.expired : left === 0 ? EXPIRY_TEXT.today : EXPIRY_TEXT.dday(left)
  return (
    <p className="text-[11px] text-ink-mute">
      {XT.label} {doc.expiresOn} · <span className={left < 0 ? 'text-danger font-bold' : 'font-medium text-ink-sub'}>{label}</span>
    </p>
  )
}

const slotKey = (category: string, language: Language | null) => `${category}:${language ?? ''}`

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export default function DocumentsPage() {
  const [vault, setVault] = useState<VaultResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [busyKey, setBusyKey] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // 고정 슬롯 업로드용 숨은 input
  const fixedInputRef = useRef<HTMLInputElement>(null)
  const pendingFixed = useRef<{ category: string; language: Language | null } | null>(null)

  // 기타 추가 모달
  const [otherOpen, setOtherOpen] = useState(false)
  const [otherTitle, setOtherTitle] = useState('')
  const [otherDesc, setOtherDesc] = useState('')
  const [otherFile, setOtherFile] = useState<File | null>(null)
  const [otherExpiry, setOtherExpiry] = useState<string | null>(null)
  const [otherSubmitting, setOtherSubmitting] = useState(false)

  const load = async () => {
    try {
      const { data } = await getMemberDocumentVault()
      setVault(data)
      setError(null)
    } catch {
      setError('서류함을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const stats = useMemo(() => {
    if (!vault) return { stored: 0, fixedMissing: 0 }
    const files = vault.fixedSlots.flatMap((s) => s.files)
    const fixedFilled = files.filter((f) => f.document).length
    return {
      stored: fixedFilled + vault.others.length,
      fixedMissing: files.length - fixedFilled,
    }
  }, [vault])

  // ── 고정 슬롯: 파일만 업로드/교체 ──
  const triggerFixedUpload = (category: string, language: Language | null) => {
    pendingFixed.current = { category, language }
    fixedInputRef.current?.click()
  }

  const handleFixedFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    const target = pendingFixed.current
    e.target.value = '' // 같은 파일 재선택 허용
    if (!file || !target) return
    setBusyKey(slotKey(target.category, target.language))
    try {
      await uploadMemberDocument(file, target.category, undefined, undefined, undefined, target.language ?? undefined)
      await load()
    } catch {
      setError('업로드에 실패했습니다.')
    } finally {
      setBusyKey(null)
    }
  }

  const handleDelete = async (doc: MemberDocumentDto) => {
    if (!window.confirm(`"${doc.displayName || doc.fileName}" 서류를 삭제하시겠습니까?`)) return
    setBusyKey(doc.id)
    try {
      await deleteMemberDocument(doc.id)
      await load()
    } catch {
      setError('삭제에 실패했습니다.')
    } finally {
      setBusyKey(null)
    }
  }

  // ── 기타 서류: 제목 + 파일 + 설명 모달 ──
  const submitOther = async () => {
    if (!otherTitle.trim() || !otherFile) {
      setError('서류 이름과 파일을 모두 입력하세요.')
      return
    }
    setOtherSubmitting(true)
    try {
      await uploadMemberDocument(otherFile, 'OTHER', otherTitle.trim(), otherDesc.trim() || undefined,
        otherExpiry ?? undefined)
      setOtherOpen(false)
      setOtherTitle('')
      setOtherDesc('')
      setOtherFile(null)
      setOtherExpiry(null)
      await load()
    } catch {
      setError('등록에 실패했습니다.')
    } finally {
      setOtherSubmitting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="mb-6 flex items-end justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-ink">서류함</h2>
          <p className="text-sm text-ink-mute mt-1">{VT.desc}</p>
          <p className="text-xs text-ink-mute mt-1">{VT.excluded}</p>
        </div>
        <Button variant="outline" size="sm" asChild>
          <Link to={EXPIRY_PATH}><i className="fa-regular fa-calendar mr-1.5" />{XT.shortcut}</Link>
        </Button>
      </div>

      {error && (
        <div className="rounded-lg bg-danger-soft border border-danger/30 px-4 py-2.5 text-sm text-danger">
          <i className="fa-solid fa-circle-exclamation mr-1.5" />
          {error}
        </div>
      )}

      {/* 요약 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-2">
        <StatCard
          label="보관 중"
          value={stats.stored}
          unit="건"
          icon="fa-solid fa-folder-open"
          iconBg="bg-success-soft"
          iconColor="text-success"
        />
        <StatCard
          label="공통 서류 미등록"
          value={stats.fixedMissing}
          unit="건"
          icon="fa-solid fa-triangle-exclamation"
          iconBg="bg-warning-soft"
          iconColor="text-warning"
          dark={stats.fixedMissing > 0}
        />
      </div>

      {/* 숨은 파일 input (고정 슬롯 공용) */}
      <input
        ref={fixedInputRef}
        type="file"
        className="hidden"
        onChange={handleFixedFile}
      />

      {/* 고정 카테고리 슬롯 */}
      <div>
        <h3 className="text-sm font-bold text-ink-sub mb-3">공통 서류</h3>
        {loading ? (
          <div className="text-sm text-ink-mute py-8 text-center">
            <i className="fa-solid fa-spinner fa-spin mr-2" />불러오는 중…
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {vault?.fixedSlots.map((slot) => {
              const filled = slot.files.filter((f) => f.document).length
              const hint = VT.slotHint[slot.category]
              return (
                <Card key={slot.category} className="p-5 flex flex-col">
                  <div className="flex items-start justify-between gap-2">
                    <h4 className="text-sm font-bold text-ink truncate">{slot.categoryLabel}</h4>
                    {filled === slot.files.length ? (
                      <Badge variant="active" size="sm">
                        <i className="fa-solid fa-check mr-1" /> 완료
                      </Badge>
                    ) : filled > 0 ? (
                      <Badge variant="pending" size="sm">{VT.partial}</Badge>
                    ) : (
                      <Badge variant="closing" size="sm">
                        미등록
                      </Badge>
                    )}
                  </div>
                  {hint && <p className="text-[11px] text-ink-mute mt-1">{hint}</p>}

                  <div className="flex-1 mt-3 space-y-4">
                    {slot.files.map((f) => {
                      const key = slotKey(slot.category, f.language)
                      const doc = f.document
                      const busy = busyKey === key || (doc && busyKey === doc.id)
                      return (
                        <div key={key} className="space-y-2">
                          {f.language && <p className="text-xs font-bold text-ink-sub">{VT.language[f.language]}</p>}
                          {doc ? (
                            <div className="flex items-center gap-2 rounded-lg bg-surface border border-line-soft px-3 py-2">
                              <i className="fa-solid fa-file-lines text-brand" />
                              <div className="min-w-0">
                                <p className="text-xs font-medium text-ink-sub truncate">{doc.fileName}</p>
                                <p className="text-[11px] text-ink-mute">
                                  {formatSize(doc.fileSize)} · {doc.createdAt?.slice(0, 10)}
                                </p>
                                <ExpiryLine doc={doc} withLink />
                              </div>
                            </div>
                          ) : (
                            <div className="flex items-center justify-center rounded-lg border border-dashed border-line px-3 py-4 text-xs text-ink-mute">
                              <i className="fa-solid fa-cloud-arrow-up mr-1.5" /> 파일을 업로드하세요
                            </div>
                          )}
                          <div className="flex gap-2">
                            <Button
                              variant={doc ? 'outline' : 'primary'}
                              size="sm"
                              className="flex-1"
                              disabled={!!busy}
                              onClick={() => triggerFixedUpload(slot.category, f.language)}
                            >
                              <i
                                className={`fa-solid ${busy ? 'fa-spinner fa-spin' : doc ? 'fa-rotate' : 'fa-arrow-up-from-bracket'} mr-1.5`}
                              />
                              {doc ? '교체' : '업로드'}
                            </Button>
                            {doc && (
                              <Button
                                variant="ghost"
                                size="icon"
                                className="text-ink-mute hover:text-danger"
                                title="삭제"
                                disabled={!!busy}
                                onClick={() => handleDelete(doc)}
                              >
                                <i className="fa-regular fa-trash-can" />
                              </Button>
                            )}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </div>

      {/* 기타 서류 */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-bold text-ink-sub">기타 서류</h3>
          <Button variant="outline" size="sm" onClick={() => setOtherOpen(true)}>
            <i className="fa-solid fa-plus mr-1.5" /> 기타 서류 추가
          </Button>
        </div>

        {vault && vault.others.length > 0 ? (
          <div className="space-y-2">
            {vault.others.map((doc) => (
              <Card key={doc.id} className="px-4 py-3 flex items-center gap-3">
                <i className="fa-solid fa-file-lines text-brand text-lg shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-ink truncate">{doc.displayName || doc.fileName}</p>
                  <p className="text-xs text-ink-mute truncate">
                    {doc.fileName} · {formatSize(doc.fileSize)}
                    {doc.description ? ` · ${doc.description}` : ''}
                  </p>
                  <ExpiryLine doc={doc} />
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-ink-mute hover:text-danger shrink-0"
                  title="삭제"
                  disabled={busyKey === doc.id}
                  onClick={() => handleDelete(doc)}
                >
                  <i className="fa-regular fa-trash-can" />
                </Button>
              </Card>
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-dashed border-line px-4 py-6 text-center text-xs text-ink-mute">
            등록된 기타 서류가 없습니다.
          </div>
        )}
      </div>

      {/* 기타 서류 추가 모달 */}
      <Dialog open={otherOpen} onOpenChange={(o) => !otherSubmitting && setOtherOpen(o)}>
        <DialogContent showCloseButton={!otherSubmitting}>
          <DialogHeader>
            <DialogTitle>기타 서류 추가</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-ink-sub mb-1">
                서류 이름 <span className="text-danger">*</span>
              </label>
              <input
                type="text"
                value={otherTitle}
                onChange={(e) => setOtherTitle(e.target.value)}
                placeholder="예: 품질인증서"
                className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand/40"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-ink-sub mb-1">
                파일 <span className="text-danger">*</span>
              </label>
              <input
                type="file"
                onChange={(e) => setOtherFile(e.target.files?.[0] ?? null)}
                className="w-full text-sm text-ink-sub file:mr-3 file:rounded-md file:border-0 file:bg-surface file:px-3 file:py-1.5 file:text-sm file:font-medium hover:file:bg-line"
              />
              {otherFile && (
                <p className="mt-1 text-[11px] text-ink-mute">
                  {otherFile.name} · {formatSize(otherFile.size)}
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-ink-sub mb-1">{XT.field}</label>
              <div className="flex items-center gap-2">
                <DatePick plain value={otherExpiry} onSave={setOtherExpiry} label={XT.field} />
                {otherExpiry && (
                  <Button variant="ghost" size="sm" onClick={() => setOtherExpiry(null)}>{XT.clear}</Button>
                )}
              </div>
              <p className="mt-1 text-[11px] text-ink-mute">{XT.fieldHint}</p>
            </div>
            <div>
              <label className="block text-xs font-medium text-ink-sub mb-1">설명</label>
              <textarea
                value={otherDesc}
                onChange={(e) => setOtherDesc(e.target.value)}
                rows={2}
                placeholder="간단한 메모 (선택)"
                className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand/40 resize-none"
              />
            </div>
          </DialogBody>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setOtherOpen(false)} disabled={otherSubmitting}>
              취소
            </Button>
            <Button variant="primary" onClick={submitOther} disabled={otherSubmitting}>
              {otherSubmitting ? (
                <>
                  <i className="fa-solid fa-spinner fa-spin mr-1.5" /> 등록 중…
                </>
              ) : (
                '등록'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
