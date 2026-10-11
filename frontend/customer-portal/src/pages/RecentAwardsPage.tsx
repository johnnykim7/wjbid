import { Navigate, useSearchParams } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BasisNote, GoCell, GoLegend, GoLink, LoadState, MarketApplyBand, MarketPager, TitleCell } from '../components/market/MarketParts'
import { fmtDate, money, monthLabel, parseAmount, rowTarget, targetOf, useGo } from '../components/market/marketUtils'
import { marketApi } from '../api/market'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { AWARD_TEXT, MARKET_COMMON } from '../data/marketContent'
import type { AwardSort } from '../types/market'

const PLACE = findMenuItem('awards', 'recent-awards')
const PAGE_SIZE = 20
const SORTS: AwardSort[] = ['LATEST', 'AMOUNT']

/** 최근 낙찰 (CR-135, BID-MKT-004, T3-3 「최근 낙찰」). 낙찰 공지와 공개된 금액, 같은 공고번호의 원래 공고 */
export default function RecentAwardsPage() {
  const [params, setParams] = useSearchParams()
  const month = params.get('month') ?? ''
  const sort: AwardSort = params.get('sort') === 'AMOUNT' ? 'AMOUNT' : 'LATEST'
  const page = Number(params.get('page')) || 0
  const go = useGo()

  const basis = useMarketLoad(() => marketApi.awards({ size: 1 }), 'basis')
  const list = useMarketLoad(
    () => marketApi.awards({ month: month || undefined, sort, page, size: PAGE_SIZE }),
    `${month}|${sort}|${page}`,
  )

  if (!PLACE) return <Navigate to="/" replace />
  const t = AWARD_TEXT
  const b = basis.data
  const d = list.data

  const update = (next: { month?: string; sort?: AwardSort; page?: number }) => {
    const p = new URLSearchParams()
    const nm = next.month ?? month
    const ns = next.sort ?? sort
    if (nm) p.set('month', nm)
    if (ns !== 'LATEST') p.set('sort', ns)
    if (next.page) p.set('page', String(next.page))
    setParams(p)
  }

  const months = d?.monthCounts ?? []
  const years = new Set(months.map((m) => m.month.slice(0, 4)))
  const rows = d?.content ?? []
  const sum = d?.summary

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        {b && b.dataFrom && (
          <BasisNote text={t.basis(fmtDate(b.dataFrom), fmtDate(b.dataTo), b.summary.total, b.summary.amountKnownCount, b.summary.awardeeKnownCount)} />
        )}
        {sum && (
          <div className="nums3">
            <div><small>{t.nums.count}</small><b>{sum.total}<em>{t.nums.unit}</em></b></div>
            <div><small>{t.nums.sum(sum.amountKnownCount)}</small><b>{sum.amountSum != null ? money(sum.amountSum) : '-'}</b></div>
            <div><small>{t.nums.max}</small><b>{sum.amountMax != null ? money(sum.amountMax) : '-'}</b></div>
          </div>
        )}
        <div className="tbar">
          <div className="chips">
            <Button className={`wj-reset${month ? '' : ' on'}`} onClick={() => update({ month: '', page: 0 })}>{t.all}<em>{d?.total ?? 0}</em></Button>
            {months.map((m) => (
              <Button key={m.month} className={`wj-reset${month === m.month ? ' on' : ''}`} onClick={() => update({ month: m.month, page: 0 })}>
                {monthLabel(m.month, years.size > 1)}<em>{m.count}</em>
              </Button>
            ))}
          </div>
          <div className="seg">
            {SORTS.map((s) => (
              <Button key={s} className={`wj-reset${sort === s ? ' on' : ''}`} aria-pressed={sort === s} onClick={() => update({ sort: s, page: 0 })}>{t.sort[s]}</Button>
            ))}
          </div>
        </div>
        <table className="otbl">
          <thead>
            <tr>
              <th style={{ width: 118 }}>{t.head.date}</th>
              <th>{t.head.title}</th>
              <th className="am" style={{ width: 150 }}>{t.head.amount}</th>
              <th style={{ width: 112 }}>{t.head.original}</th>
              <th style={{ width: 44 }} aria-hidden="true" />
            </tr>
          </thead>
          {rows.length > 0 && (
            <tbody>
              {rows.map((r) => {
                const amount = parseAmount(r.awardAmount)
                const original = r.originalId ? targetOf(r.originalPublicNoticeId, r.originalId) : null
                return (
                  <tr key={r.id} className="row" onClick={() => go(rowTarget(r))}>
                    <td className="dt">{fmtDate(r.postedDate)}</td>
                    <TitleCell title={r.title} titleKo={r.titleKo} sol={r.solicitationNumber}>
                      {r.awardeeName && <span className="tag2">{t.awardee(r.awardeeName)}</span>}
                    </TitleCell>
                    {amount != null ? <td className="am">{money(amount)}</td> : <td className="am na">{MARKET_COMMON.notDisclosed}</td>}
                    {original ? (
                      <td className="og">
                        <GoLink target={original}>{t.original}<Icon name="cr" /></GoLink>
                      </td>
                    ) : <td className="og na">-</td>}
                    <GoCell target={rowTarget(r)} />
                  </tr>
                )
              })}
            </tbody>
          )}
        </table>
        {rows.length === 0 && (
          <LoadState state={list.status === 'loading' ? 'loading' : list.status === 'error' ? 'error' : 'empty'} icon="okc" onRetry={list.reload} />
        )}
        {rows.length > 0 && (
          <>
            <GoLegend />
            <MarketPager page={page} totalPages={d?.totalPages ?? 1} onChange={(p) => update({ page: p })} />
          </>
        )}
      </section>
      <MarketApplyBand title={MARKET_COMMON.band.title} desc={MARKET_COMMON.band.desc} />
    </SubPageFrame>
  )
}
