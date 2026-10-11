/**
 * CR-136 — 기지 관리 (T3-3 「기지 관리」, T3-4 시나리오 5).
 * 고객 화면 기지별 지도가 이 목록을 읽는다. 목록이 비어 있으면 고객 화면은 빈 상태다.
 */
import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@therecommerce-develop/ds-ui'
import {
  createAdminBase,
  deleteAdminBase,
  getAdminBases,
  getUnmatchedOpportunities,
  updateAdminBase,
  type AdminBase,
  type BaseInput,
  type UnmatchedRow,
} from '../api/client'

const TERM_SEPARATOR = ','

const T = {
  title: '기지 관리',
  desc: '고객 화면 기지별 지도에 쓰는 기지 목록입니다. 공고 영문 제목에 찾을 낱말이 낱말 단위로 들어 있으면 그 기지 공고로 셉니다.',
  add: '기지 추가',
  edit: '고치기',
  remove: '지우기',
  cols: ['순서', '한글 이름', '영문 이름', '찾을 낱말', '지역', '위치', '맞는 공고', ''],
  noPosition: '지도에 안 나옴',
  empty: '기지가 없습니다. 기지를 넣기 전에는 고객 화면 기지별 지도가 빈 상태로 보입니다.',
  loadFail: '불러오지 못했습니다.',
  retry: '다시 시도',
  formAdd: '기지 추가',
  formEdit: '기지 고치기',
  fields: {
    nameKo: '한글 이름',
    nameEn: '영문 이름',
    matchTerms: '찾을 낱말',
    matchTermsHint: '쉼표로 여럿. 예: Humphreys, Pyeongtaek',
    region: '지역',
    latitude: '위도 (-90 ~ 90)',
    longitude: '경도 (-180 ~ 180)',
    positionHint: '위도와 경도를 함께 넣어야 지도에 점이 찍힙니다',
    sortOrder: '순서',
  },
  required: '필수',
  notNumber: '숫자로 넣으세요',
  cancel: '취소',
  save: '저장',
  saving: '저장 중',
  saveFail: '저장하지 못했습니다.',
  confirmTitle: '기지 지우기',
  confirm: (name: string) => `"${name}" 기지를 지웁니다. 고객 화면 기지별 지도에서 빠집니다.`,
  deleteFail: '지우지 못했습니다.',
  unmatchedTitle: (n: number) => `기지를 못 찾은 공고 ${n}개`,
  unmatchedDesc: '어느 기지 낱말에도 걸리지 않은 공고입니다(공고번호마다 마지막 판, 게시일 최신순). 제목을 보고 기지의 찾을 낱말을 더하세요.',
  unmatchedCols: ['게시일', '공고번호', '제목'],
  unmatchedNone: '모든 공고가 기지에 걸려 있습니다.',
}

interface FormState {
  nameKo: string
  nameEn: string
  matchTerms: string
  region: string
  latitude: string
  longitude: string
  sortOrder: string
}

const EMPTY_FORM: FormState = { nameKo: '', nameEn: '', matchTerms: '', region: '', latitude: '', longitude: '', sortOrder: '' }

const toForm = (b: AdminBase): FormState => ({
  nameKo: b.nameKo,
  nameEn: b.nameEn,
  matchTerms: b.matchTerms.join(`${TERM_SEPARATOR} `),
  region: b.region ?? '',
  latitude: b.latitude?.toString() ?? '',
  longitude: b.longitude?.toString() ?? '',
  sortOrder: String(b.sortOrder),
})

const numberOrNull = (s: string): number | null | 'bad' => {
  const t = s.trim()
  if (!t) return null
  const n = Number(t)
  return Number.isFinite(n) ? n : 'bad'
}

const serverMessage = (e: unknown): { status?: number; message?: string } => {
  const r = (e as { response?: { status?: number; data?: { message?: string } } })?.response
  return { status: r?.status, message: r?.data?.message }
}

export default function BaseAdminPage() {
  const [bases, setBases] = useState<AdminBase[] | null>(null)
  const [unmatched, setUnmatched] = useState<{ count: number; rows: UnmatchedRow[] } | null>(null)
  const [failed, setFailed] = useState(false)

  const [editing, setEditing] = useState<AdminBase | 'new' | null>(null)
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [fieldError, setFieldError] = useState<Partial<Record<keyof FormState, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [removing, setRemoving] = useState<AdminBase | null>(null)
  const [removeError, setRemoveError] = useState<string | null>(null)

  const load = useCallback(() => {
    Promise.all([getAdminBases(), getUnmatchedOpportunities()])
      .then(([b, u]) => {
        setBases(b.data)
        setUnmatched(u.data)
        setFailed(false)
      })
      .catch(() => setFailed(true))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const openForm = (target: AdminBase | 'new') => {
    setEditing(target)
    setForm(target === 'new' ? EMPTY_FORM : toForm(target))
    setFieldError({})
    setFormError(null)
  }

  const set = (key: keyof FormState) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }))

  const submit = async () => {
    const errors: Partial<Record<keyof FormState, string>> = {}
    const terms = form.matchTerms.split(TERM_SEPARATOR).map((s) => s.trim()).filter(Boolean)
    if (!form.nameKo.trim()) errors.nameKo = T.required
    if (!form.nameEn.trim()) errors.nameEn = T.required
    if (terms.length === 0) errors.matchTerms = T.required
    const latitude = numberOrNull(form.latitude)
    const longitude = numberOrNull(form.longitude)
    const sortOrder = numberOrNull(form.sortOrder)
    if (latitude === 'bad') errors.latitude = T.notNumber
    if (longitude === 'bad') errors.longitude = T.notNumber
    if (sortOrder === 'bad' || (sortOrder !== null && !Number.isInteger(sortOrder))) errors.sortOrder = T.notNumber
    setFieldError(errors)
    setFormError(null)
    if (Object.keys(errors).length > 0 || latitude === 'bad' || longitude === 'bad' || sortOrder === 'bad') return

    const body: BaseInput = {
      nameKo: form.nameKo.trim(),
      nameEn: form.nameEn.trim(),
      matchTerms: terms,
      region: form.region.trim() || null,
      latitude,
      longitude,
      sortOrder,
    }
    setSaving(true)
    try {
      if (editing === 'new') await createAdminBase(body)
      else if (editing) await updateAdminBase(editing.id, body)
      setEditing(null)
      load()
    } catch (e) {
      const { status, message } = serverMessage(e)
      if (status === 409) setFieldError({ nameEn: message ?? T.saveFail })
      else setFormError(message ?? T.saveFail)
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    if (!removing) return
    setSaving(true)
    try {
      await deleteAdminBase(removing.id)
      setRemoving(null)
      load()
    } catch (e) {
      setRemoveError(serverMessage(e).message ?? T.deleteFail)
    } finally {
      setSaving(false)
    }
  }

  const field = (key: keyof FormState, label: string, opts?: { required?: boolean; hint?: string; inputMode?: 'decimal' | 'numeric' }) => (
    <div className="space-y-1">
      <Label htmlFor={`base-${key}`}>
        {label}{opts?.required && <span className="text-red-500 ml-0.5">*</span>}
      </Label>
      <Input id={`base-${key}`} value={form[key]} onChange={set(key)} inputMode={opts?.inputMode} aria-invalid={!!fieldError[key]} />
      {fieldError[key] ? (
        <p className="text-xs text-red-600">{fieldError[key]}</p>
      ) : (
        opts?.hint && <p className="text-xs text-gray-400">{opts.hint}</p>
      )}
    </div>
  )

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-gray-900">{T.title}</h1>
          <p className="text-sm text-gray-500 mt-0.5">{T.desc}</p>
        </div>
        <Button variant="primary" onClick={() => openForm('new')}>
          <i className="fa-solid fa-plus" /> {T.add}
        </Button>
      </div>

      {failed && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-600 flex items-center gap-3">
          <i className="fa-solid fa-triangle-exclamation" /> {T.loadFail}
          <Button variant="outline" size="sm" onClick={load}>{T.retry}</Button>
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200">
        <Table>
          <TableHeader>
            <TableRow>
              {T.cols.map((c, i) => <TableHead key={i} className="whitespace-nowrap">{c}</TableHead>)}
            </TableRow>
          </TableHeader>
          <TableBody>
            {bases?.length === 0 && (
              <TableRow>
                <TableCell colSpan={T.cols.length} className="py-10 text-center text-sm text-gray-500">{T.empty}</TableCell>
              </TableRow>
            )}
            {bases?.map((b) => (
              <TableRow key={b.id}>
                <TableCell className="w-14 text-gray-500">{b.sortOrder}</TableCell>
                <TableCell className="font-semibold">{b.nameKo}</TableCell>
                <TableCell>{b.nameEn}</TableCell>
                <TableCell className="text-gray-600">{b.matchTerms.join(`${TERM_SEPARATOR} `)}</TableCell>
                <TableCell>{b.region ?? '-'}</TableCell>
                <TableCell className="whitespace-nowrap">
                  {b.latitude !== null && b.longitude !== null
                    ? <span className="font-mono text-xs">{b.latitude}, {b.longitude}</span>
                    : <span className="text-xs text-gray-400">{T.noPosition}</span>}
                </TableCell>
                <TableCell className="text-right font-semibold">{b.matchedCount}</TableCell>
                <TableCell className="whitespace-nowrap text-right">
                  <Button variant="ghost" size="sm" onClick={() => openForm(b)}>{T.edit}</Button>
                  <Button variant="ghost" size="sm" className="text-red-600" onClick={() => { setRemoveError(null); setRemoving(b) }}>
                    {T.remove}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {unmatched && (
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
          <div>
            <h2 className="text-base font-bold text-gray-900">{T.unmatchedTitle(unmatched.count)}</h2>
            <p className="text-xs text-gray-500 mt-0.5">{T.unmatchedDesc}</p>
          </div>
          {unmatched.count === 0 ? (
            <p className="text-sm text-gray-500">{T.unmatchedNone}</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  {T.unmatchedCols.map((c) => <TableHead key={c}>{c}</TableHead>)}
                </TableRow>
              </TableHeader>
              <TableBody>
                {unmatched.rows.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="w-28 whitespace-nowrap text-gray-500">{r.postedDate ?? '-'}</TableCell>
                    <TableCell className="w-40 font-mono text-xs">{r.solicitationNumber ?? r.noticeId}</TableCell>
                    <TableCell>
                      <div className="font-medium">{r.title}</div>
                      {r.titleKo && <div className="text-xs text-gray-500">{r.titleKo}</div>}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      )}

      <Dialog open={editing !== null} onOpenChange={(o) => !saving && !o && setEditing(null)}>
        <DialogContent showCloseButton={!saving}>
          <DialogHeader>
            <DialogTitle>{editing === 'new' ? T.formAdd : T.formEdit}</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              {field('nameKo', T.fields.nameKo, { required: true })}
              {field('nameEn', T.fields.nameEn, { required: true })}
            </div>
            {field('matchTerms', T.fields.matchTerms, { required: true, hint: T.fields.matchTermsHint })}
            <div className="grid grid-cols-2 gap-3">
              {field('region', T.fields.region)}
              {field('sortOrder', T.fields.sortOrder, { inputMode: 'numeric' })}
            </div>
            <div className="grid grid-cols-2 gap-3">
              {field('latitude', T.fields.latitude, { inputMode: 'decimal' })}
              {field('longitude', T.fields.longitude, { inputMode: 'decimal' })}
            </div>
            <p className="text-xs text-gray-400">{T.fields.positionHint}</p>
            {formError && <p className="text-sm text-red-600">{formError}</p>}
          </DialogBody>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setEditing(null)} disabled={saving}>{T.cancel}</Button>
            <Button variant="primary" onClick={submit} disabled={saving}>{saving ? T.saving : T.save}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={removing !== null} onOpenChange={(o) => !saving && !o && setRemoving(null)}>
        <DialogContent showCloseButton={!saving}>
          <DialogHeader>
            <DialogTitle>{T.confirmTitle}</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-2">
            <p className="text-sm text-gray-700">{removing && T.confirm(removing.nameKo)}</p>
            {removeError && <p className="text-sm text-red-600">{removeError}</p>}
          </DialogBody>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setRemoving(null)} disabled={saving}>{T.cancel}</Button>
            <Button variant="destructive" onClick={remove} disabled={saving}>{T.remove}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
