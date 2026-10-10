import { useEffect, useMemo, useRef, useState } from 'react'
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
  createdAt: string
}

interface FixedSlot {
  category: string
  categoryLabel: string
  document: MemberDocumentDto | null
}

interface VaultResponse {
  fixedSlots: FixedSlot[]
  others: MemberDocumentDto[]
}

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
  const pendingFixedCategory = useRef<string | null>(null)

  // 기타 추가 모달
  const [otherOpen, setOtherOpen] = useState(false)
  const [otherTitle, setOtherTitle] = useState('')
  const [otherDesc, setOtherDesc] = useState('')
  const [otherFile, setOtherFile] = useState<File | null>(null)
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
    const fixedFilled = vault.fixedSlots.filter((s) => s.document).length
    return {
      stored: fixedFilled + vault.others.length,
      fixedMissing: vault.fixedSlots.length - fixedFilled,
    }
  }, [vault])

  // ── 고정 슬롯: 파일만 업로드/교체 ──
  const triggerFixedUpload = (category: string) => {
    pendingFixedCategory.current = category
    fixedInputRef.current?.click()
  }

  const handleFixedFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    const category = pendingFixedCategory.current
    e.target.value = '' // 같은 파일 재선택 허용
    if (!file || !category) return
    setBusyKey(category)
    try {
      await uploadMemberDocument(file, category)
      await load()
    } catch {
      setError('업로드에 실패했습니다.')
    } finally {
      setBusyKey(null)
    }
  }

  const handleDelete = async (doc: MemberDocumentDto) => {
    if (!window.confirm(`"${doc.displayName || doc.fileName}" 서류를 삭제할까요?`)) return
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
      setError('문서제목과 파일을 모두 입력하세요.')
      return
    }
    setOtherSubmitting(true)
    try {
      await uploadMemberDocument(otherFile, 'OTHER', otherTitle.trim(), otherDesc.trim() || undefined)
      setOtherOpen(false)
      setOtherTitle('')
      setOtherDesc('')
      setOtherFile(null)
      await load()
    } catch {
      setError('등록에 실패했습니다.')
    } finally {
      setOtherSubmitting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-gray-800">서류함</h2>
        <p className="text-sm text-gray-500 mt-1">
          사업자등록증·회사소개서 등 공통 서류를 한 번 등록해 두면 제안서 작성 시 자동으로 채워집니다.
        </p>
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-2.5 text-sm text-red-600">
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
          iconBg="bg-emerald-100"
          iconColor="text-emerald-600"
        />
        <StatCard
          label="고정 서류 미등록"
          value={stats.fixedMissing}
          unit="건"
          icon="fa-solid fa-triangle-exclamation"
          iconBg="bg-amber-100"
          iconColor="text-amber-600"
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
        <h3 className="text-sm font-bold text-gray-700 mb-3">공통 서류</h3>
        {loading ? (
          <div className="text-sm text-gray-400 py-8 text-center">
            <i className="fa-solid fa-spinner fa-spin mr-2" />불러오는 중…
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {vault?.fixedSlots.map((slot) => {
              const doc = slot.document
              const done = !!doc
              const busy = busyKey === slot.category || (doc && busyKey === doc.id)
              return (
                <Card key={slot.category} className="p-5 flex flex-col">
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <h4 className="text-sm font-bold text-gray-800 truncate">{slot.categoryLabel}</h4>
                    {done ? (
                      <Badge variant="active" size="sm">
                        <i className="fa-solid fa-check mr-1" /> 완료
                      </Badge>
                    ) : (
                      <Badge variant="closing" size="sm">
                        미등록
                      </Badge>
                    )}
                  </div>

                  <div className="flex-1 my-3">
                    {done ? (
                      <div className="flex items-center gap-2 rounded-lg bg-gray-50 border border-gray-100 px-3 py-2">
                        <i className="fa-solid fa-file-lines text-secondary" />
                        <div className="min-w-0">
                          <p className="text-xs font-medium text-gray-700 truncate">{doc!.fileName}</p>
                          <p className="text-[11px] text-gray-400">
                            {formatSize(doc!.fileSize)} · {doc!.createdAt?.slice(0, 10)}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-center justify-center rounded-lg border border-dashed border-gray-200 px-3 py-4 text-xs text-gray-400">
                        <i className="fa-solid fa-cloud-arrow-up mr-1.5" /> 파일을 업로드하세요
                      </div>
                    )}
                  </div>

                  <div className="flex gap-2">
                    <Button
                      variant={done ? 'outline' : 'primary'}
                      size="sm"
                      className="flex-1"
                      disabled={!!busy}
                      onClick={() => triggerFixedUpload(slot.category)}
                    >
                      <i
                        className={`fa-solid ${busy ? 'fa-spinner fa-spin' : done ? 'fa-rotate' : 'fa-arrow-up-from-bracket'} mr-1.5`}
                      />
                      {done ? '교체' : '업로드'}
                    </Button>
                    {done && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="text-gray-400 hover:text-red-500"
                        title="삭제"
                        disabled={!!busy}
                        onClick={() => handleDelete(doc!)}
                      >
                        <i className="fa-regular fa-trash-can" />
                      </Button>
                    )}
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
          <h3 className="text-sm font-bold text-gray-700">기타 서류</h3>
          <Button variant="outline" size="sm" onClick={() => setOtherOpen(true)}>
            <i className="fa-solid fa-plus mr-1.5" /> 기타 서류 추가
          </Button>
        </div>

        {vault && vault.others.length > 0 ? (
          <div className="space-y-2">
            {vault.others.map((doc) => (
              <Card key={doc.id} className="px-4 py-3 flex items-center gap-3">
                <i className="fa-solid fa-file-lines text-secondary text-lg shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-gray-800 truncate">{doc.displayName || doc.fileName}</p>
                  <p className="text-xs text-gray-400 truncate">
                    {doc.fileName} · {formatSize(doc.fileSize)}
                    {doc.description ? ` · ${doc.description}` : ''}
                  </p>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-gray-400 hover:text-red-500 shrink-0"
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
          <div className="rounded-lg border border-dashed border-gray-200 px-4 py-6 text-center text-xs text-gray-400">
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
              <label className="block text-xs font-medium text-gray-600 mb-1">
                문서제목 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={otherTitle}
                onChange={(e) => setOtherTitle(e.target.value)}
                placeholder="예: 품질인증서"
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-secondary/40"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">
                파일 <span className="text-red-500">*</span>
              </label>
              <input
                type="file"
                onChange={(e) => setOtherFile(e.target.files?.[0] ?? null)}
                className="w-full text-sm text-gray-600 file:mr-3 file:rounded-md file:border-0 file:bg-gray-100 file:px-3 file:py-1.5 file:text-sm file:font-medium hover:file:bg-gray-200"
              />
              {otherFile && (
                <p className="mt-1 text-[11px] text-gray-400">
                  {otherFile.name} · {formatSize(otherFile.size)}
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">설명</label>
              <textarea
                value={otherDesc}
                onChange={(e) => setOtherDesc(e.target.value)}
                rows={2}
                placeholder="간단한 메모 (선택)"
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-secondary/40 resize-none"
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
