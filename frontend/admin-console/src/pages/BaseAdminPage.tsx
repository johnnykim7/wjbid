/**
 * CR-136: 기지 관리 (T3-3 「기지 관리」, T3-4 시나리오 5).
 * 고객 화면 기지별 지도가 이 목록을 읽는다. 목록이 비어 있으면 고객 화면은 빈 상태다.
 */
import { useCallback, useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTable,
  CardTitle,
  CardToolbar,
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
import { TableMessageRow } from '../components/LoadState'

const TERM_SEPARATOR = ','

const T = {
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
    await deleteAdminBase(removing.id)
    setRemoving(null)
    load()
  }

  const field = (key: keyof FormState, label: string, opts?: { required?: boolean; hint?: string; inputMode?: 'decimal' | 'numeric' }) => (
    <div className="space-y-1">
      <Label htmlFor={`base-${key}`}>
        {label}{opts?.required && <span className="ml-0.5 text-destructive">*</span>}
      </Label>
      <Input
        id={`base-${key}`}
        value={form[key]}
        onChange={set(key)}
        inputMode={opts?.inputMode}
        aria-invalid={!!fieldError[key]}
        aria-describedby={fieldError[key] ? `base-${key}-error` : undefined}
      />
      {fieldError[key] ? (
        <FieldError id={`base-${key}-error`}>{fieldError[key]}</FieldError>
      ) : (
        opts?.hint && <p className="text-xs text-muted-foreground">{opts.hint}</p>
      )}
    </div>
  )

  return (
    <div className="space-y-4">
      {failed && (
        <Alert variant="destructive" appearance="light" className="items-center">
          <AlertDescription>{T.loadFail}</AlertDescription>
          <Button variant="outline" size="sm" className="ms-auto" onClick={load}>{T.retry}</Button>
        </Alert>
      )}

      <Card>
        <CardHeader className="py-2.5">
          <CardToolbar className="ms-auto">
            <Button variant="primary" onClick={() => openForm('new')}>
              <Plus />
              {T.add}
            </Button>
          </CardToolbar>
        </CardHeader>
        <CardTable>
          <Table>
            <TableHeader className="bg-muted">
              <TableRow>
                {T.cols.map((c, i) => <TableHead key={i} className="whitespace-nowrap">{c}</TableHead>)}
              </TableRow>
            </TableHeader>
            <TableBody>
              {bases === null && !failed && <TableMessageRow colSpan={T.cols.length} loading />}
              {bases?.length === 0 && <TableMessageRow colSpan={T.cols.length}>{T.empty}</TableMessageRow>}
              {bases?.map((b) => (
                <TableRow key={b.id}>
                  <TableCell className="w-14 text-muted-foreground">{b.sortOrder}</TableCell>
                  <TableCell className="font-semibold">{b.nameKo}</TableCell>
                  <TableCell>{b.nameEn}</TableCell>
                  <TableCell className="text-muted-foreground">{b.matchTerms.join(`${TERM_SEPARATOR} `)}</TableCell>
                  <TableCell>{b.region ?? '-'}</TableCell>
                  <TableCell className="whitespace-nowrap">
                    {b.latitude !== null && b.longitude !== null
                      ? <span className="font-mono text-xs">{b.latitude}, {b.longitude}</span>
                      : <span className="text-xs text-muted-foreground">{T.noPosition}</span>}
                  </TableCell>
                  <TableCell className="text-right font-semibold">{b.matchedCount}</TableCell>
                  <TableCell className="whitespace-nowrap text-right">
                    <Button variant="ghost" size="sm" onClick={() => openForm(b)}>{T.edit}</Button>
                    <Button variant="ghost" size="sm" className="text-destructive" onClick={() => setRemoving(b)}>
                      {T.remove}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardTable>
      </Card>

      {unmatched && (
        <Card>
          <CardHeader className="py-3">
            <div className="space-y-1">
              <CardTitle>{T.unmatchedTitle(unmatched.count)}</CardTitle>
              <CardDescription>{T.unmatchedDesc}</CardDescription>
            </div>
          </CardHeader>
          {unmatched.count === 0 ? (
            <CardContent>
              <p className="text-sm text-muted-foreground">{T.unmatchedNone}</p>
            </CardContent>
          ) : (
            <CardTable>
              <Table>
                <TableHeader className="bg-muted">
                  <TableRow>
                    {T.unmatchedCols.map((c) => <TableHead key={c}>{c}</TableHead>)}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {unmatched.rows.map((r) => (
                    <TableRow key={r.id}>
                      <TableCell className="w-28 whitespace-nowrap text-muted-foreground">{r.postedDate ?? '-'}</TableCell>
                      <TableCell className="w-40 font-mono text-xs">{r.solicitationNumber ?? r.noticeId}</TableCell>
                      <TableCell>
                        <div className="font-medium">{r.title}</div>
                        {r.titleKo && <div className="text-xs text-muted-foreground">{r.titleKo}</div>}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardTable>
          )}
        </Card>
      )}

      <Dialog open={editing !== null} onOpenChange={(o) => !saving && !o && setEditing(null)}>
        <DialogContent showCloseButton={!saving}>
          <DialogHeader>
            <DialogTitle>{editing === 'new' ? T.formAdd : T.formEdit}</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-3">
            {formError && (
              <Alert variant="destructive" appearance="light">
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}
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
            <p className="text-xs text-muted-foreground">{T.fields.positionHint}</p>
          </DialogBody>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setEditing(null)} disabled={saving}>{T.cancel}</Button>
            <Button variant="primary" onClick={submit} disabled={saving}>{saving ? T.saving : T.save}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(o) => { if (!o) setRemoving(null) }}
        title={T.confirmTitle}
        description={removing ? T.confirm(removing.nameKo) : undefined}
        confirmLabel={T.remove}
        confirmVariant="destructive"
        onConfirm={remove}
        errorText={(e: unknown) => serverMessage(e).message ?? T.deleteFail}
      />
    </div>
  )
}
