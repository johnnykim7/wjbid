import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BasisNote, GoCell, LoadState, MarketApplyBand, TitleCell } from '../components/market/MarketParts'
import { fmtDate, rowTarget, useGo } from '../components/market/marketUtils'
import { marketApi } from '../api/market'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { MARKET_COMMON, NAICS_TEXT } from '../data/marketContent'

const PLACE = findMenuItem('analysis', 'naics-trend')
/** 업종 코드가 없는 공고의 묶음 값(백엔드 NaicsTopLevelKoMapper.NONE) */
const NONE = 'NONE'

interface Bar {
  code: string
  name: string
  count: number
}

/** 업종별 발주 (CR-135, BID-MKT-006, T3-3 「업종별 발주」). 업종 대분류별 공고 수, 고른 업종의 세부 코드와 최근 공고 */
export default function NaicsTrendPage() {
  const [params, setParams] = useSearchParams()
  const [tip, setTip] = useState<{ x: number; y: number; text: string } | null>(null)
  const go = useGo()
  const stats = useMarketLoad(() => marketApi.naics(), 'naics')
  const t = NAICS_TEXT
  const d = stats.data

  const bars: Bar[] = [
    ...(d?.sectors ?? []).map((s) => ({ code: s.code, name: s.nameKo, count: s.count })),
    ...(d && d.noneCount > 0 ? [{ code: NONE, name: t.none, count: d.noneCount }] : []),
  ]
  const sel = params.get('sector') || bars[0]?.code || ''
  const recent = useMarketLoad(
    () => (sel ? marketApi.collected({ sector: sel, latestOnly: true, size: t.recentSize }) : Promise.resolve(null)),
    sel,
  )

  if (!PLACE) return <Navigate to="/" replace />
  const max = Math.max(1, ...bars.map((b) => b.count))
  const selBar = bars.find((b) => b.code === sel)
  const subCodes = [...(d?.sectors.find((s) => s.code === sel)?.subCodes ?? [])].sort((a, b) => b.count - a.count)
  const subMax = Math.max(1, ...subCodes.map((s) => s.count))
  const rows = recent.data?.content ?? []
  const pct = (n: number) => Math.round((100 * n) / Math.max(1, d?.total ?? 1))

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        {d && d.dataFrom && <BasisNote text={t.basis(fmtDate(d.dataFrom), fmtDate(d.dataTo), d.total, d.codedCount, d.noneCount)} />}
        <div className="chd"><h2 className="h2">{t.chartTitle} <small>{t.chartSub}</small></h2></div>
        {bars.length === 0 ? (
          <LoadState state={stats.status === 'error' ? 'error' : stats.status === 'loading' ? 'loading' : 'empty'} icon="folder" onRetry={stats.reload} />
        ) : (
          <div className="chw" onMouseLeave={() => setTip(null)}>
            <div className="nbars">
              {bars.map((b) => {
                const text = t.tip(b.name, b.count, pct(b.count))
                return (
                  <Button
                    key={b.code}
                    className={`wj-reset nb${b.code === sel ? ' on' : ''}${b.code === NONE ? ' none' : ''}`}
                    aria-pressed={b.code === sel}
                    aria-label={text}
                    onClick={() => setParams({ sector: b.code })}
                    onMouseMove={(e) => {
                      const box = e.currentTarget.parentElement?.getBoundingClientRect()
                      if (box) setTip({ x: e.clientX - box.left, y: e.clientY - box.top, text })
                    }}
                  >
                    <span className="nm">{b.name}{b.code !== NONE && <span className="cd">{b.code}</span>}</span>
                    <span className="tr"><i style={{ width: `${(100 * b.count) / max}%` }} /></span>
                    <b>{b.count}</b>
                  </Button>
                )
              })}
            </div>
            {tip && <div className="ctip" style={{ left: tip.x, top: tip.y }}>{tip.text}</div>}
          </div>
        )}
      </section>
      {selBar && (
        <section className="cbx pad">
          <h2 className="h2">
            {selBar.name}
            {selBar.code !== NONE && <span className="mono h2c">{selBar.code}</span>}
            <small>{t.count(selBar.count)}</small>
          </h2>
          <div className="nsel">
            <div>
              {subCodes.length > 0 && (
                <>
                  <h3 className="h3">{t.subTitle}</h3>
                  <ul className="sbar">
                    {subCodes.map((s) => (
                      <li key={s.code}><span className="mono">{s.code}</span><span className="tr"><i style={{ width: `${(100 * s.count) / subMax}%` }} /></span><b>{s.count}</b></li>
                    ))}
                  </ul>
                  <p className="chn">{t.subNote}</p>
                </>
              )}
            </div>
            <div>
              <h3 className="h3">{t.recentTitle}</h3>
              <table className="otbl">
                <thead><tr><th>{t.head.title}</th><th style={{ width: 112 }}>{t.head.posted}</th><th style={{ width: 40 }} aria-hidden="true" /></tr></thead>
                {rows.length > 0 && (
                  <tbody>
                    {rows.map((r) => (
                      <tr key={r.id} className="row" onClick={() => go(rowTarget(r))}>
                        <TitleCell title={r.title} titleKo={r.titleKo} sol={r.solicitationNumber} />
                        <td className="dt">{fmtDate(r.postedDate)}</td>
                        <GoCell target={rowTarget(r)} />
                      </tr>
                    ))}
                  </tbody>
                )}
              </table>
              {rows.length === 0 && (
                <LoadState state={recent.status === 'loading' ? 'loading' : recent.status === 'error' ? 'error' : 'empty'} icon="folder" onRetry={recent.reload} />
              )}
            </div>
          </div>
        </section>
      )}
      <MarketApplyBand title={MARKET_COMMON.band.title} desc={MARKET_COMMON.band.desc} />
    </SubPageFrame>
  )
}
