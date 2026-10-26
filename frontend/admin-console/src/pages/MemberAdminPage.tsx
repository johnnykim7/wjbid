/**
 * 회원 관리 (BID-ADMIN-002). CR-157: 회원 여부와 끝일, 회원 기간 넣기, 지우기, 이력 (T3-3 「회원 관리」, T3-4 시나리오 3).
 * 결제 연동 전까지 회원 기간은 이 화면으로만 생긴다(BIZ-072).
 */
import { useCallback, useEffect, useState } from 'react'
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
} from '@therecommerce-develop/ds-ui'
import { deleteMembership, getMembers, getMemberships, grantMembership, type MembershipLine } from '../api/client'
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
  open: '회원 기간',
  dialogTitle: (email: string) => `회원 기간, ${email}`,
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

function membershipLabel(m: Member) {
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
                      {m.role !== 'ADMIN' && (
                        <Button variant="outline" size="sm" onClick={() => setTarget(m)}>{T.open}</Button>
                      )}
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
        <MembershipDialog
          member={target}
          onClose={() => setTarget(null)}
          onChanged={() => load(page)}
        />
      )}
    </div>
  )
}

type History = { status: 'loading' } | { status: 'error' } | { status: 'ok'; lines: MembershipLine[] }

function MembershipDialog({ member, onClose, onChanged }: { member: Member; onClose: () => void; onChanged: () => void }) {
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
    <>
      <Dialog open onOpenChange={(o) => !saving && !o && onClose()}>
        <DialogContent showCloseButton={!saving} className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>{T.dialogTitle(member.email)}</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-5">
            {grantError && (
              <Alert variant="destructive" appearance="light">
                <AlertDescription>{grantError}</AlertDescription>
              </Alert>
            )}
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
          </DialogBody>
          <DialogFooter>
            <Button variant="ghost" onClick={onClose} disabled={saving}>{T.close}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

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
    </>
  )
}
