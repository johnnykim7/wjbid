/**
 * 회원 관리 (BID-ADMIN-002). CR-157: 회원 여부와 끝일, 회원 기간 넣기, 지우기, 이력 (T3-3 「회원 관리」, T3-4 시나리오 3).
 * 결제 연동 전까지 회원 기간은 이 화면으로만 생긴다(BIZ-072).
 */
import { useCallback, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
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
import { deleteMembership, getMembers, getMemberships, grantMembership, type MembershipLine } from '../api/client'
import DateField from '../components/DateField'

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '관리자',
  CUSTOMER: '고객',
}

const T = {
  title: '회원 관리',
  desc: '가입된 회원 목록을 조회합니다. 결제 연동 전까지 회원 기간은 여기서 넣습니다.',
  cols: ['이메일', '회사명', '담당자', '역할', '회원', '가입일', '최근 로그인', ''],
  loading: '불러오는 중...',
  empty: '회원이 없습니다.',
  prev: '이전',
  next: '다음',
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
  cancel: '취소',
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
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">{T.title}</h1>
        <p className="text-sm text-gray-500 mt-0.5">{T.desc}</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48 text-gray-400">
            <i className="fa-solid fa-circle-notch fa-spin mr-2" /> {T.loading}
          </div>
        ) : members.length === 0 ? (
          <div className="flex items-center justify-center h-48 text-sm text-gray-400">{T.empty}</div>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  {T.cols.map((h, i) => <TableHead key={i}>{h}</TableHead>)}
                </TableRow>
              </TableHeader>
              <TableBody>
                {members.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell className="font-medium text-gray-900">{m.email}</TableCell>
                    <TableCell className="text-gray-600">{m.companyName ?? '-'}</TableCell>
                    <TableCell className="text-gray-600">{m.contactPerson ?? '-'}</TableCell>
                    <TableCell>
                      <span className="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                        {ROLE_LABELS[m.role] ?? m.role}
                      </span>
                    </TableCell>
                    <TableCell className={m.member ? 'text-gray-900' : 'text-gray-400'}>{membershipLabel(m)}</TableCell>
                    <TableCell className="text-gray-500 text-xs">
                      {m.createdAt ? new Date(m.createdAt).toLocaleDateString('ko-KR') : '-'}
                    </TableCell>
                    <TableCell className="text-gray-500 text-xs">
                      {m.lastLoginAt ? new Date(m.lastLoginAt).toLocaleDateString('ko-KR') : '-'}
                    </TableCell>
                    <TableCell className="text-right">
                      {m.role !== 'ADMIN' && (
                        <Button variant="outline" size="sm" onClick={() => setTarget(m)}>{T.open}</Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 px-5 py-4 border-t border-gray-100">
                <Button variant="outline" size="sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>
                  {T.prev}
                </Button>
                <span className="text-xs text-gray-500">{page + 1} / {totalPages}</span>
                <Button variant="outline" size="sm" onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>
                  {T.next}
                </Button>
              </div>
            )}
          </>
        )}
      </div>

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
  const [formError, setFormError] = useState<string | null>(null)
  const [removing, setRemoving] = useState<MembershipLine | null>(null)
  const [removeError, setRemoveError] = useState<string | null>(null)

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
      setFormError(errorText(e))
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    if (!removing) return
    setSaving(true)
    setRemoveError(null)
    try {
      await deleteMembership(member.id, removing.id)
      setRemoving(null)
      fetchHistory()
      onChanged()
    } catch (e) {
      setRemoveError(errorText(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Dialog open onOpenChange={(o) => !saving && !o && onClose()}>
        <DialogContent showCloseButton={!saving} className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>{T.dialogTitle(member.email)}</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-5">
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
              {formError && <p className="text-sm text-red-600">{formError}</p>}
            </div>

            <div className="space-y-2">
              <h3 className="text-sm font-semibold text-gray-700">{T.history}</h3>
              {history.status === 'loading' ? (
                <p className="text-sm text-gray-400"><i className="fa-solid fa-circle-notch fa-spin mr-2" />{T.loading}</p>
              ) : history.status === 'error' ? (
                <div className="flex items-center gap-2 text-sm text-gray-500">
                  {T.loadError}
                  <Button variant="outline" size="sm" onClick={retry}>{T.retry}</Button>
                </div>
              ) : history.lines.length === 0 ? (
                <p className="text-sm text-gray-400">{T.noHistory}</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      {T.historyCols.map((h, i) => <TableHead key={i}>{h}</TableHead>)}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {history.lines.map((line) => (
                      <TableRow key={line.id}>
                        <TableCell>
                          {dot(line.startsOn)}
                          {line.current && (
                            <span className="ml-2 inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">{T.current}</span>
                          )}
                        </TableCell>
                        <TableCell>{dot(line.endsOn)}</TableCell>
                        <TableCell className="text-gray-600">{line.memo ?? '-'}</TableCell>
                        <TableCell className="text-gray-600">{line.createdByEmail ?? '-'}</TableCell>
                        <TableCell className="text-gray-500 text-xs">{new Date(line.createdAt).toLocaleString('ko-KR')}</TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600"
                            onClick={() => { setRemoveError(null); setRemoving(line) }}
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

      <Dialog open={removing !== null} onOpenChange={(o) => !saving && !o && setRemoving(null)}>
        <DialogContent showCloseButton={!saving}>
          <DialogHeader>
            <DialogTitle>{T.confirmTitle}</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-2">
            <p className="text-sm text-gray-700">{removing && T.confirm(removing)}</p>
            {removeError && <p className="text-sm text-red-600">{removeError}</p>}
          </DialogBody>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setRemoving(null)} disabled={saving}>{T.cancel}</Button>
            <Button variant="destructive" onClick={remove} disabled={saving}>{T.remove}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
