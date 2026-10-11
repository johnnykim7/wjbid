import { Fragment, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BasisNote, DeadlineCell, GoLegend, GoLink, LoadState, MarketApplyBand, MarketPager, TitleCell } from '../components/market/MarketParts'
import { fmtDate, rowTarget } from '../components/market/marketUtils'
import { marketApi } from '../api/market'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { AMEND_TEXT, MARKET_COMMON } from '../data/marketContent'
import type { AmendmentGroup } from '../types/market'

const PLACE = findMenuItem('bids', 'amendment')
const PAGE_SIZE = 20

/** 변경 공고 (CR-135, BID-MKT-002, T3-3 「변경 공고」). 같은 공고번호로 다시 올라온 공고와 판마다 바뀐 마감일 */
export default function AmendmentsPage() {
  const [params, setParams] = useSearchParams()
  const sol = params.get('sol') ?? ''
  const all = params.get('view') === 'all'
  const page = Number(params.get('page')) || 0
  const [toggled, setToggled] = useState<{ key: string; opened: Set<string> } | null>(null)

  const list = useMarketLoad(
    () => marketApi.amendments({ solicitationNumber: sol || undefined, openOnly: !sol && !all, page, size: PAGE_SIZE }),
    `${sol}|${all}|${page}`,
  )
  // 목록이 바뀌면 첫 묶음만 펼친 채로 시작한다(화면 정본). 펼침을 바꾼 목록에서만 바꾼 값을 쓴다
  const firstSol = list.data?.content[0]?.solicitationNumber
  const openKey = `${sol}|${all}|${page}|${firstSol ?? ''}`
  const opened = toggled?.key === openKey ? toggled.opened : new Set(firstSol ? [firstSol] : [])

  if (!PLACE) return <Navigate to="/" replace />
  const t = AMEND_TEXT
  const d = list.data

  const update = (next: { all?: boolean; page?: number }) => {
    const p = new URLSearchParams()
    if (next.all ?? all) p.set('view', 'all')
    if (next.page) p.set('page', String(next.page))
    setParams(p)
  }
  const toggle = (key: string) => {
    const next = new Set(opened)
    if (next.has(key)) next.delete(key)
    else next.add(key)
    setToggled({ key: openKey, opened: next })
  }
  const rows = d?.content ?? []

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        {d && d.dataFrom && <BasisNote text={t.basis(fmtDate(d.dataFrom), fmtDate(d.dataTo), d.total, d.versionCount)} />}
        <div className="tbar">
          {sol ? (
            <span className="tnote">{t.solOnly(sol)} <Link className="tag2 b" to="/bids/amendments">{t.solClear}</Link></span>
          ) : (
            <>
              <div className="chips">
                <Button className={`wj-reset${all ? '' : ' on'}`} onClick={() => update({ all: false, page: 0 })}>{t.open}<em>{d?.openCount ?? 0}</em></Button>
                <Button className={`wj-reset${all ? ' on' : ''}`} onClick={() => update({ all: true, page: 0 })}>{t.all}<em>{d?.total ?? 0}</em></Button>
              </div>
              <span className="tnote">{t.note}</span>
            </>
          )}
        </div>
        <table className="otbl">
          <thead>
            <tr>
              <th>{t.head.title}</th>
              <th style={{ width: 92 }}>{t.head.count}</th>
              <th style={{ width: 112 }}>{t.head.first}</th>
              <th style={{ width: 112 }}>{t.head.last}</th>
              <th style={{ width: 118 }}>{t.head.deadline}</th>
              <th style={{ width: 44 }} aria-hidden="true" />
            </tr>
          </thead>
          {rows.length > 0 && (
            <tbody>
              {rows.map((g) => (
                <GroupRows key={g.solicitationNumber} group={g} open={opened.has(g.solicitationNumber)} onToggle={() => toggle(g.solicitationNumber)} />
              ))}
            </tbody>
          )}
        </table>
        {rows.length === 0 && (
          <LoadState state={list.status === 'loading' ? 'loading' : list.status === 'error' ? 'error' : 'empty'} icon="file" onRetry={list.reload} />
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

function GroupRows({ group: g, open, onToggle }: { group: AmendmentGroup; open: boolean; onToggle: () => void }) {
  const t = AMEND_TEXT
  return (
    <Fragment>
      <tr className={`row grp${open ? ' open' : ''}`} onClick={onToggle}>
        <TitleCell title={g.title} titleKo={g.titleKo} sol={g.solicitationNumber}>
          {g.deadlineChanged && <span className="tag2">{t.deadlineChanged}</span>}
          {g.hasAward && <span className="tag2 k">{t.awarded}</span>}
        </TitleCell>
        <td className="cnt"><b>{t.count(g.count)}</b></td>
        <td className="dt">{fmtDate(g.firstPostedDate)}</td>
        <td className="dt">{fmtDate(g.lastPostedDate)}</td>
        <DeadlineCell deadline={g.lastResponseDeadline} />
        <td className="go">
          <Button className="wj-reset" aria-expanded={open} aria-label={open ? t.collapse : t.expand} onClick={(e) => { e.stopPropagation(); onToggle() }}>
            <Icon name={open ? 'cu' : 'cd'} />
          </Button>
        </td>
      </tr>
      {open && (
        <tr className="vers">
          <td colSpan={6}>
            <table className="vtbl">
              <thead>
                <tr>
                  <th style={{ width: 70 }}>{t.vhead.no}</th>
                  <th style={{ width: 130 }}>{t.vhead.posted}</th>
                  <th>{t.vhead.type}</th>
                  <th style={{ width: 220 }}>{t.vhead.deadline}</th>
                  <th style={{ width: 110 }} aria-hidden="true" />
                </tr>
              </thead>
              <tbody>
                {g.versions.map((v, i) => {
                  const prev = i > 0 ? g.versions[i - 1].responseDeadline : undefined
                  const changed = i > 0 && v.responseDeadline != null && v.responseDeadline !== prev
                  const deadline = v.responseDeadline ? fmtDate(v.responseDeadline) : MARKET_COMMON.noDeadline
                  const target = rowTarget(v)
                  return (
                    <tr key={v.id}>
                      <td className="vn">{t.version(i + 1)}</td>
                      <td>{fmtDate(v.postedDate)}</td>
                      <td>{v.typeKo ?? '-'}</td>
                      <td>{changed ? <><b>{deadline}</b><span className="chg">{t.changed}</span></> : deadline}</td>
                      <td className="vgo">
                        <GoLink target={target}>
                          {t.goNotice}<Icon name="cr" />
                        </GoLink>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </td>
        </tr>
      )}
    </Fragment>
  )
}
