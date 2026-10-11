/**
 * CR-153 — 고객 화면 사진 (T3-3 「고객 화면 사진」, T3-4 「고객 화면 사진」).
 * 사진 자리마다 여러 장을 올려 두면 고객 화면이 손님이 들어올 때마다 한 장을 고른다. 사진을 바꾸는 데 배포가 필요 없다.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Badge,
  Button,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Spinner,
  Switch,
} from '@therecommerce-develop/ds-ui'
import {
  deleteAdminSitePhoto,
  getAdminSitePhotos,
  setAdminSitePhotoVisible,
  uploadAdminSitePhoto,
  type AdminSitePhoto,
  type AdminSitePhotoSlot,
  type SitePhotoRules,
} from '../api/client'

const T = {
  title: '고객 화면 사진',
  desc: '사진 자리마다 여러 장을 올려 두면 손님이 들어올 때마다 그 자리의 사진 중 한 장을 골라 보여줍니다. 바꾼 사진은 손님이 다음에 들어올 때부터 보입니다.',
  slots: (n: number) => `사진 자리 ${n}곳`,
  slotsCount: '보임 / 전체',
  counts: (visible: number, hidden: number) => `보임 ${visible}장 · 숨김 ${hidden}장`,
  upload: '사진 올리기',
  rules: (r: SitePhotoRules) =>
    `${r.allowedTypes.map((t) => t.slice(t.indexOf('/') + 1).toUpperCase()).join(', ')} · 한 장 ${r.maxUploadMb}MB 까지 · 가로 ${r.minWidth} 이상. ` +
    `큰 사진은 가로 ${r.saveWidth}, ${r.saveMaxKb}KB 안팎으로 줄여 저장합니다. 가로로 긴(3:2) 사진이 자리에 잘 맞습니다.`,
  bundled: '처음부터 있던 사진',
  uploadedOn: (createdAt: string) => `${createdAt.slice(5, 10).replace('-', '/')} 올림`,
  resizedFrom: (w: number, h: number) => `${w}x${h} 에서 `,
  visible: '보임',
  hidden: '숨김',
  uploading: '크기를 줄여 저장하는 중',
  uploadFail: '올리지 못했습니다.',
  close: '닫기',
  emptyTitle: '보이는 사진이 없습니다',
  emptyDesc: '지금 고객 화면에는 이 자리에 사진 없이 바탕색만 보입니다.',
  loadFail: '불러오지 못했습니다.',
  retry: '다시 시도',
  toggleFail: '보임을 바꾸지 못했습니다.',
  remove: '지우기',
  confirmTitle: '사진을 지울까요?',
  confirm: (slotName: string) =>
    `${slotName}에서 이 사진을 지웁니다. 고객 화면에서 바로 빠지고 되돌릴 수 없습니다. 잠시 빼 두려면 「숨김」을 쓰세요.`,
  deleteFail: '지우지 못했습니다.',
}

interface UploadItem {
  id: number
  slotKey: string
  name: string
  size: number
  error: string | null
}

const serverMessage = (e: unknown): string | undefined =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message

const kb = (bytes: number) => `${Math.round(bytes / 1024)}KB`
const mbOrKb = (bytes: number) => (bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)}MB` : kb(bytes))

export default function SitePhotoAdminPage() {
  const [rules, setRules] = useState<SitePhotoRules | null>(null)
  const [slots, setSlots] = useState<AdminSitePhotoSlot[] | null>(null)
  const [failed, setFailed] = useState(false)
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [uploads, setUploads] = useState<UploadItem[]>([])
  const [actionError, setActionError] = useState<string | null>(null)
  const [removing, setRemoving] = useState<AdminSitePhoto | null>(null)
  const [removeError, setRemoveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const fileInput = useRef<HTMLInputElement>(null)
  const uploadSeq = useRef(0)

  const load = useCallback(() => {
    getAdminSitePhotos()
      .then(({ data }) => {
        setRules(data.rules)
        setSlots(data.slots)
        setFailed(false)
      })
      .catch(() => setFailed(true))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const selected = slots?.find((s) => s.key === selectedKey) ?? slots?.[0] ?? null

  // 고른 파일을 한 장씩 차례로 올린다. 서버가 한 장마다 줄여 다시 저장하므로 한꺼번에 보내지 않는다
  const uploadFiles = async (slotKey: string, files: File[]) => {
    const items = files.map((f) => ({ id: ++uploadSeq.current, slotKey, name: f.name, size: f.size, error: null }))
    setUploads((u) => [...u, ...items])
    for (const [i, file] of files.entries()) {
      const item = items[i]
      try {
        await uploadAdminSitePhoto(slotKey, file)
        setUploads((u) => u.filter((x) => x.id !== item.id))
        load()
      } catch (e) {
        setUploads((u) => u.map((x) => (x.id === item.id ? { ...x, error: serverMessage(e) ?? T.uploadFail } : x)))
      }
    }
  }

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? [])
    e.target.value = ''
    if (selected && files.length > 0) void uploadFiles(selected.key, files)
  }

  const toggle = async (photo: AdminSitePhoto, visible: boolean) => {
    setActionError(null)
    const patch = (v: boolean) =>
      setSlots((s) => s?.map((slot) => ({ ...slot, photos: slot.photos.map((p) => (p.id === photo.id ? { ...p, visible: v } : p)) })) ?? null)
    patch(visible)
    try {
      await setAdminSitePhotoVisible(photo.id, visible)
    } catch (e) {
      patch(!visible)
      setActionError(serverMessage(e) ?? T.toggleFail)
    }
  }

  const remove = async () => {
    if (!removing) return
    setSaving(true)
    try {
      await deleteAdminSitePhoto(removing.id)
      setRemoving(null)
      load()
    } catch (e) {
      setRemoveError(serverMessage(e) ?? T.deleteFail)
    } finally {
      setSaving(false)
    }
  }

  const visibleCount = (slot: AdminSitePhotoSlot) => slot.photos.filter((p) => p.visible).length
  const slotUploads = uploads.filter((u) => u.slotKey === selected?.key)

  return (
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">{T.title}</h1>
        <p className="text-sm text-gray-500 mt-0.5">{T.desc}</p>
      </div>

      {failed && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-600 flex items-center gap-3">
          <i className="fa-solid fa-triangle-exclamation" /> {T.loadFail}
          <Button variant="outline" size="sm" onClick={load}>{T.retry}</Button>
        </div>
      )}
      {actionError && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-3 text-sm text-red-600 flex items-center gap-3">
          <i className="fa-solid fa-triangle-exclamation" /> {actionError}
        </div>
      )}

      {slots && (
        <div className="flex gap-5 items-start">
          <div className="w-80 flex-shrink-0 bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
              <span className="text-sm font-bold text-gray-900">{T.slots(slots.length)}</span>
              <span className="text-xs text-gray-400">{T.slotsCount}</span>
            </div>
            {slots.map((slot) => {
              const cover = slot.photos.find((p) => p.visible)
              const shown = visibleCount(slot)
              const active = slot.key === selected?.key
              return (
                <Button
                  key={slot.key}
                  variant="ghost"
                  onClick={() => setSelectedKey(slot.key)}
                  className={`w-full h-auto justify-start gap-3 rounded-none px-4 py-2.5 text-left font-normal border-b border-gray-100 last:border-b-0 border-l-[3px] ${
                    active ? 'bg-blue-50 border-l-secondary hover:bg-blue-50' : 'border-l-transparent hover:bg-gray-50'
                  }`}
                >
                  <div className="w-14 h-9 flex-shrink-0 rounded bg-gray-100 overflow-hidden flex items-center justify-center">
                    {cover ? <img src={cover.url} alt="" className="w-full h-full object-cover" /> : <i className="fa-regular fa-image text-gray-300" />}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-semibold text-gray-900 truncate">{slot.name}</div>
                    <div className="text-xs text-gray-500 truncate">{slot.location}</div>
                  </div>
                  <span className={`text-xs font-semibold ${shown === 0 ? 'text-amber-600' : 'text-secondary'}`}>
                    {shown} / {slot.photos.length}
                  </span>
                </Button>
              )
            })}
          </div>

          {selected && rules && (
            <div className="flex-1 min-w-0 bg-white rounded-xl border border-gray-200">
              <div className="flex items-start justify-between gap-4 px-5 py-4 border-b border-gray-200">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-base font-bold text-gray-900">{selected.name}</h2>
                    <Badge className="bg-blue-50 text-secondary font-semibold">
                      {T.counts(visibleCount(selected), selected.photos.length - visibleCount(selected))}
                    </Badge>
                  </div>
                  <p className="text-sm text-gray-500 mt-1">{selected.location}</p>
                </div>
                <Button variant="primary" onClick={() => fileInput.current?.click()}>
                  <i className="fa-solid fa-upload" /> {T.upload}
                </Button>
                <input ref={fileInput} type="file" multiple accept={rules.allowedTypes.join(',')} className="hidden" onChange={onPick} />
              </div>

              <div className="p-5 space-y-4">
                <div className="flex items-start gap-2 rounded-lg bg-gray-50 border border-gray-200 px-3 py-2 text-xs text-gray-600">
                  <i className="fa-solid fa-circle-info mt-0.5 text-gray-400" /> {T.rules(rules)}
                </div>

                {visibleCount(selected) === 0 && (
                  <div className="rounded-xl border border-dashed border-amber-300 bg-amber-50/50 py-8 text-center">
                    <i className="fa-regular fa-image text-2xl text-amber-500" />
                    <div className="mt-2 text-sm font-semibold text-gray-900">{T.emptyTitle}</div>
                    <div className="text-xs text-gray-500 mt-0.5">{T.emptyDesc}</div>
                  </div>
                )}

                {(selected.photos.length > 0 || slotUploads.length > 0) && (
                  <div className="grid grid-cols-3 gap-4">
                    {selected.photos.map((p, i) => (
                      <div key={p.id} className={`rounded-xl border overflow-hidden ${p.visible ? 'border-gray-200' : 'border-dashed border-gray-300'}`}>
                        <div className={`relative aspect-[3/2] bg-gray-100 ${p.visible ? '' : 'opacity-50'}`}>
                          <img src={p.url} alt="" className="w-full h-full object-cover" />
                          <span className="absolute left-2 top-2 rounded bg-gray-900/80 px-1.5 text-xs font-semibold text-white">{i + 1}</span>
                          {!p.visible && (
                            <span className="absolute right-2 top-2 rounded bg-white/90 px-1.5 text-xs text-gray-700">
                              <i className="fa-regular fa-eye-slash" /> {T.hidden}
                            </span>
                          )}
                        </div>
                        <div className="px-3 py-2.5 space-y-2">
                          <div>
                            <div className="text-sm font-medium text-gray-900 truncate">{p.fileName}</div>
                            <div className="text-xs text-gray-500">
                              {p.originalWidth !== null && p.originalHeight !== null && (p.originalWidth !== p.width || p.originalHeight !== p.height)
                                ? T.resizedFrom(p.originalWidth, p.originalHeight)
                                : ''}
                              {p.width}x{p.height} · {kb(p.sizeBytes)} · {p.bundled ? T.bundled : T.uploadedOn(p.createdAt)}
                            </div>
                          </div>
                          <div className="flex items-center justify-between">
                            <label className="flex items-center gap-2 text-xs text-gray-600">
                              <Switch size="sm" checked={p.visible} onCheckedChange={(v) => toggle(p, v)} />
                              {p.visible ? T.visible : T.hidden}
                            </label>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-red-600"
                              onClick={() => { setRemoveError(null); setRemoving(p) }}
                            >
                              <i className="fa-regular fa-trash-can" /> {T.remove}
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}

                    {slotUploads.map((u) => (
                      <div key={u.id} className={`rounded-xl border overflow-hidden ${u.error ? 'border-red-200 bg-red-50/40' : 'border-gray-200'}`}>
                        <div className="aspect-[3/2] flex flex-col items-center justify-center gap-2 bg-gray-50 px-4 text-center">
                          {u.error ? (
                            <>
                              <i className="fa-solid fa-triangle-exclamation text-red-500 text-lg" />
                              <span className="text-xs text-red-600">{u.error}</span>
                            </>
                          ) : (
                            <>
                              <Spinner />
                              <span className="text-xs text-gray-500">{T.uploading}</span>
                            </>
                          )}
                        </div>
                        <div className="flex items-center justify-between px-3 py-2.5">
                          <div className="min-w-0">
                            <div className="text-sm font-medium text-gray-900 truncate">{u.name}</div>
                            <div className="text-xs text-gray-500">{mbOrKb(u.size)}</div>
                          </div>
                          {u.error && (
                            <Button variant="ghost" size="sm" onClick={() => setUploads((list) => list.filter((x) => x.id !== u.id))}>
                              {T.close}
                            </Button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      <Dialog open={removing !== null} onOpenChange={(o) => !saving && !o && setRemoving(null)}>
        <DialogContent showCloseButton={!saving}>
          <DialogHeader>
            <DialogTitle>{T.confirmTitle}</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-2">
            <div className="flex gap-3">
              {removing && <img src={removing.url} alt="" className="w-28 aspect-[3/2] flex-shrink-0 rounded object-cover bg-gray-100" />}
              <p className="text-sm text-gray-700">{removing && T.confirm(slots?.find((s) => s.key === removing.slotKey)?.name ?? '')}</p>
            </div>
            {removeError && <p className="text-sm text-red-600">{removeError}</p>}
          </DialogBody>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setRemoving(null)} disabled={saving}>{T.close}</Button>
            <Button variant="destructive" onClick={remove} disabled={saving}>{T.remove}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
