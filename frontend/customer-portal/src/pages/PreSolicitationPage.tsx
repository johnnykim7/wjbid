import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { Switch } from '@therecommerce-develop/ds-ui'
import { Button } from '../components/ui/button'
import { Icon } from '../components/layout/Icon'
import { academyHref, findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BasisNote, DeadlineCell, GoCell, GoLegend, LoadState, MarketApplyBand, MarketPager, TitleCell, TypeTag } from '../components/market/MarketParts'
import { fmtDate, rowTarget, useGo } from '../components/market/marketUtils'
import { marketApi } from '../api/market'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { MARKET_COMMON, PRE_TEXT } from '../data/marketContent'
import type { TypeCount } from '../types/market'

const PLACE = findMenuItem('bids', 'pre-solicitation')
const PAGE_SIZE = 20
/** SAM.gov 공고 유형 값. 탭 순서와 자료 기준 줄에 쓴다 */
const PRESOL = 'Presolicitation'
const SOURCES = 'Sources Sought'

const countOf = (counts: TypeCount[] | undefined, type: string) => counts?.find((c) => c.type === type)?.count ?? 0

/** 사전 공고 (CR-135, BID-MKT-001, T3-3 「사전 공고」). 사전 공고와 사전 수요 조사, 같은 공고번호로 본 공고가 나왔는지 */
export default function PreSolicitationPage() {
  const [params, setParams] = useSearchParams()
  const type = params.get('type') ?? ''
  const openOnly = params.get('open') === '1'
  const page = Number(params.get('page')) || 0
  const go = useGo()

  const basis = useMarketLoad(() => marketApi.collected({ group: 'PRE', size: 1 }), 'basis')
  const list = useMarketLoad(
    () => marketApi.collected({ group: 'PRE', type: type || undefined, openOnly, page, size: PAGE_SIZE }),
    `${type}|${openOnly}|${page}`,
  )

  if (!PLACE) return <Navigate to="/" replace />
  const t = PRE_TEXT

  const update = (next: { type?: string; open?: boolean; page?: number }) => {
    const p = new URLSearchParams()
    const nt = next.type ?? type
    const no = next.open ?? openOnly
    if (nt) p.set('type', nt)
    if (no) p.set('open', '1')
    if (next.page) p.set('page', String(next.page))
    setParams(p)
  }

  const b = basis.data
  const counts = list.data?.typeCounts
  const tabs = [
    { key: '', label: t.all, count: (counts ?? []).reduce((n, c) => n + c.count, 0) },
    { key: PRESOL, label: t.presol, count: countOf(counts, PRESOL) },
    { key: SOURCES, label: t.sources, count: countOf(counts, SOURCES) },
  ]
  const rows = list.data?.content ?? []

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        {b && b.dataFrom && (
          <BasisNote text={t.basis(fmtDate(b.dataFrom), fmtDate(b.dataTo), countOf(b.typeCounts, PRESOL), countOf(b.typeCounts, SOURCES))} />
        )}
        <div className="tbar">
          <div className="chips">
            {tabs.map((tab) => (
              <Button key={tab.key} className={`wj-reset${type === tab.key ? ' on' : ''}`} onClick={() => update({ type: tab.key, page: 0 })}>
                {tab.label}<em>{tab.count}</em>
              </Button>
            ))}
          </div>
          <label className="tgl">
            <Switch checked={openOnly} onCheckedChange={(v) => update({ open: v, page: 0 })} />
            {t.openOnly}
          </label>
        </div>
        <table className="otbl">
          <thead>
            <tr>
              <th style={{ width: 132 }}>{t.head.type}</th>
              <th>{t.head.title}</th>
              <th style={{ width: 118 }}>{t.head.posted}</th>
              <th style={{ width: 118 }}>{t.head.deadline}</th>
              <th style={{ width: 44 }} aria-hidden="true" />
            </tr>
          </thead>
          {rows.length > 0 && (
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="row" onClick={() => go(rowTarget(r))}>
                  <td><TypeTag label={r.typeKo} /></td>
                  <TitleCell title={r.title} titleKo={r.titleKo} sol={r.solicitationNumber}>
                    {r.hasMainNotice && r.solicitationNumber && (
                      <Link className="tag2 b" to={`/bids/amendments?sol=${encodeURIComponent(r.solicitationNumber)}`} onClick={(e) => e.stopPropagation()}>
                        {t.mainNotice}
                      </Link>
                    )}
                  </TitleCell>
                  <td className="dt">{fmtDate(r.postedDate)}</td>
                  <DeadlineCell deadline={r.responseDeadline} />
                  <GoCell target={rowTarget(r)} />
                </tr>
              ))}
            </tbody>
          )}
        </table>
        {rows.length === 0 && (
          <LoadState state={list.status === 'loading' ? 'loading' : list.status === 'error' ? 'error' : 'empty'} icon="bell" onRetry={list.reload} />
        )}
        {rows.length > 0 && (
          <>
            <GoLegend />
            <MarketPager page={page} totalPages={list.data?.totalPages ?? 1} onChange={(p) => update({ page: p })} />
          </>
        )}
      </section>
      <section className="cbx pad">
        <h2 className="h2">{t.howTitle}</h2>
        <div className="wh">
          {t.how.map((h) => (
            <div key={h.title}>
              <Icon name={h.icon} />
              <b>{h.title}</b>
              <p>{h.desc}</p>
            </div>
          ))}
        </div>
        <Link className="lnk" to={academyHref('materials')}>{t.howLink}<Icon name="cr" /></Link>
      </section>
      <MarketApplyBand title={MARKET_COMMON.band.title} desc={MARKET_COMMON.band.desc} />
    </SubPageFrame>
  )
}
