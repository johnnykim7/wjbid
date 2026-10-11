import { Navigate, useLocation, useSearchParams } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadState, MarketPager, Rich } from '../components/market/MarketParts'
import {
  ContractTable, GuestGate, ListCount, NaicsPick, RecompeteBand, RuleBox, SortSeg, SourceNote, SyncingBox,
} from '../components/recompete/RecompeteParts'
import { isSynced, useNaicsName } from '../components/recompete/recompeteUtils'
import { federalApi } from '../api/federal'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { useAuth } from '../context/AuthContext'
import { FEDERAL_COMMON as C, FEDERAL_LINKS as L, NEXT_MONTH_TEXT as T } from '../data/recompeteContent'
import { RECOMPETE_PATHS } from '../data/recompetePaths'
import type { RecompeteCategory, RecompeteSort } from '../types/federal'

const PLACE = findMenuItem('recompete', 'next-month')
const PAGE_SIZE = 20
const NUM_CATEGORIES: (keyof typeof T.nums)[] = ['RECOMPETE', 'EXTENDABLE', 'IN_PARENT']
const CATEGORIES: RecompeteCategory[] = ['RECOMPETE', 'EXTENDABLE', 'IN_PARENT', 'UNCONFIRMED']

/** 다음 달 공고 (CR-142, BID-MKT-028, T3-3 「다음 달 공고」). 다음 달 끝나는 계약을 구분(예측)과 근거로 */
export default function NextMonthPage() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const [params, setParams] = useSearchParams()
  const rawCategory = params.get('category') as RecompeteCategory | null
  const category = rawCategory && CATEGORIES.includes(rawCategory) ? rawCategory : null
  const naics = params.get('naics') || null
  const sort: RecompeteSort = params.get('sort') === 'AMOUNT' ? 'AMOUNT' : 'END'
  const page = Number(params.get('page')) || 0
  const naicsName = useNaicsName(naics)

  const list = useMarketLoad(
    () => (isAuthenticated
      ? federalApi.recompete({ category: category ?? undefined, naics: naics ?? undefined, sort, page, size: PAGE_SIZE })
      : Promise.resolve(null)),
    `${isAuthenticated}|${category}|${naics}|${sort}|${page}`,
  )

  if (!PLACE) return <Navigate to="/" replace />
  const d = list.data

  const update = (next: { category?: RecompeteCategory | null; naics?: string | null; sort?: RecompeteSort; page?: number }) => {
    const p = new URLSearchParams()
    const nc = next.category !== undefined ? next.category : category
    const nn = next.naics !== undefined ? next.naics : naics
    const ns = next.sort ?? sort
    if (nc) p.set('category', nc)
    if (nn) p.set('naics', nn)
    if (ns !== 'END') p.set('sort', ns)
    if (next.page) p.set('page', String(next.page))
    setParams(p)
  }

  const head = <PageHead title={T.title} desc={T.desc} />
  const frame = (body: React.ReactNode) => <SubPageFrame group={PLACE.group} item={PLACE.item}>{head}{body}</SubPageFrame>

  if (!isAuthenticated) return frame(<GuestGate from={location.pathname + location.search} />)
  if (!d) {
    return frame(
      <section className="cbx pad">
        <LoadState state={list.status === 'error' ? 'error' : 'loading'} onRetry={list.reload} />
      </section>,
    )
  }
  if (!isSynced(d.source)) {
    return frame(
      <>
        <SyncingBox desc={T.syncingDesc} />
        <RecompeteBand icon="bell" title={T.syncingBand.title} desc={T.syncingBand.desc} links={[{ label: L.optionAlert, to: RECOMPETE_PATHS.optionAlert }]} />
      </>,
    )
  }

  const monthTitle = C.monthTitle(d.month)
  const totalPages = Math.max(1, Math.ceil(d.totalElements / PAGE_SIZE))

  return frame(
    <>
      <SourceNote source={d.source} />
      <RuleBox defaultOpen={location.hash === '#rule'} />
      <div className="rc-nums">
        {NUM_CATEGORIES.map((c) => (
          <Button key={c} className={`wj-reset${category === c ? ' on' : ''}`} aria-pressed={category === c} onClick={() => update({ category: category === c ? null : c, page: 0 })}>
            <span>{C.category[c]}{c !== 'IN_PARENT' && <i>{C.predict}</i>}</span>
            <b className={c === 'RECOMPETE' ? 'bl' : undefined}>{d.counts[c] ?? 0}<small>{T.unit}</small></b>
            <em>{T.nums[c]}</em>
          </Button>
        ))}
      </div>
      <p className="rc-uc"><Rich text={T.unconfirmed(monthTitle, d.counts.UNCONFIRMED ?? 0)} /></p>
      <section className="cbx pad">
        <h2 className="h2">{C.endingIn(monthTitle)}<small>{C.endingSub}</small></h2>
        <div className="rc-fl" style={{ marginTop: 14 }}>
          <NaicsPick
            value={naics}
            label={naics ? <><span className="mono">{naics}</span><b>{naicsName ?? ''}</b></> : undefined}
            onChange={(code) => update({ naics: code, page: 0 })}
          />
          <SortSeg value={sort} onChange={(s) => update({ sort: s, page: 0 })} />
          <ListCount label={category ? C.category[category] : null} total={d.totalElements} page={page} size={PAGE_SIZE} />
        </div>
        <ContractTable rows={d.items} empty={category ? C.emptyCategory : T.empty} />
        {d.totalElements > 0 && <MarketPager page={page} totalPages={totalPages} onChange={(p) => update({ page: p })} />}
        <p className="fn">{C.listNote}</p>
      </section>
      <RecompeteBand
        icon="cal"
        title={T.band.title}
        desc={T.band.desc}
        links={[{ label: L.optionAlert, to: RECOMPETE_PATHS.optionAlert }, { label: L.calendar, to: RECOMPETE_PATHS.calendar }]}
      />
    </>,
  )
}
