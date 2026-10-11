import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BasisNote, GoCell, GoLegend, LoadState, MarketApplyBand, MarketPager, TitleCell, TypeTag } from '../components/market/MarketParts'
import { fmtDate, monthLabel, monthNo, rowTarget, useGo, useNow } from '../components/market/marketUtils'
import { marketApi } from '../api/market'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { MARKET_COMMON, MONTHLY_TEXT } from '../data/marketContent'
import type { MonthStat } from '../types/market'

const PLACE = findMenuItem('analysis', 'monthly-trend')
const PAGE_SIZE = 20

/** 쌓는 순서(아래부터)이자 범례 순서. 색은 wjbid-market.css 의 묶음 색 */
const ORDER = ['bid', 'pre', 'award', 'other'] as const
type Part = (typeof ORDER)[number]
const PART_GROUP = { bid: 'BID', pre: 'PRE', award: 'AWARD', other: 'OTHER' } as const

/** 그래프 크기. 화면 정본의 막대 그래프와 같다 */
const W = 848
const H = 320
const M = { l: 44, r: 8, t: 30, b: 40 }
const BAR_MAX = 44
const SEG_GAP = 2
const RADIUS = 4
const TICKS = 4

/** 눈금 간격: 최댓값을 TICKS 칸 남짓으로 나누는 1, 2, 2.5, 3, 5 의 10 거듭제곱 배 */
function niceStep(max: number) {
  const raw = Math.max(max, 1) / TICKS
  const pow = 10 ** Math.floor(Math.log10(raw))
  return [1, 2, 2.5, 3, 5, 10].map((m) => m * pow).find((s) => s >= raw) ?? raw
}

/** 월별 동향 (CR-135, BID-MKT-005, T3-3 「월별 동향」). 달마다 공고 수를 유형 넷으로 쌓은 막대와 표, 고른 달의 공고 */
export default function MonthlyTrendPage() {
  const [params, setParams] = useSearchParams()
  const [tip, setTip] = useState<{ x: number; y: number; text: string } | null>(null)
  const page = Number(params.get('page')) || 0
  const go = useGo()
  const stats = useMarketLoad(() => marketApi.monthly(), 'monthly')
  const now = useNow()

  const months = stats.data?.months ?? []
  const busiest = months.reduce<MonthStat | null>((a, m) => (!a || m.total > a.total ? m : a), null)
  const sel = params.get('month') || busiest?.month || ''
  const list = useMarketLoad(
    () => (sel ? marketApi.collected({ month: sel, page, size: PAGE_SIZE }) : Promise.resolve(null)),
    `${sel}|${page}`,
  )

  if (!PLACE) return <Navigate to="/" replace />
  const t = MONTHLY_TEXT
  const d = stats.data
  const years = new Set(months.map((m) => m.month.slice(0, 4)))
  const label = (ym: string) => monthLabel(ym, years.size > 1)
  const choose = (ym: string) => setParams(ym ? { month: ym } : {})

  const step = niceStep(Math.max(0, ...months.map((m) => m.total)))
  const yMax = Math.max(step, Math.ceil(Math.max(0, ...months.map((m) => m.total)) / step) * step)
  const pw = W - M.l - M.r
  const ph = H - M.t - M.b
  const band = months.length ? pw / months.length : pw
  const bw = Math.min(BAR_MAX, band * 0.6)
  const y = (v: number) => M.t + ph - (ph * v) / yMax
  const tipText = (m: MonthStat) =>
    [t.tip(monthNo(m.month), m.total), ...ORDER.map((p) => `${t.groups[PART_GROUP[p]]} ${m[p]}`)].join(' · ')

  const today = now.today
  const basisText = d && d.dataFrom
    ? t.basis(
      fmtDate(d.dataFrom), fmtDate(d.dataTo), d.total,
      d.emptyMonths.map(label).join('·'),
      d.currentMonth === today.slice(0, 7) ? t.currentMonth(monthNo(today), Number(today.slice(8, 10))) : '',
    )
    : ''
  const selStat = months.find((m) => m.month === sel)
  const rows = list.data?.content ?? []

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        {basisText && <BasisNote text={basisText} />}
        <div className="chd">
          <h2 className="h2">{t.chartTitle}</h2>
          <div className="lgd">
            {ORDER.map((p) => <span key={p}><i className={`sw ${p}`} />{t.groups[PART_GROUP[p]]}</span>)}
          </div>
        </div>
        {months.length === 0 ? (
          <LoadState state={stats.status === 'error' ? 'error' : stats.status === 'loading' ? 'loading' : 'empty'} icon="cal" onRetry={stats.reload} />
        ) : (
          <>
            <div className="chw">
              <svg className="mchart" viewBox={`0 0 ${W} ${H}`} role="img" aria-label={t.chartTitle}>
                {Array.from({ length: Math.round(yMax / step) + 1 }, (_, i) => i * step).map((v) => (
                  <g key={v}>
                    <line x1={M.l} x2={W - M.r} y1={y(v)} y2={y(v)} className={v === 0 ? 'base' : 'grid'} />
                    <text x={M.l - 10} y={y(v) + 5} className="yl">{v}</text>
                  </g>
                ))}
                {months.map((m, i) => {
                  const cx = M.l + band * i + band / 2
                  const x0 = cx - bw / 2
                  const parts = ORDER.filter((p) => m[p] > 0)
                  let base = M.t + ph
                  const show = () => setTip({ x: (cx / W) * 100, y: (y(m.total) / H) * 100, text: tipText(m) })
                  return (
                    <g
                      key={m.month}
                      className="hit"
                      tabIndex={0}
                      role="button"
                      aria-pressed={m.month === sel}
                      aria-label={tipText(m)}
                      onClick={() => choose(m.month)}
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); choose(m.month) } }}
                      onMouseEnter={show}
                      onFocus={show}
                      onMouseLeave={() => setTip(null)}
                      onBlur={() => setTip(null)}
                    >
                      {m.month === sel && <rect x={M.l + band * i + 4} y={M.t - 26} width={band - 8} height={ph + 26 + 34} rx={8} className="selb" />}
                      <rect className="hb" x={M.l + band * i} y={M.t - 26} width={band} height={ph + 26 + 34} fill="transparent" />
                      {parts.map((p, j) => {
                        const h = (ph * m[p]) / yMax
                        const top = base - h
                        const last = j === parts.length - 1
                        const hh = Math.max(h - (last ? 0 : SEG_GAP), 1)
                        const yt = top + (last ? 0 : SEG_GAP)
                        const bottom = base
                        base = top
                        if (!last) return <rect key={p} className={p} x={x0} y={yt} width={bw} height={hh} />
                        const r = Math.min(RADIUS, hh)
                        return (
                          <path
                            key={p}
                            className={p}
                            d={`M${x0},${bottom} V${yt + r} Q${x0},${yt} ${x0 + r},${yt} H${x0 + bw - r} Q${x0 + bw},${yt} ${x0 + bw},${yt + r} V${bottom} Z`}
                          />
                        )
                      })}
                      <text x={cx} y={y(m.total) - 8} className="tv">{m.total}</text>
                      <text x={cx} y={M.t + ph + 24} className={`xl${m.month === sel ? ' on' : ''}`}>{label(m.month)}</text>
                    </g>
                  )
                })}
              </svg>
              {tip && <div className="ctip" style={{ left: `${tip.x}%`, top: `${tip.y}%` }}>{tip.text}</div>}
            </div>
            <p className="chn">{t.chartNote}</p>
            <table className="mtbl">
              <thead>
                <tr><th>{t.head.type}</th>{months.map((m) => <th key={m.month}>{label(m.month)}</th>)}<th className="sum">{t.head.sum}</th></tr>
              </thead>
              <tbody>
                {ORDER.map((p) => (
                  <tr key={p}>
                    <th><i className={`sw ${p}`} />{t.groups[PART_GROUP[p]]}</th>
                    {months.map((m) => <td key={m.month} className={m[p] === 0 ? 'z' : undefined}>{m[p]}</td>)}
                    <td className="sum">{months.reduce((n, m) => n + m[p as Part], 0)}</td>
                  </tr>
                ))}
                <tr className="tot">
                  <th>{t.total}</th>
                  {months.map((m) => <td key={m.month}>{m.total}</td>)}
                  <td className="sum">{d?.total ?? 0}</td>
                </tr>
              </tbody>
            </table>
          </>
        )}
      </section>
      {sel && (
        <section className="cbx pad">
          <h2 className="h2">{t.monthTitle(label(sel))} <small>{t.monthSub(selStat?.total ?? list.data?.totalElements ?? 0)}</small></h2>
          <table className="otbl">
            <thead>
              <tr>
                <th style={{ width: 150 }}>{t.listHead.type}</th>
                <th>{t.listHead.title}</th>
                <th style={{ width: 118 }}>{t.listHead.posted}</th>
                <th style={{ width: 44 }} aria-hidden="true" />
              </tr>
            </thead>
            {rows.length > 0 && (
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id} className="row" onClick={() => go(rowTarget(r))}>
                    <td><TypeTag label={r.typeKo} /></td>
                    <TitleCell title={r.title} titleKo={r.titleKo} sol={r.solicitationNumber} />
                    <td className="dt">{fmtDate(r.postedDate)}</td>
                    <GoCell target={rowTarget(r)} />
                  </tr>
                ))}
              </tbody>
            )}
          </table>
          {rows.length === 0 && (
            <LoadState state={list.status === 'loading' ? 'loading' : list.status === 'error' ? 'error' : 'empty'} icon="cal" onRetry={list.reload} />
          )}
          {rows.length > 0 && (
            <>
              <GoLegend />
              <MarketPager page={page} totalPages={list.data?.totalPages ?? 1} onChange={(p) => setParams(p ? { month: sel, page: String(p) } : { month: sel })} />
            </>
          )}
        </section>
      )}
      <MarketApplyBand title={MARKET_COMMON.band.title} desc={MARKET_COMMON.band.desc} />
    </SubPageFrame>
  )
}
