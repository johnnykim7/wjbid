import { useMemo } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BasisNote, DeadlineCell, GoCell, GoLegend, LoadState, MarketApplyBand, TitleCell, TypeTag } from '../components/market/MarketParts'
import { fmtDate, rowTarget, useGo } from '../components/market/marketUtils'
import { KoreaMap } from '../components/basemap/KoreaMap'
import { awardeeBaseApi } from '../api/awardeeBase'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { BASEMAP_TEXT as T } from '../data/awardeeBaseContent'
import type { BaseStat } from '../types/awardeeBase'
import '../wjbid-basemap.css'

const PLACE = findMenuItem('analysis', 'base-map')
/** 고른 기지 아래에 보이는 최근 공고 수 */
const RECENT_SIZE = 10
const OPEN_PATH = '/search'

const labelOf = (b: BaseStat) => T.dotLabel(b.nameKo, b.opportunityCount)
const titleOf = (b: BaseStat) => T.dotTitle(b.nameKo, b.opportunityCount)
const hasPosition = (b: BaseStat) => b.latitude != null && b.longitude != null

function Num({ value }: { value: number }) {
  return <span className={`num${value === 0 ? ' z' : ''}`}>{value}</span>
}

/** 기지별 지도 (CR-136, BID-MKT-012, T3-3 「기지별 지도」). 공고 제목에 기지 이름이 있는 공고만 센다(BIZ-031) */
export default function BaseMapPage() {
  const [params, setParams] = useSearchParams()
  const go = useGo()
  const stats = useMarketLoad(() => awardeeBaseApi.baseStats(), 'stats')

  // 공고가 많은 기지부터. 같으면 관리자가 정한 순서 그대로
  const bases = useMemo(
    () => [...(stats.data?.bases ?? [])].sort((a, b) => b.opportunityCount - a.opportunityCount),
    [stats.data],
  )
  const asked = params.get('base')
  const selected = bases.find((b) => b.id === asked) ?? bases[0] ?? null
  const selectedId = selected?.id ?? null

  const recent = useMarketLoad(
    () => (selectedId ? awardeeBaseApi.baseOpportunities(selectedId, RECENT_SIZE) : Promise.resolve([])),
    selectedId ?? '',
  )

  if (!PLACE) return <Navigate to="/" replace />
  const s = stats.data
  const select = (id: string) => setParams({ base: id }, { replace: true })
  const rows = recent.data ?? []

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={T.title} desc={T.desc} />

      {!s && <section className="cbx pad"><LoadState state={stats.status === 'error' ? 'error' : 'loading'} onRetry={stats.reload} /></section>}

      {s && bases.length === 0 && (
        <section className="cbx pad">
          <div className="empty2 bmp-empty">
            <div className="icn"><Icon name="pin" /></div>
            <b>{T.emptyTitle}</b>
            <span>{T.emptyDesc}</span>
            <Link className="bigb o bmp-go" to={OPEN_PATH}>{T.emptyAction}</Link>
          </div>
        </section>
      )}

      {s && bases.length > 0 && (
        <div className="bmp">
          {s.dataFrom && s.dataTo && (
            <BasisNote text={T.basis(fmtDate(s.dataFrom), fmtDate(s.dataTo), s.totalNumbers, s.unmatchedNumbers)} />
          )}
          <div className="mapw">
            <section className="cbx bmap">
              <h3>{T.mapTitle}</h3>
              <div className="lg">
                <span><i />{T.legend.few}</span>
                <span><i className="b" />{T.legend.many}</span>
                <span>{T.legend.dmz}</span>
              </div>
              <KoreaMap bases={bases} selectedId={selectedId} onSelect={select} ariaLabel={T.mapAria} labelOf={labelOf} titleOf={titleOf} />
            </section>
            <section className="cbx btbl">
              <h3>{T.tableTitle(bases.length)}</h3>
              <table className="otbl">
                <thead>
                  <tr>
                    <th>{T.head.name}</th>
                    <th className="w-reg">{T.head.region}</th>
                    <th className="r w-n">{T.head.opp}</th>
                    <th className="r w-n">{T.head.award}</th>
                    <th className="r w-open">{T.head.open}</th>
                  </tr>
                </thead>
                <tbody>
                  {bases.map((b) => (
                    <tr key={b.id} className={`row${b.id === selectedId ? ' on' : ''}`} onClick={() => select(b.id)}>
                      <td>
                        <Button
                          className="wj-reset pick"
                          aria-pressed={b.id === selectedId}
                          onClick={(e) => {
                            e.stopPropagation()
                            select(b.id)
                          }}
                        >
                          {b.nameKo}
                        </Button>
                        {!hasPosition(b) && <span className="nopos">{T.noPosition}</span>}
                        <span className="eng">{b.nameEn}</span>
                      </td>
                      <td>{b.region ?? '-'}</td>
                      <td className="r"><Num value={b.opportunityCount} /></td>
                      <td className="r"><Num value={b.awardCount} /></td>
                      <td className="r"><Num value={b.openCount} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>

          {selected && (
            <section className="cbx pad sech">
              <h2 className="h2">
                {T.recentTitle(selected.nameKo)}
                {recent.status === 'done' && <small>{T.recentSub(rows.length)}</small>}
              </h2>
              {recent.status !== 'done' ? (
                <LoadState state={recent.status === 'error' ? 'error' : 'loading'} onRetry={recent.reload} />
              ) : rows.length === 0 ? (
                <p className="none">{T.recentEmpty}</p>
              ) : (
                <>
                  <table className="otbl">
                    <thead>
                      <tr>
                        <th className="w-type">{T.recentHead.type}</th>
                        <th>{T.recentHead.title}</th>
                        <th className="w-posted">{T.recentHead.posted}</th>
                        <th className="w-dl">{T.recentHead.deadline}</th>
                        <th className="w-go" aria-hidden="true" />
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((r) => (
                        <tr key={r.id} className="row" onClick={() => go(rowTarget(r))}>
                          <td><TypeTag label={r.typeKo} /></td>
                          <TitleCell title={r.title} titleKo={r.titleKo} sol={r.solicitationNumber} />
                          <td className="dt">{fmtDate(r.postedDate)}</td>
                          <DeadlineCell deadline={r.responseDeadline} />
                          <GoCell target={rowTarget(r)} />
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <GoLegend />
                </>
              )}
            </section>
          )}
        </div>
      )}

      <MarketApplyBand title={T.band.title} desc={T.band.desc} />
    </SubPageFrame>
  )
}
