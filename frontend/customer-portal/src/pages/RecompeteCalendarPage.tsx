import { Link, Navigate, useLocation, useSearchParams } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadState, MarketPager } from '../components/market/MarketParts'
import {
  ContractTable, GuestGate, ListCount, NaicsPick, RecompeteBand, SortSeg, SourceNote, SyncingBox,
} from '../components/recompete/RecompeteParts'
import { isSynced, useNaicsName } from '../components/recompete/recompeteUtils'
import { federalApi } from '../api/federal'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { useAuth } from '../context/AuthContext'
import { todayKst } from '../components/market/marketUtils'
import { CALENDAR_TEXT as T, FEDERAL_COMMON as C, FEDERAL_LINKS as L, RULE_TEXT } from '../data/recompeteContent'
import { RECOMPETE_PATHS } from '../data/recompetePaths'
import type { CalendarMonth, RecompeteSort } from '../types/federal'

const PLACE = findMenuItem('recompete', 'recompete-calendar')
const PAGE_SIZE = 20
const MONTH_RE = /^\d{4}-\d{2}$/

/** 막대 한 칸의 너비(%) */
const share = (n: number, total: number) => `${((n / total) * 100).toFixed(1)}%`

function MonthCell({ m, on, thisMonth, onPick }: { m: CalendarMonth; on: boolean; thisMonth: boolean; onPick: () => void }) {
  const total = m.RECOMPETE + m.EXTENDABLE + m.IN_PARENT + m.UNCONFIRMED || 1
  const seg = (n: number, cls: string) => (n > 0 ? <i className={cls} style={{ width: share(n, total) }} /> : null)
  return (
    <Button className={`wj-reset rc-mc${on ? ' on' : ''}`} aria-pressed={on} onClick={onPick}>
      <div className="mh">{C.monthTitle(m.month)}{thisMonth && <small>{T.thisMonth}</small>}</div>
      <div className="rcn"><b className={m.RECOMPETE ? undefined : 'z'}>{m.RECOMPETE}</b>{C.category.RECOMPETE}</div>
      <div className="sb">{seg(m.RECOMPETE, 'rc-a')}{seg(m.EXTENDABLE, 'rc-b')}{seg(m.IN_PARENT, 'rc-c')}{seg(m.UNCONFIRMED, 'rc-d')}</div>
      <dl>
        <dt>{C.category.EXTENDABLE}</dt><dd className={m.EXTENDABLE ? undefined : 'z'}>{m.EXTENDABLE}</dd>
        <dt>{C.category.IN_PARENT}</dt><dd>{m.IN_PARENT}</dd>
        {m.UNCONFIRMED > 0 && <><dt>{T.unconfirmedShort}</dt><dd>{m.UNCONFIRMED}</dd></>}
      </dl>
    </Button>
  )
}

/** 재발주 달력 (CR-142, BID-MKT-029, T3-3 「재발주 달력」). 이번 달부터 12달, 달마다 구분별 계약 수와 고른 달 목록 */
export default function RecompeteCalendarPage() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const [params, setParams] = useSearchParams()
  const rawMonth = params.get('month') ?? ''
  const naics = params.get('naics') || null
  const sort: RecompeteSort = params.get('sort') === 'AMOUNT' ? 'AMOUNT' : 'END'
  const page = Number(params.get('page')) || 0
  const naicsName = useNaicsName(naics)

  const cal = useMarketLoad(
    () => (isAuthenticated ? federalApi.calendar(naics ?? undefined) : Promise.resolve(null)),
    `${isAuthenticated}|${naics}`,
  )
  const months = cal.data?.months ?? []
  const month = MONTH_RE.test(rawMonth) && months.some((m) => m.month === rawMonth) ? rawMonth : (months[0]?.month ?? '')
  const list = useMarketLoad(
    () => (isAuthenticated && month
      ? federalApi.recompete({ month, naics: naics ?? undefined, sort, page, size: PAGE_SIZE })
      : Promise.resolve(null)),
    `${isAuthenticated}|${month}|${naics}|${sort}|${page}`,
  )

  if (!PLACE) return <Navigate to="/" replace />

  const update = (next: { month?: string; naics?: string | null; sort?: RecompeteSort; page?: number }) => {
    const p = new URLSearchParams()
    const nm = next.month ?? (rawMonth ? month : '')
    const nn = next.naics !== undefined ? next.naics : naics
    const ns = next.sort ?? sort
    if (nm) p.set('month', nm)
    if (nn) p.set('naics', nn)
    if (ns !== 'END') p.set('sort', ns)
    if (next.page) p.set('page', String(next.page))
    setParams(p)
  }

  const head = <PageHead title={T.title} desc={T.desc} />
  const frame = (body: React.ReactNode) => <SubPageFrame group={PLACE.group} item={PLACE.item}>{head}{body}</SubPageFrame>
  const band = (
    <RecompeteBand icon="bell" title={T.band.title} desc={T.band.desc} links={[{ label: L.optionAlert, to: RECOMPETE_PATHS.optionAlert }]} />
  )

  if (!isAuthenticated) return frame(<GuestGate from={location.pathname + location.search} />)
  const c = cal.data
  if (!c) {
    return frame(
      <section className="cbx pad">
        <LoadState state={cal.status === 'error' ? 'error' : 'loading'} onRetry={cal.reload} />
      </section>,
    )
  }
  if (!isSynced(c.source)) return frame(<><SyncingBox desc={T.syncingDesc} />{band}</>)

  const thisMonth = todayKst().slice(0, 7)
  const anyUnconfirmed = months.some((m) => m.UNCONFIRMED > 0)
  const d = list.data
  const totalPages = d ? Math.max(1, Math.ceil(d.totalElements / PAGE_SIZE)) : 1

  return frame(
    <>
      <SourceNote source={c.source} />
      <div className="rc-fl">
        <NaicsPick
          value={naics}
          label={naics ? <><span className="mono">{naics}</span><b>{naicsName ?? ''}</b></> : undefined}
          onChange={(code) => update({ naics: code, page: 0 })}
        />
        <Link className="rlk" to={`${RECOMPETE_PATHS.nextMonth}#rule`}>{RULE_TEXT.title}</Link>
      </div>
      <div className="rc-cal">
        {months.map((m) => (
          <MonthCell key={m.month} m={m} on={m.month === month} thisMonth={m.month === thisMonth} onPick={() => update({ month: m.month, page: 0 })} />
        ))}
      </div>
      <div className="rc-lg">
        <span><i className="rc-a" />{C.category.RECOMPETE}</span>
        <span><i className="rc-b" />{C.category.EXTENDABLE}</span>
        <span><i className="rc-c" />{C.category.IN_PARENT}</span>
        {anyUnconfirmed && <span><i className="rc-d" />{C.category.UNCONFIRMED}</span>}
      </div>
      <section className="cbx pad">
        <h2 className="h2">{month && C.endingIn(C.monthTitle(month))}<small>{C.endingSub}</small></h2>
        {d ? (
          <>
            <div className="rc-fl" style={{ marginTop: 14 }}>
              <SortSeg value={sort} onChange={(s) => update({ sort: s, page: 0 })} />
              <ListCount label={null} total={d.totalElements} page={page} size={PAGE_SIZE} />
            </div>
            <ContractTable rows={d.items} empty={C.emptyMonth} />
            {d.totalElements > 0 && <MarketPager page={page} totalPages={totalPages} onChange={(p) => update({ page: p })} />}
            <p className="fn">{C.listNote}</p>
          </>
        ) : (
          <LoadState state={list.status === 'error' ? 'error' : 'loading'} onRetry={list.reload} />
        )}
      </section>
      {band}
    </>,
  )
}
