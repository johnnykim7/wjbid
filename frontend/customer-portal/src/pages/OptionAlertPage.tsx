import { useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Button, Input } from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadState, Rich } from '../components/market/MarketParts'
import { daysBetween, money } from '../components/market/marketUtils'
import { AwardLink, GuestGate, RcIcon, RecompeteBand } from '../components/recompete/RecompeteParts'
import { DatePick } from '../components/expiry/DatePick'
import { federalErrorCode, memberContractApi } from '../api/federal'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { useAuth } from '../context/AuthContext'
import { FEDERAL_COMMON as C, FEDERAL_LINKS as L, OPTION_ALERT_TEXT as T } from '../data/recompeteContent'
import { RECOMPETE_PATHS } from '../data/recompetePaths'
import type { ContractDateKind, ContractRow, MemberContractInput, MemberContractRow, MemberContractsResponse } from '../types/federal'
import '../wjbid-expiry.css'

const PLACE = findMenuItem('recompete', 'option-alert')
const DAY_MS = 86_400_000

/** 'YYYY-MM-DD' 에서 n 일 앞 날짜. 날짜만 다루므로 UTC 로 센다 */
function minusDays(ymd: string, n: number): string {
  const [y, m, d] = ymd.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d) - n * DAY_MS).toISOString().slice(0, 10)
}

type Look =
  | { state: 'idle' }
  | { state: 'busy' }
  | { state: 'found'; piid: string; contract: ContractRow; already: boolean }
  | { state: 'miss'; piid: string }
  | { state: 'fail'; piid: string }

const errorText = (e: unknown) => T.error[federalErrorCode(e) ?? ''] ?? T.error.fallback

/** 남은 날 꼬리. 가장 가까운 알림 날 안이면 주황, 지났으면 빨강 */
function DaysLeft({ days, soon }: { days: number | null; soon: number | null }) {
  if (days == null) return null
  const tone = days < 0 ? 'x' : soon != null && days <= soon ? 's' : 'o'
  return <span className={`dleft ${tone}`}>{C.daysLeft(days)}</span>
}

function FoundBox({ look, data, onRegister, busy, error }: {
  look: Extract<Look, { state: 'found' }>
  data: MemberContractsResponse
  onRegister: () => void
  busy: boolean
  error: string | null
}) {
  const f = look.contract
  const today = data.today
  const plan: string[] = []
  ;([['CURRENT_END', f.endDate], ['POTENTIAL_END', f.potentialEndDate]] as [ContractDateKind, string | null][]).forEach(([kind, date]) => {
    if (!date) return
    data.alertDays.forEach((n) => {
      const on = minusDays(date, n)
      plan.push(T.found.planItem(C.kind[kind], n, on, on < today))
    })
  })
  const synced = data.source.lastSyncedAt?.slice(0, 10)
  return (
    <div className="rc-found">
      <div className="ft3"><Icon name="okc" />{T.found.title}{synced && <small>{T.found.basis(synced)}</small>}</div>
      <h3>{f.description || f.piid}</h3>
      <div className="rc-meta">
        <span className="mono">{f.piid}</span>
        <span>{C.type[f.awardType]}</span>
        {f.recipientName && <span>{f.recipientName}</span>}
        <span><AwardLink row={f} /></span>
      </div>
      <dl>
        <div><dt>{C.date.start}</dt><dd>{f.startDate ?? '-'}</dd></div>
        <div>
          <dt>{C.kind.CURRENT_END}</dt>
          <dd>{f.endDate ?? C.date.unknown}{f.endDate && <small className="k">{C.daysLeft(daysBetween(today, f.endDate))}</small>}</dd>
        </div>
        <div>
          <dt>{C.kind.POTENTIAL_END}</dt>
          <dd>{f.potentialEndDate ?? C.date.unknown}{f.potentialEndDate && <small>{C.daysLeft(daysBetween(today, f.potentialEndDate))}</small>}</dd>
        </div>
        <div><dt>{T.found.amount}</dt><dd>{f.amountUsd != null ? money(f.amountUsd) : '-'}</dd></div>
      </dl>
      <div className="act">
        <span>{plan.length > 0 ? <Rich text={T.found.plan(plan)} /> : T.off}</span>
        <Button className="wj-reset bigb" disabled={look.already || busy} onClick={onRegister}>
          {look.already ? T.found.already : T.found.register}
        </Button>
      </div>
      {error && <p className="err">{error}</p>}
    </div>
  )
}

function ManualBox({ look, lagDays, onRegister, onRetry, busy, error }: {
  look: Extract<Look, { state: 'miss' | 'fail' }>
  lagDays: number | null
  onRegister: (end: string, potential: string | null) => void
  onRetry: () => void
  busy: boolean
  error: string | null
}) {
  const m = T.miss
  const [end, setEnd] = useState<string | null>(null)
  const [potential, setPotential] = useState<string | null>(null)
  const failed = look.state === 'fail'
  return (
    <div className="rc-miss">
      <b className="t"><Icon name="search" />{failed ? m.failTitle : m.title}</b>
      <p>{failed ? m.failDesc : m.desc(lagDays)}</p>
      {failed && <Button className="wj-reset bigb o retry" onClick={onRetry} disabled={busy}>{m.retry}</Button>}
      <div className="row">
        <div>
          <label>{m.end}</label>
          <DatePick value={end} onSave={setEnd} placeholder={m.endPlaceholder} label={m.end} />
        </div>
        <div>
          <label>{m.potential}<small>{m.optional}</small></label>
          <DatePick value={potential} onSave={setPotential} placeholder={m.potentialPlaceholder} label={m.potential} />
        </div>
        <Button className="wj-reset bigb" disabled={!end || busy} onClick={() => end && onRegister(end, potential)}>{m.submit}</Button>
      </div>
      {error && <p className="err">{error}</p>}
    </div>
  )
}

function MyContract({ c, data, onChanged }: { c: MemberContractRow; data: MemberContractsResponse; onChanged: () => void }) {
  const t = T.my
  const [editing, setEditing] = useState(false)
  const [asking, setAsking] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const soon = data.alertDays.length > 0 ? Math.min(...data.alertDays) : null

  const run = (job: Promise<unknown>) => {
    setBusy(true)
    setError(null)
    job.then(onChanged).catch((e) => setError(errorText(e))).finally(() => setBusy(false))
  }
  const save = (patch: MemberContractInput) =>
    run(memberContractApi.update(c.id, { endDate: c.endDate, potentialEndDate: c.potentialEndDate, description: c.description, ...patch }))

  const kindDate = c.nextAlertKind === 'POTENTIAL_END' ? c.potentialEndDate : c.endDate
  const fed = c.federalContract
  return (
    <div className="it">
      <div className="hd3">
        <div>
          <b>{c.description || c.piid}</b>
          <small>
            <span className="mono">{c.piid}</span>
            {fed && <span>{C.type[fed.awardType]}</span>}
            <span className={`src3 ${c.source}`}>{t.source[c.source]}</span>
            {fed && <AwardLink row={fed} />}
          </small>
        </div>
        <div className="acts">
          {asking ? (
            <>
              <span>{t.removeAsk}</span>
              <Button className="wj-reset dg" disabled={busy} onClick={() => run(memberContractApi.remove(c.id))}>{t.removeYes}</Button>
              <Button className="wj-reset m" onClick={() => setAsking(false)}>{t.removeNo}</Button>
            </>
          ) : (
            <>
              {c.source === 'MANUAL' && <Button className="wj-reset" onClick={() => setEditing(!editing)}>{editing ? t.editDone : t.edit}</Button>}
              <Button className="wj-reset m" onClick={() => setAsking(true)}>{t.remove}</Button>
            </>
          )}
        </div>
      </div>
      <dl>
        <div><dt>{C.kind.CURRENT_END}</dt><dd>{c.endDate}<DaysLeft days={c.endDaysLeft} soon={soon} /></dd></div>
        <div>
          <dt>{C.kind.POTENTIAL_END}</dt>
          {c.potentialEndDate ? <dd>{c.potentialEndDate}<DaysLeft days={c.potentialDaysLeft} soon={null} /></dd> : <dd className="no">{t.unknown}</dd>}
        </div>
        <div>
          <dt>{t.next}</dt>
          {c.nextAlertOn && c.nextAlertKind && kindDate ? (
            <dd>{c.nextAlertOn}<small>{t.nextLabel(C.kind[c.nextAlertKind], daysBetween(c.nextAlertOn, kindDate))}</small></dd>
          ) : <dd className="no">{t.noNext}</dd>}
        </div>
        <div><dt>{t.last}</dt>{c.lastAlertedAt ? <dd>{c.lastAlertedAt.slice(0, 10)}</dd> : <dd className="no">{t.noLast}</dd>}</div>
      </dl>
      {editing && (
        <div className="edt">
          <div>
            <label>{C.kind.CURRENT_END}</label>
            <DatePick value={c.endDate} busy={busy} label={C.kind.CURRENT_END} onSave={(d) => save({ endDate: d })} />
          </div>
          <div>
            <label>{C.kind.POTENTIAL_END}</label>
            <DatePick value={c.potentialEndDate} busy={busy} label={C.kind.POTENTIAL_END} placeholder={T.miss.potentialPlaceholder} onSave={(d) => save({ potentialEndDate: d })} />
          </div>
          {c.potentialEndDate && <Button className="wj-reset lk" disabled={busy} onClick={() => save({ potentialEndDate: null })}>{t.clearPotential}</Button>}
        </div>
      )}
      {c.datesChangedOn && <div className="rc-chg"><Icon name="alert" />{t.changed(c.datesChangedOn)}</div>}
      {error && <div className="rc-chg"><Icon name="alert" />{error}</div>}
    </div>
  )
}

/** 옵션 연장 알림 (CR-142, BID-MKT-031~032, T3-3 「옵션 연장 알림」). 계약번호로 끝날짜를 찾아 넣고, 끝날짜 전에 메일 */
export default function OptionAlertPage() {
  const { isAuthenticated, userEmail } = useAuth()
  const location = useLocation()
  const list = useMarketLoad(() => (isAuthenticated ? memberContractApi.list() : Promise.resolve(null)), String(isAuthenticated))
  const [piid, setPiid] = useState('')
  const [look, setLook] = useState<Look>({ state: 'idle' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lookError, setLookError] = useState<string | null>(null)

  if (!PLACE) return <Navigate to="/" replace />
  const d = list.data

  const find = (value: string) => {
    if (!value.trim()) {
      setLookError(T.error.CONTRACT_PIID_INVALID)
      return
    }
    setLook({ state: 'busy' })
    setError(null)
    setLookError(null)
    memberContractApi.lookup(value)
      .then((r) => setLook(r.found && r.contract
        ? { state: 'found', piid: value, contract: r.contract, already: r.alreadyRegistered }
        : { state: 'miss', piid: value }))
      .catch((e) => {
        const code = federalErrorCode(e)
        if (code === 'CONTRACT_PIID_INVALID') {
          setLook({ state: 'idle' })
          setLookError(T.error.CONTRACT_PIID_INVALID)
        } else {
          setLook({ state: 'fail', piid: value })
        }
      })
  }

  const register = (body: MemberContractInput) => {
    setBusy(true)
    setError(null)
    memberContractApi.register(body)
      .then(() => {
        setLook({ state: 'idle' })
        setPiid('')
        list.reload()
      })
      .catch((e) => setError(errorText(e)))
      .finally(() => setBusy(false))
  }

  const head = (
    <div className="ph2">
      <div>
        <h1>{T.title}</h1>
        <p>{T.desc(d?.alertDays ?? [])}</p>
      </div>
      {isAuthenticated && userEmail && <div className="rcv">{T.receiver}<b><RcIcon name="mail" />{userEmail}</b></div>}
    </div>
  )
  const band = (
    <RecompeteBand icon="cal" title={T.band.title} desc={T.band.desc} links={[{ label: L.nextMonth, to: RECOMPETE_PATHS.nextMonth }]} />
  )
  const frame = (body: React.ReactNode) => <SubPageFrame group={PLACE.group} item={PLACE.item}>{head}{body}{band}</SubPageFrame>

  if (!isAuthenticated) return frame(<GuestGate from={location.pathname + location.search} />)
  if (!d) {
    return frame(
      <section className="cbx pad">
        <LoadState state={list.status === 'error' ? 'error' : 'loading'} onRetry={list.reload} />
      </section>,
    )
  }

  return frame(
    <>
      {d.alertDays.length === 0 && <p className="rc-off">{T.off}</p>}
      <section className="cbx rc-look">
        <label htmlFor="rc-piid">{T.look.label}</label>
        <form className="gsch" onSubmit={(e) => { e.preventDefault(); find(piid) }} role="search">
          <Input id="rc-piid" className="wj-reset in" value={piid} onChange={(e) => setPiid(e.target.value)} placeholder={T.look.placeholder} autoComplete="off" />
          <Button type="submit" className="wj-reset bigb" disabled={look.state === 'busy'}><Icon name="search" />{T.look.find}</Button>
        </form>
        {lookError ? <p className="err">{lookError}</p> : <p className="hint">{T.look.hint}</p>}
        {look.state === 'busy' && <LoadState state="loading" />}
        {look.state === 'found' && (
          <FoundBox look={look} data={d} busy={busy} error={error} onRegister={() => register({ piid: look.piid })} />
        )}
        {(look.state === 'miss' || look.state === 'fail') && (
          <ManualBox
            key={`${look.state}|${look.piid}`}
            look={look}
            lagDays={d.source.lagDays}
            busy={busy}
            error={error}
            onRetry={() => find(look.piid)}
            onRegister={(end, potential) => register({ piid: look.piid, endDate: end, potentialEndDate: potential })}
          />
        )}
      </section>
      <section className="cbx pad">
        <h2 className="h2">{T.my.title} {T.my.count(d.contracts.length)}<small>{T.my.sub}</small></h2>
        <div className="rc-my">
          {d.contracts.length === 0
            ? <div className="rc-my-empty">{T.my.empty}</div>
            : d.contracts.map((c) => <MyContract key={c.id} c={c} data={d} onChanged={list.reload} />)}
        </div>
        <div className="xnote"><Icon name="alert" /><div>{T.my.note}</div></div>
      </section>
    </>,
  )
}
