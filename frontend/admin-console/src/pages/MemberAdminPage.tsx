/**
 * 회원 관리 (BID-ADMIN-002). CR-157: 회원 여부와 끝일, 회원 기간 넣기, 지우기, 이력 (T3-3 「회원 관리」, T3-4 시나리오 3).
 * CR-183: 줄의 상세로 회원 상세 창을 열어 가입 정보를 보고 고친다 (T3-4 시나리오 4, 5).
 * 결제 연동 전까지 회원 기간은 이 화면으로만 생긴다(BIZ-072).
 */
import { Fragment, useCallback, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import {
  Alert,
  AlertDescription,
  Button,
  Card,
  CardFooter,
  CardTable,
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
  toast,
} from '@therecommerce-develop/ds-ui'
import {
  deleteMembership,
  getMember,
  getMembers,
  getMemberships,
  grantMembership,
  updateMember,
  type MemberDetail,
  type MemberEdit,
  type MembershipLine,
} from '../api/client'
import DateField from '../components/DateField'
import ListPagination from '../components/ListPagination'
import { Loading, TableMessageRow } from '../components/LoadState'
import { ToneBadge } from '../components/StateBadge'

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '관리자',
  CUSTOMER: '고객',
}

const T = {
  cols: ['이메일', '회사명', '담당자', '역할', '회원', '가입일', '최근 로그인', ''],
  empty: '회원이 없습니다.',
  memberUntil: (ymd: string) => `회원, ${dot(ymd)} 까지`,
  admin: '관리자',
  free: '무료회원',
  open: '상세',
  dialogTitle: (email: string) => `회원 상세, ${email}`,
  info: '회원 정보',
  period: '회원 기간',
  edit: '수정',
  save: '저장',
  saving: '저장하는 중...',
  cancel: '취소',
  saved: '저장했습니다',
  needCompany: '회사명을 넣으세요',
  startsOn: '시작일',
  endsOn: '끝일',
  memo: '메모',
  memoHint: '입금 확인 등',
  pick: '날짜 고르기',
  grant: '넣기',
  granting: '넣는 중...',
  needDates: '시작일과 끝일을 고르세요',
  history: '기간 이력',
  noHistory: '넣은 기간이 없습니다.',
  historyCols: ['시작일', '끝일', '메모', '넣은 관리자', '넣은 시각', ''],
  current: '지금 기간',
  remove: '지우기',
  confirmTitle: '회원 기간 지우기',
  confirm: (line: MembershipLine) => `${dot(line.startsOn)} ~ ${dot(line.endsOn)} 기간을 지웁니다. 지운 기간은 되살리지 않습니다.`,
  close: '닫기',
  loadError: '불러오지 못했습니다',
  retry: '다시 시도',
  saveError: '저장하지 못했습니다. 다시 시도하세요',
}

type InfoKey = keyof MemberDetail
const INFO: { label: string; key: InfoKey; wide?: boolean }[] = [
  { label: '이메일', key: 'email' },
  { label: '이메일 확인', key: 'emailVerifiedAt' },
  { label: '회사명', key: 'companyName' },
  { label: '사업자등록번호', key: 'businessRegistrationNumber' },
  { label: '담당자', key: 'contactPerson' },
  { label: '전화번호', key: 'phone' },
  { label: '주소', key: 'address', wide: true },
  { label: '역할', key: 'role' },
  { label: '회원', key: 'member' },
  { label: '가입일', key: 'createdAt' },
  { label: '최근 로그인', key: 'lastLoginAt' },
]

// 관리자가 고치는 칸과 길이. 길이는 표 칸과 같고 주소는 길이 제한이 없다 (T3-2 PATCH /admin/members/{memberId})
const EDIT_MAX: Record<keyof MemberEdit, number | undefined> = {
  companyName: 255,
  businessRegistrationNumber: 50,
  contactPerson: 100,
  phone: 50,
  address: undefined,
}
const isEditable = (key: InfoKey): key is keyof MemberEdit => key in EDIT_MAX

// 서버 오류 코드별 안내. 없는 코드는 saveError
const ERROR_TEXT: Record<string, string> = {
  MEMBERSHIP_PERIOD_INVALID: '끝일은 시작일과 같거나 뒤여야 합니다',
  MEMBER_NOT_FOUND: '회원이 없습니다',
  MEMBERSHIP_NOT_FOUND: '이미 지워진 기간입니다',
}

const MEMO_MAX = 500

interface Member {
  id: string
  email: string
  companyName: string
  contactPerson: string
  phone: string
  role: string
  createdAt: string
  lastLoginAt: string
  member: boolean
  membershipEndsOn?: string
}

function dot(ymd: string) {
  return ymd.replaceAll('-', '.')
}

function errorText(e: unknown) {
  const code = isAxiosError(e) ? e.response?.data?.error : undefined
  return (code && ERROR_TEXT[code]) || T.saveError
}

function membershipLabel(m: Pick<Member, 'role' | 'member' | 'membershipEndsOn'>) {
  if (m.role === 'ADMIN') return T.admin
  return m.member && m.membershipEndsOn ? T.memberUntil(m.membershipEndsOn) : T.free
}

export default function MemberAdminPage() {
  const [members, setMembers] = useState<Member[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [target, setTarget] = useState<Member | null>(null)

  const load = useCallback(async (p: number) => {
    setLoading(true)
    try {
      const { data } = await getMembers(p)
      setMembers(data.content ?? [])
      setTotalPages(data.totalPages ?? 0)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(page) }, [load, page])

  return (
    <div className="space-y-4">
      <Card>
        <CardTable>
          <Table>
            <TableHeader className="bg-muted">
              <TableRow>
                {T.cols.map((h, i) => <TableHead key={i}>{h}</TableHead>)}
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableMessageRow colSpan={T.cols.length} loading />
              ) : members.length === 0 ? (
                <TableMessageRow colSpan={T.cols.length}>{T.empty}</TableMessageRow>
              ) : (
                members.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell className="font-medium text-foreground">{m.email}</TableCell>
                    <TableCell className="text-muted-foreground">{m.companyName ?? '-'}</TableCell>
                    <TableCell className="text-muted-foreground">{m.contactPerson ?? '-'}</TableCell>
                    <TableCell>
                      <ToneBadge tone="info">{ROLE_LABELS[m.role] ?? m.role}</ToneBadge>
                    </TableCell>
                    <TableCell className={m.member ? 'text-foreground' : 'text-muted-foreground'}>{membershipLabel(m)}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {m.createdAt ? new Date(m.createdAt).toLocaleDateString('ko-KR') : '-'}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {m.lastLoginAt ? new Date(m.lastLoginAt).toLocaleDateString('ko-KR') : '-'}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="outline" size="sm" onClick={() => setTarget(m)}>{T.open}</Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardTable>
        {totalPages > 1 && (
          <CardFooter className="justify-center py-2">
            <ListPagination page={page} totalPages={totalPages} onChange={setPage} />
          </CardFooter>
        )}
      </Card>

      {target && (
        <MemberDetailDialog
          member={target}
          onClose={() => setTarget(null)}
          onChanged={() => load(page)}
        />
      )}
    </div>
  )
}

function infoValue(m: MemberDetail, key: InfoKey) {
  if (key === 'role') return ROLE_LABELS[m.role] ?? m.role
  if (key === 'member') return membershipLabel(m)
  if (key === 'createdAt') return dot(m.createdAt.slice(0, 10))
  const v = m[key]
  if (typeof v !== 'string' || v === '') return '-'
  return key === 'emailVerifiedAt' || key === 'lastLoginAt' ? dot(v) : v
}

function MemberDetailDialog({ member, onClose, onChanged }: { member: Member; onClose: () => void; onChanged: () => void }) {
  // 어느 쪽이든 저장하는 동안은 창을 닫지 않는다
  const [infoBusy, setInfoBusy] = useState(false)
  const [periodBusy, setPeriodBusy] = useState(false)
  // 회원 기간이 바뀌면 회원 정보의 회원 칸도 다시 부른다
  const [periodVersion, setPeriodVersion] = useState(0)
  const busy = infoBusy || periodBusy

  const periodChanged = useCallback(() => {
    setPeriodVersion((v) => v + 1)
    onChanged()
  }, [onChanged])

  return (
    <Dialog open onOpenChange={(o) => !busy && !o && onClose()}>
      <DialogContent showCloseButton={!busy} className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>{T.dialogTitle(member.email)}</DialogTitle>
        </DialogHeader>
        <DialogBody className="space-y-5">
          <MemberInfoSection memberId={member.id} reloadKey={periodVersion} onBusy={setInfoBusy} onChanged={onChanged} />
          {member.role !== 'ADMIN' && (
            <div className="border-t border-border pt-5">
              <MembershipSection member={member} onBusy={setPeriodBusy} onChanged={periodChanged} />
            </div>
          )}
        </DialogBody>
        <DialogFooter>
          <Button variant="ghost" onClick={onClose} disabled={busy}>{T.close}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

type Detail = { status: 'loading' } | { status: 'error'; notFound: boolean } | { status: 'ok'; member: MemberDetail }

function toEdit(m: MemberDetail): MemberEdit {
  return {
    companyName: m.companyName ?? '',
    businessRegistrationNumber: m.businessRegistrationNumber ?? '',
    contactPerson: m.contactPerson ?? '',
    phone: m.phone ?? '',
    address: m.address ?? '',
  }
}

function MemberInfoSection({ memberId, reloadKey, onBusy, onChanged }: {
  memberId: string
  reloadKey: number
  onBusy: (busy: boolean) => void
  onChanged: () => void
}) {
  const [detail, setDetail] = useState<Detail>({ status: 'loading' })
  const [draft, setDraft] = useState<MemberEdit | null>(null)
  const [saving, setSaving] = useState(false)
  const [companyError, setCompanyError] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)

  const fetchDetail = useCallback(() => {
    getMember(memberId)
      .then((res) => setDetail({ status: 'ok', member: res.data }))
      .catch((e) => setDetail({ status: 'error', notFound: isAxiosError(e) && e.response?.status === 404 }))
  }, [memberId])

  useEffect(() => { fetchDetail() }, [fetchDetail, reloadKey])
  useEffect(() => { onBusy(saving) }, [onBusy, saving])

  const retry = () => {
    setDetail({ status: 'loading' })
    fetchDetail()
  }

  const startEdit = (m: MemberDetail) => {
    setDraft(toEdit(m))
    setCompanyError(null)
    setSaveError(null)
  }

  const cancel = () => {
    setDraft(null)
    setCompanyError(null)
    setSaveError(null)
  }

  const save = async () => {
    if (!draft) return
    setSaveError(null)
    if (!draft.companyName.trim()) {
      setCompanyError(T.needCompany)
      return
    }
    setCompanyError(null)
    setSaving(true)
    try {
      const { data } = await updateMember(memberId, draft)
      setDetail({ status: 'ok', member: data })
      setDraft(null)
      toast.success(T.saved)
      onChanged()
    } catch (e) {
      setSaveError(errorText(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-foreground">{T.info}</h3>
        {detail.status === 'ok' && !draft && (
          <Button variant="outline" size="sm" onClick={() => startEdit(detail.member)}>{T.edit}</Button>
        )}
      </div>
      {saveError && (
        <Alert variant="destructive" appearance="light">
          <AlertDescription>{saveError}</AlertDescription>
        </Alert>
      )}
      {detail.status === 'loading' ? (
        <Loading className="h-16" />
      ) : detail.status === 'error' ? (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          {detail.notFound ? ERROR_TEXT.MEMBER_NOT_FOUND : T.loadError}
          {!detail.notFound && <Button variant="outline" size="sm" onClick={retry}>{T.retry}</Button>}
        </div>
      ) : (
        <>
          <dl className="grid grid-cols-[7rem_minmax(0,1fr)] items-center gap-x-4 gap-y-2.5 text-sm sm:grid-cols-[7rem_minmax(0,1fr)_7rem_minmax(0,1fr)]">
            {INFO.map(({ label, key, wide }) => (
              <Fragment key={key}>
                <dt className="text-muted-foreground">{label}</dt>
                <dd className={`break-words text-foreground ${wide ? 'sm:col-span-3' : ''}`}>
                  {draft && isEditable(key) ? (
                    <div className="flex flex-col gap-1">
                      <Input
                        aria-label={label}
                        value={draft[key]}
                        maxLength={EDIT_MAX[key]}
                        onChange={(e) => setDraft({ ...draft, [key]: e.target.value })}
                        disabled={saving}
                        aria-invalid={key === 'companyName' && companyError !== null}
                      />
                      {key === 'companyName' && <FieldError>{companyError}</FieldError>}
                    </div>
                  ) : (
                    infoValue(detail.member, key)
                  )}
                </dd>
              </Fragment>
            ))}
          </dl>
          {draft && (
            <div className="flex justify-end gap-2 pt-1">
              <Button variant="outline" onClick={cancel} disabled={saving}>{T.cancel}</Button>
              <Button variant="primary" onClick={save} disabled={saving}>{saving ? T.saving : T.save}</Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

type History = { status: 'loading' } | { status: 'error' } | { status: 'ok'; lines: MembershipLine[] }

function MembershipSection({ member, onBusy, onChanged }: {
  member: Member
  onBusy: (busy: boolean) => void
  onChanged: () => void
}) {
  const [history, setHistory] = useState<History>({ status: 'loading' })
  const [startsOn, setStartsOn] = useState<string | null>(null)
  const [endsOn, setEndsOn] = useState<string | null>(null)
  const [memo, setMemo] = useState('')
  const [saving, setSaving] = useState(false)
  // 고른 날짜가 모자라거나 틀린 것은 칸 아래, 서버가 받지 않은 것은 창 맨 위 띠로 보인다
  const [formError, setFormError] = useState<string | null>(null)
  const [grantError, setGrantError] = useState<string | null>(null)
  const [removing, setRemoving] = useState<MembershipLine | null>(null)

  const fetchHistory = useCallback(() => {
    getMemberships(member.id)
      .then((res) => setHistory({ status: 'ok', lines: res.data }))
      .catch(() => setHistory({ status: 'error' }))
  }, [member.id])

  useEffect(() => { fetchHistory() }, [fetchHistory])
  useEffect(() => { onBusy(saving) }, [onBusy, saving])

  const retry = () => {
    setHistory({ status: 'loading' })
    fetchHistory()
  }

  const grant = async () => {
    setGrantError(null)
    if (!startsOn || !endsOn) {
      setFormError(T.needDates)
      return
    }
    // 서버도 같은 규칙으로 받지 않지만 고르자마자 알린다(BIZ-072)
    if (endsOn < startsOn) {
      setFormError(ERROR_TEXT.MEMBERSHIP_PERIOD_INVALID)
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      await grantMembership(member.id, { startsOn, endsOn, memo: memo.trim() })
      setStartsOn(null)
      setEndsOn(null)
      setMemo('')
      fetchHistory()
      onChanged()
    } catch (e) {
      setGrantError(errorText(e))
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    if (!removing) return
    await deleteMembership(member.id, removing.id)
    setRemoving(null)
    fetchHistory()
    onChanged()
  }

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-semibold text-foreground">{T.period}</h3>
      {grantError && (
        <Alert variant="destructive" appearance="light">
          <AlertDescription>{grantError}</AlertDescription>
        </Alert>
      )}
      <div className="space-y-5">
        <div className="space-y-3">
          <div className="flex flex-wrap items-end gap-3">
            <div className="flex flex-col gap-1">
              <Label>{T.startsOn}</Label>
              <DateField value={startsOn} onChange={setStartsOn} placeholder={T.pick} label={T.startsOn} disabled={saving} />
            </div>
            <div className="flex flex-col gap-1">
              <Label>{T.endsOn}</Label>
              <DateField value={endsOn} onChange={setEndsOn} placeholder={T.pick} label={T.endsOn} disabled={saving} />
            </div>
            <div className="flex flex-col gap-1 flex-1 min-w-48">
              <Label htmlFor="membership-memo">{T.memo}</Label>
              <Input
                id="membership-memo"
                value={memo}
                maxLength={MEMO_MAX}
                placeholder={T.memoHint}
                onChange={(e) => setMemo(e.target.value)}
                disabled={saving}
              />
            </div>
            <Button variant="primary" onClick={grant} disabled={saving}>{saving ? T.granting : T.grant}</Button>
          </div>
          <FieldError>{formError}</FieldError>
        </div>

        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-foreground">{T.history}</h3>
          {history.status === 'loading' ? (
            <Loading className="h-16" />
          ) : history.status === 'error' ? (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              {T.loadError}
              <Button variant="outline" size="sm" onClick={retry}>{T.retry}</Button>
            </div>
          ) : history.lines.length === 0 ? (
            <p className="text-sm text-muted-foreground">{T.noHistory}</p>
          ) : (
            <Table>
              <TableHeader className="bg-muted">
                <TableRow>
                  {T.historyCols.map((h, i) => <TableHead key={i}>{h}</TableHead>)}
                </TableRow>
              </TableHeader>
              <TableBody>
                {history.lines.map((line) => (
                  <TableRow key={line.id}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {dot(line.startsOn)}
                        {line.current && <ToneBadge tone="success">{T.current}</ToneBadge>}
                      </div>
                    </TableCell>
                    <TableCell>{dot(line.endsOn)}</TableCell>
                    <TableCell className="text-muted-foreground">{line.memo ?? '-'}</TableCell>
                    <TableCell className="text-muted-foreground">{line.createdByEmail ?? '-'}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">{new Date(line.createdAt).toLocaleString('ko-KR')}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        onClick={() => setRemoving(line)}
                        disabled={saving}
                      >
                        {T.remove}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(o) => { if (!o) setRemoving(null) }}
        title={T.confirmTitle}
        description={removing ? T.confirm(removing) : undefined}
        confirmLabel={T.remove}
        confirmVariant="destructive"
        onConfirm={remove}
        errorText={errorText}
      />
    </div>
  )
}
