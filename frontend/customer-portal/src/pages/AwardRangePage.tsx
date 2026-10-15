import { useState } from 'react'
import { Navigate, useLocation, useSearchParams } from 'react-router-dom'
import { Button, CommandItem, Input } from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadState, Rich } from '../components/market/MarketParts'
import { money } from '../components/market/marketUtils'
import { GuestGate, NaicsPick, RecompeteBand, SourceNote, SyncingBox } from '../components/recompete/RecompeteParts'
import { isSynced, useNaicsName } from '../components/recompete/recompeteUtils'
import { federalApi, federalErrorCode } from '../api/federal'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { useAuth } from '../context/AuthContext'
import { AWARD_RANGE_TEXT as T, FEDERAL_COMMON as C, FEDERAL_LINKS as L } from '../data/recompeteContent'
import type { AwardRangeResponse, ContractRow, RangeRow } from '../types/federal'

const PLACE = findMenuItem('awards', 'award-range')

/** 서버가 범위 대신 돌려준 사유. 화면은 사유마다 안내를 바꾼다 */
const HANDLED = ['FEDERAL_NOTICE_NOT_FOUND', 'FEDERAL_NOTICE_WITHOUT_NAICS'] as const
type Handled = (typeof HANDLED)[number]
type RangeResult = { ok: AwardRangeResponse } | { reason: Handled }

/** 금액 축. 근거 금액이 걸친 10의 거듭제곱 구간을 다 보인다 */
function axisOf(rows: RangeRow[]) {
  const values = rows.flatMap((r) => [r.min, r.max]).filter((v): v is number => v != null && v > 0)
  if (values.length === 0) return null
  const lo = Math.floor(Math.log10(Math.min(...values)))
  const hi = Math.max(lo + 1, Math.ceil(Math.log10(Math.max(...values))))
  const at = (v: number) => ((Math.log10(Math.max(v, 10 ** lo)) - lo) / (hi - lo)) * 100
  const ticks = Array.from({ length: hi - lo + 1 }, (_, i) => lo + i)
  return { at, ticks }
}

const UNITS: [number, string][] = [[9, 'B'], [6, 'M'], [3, 'K']]

/** 눈금 글. 10^p 를 $1K, $10M 처럼 */
function tickLabel(p: number): string {
  const unit = UNITS.find(([e]) => p >= e)
  return unit ? `$${10 ** (p - unit[0])}${unit[1]}` : `$${10 ** p}`
}

const hasRange = (r: RangeRow) => r.min != null && r.p25 != null && r.p50 != null && r.p75 != null && r.max != null

/** 0건인 줄은 빼고 범위를 낸 줄을 먼저 보인다. 0건 줄이 첫 줄을 차지해 범위가 없는 화면처럼 보였다 (CR-164) */
const shownRows = (rows: RangeRow[]) =>
  rows.filter((r) => r.count > 0).sort((a, b) => Number(hasRange(b)) - Number(hasRange(a)))

function RangeLine({ r, axis }: { r: RangeRow; axis: ReturnType<typeof axisOf> }) {
  const [name, kinds] = T.group[r.group]
  const ready = hasRange(r) && axis
  return (
    <div className="rc-rr">
      <div className="lb"><b>{name}</b><span>{kinds}</span><em>{T.count(r.count)}</em></div>
      {ready ? (
        <>
          <div className="rc-trk">
            <div className="ax" />
            <div className="wk" style={{ left: `${axis.at(r.min!)}%`, width: `${axis.at(r.max!) - axis.at(r.min!)}%` }} />
            <div className="bx" style={{ left: `${axis.at(r.p25!)}%`, width: `${axis.at(r.p75!) - axis.at(r.p25!)}%` }} />
            <div className="md" style={{ left: `${axis.at(r.p50!)}%` }} />
          </div>
          <div className="rc-tks">
            {axis.ticks.map((p) => <span key={p} style={{ left: `${axis.at(10 ** p)}%` }}>{tickLabel(p)}</span>)}
          </div>
          <dl className="rc-st">
            <div><dt>{T.stats.min}</dt><dd>{money(r.min!)}</dd></div>
            <div><dt>{T.stats.p25}</dt><dd>{money(r.p25!)}</dd></div>
            <div className="p50"><dt>{T.stats.p50}</dt><dd>{money(r.p50!)}</dd></div>
            <div><dt>{T.stats.p75}</dt><dd>{money(r.p75!)}</dd></div>
            <div><dt>{T.stats.max}</dt><dd>{money(r.max!)}</dd></div>
          </dl>
        </>
      ) : (
        <div className="rc-few"><Icon name="alert" /><span><Rich text={T.few(r.count, r.minCount)} /></span></div>
      )}
    </div>
  )
}

const openAward = (row: ContractRow) => window.open(row.usaspendingUrl, '_blank', 'noopener,noreferrer')

function Evidence({ rows }: { rows: ContractRow[] }) {
  const e = T.evidence
  return (
    <section className="cbx pad">
      <h2 className="h2">{e.title}<small>{e.sub(rows.length)}</small></h2>
      <table className="tbl rc-ev">
        <thead>
          <tr>
            <th className="c" style={{ width: 120 }}>{e.head.start}</th>
            <th>{e.head.desc}</th>
            <th style={{ width: 100 }}>{e.head.type}</th>
            <th className="r" style={{ width: 140 }}>{e.head.amount}</th>
            <th className="c" style={{ width: 56 }} aria-hidden="true" />
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id} className="row" onClick={() => openAward(r)}>
              <td className="c mono">{r.startDate ?? '-'}</td>
              <td className="ttl">
                <b>{r.description || r.piid}</b>
                <span><span className="mono">{r.piid}</span>{r.recipientName && <span>{r.recipientName}</span>}</span>
              </td>
              <td>{C.type[r.awardType]}</td>
              <td className="r">{r.amountUsd != null ? <span className="amt">{money(r.amountUsd)}</span> : '-'}</td>
              <td className="c">
                <a className="ext" href={r.usaspendingUrl} target="_blank" rel="noopener noreferrer" onClick={(ev) => ev.stopPropagation()} aria-label={C.awardPage}>
                  <Icon name="ext" />
                </a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="fn">{e.note}</p>
    </section>
  )
}

/** 낙찰가 범위 (CR-142, BID-MKT-030, T3-3 「낙찰가 범위」). 업종별 지난 한국 내 미군 계약 금액 범위와 근거 계약 */
export default function AwardRangePage() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const [params, setParams] = useSearchParams()
  const notice = params.get('notice') || null
  const naics = params.get('naics') || null
  const q = params.get('q') || null

  const [draft, setDraft] = useState<{ notice: string | null; naics: string | null; q: string }>({ notice, naics, q: q ?? '' })
  const draftName = useNaicsName(draft.naics)

  const basis = useMarketLoad(() => (isAuthenticated ? federalApi.naics() : Promise.resolve(null)), String(isAuthenticated))
  const range = useMarketLoad<RangeResult | null>(
    () => (isAuthenticated && (notice || naics)
      ? federalApi.awardRange({ noticeNumber: notice ?? undefined, naics: naics ?? undefined, q: q ?? undefined })
        .then((ok): RangeResult => ({ ok }))
        .catch((e): RangeResult => {
          const code = federalErrorCode(e)
          if (HANDLED.includes(code as Handled)) return { reason: code as Handled }
          throw e
        })
      : Promise.resolve(null)),
    `${isAuthenticated}|${notice}|${naics}|${q}`,
  )

  if (!PLACE) return <Navigate to="/" replace />

  const search = (next = draft) => {
    const p = new URLSearchParams()
    if (next.notice) p.set('notice', next.notice)
    else if (next.naics) p.set('naics', next.naics)
    if (next.q.trim()) p.set('q', next.q.trim())
    setParams(p)
  }

  const head = <PageHead title={T.title} desc={T.desc} />
  const band = (
    <RecompeteBand icon="search" title={T.band.title} desc={T.band.desc} links={[{ label: L.openNotices, to: '/search' }]} />
  )
  const frame = (body: React.ReactNode) => <SubPageFrame group={PLACE.group} item={PLACE.item}>{head}{body}{band}</SubPageFrame>

  if (!isAuthenticated) return frame(<GuestGate from={location.pathname + location.search} />)
  const b = basis.data
  if (!b) {
    return frame(
      <section className="cbx pad">
        <LoadState state={basis.status === 'error' ? 'error' : 'loading'} onRetry={basis.reload} />
      </section>,
    )
  }
  if (!isSynced(b.source)) return frame(<SyncingBox desc={T.syncingDesc} />)

  const result = range.data
  const ok = result && 'ok' in result ? result.ok : null
  const reason = result && 'reason' in result ? result.reason : null
  const excluded = ok ? ok.rows.reduce((s, r) => s + r.excludedCount, 0) : null
  const total = ok ? ok.rows.reduce((s, r) => s + r.count, 0) : 0
  const axis = ok ? axisOf(ok.rows) : null

  const targetLabel = draft.notice
    ? <span>{T.target.notice(draft.notice)}</span>
    : draft.naics ? <><span className="mono">{draft.naics}</span><b>{draftName ?? ''}</b></> : undefined

  const message = (text: string) => <section className="cbx pad" style={{ marginTop: 20 }}><div className="rc-msg">{text}</div></section>

  return frame(
    <>
      <SourceNote source={ok?.source ?? b.source} extra={excluded != null ? T.excluded(excluded) : ''} />
      <section className="cbx pad">
        <form className="rc-rgs" onSubmit={(e) => { e.preventDefault(); search() }}>
          <div>
            <label>{T.target.label}</label>
            <NaicsPick
              large
              value={draft.naics}
              label={targetLabel}
              placeholder={T.target.placeholder}
              onChange={(code) => setDraft({ ...draft, naics: code, notice: null })}
              extra={(text, close) => text.trim() && (
                <CommandItem value="__notice" onSelect={() => { setDraft({ ...draft, notice: text.trim(), naics: null }); close() }}>
                  {T.target.byNotice(text.trim())}
                </CommandItem>
              )}
            />
          </div>
          <div className="gsch">
            <div>
              <label htmlFor="rc-word">{T.word.label}<small>{T.word.optional}</small></label>
              <Input id="rc-word" className="wj-reset in" value={draft.q} onChange={(e) => setDraft({ ...draft, q: e.target.value })} placeholder={T.word.placeholder} />
            </div>
            <Button type="submit" className="wj-reset bigb" disabled={!draft.notice && !draft.naics}>{T.find}</Button>
          </div>
        </form>
      </section>

      {!notice && !naics ? message(T.empty.pick)
        : reason === 'FEDERAL_NOTICE_NOT_FOUND' ? message(T.empty.noNotice)
        : reason === 'FEDERAL_NOTICE_WITHOUT_NAICS' ? message(T.empty.noNaics)
        : !ok ? (
          <section className="cbx pad" style={{ marginTop: 20 }}>
            <LoadState state={range.status === 'error' ? 'error' : 'loading'} onRetry={range.reload} />
          </section>
        ) : (
          <>
            <section className="cbx pad" style={{ marginTop: 20 }}>
              <div className="rc-nh"><span className="mono">{ok.naicsCode}</span><b>{ok.naicsName ?? ''}</b></div>
              {ok.noticeNumber && <p className="rc-ntl">{T.noticeLine(ok.noticeNumber, ok.noticeTitle)}</p>}
              {total === 0 ? <div className="rc-msg">{T.empty.none}</div> : (
                <>
                  <p className="rc-nsub">{T.sub(q, total)}</p>
                  {shownRows(ok.rows).map((r) => <RangeLine key={r.group} r={r} axis={axis} />)}
                </>
              )}
            </section>
            {ok.evidence.length > 0 && <div style={{ marginTop: 20 }}><Evidence rows={ok.evidence} /></div>}
          </>
        )}
    </>,
  )
}
