import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { Button, Input } from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BasisNote, GoLink, LoadState, MarketApplyBand } from '../components/market/MarketParts'
import { fmtDate, money, rowTarget } from '../components/market/marketUtils'
import { awardeeBaseApi } from '../api/awardeeBase'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { AWARDEE_TEXT as T } from '../data/awardeeBaseContent'
import { MARKET_COMMON } from '../data/marketContent'
import type { Awardee, AwardeeAward, AwardeeSort } from '../types/awardeeBase'
import '../wjbid-awardee.css'

const PLACE = findMenuItem('awards', 'awardees')
const SORTS: AwardeeSort[] = ['COUNT', 'AMOUNT']
/** 순위 꼬리표를 진하게 칠하는 위 몇 곳 */
const TOP_RANKS = 3
const RECENT_AWARDS_PATH = '/awards/recent'

function AmountCell({ value, children }: { value: number | null; children?: ReactNode }) {
  if (value == null) return <td className="am na">{MARKET_COMMON.notDisclosed}</td>
  return <td className="am">{money(value)}{children}</td>
}

function AwardLines({ awards }: { awards: AwardeeAward[] }) {
  return (
    <table>
      <tbody>
        {awards.map((w) => {
          const target = rowTarget(w)
          return (
            <tr key={w.id}>
              <td className="dt">{fmtDate(w.postedDate)}</td>
              <td className="ttl"><b>{w.titleKo ?? w.title}</b><span>{w.solicitationNumber ?? '-'}</span></td>
              <AmountCell value={w.awardAmountUsd} />
              <td className="go">
                <GoLink className="ext" target={target}>
                  {T.goNotice}
                  <Icon name="cr" />
                </GoLink>
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

function AwardeeRows({ a, open, onToggle }: { a: Awardee; open: boolean; onToggle: () => void }) {
  const partial = a.amountSum != null && a.amountCount < a.awardCount
  return (
    <>
      <tr className={`row${open ? ' opn' : ''}`} onClick={onToggle}>
        <td className="c"><span className={`rk${a.rank <= TOP_RANKS ? ' t' : ''}`}>{a.rank}</span></td>
        <td className="co"><b>{a.name}</b>{a.uei && <small>{T.uei(a.uei)}</small>}</td>
        <td className="cnt"><span className="num">{a.awardCount}</span>{T.unit}</td>
        <AmountCell value={a.amountSum}>{partial && <small>{T.amountPart(a.amountCount)}</small>}</AmountCell>
        <td className="dt c">{fmtDate(a.lastPostedDate)}</td>
        <td className="tog">
          <Button
            className="wj-reset"
            aria-expanded={open}
            aria-label={open ? T.close : T.open}
            onClick={(e) => {
              e.stopPropagation()
              onToggle()
            }}
          >
            <Icon name={open ? 'cu' : 'cd'} />
          </Button>
        </td>
      </tr>
      {open && (
        <tr className="sub2">
          <td colSpan={6}><AwardLines awards={a.awards} /></td>
        </tr>
      )}
    </>
  )
}

/** 낙찰 업체 (CR-136, BID-MKT-009, T3-3 「낙찰 업체」). 업체 이름이 있는 낙찰 공지를 업체별로 센다(BIZ-029) */
export default function AwardeesPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q')?.trim() ?? ''
  const sort: AwardeeSort = params.get('sort') === 'AMOUNT' ? 'AMOUNT' : 'COUNT'
  const [draft, setDraft] = useState(q)
  const [open, setOpen] = useState<ReadonlySet<string>>(() => new Set())
  const list = useMarketLoad(() => awardeeBaseApi.awardees({ q: q || undefined, sort }), `${q}|${sort}`)

  if (!PLACE) return <Navigate to="/" replace />
  const d = list.data

  const update = (next: { q?: string; sort?: AwardeeSort }) => {
    const p = new URLSearchParams()
    const nq = next.q ?? q
    const ns = next.sort ?? sort
    if (nq) p.set('q', nq)
    if (ns !== 'COUNT') p.set('sort', ns)
    setParams(p)
  }
  const search = (e: FormEvent) => {
    e.preventDefault()
    update({ q: draft.trim() })
  }
  const clear = () => {
    setDraft('')
    update({ q: '' })
  }
  const toggle = (key: string) => setOpen((prev) => {
    const next = new Set(prev)
    if (next.has(key)) next.delete(key)
    else next.add(key)
    return next
  })

  const collecting = d != null && d.awardsWithAwardee === 0

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={T.title} desc={T.desc} />
      <section className="cbx pad awd">
        {d?.dataFrom && d.dataTo && d.awardsWithAwardee > 0 && (
          <BasisNote text={T.basis(fmtDate(d.dataFrom), fmtDate(d.dataTo), d.totalAwards, d.awardsWithAwardee)} />
        )}

        {!d && <LoadState state={list.status === 'error' ? 'error' : 'loading'} onRetry={list.reload} />}

        {collecting && (
          <div className="empty2">
            <div className="icn"><Icon name="okc" /></div>
            <b>{T.emptyTitle}</b>
            <span>{T.emptyDesc}</span>
            <Link className="bigb o retry" to={RECENT_AWARDS_PATH}>{T.emptyAction}</Link>
          </div>
        )}

        {d && !collecting && (
          <>
            <div className="tbar">
              <form className="gsch" onSubmit={search} role="search">
                <Input className="wj-reset in" value={draft} onChange={(e) => setDraft(e.target.value)} placeholder={T.search} aria-label={T.search} />
                <Button type="submit" className="wj-reset bigb"><Icon name="search" />{T.searchGo}</Button>
              </form>
              <div className="seg">
                {SORTS.map((s) => (
                  <Button key={s} className={`wj-reset${sort === s ? ' on' : ''}`} aria-pressed={sort === s} onClick={() => update({ sort: s })}>{T.sort[s]}</Button>
                ))}
              </div>
            </div>

            {d.awardees.length === 0 ? (
              <div className="empty2">
                <div className="icn"><Icon name="search" /></div>
                <b>{T.noMatch(q)}</b>
                <Button className="wj-reset bigb o retry" onClick={clear}>{T.clear}</Button>
              </div>
            ) : (
              <table className="otbl">
                <thead>
                  <tr>
                    <th className="c w-rank">{T.head.rank}</th>
                    <th>{T.head.name}</th>
                    <th className="cnt w-cnt">{T.head.count}</th>
                    <th className="am w-sum">{T.head.sum}</th>
                    <th className="c w-last">{T.head.last}</th>
                    <th className="w-tog" aria-hidden="true" />
                  </tr>
                </thead>
                <tbody>
                  {d.awardees.map((a) => (
                    <AwardeeRows key={a.key} a={a} open={open.has(a.key)} onToggle={() => toggle(a.key)} />
                  ))}
                </tbody>
              </table>
            )}
            <p className="fn">{T.footnote}</p>
          </>
        )}
      </section>
      <MarketApplyBand title={T.band.title} desc={T.band.desc} />
    </SubPageFrame>
  )
}
