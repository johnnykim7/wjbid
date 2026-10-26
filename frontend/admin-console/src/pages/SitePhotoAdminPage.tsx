/**
 * CR-153: 고객 화면 사진 (T3-3 「고객 화면 사진」, T3-4 「고객 화면 사진」).
 * 사진 자리마다 여러 장을 올려 두면 고객 화면이 손님이 들어올 때마다 한 장을 고른다. 사진을 바꾸는 데 배포가 필요 없다.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import { EyeOff, ImageIcon, Info, Trash2, TriangleAlert, Upload } from 'lucide-react'
import {
  Alert,
  AlertContent,
  AlertDescription,
  AlertIcon,
  AlertTitle,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  ConfirmDialog,
  Input,
  Label,
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
import { Loading } from '../components/LoadState'
import { ToneBadge } from '../components/StateBadge'

const T = {
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
    await deleteAdminSitePhoto(removing.id)
    setRemoving(null)
    load()
  }

  const visibleCount = (slot: AdminSitePhotoSlot) => slot.photos.filter((p) => p.visible).length
  const slotUploads = uploads.filter((u) => u.slotKey === selected?.key)

  return (
    <div className="space-y-4">
      {failed && (
        <Alert variant="destructive" appearance="light" className="items-center">
          <AlertDescription>{T.loadFail}</AlertDescription>
          <Button variant="outline" size="sm" className="ms-auto" onClick={load}>{T.retry}</Button>
        </Alert>
      )}
      {actionError && (
        <Alert variant="destructive" appearance="light">
          <AlertDescription>{actionError}</AlertDescription>
        </Alert>
      )}

      {!slots && !failed && <Loading />}

      {slots && (
        <div className="flex items-start gap-4">
          <Card className="w-80 flex-shrink-0 overflow-hidden">
            <CardHeader>
              <CardTitle>{T.slots(slots.length)}</CardTitle>
              <span className="text-xs text-muted-foreground">{T.slotsCount}</span>
            </CardHeader>
            <div>
              {slots.map((slot) => {
                const cover = slot.photos.find((p) => p.visible)
                const shown = visibleCount(slot)
                const active = slot.key === selected?.key
                return (
                  <Button
                    key={slot.key}
                    variant="ghost"
                    onClick={() => setSelectedKey(slot.key)}
                    className={`h-auto w-full justify-start gap-3 rounded-none border-b border-l-[3px] border-border px-4 py-2.5 text-left font-normal last:border-b-0 ${
                      active ? 'border-l-secondary bg-secondary/10 hover:bg-secondary/10' : 'border-l-transparent hover:bg-muted'
                    }`}
                  >
                    <div className="flex h-9 w-14 flex-shrink-0 items-center justify-center overflow-hidden rounded bg-muted">
                      {cover
                        ? <img src={cover.url} alt="" className="h-full w-full object-cover" />
                        : <ImageIcon className="size-4 text-muted-foreground" />}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-semibold text-foreground">{slot.name}</div>
                      <div className="truncate text-xs text-muted-foreground">{slot.location}</div>
                    </div>
                    <ToneBadge tone={shown === 0 ? 'warning' : 'info'}>
                      {shown} / {slot.photos.length}
                    </ToneBadge>
                  </Button>
                )
              })}
            </div>
          </Card>

          {selected && rules && (
            <Card className="min-w-0 flex-1">
              <CardHeader className="items-start py-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <CardTitle className="text-base">{selected.name}</CardTitle>
                    <ToneBadge tone="info">
                      {T.counts(visibleCount(selected), selected.photos.length - visibleCount(selected))}
                    </ToneBadge>
                  </div>
                  <CardDescription className="text-sm">{selected.location}</CardDescription>
                </div>
                <Button variant="primary" onClick={() => fileInput.current?.click()}>
                  <Upload />
                  {T.upload}
                </Button>
                <Input
                  ref={fileInput}
                  type="file"
                  multiple
                  accept={rules.allowedTypes.join(',')}
                  className="hidden"
                  onChange={onPick}
                />
              </CardHeader>

              <CardContent className="space-y-4 p-5">
                <div className="flex items-start gap-2 rounded-lg border border-border bg-muted px-3 py-2 text-xs text-muted-foreground">
                  <Info className="mt-0.5 size-4 shrink-0" /> {T.rules(rules)}
                </div>

                {visibleCount(selected) === 0 && (
                  <Alert variant="warning" appearance="light">
                    <AlertIcon>
                      <ImageIcon />
                    </AlertIcon>
                    <AlertContent>
                      <AlertTitle>{T.emptyTitle}</AlertTitle>
                      <AlertDescription>{T.emptyDesc}</AlertDescription>
                    </AlertContent>
                  </Alert>
                )}

                {(selected.photos.length > 0 || slotUploads.length > 0) && (
                  <div className="grid grid-cols-3 gap-4">
                    {selected.photos.map((p, i) => (
                      <div
                        key={p.id}
                        className={`overflow-hidden rounded-xl border ${p.visible ? 'border-border' : 'border-dashed border-input'}`}
                      >
                        <div className={`relative aspect-[3/2] bg-muted ${p.visible ? '' : 'opacity-50'}`}>
                          <img src={p.url} alt="" className="h-full w-full object-cover" />
                          <span className="absolute left-2 top-2 rounded bg-foreground/80 px-1.5 text-xs font-semibold text-background">{i + 1}</span>
                          {!p.visible && (
                            <span className="absolute right-2 top-2 inline-flex items-center gap-1 rounded bg-background/90 px-1.5 text-xs text-foreground">
                              <EyeOff className="size-3" /> {T.hidden}
                            </span>
                          )}
                        </div>
                        <div className="space-y-2 px-3 py-2.5">
                          <div>
                            <div className="truncate text-sm font-medium text-foreground">{p.fileName}</div>
                            <div className="text-xs text-muted-foreground">
                              {p.originalWidth !== null && p.originalHeight !== null && (p.originalWidth !== p.width || p.originalHeight !== p.height)
                                ? T.resizedFrom(p.originalWidth, p.originalHeight)
                                : ''}
                              {p.width}x{p.height} · {kb(p.sizeBytes)} · {p.bundled ? T.bundled : T.uploadedOn(p.createdAt)}
                            </div>
                          </div>
                          <div className="flex items-center justify-between">
                            <Label className="flex items-center gap-2 text-xs text-muted-foreground">
                              <Switch size="sm" checked={p.visible} onCheckedChange={(v) => toggle(p, v)} />
                              {p.visible ? T.visible : T.hidden}
                            </Label>
                            <Button variant="ghost" size="sm" className="text-destructive" onClick={() => setRemoving(p)}>
                              <Trash2 />
                              {T.remove}
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}

                    {slotUploads.map((u) => (
                      <div
                        key={u.id}
                        className={`overflow-hidden rounded-xl border ${u.error ? 'border-destructive/30 bg-destructive/5' : 'border-border'}`}
                      >
                        <div className="flex aspect-[3/2] flex-col items-center justify-center gap-2 bg-muted px-4 text-center">
                          {u.error ? (
                            <>
                              <TriangleAlert className="size-4 text-destructive" />
                              <span className="text-xs text-destructive">{u.error}</span>
                            </>
                          ) : (
                            <>
                              <Spinner />
                              <span className="text-xs text-muted-foreground">{T.uploading}</span>
                            </>
                          )}
                        </div>
                        <div className="flex items-center justify-between px-3 py-2.5">
                          <div className="min-w-0">
                            <div className="truncate text-sm font-medium text-foreground">{u.name}</div>
                            <div className="text-xs text-muted-foreground">{mbOrKb(u.size)}</div>
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
              </CardContent>
            </Card>
          )}
        </div>
      )}

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(o) => { if (!o) setRemoving(null) }}
        title={T.confirmTitle}
        description={removing ? T.confirm(slots?.find((s) => s.key === removing.slotKey)?.name ?? '') : undefined}
        confirmLabel={T.remove}
        confirmVariant="destructive"
        cancelLabel={T.close}
        onConfirm={remove}
        errorText={(e: unknown) => serverMessage(e) ?? T.deleteFail}
      >
        {removing && <img src={removing.url} alt="" className="aspect-[3/2] w-28 rounded bg-muted object-cover" />}
      </ConfirmDialog>
    </div>
  )
}
