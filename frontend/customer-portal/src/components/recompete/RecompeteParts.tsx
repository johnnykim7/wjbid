import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  Button,
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@therecommerce-develop/ds-ui'
import { Icon, type IconName } from '../layout/Icon'
import { BasisNote, Rich } from '../market/MarketParts'
import { money } from '../market/marketUtils'
import { federalApi } from '../../api/federal'
import { FEDERAL_COMMON as C, RULE_TEXT } from '../../data/recompeteContent'
import type { ContractRow, FederalSource, NaicsOption, RecompeteCategory, RecompeteSort } from '../../types/federal'
import '../../wjbid-market.css'
import '../../wjbid-recompete.css'

/**
 * 미 연방 계약 자료로 여는 화면 공통 부품 (CR-142, T3-3 「미 연방 계약 자료로 여는 화면 공통」).
 * 속페이지 틀과 메뉴 속 화면 2묶음 부품(자료 기준, 빈 상태, 쪽 넘김) 위에 얹는다.
 */

const EXTRA = {
  flag: <><path d="M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 8 2a6 6 0 0 0 3.6-1.2.5.5 0 0 1 .4.4V14a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.6" /></>,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
  hour: <path d="M6 2h12M6 22h12M7 2v4l5 6-5 6v4M17 2v4l-5 6 5 6v4" />,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5" /></>,
}

export type RcIconName = IconName | keyof typeof EXTRA

export function RcIcon({ name }: { name: RcIconName }) {
  if (!(name in EXTRA)) return <Icon name={name as IconName} />
  return (
    <svg className="i" viewBox="0 0 24 24" aria-hidden="true">
      {EXTRA[name as keyof typeof EXTRA]}
    </svg>
  )
}

/** 마지막으로 받은 때. 응답은 +09:00 이 붙은 시각이라 앞 열여섯 자리를 그대로 쓴다 */
const fmtSynced = (iso: string) => iso.slice(0, 16).replace('T', ' ')

/** 자료 기준 칸. 네 화면 같고, 낙찰가 범위는 뒤에 금액 안내를 붙인다 */
export function SourceNote({ source, extra = '' }: { source: FederalSource; extra?: string }) {
  if (!source.lastSyncedAt) return null
  const text = C.source(source.totalCount.toLocaleString('en-US'), source.dataFrom.slice(0, 7), fmtSynced(source.lastSyncedAt), source.lagDays) + extra
  return <BasisNote text={text} />
}

/** 한 번도 받지 않았을 때 본문 대신 */
export function SyncingBox({ desc }: { desc: string }) {
  return (
    <section className="cbx pad">
      <div className="empty2" style={{ borderBottom: 0 }}>
        <div className="icn"><RcIcon name="hour" /></div>
        <b>{C.syncing}</b>
        <span>{desc}</span>
      </div>
    </section>
  )
}

const PREDICTED: RecompeteCategory[] = ['RECOMPETE', 'EXTENDABLE']

export function CategoryTag({ category }: { category: RecompeteCategory }) {
  return (
    <span className={`rc-tag ${category}`}>
      {C.category[category]}
      {PREDICTED.includes(category) && <i>{C.predict}</i>}
    </span>
  )
}

/** 근거 한 줄. 규칙에 쓴 날짜를 그대로 보인다 */
function whyText(r: ContractRow): string {
  const type = C.type[r.awardType]
  switch (r.category) {
    case 'RECOMPETE': return C.why.RECOMPETE(r.potentialEndDate ?? '', type)
    case 'EXTENDABLE': return C.why.EXTENDABLE(r.endDate ?? '', r.potentialEndDate ?? '')
    case 'IN_PARENT': return C.why.IN_PARENT(r.parentPiid, type)
    default: return C.why.UNCONFIRMED
  }
}

function DateLine({ label, date, basis }: { label: string; date: string | null; basis: boolean }) {
  return (
    <div className={`${basis ? 'k' : ''}${date ? '' : ' n'}`}>
      <span>{label}</span>
      <b>{date ?? C.date.unknown}</b>
    </div>
  )
}

const openAward = (url: string) => window.open(url, '_blank', 'noopener,noreferrer')

/** USAspending 계약 쪽 고리. 줄 누름과 같은 곳으로 간다 */
export function AwardLink({ row, children }: { row: ContractRow; children?: ReactNode }) {
  return (
    <a href={row.usaspendingUrl} target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()}>
      {children ?? C.awardPage}<Icon name="ext" />
    </a>
  )
}

/** 계약 목록 한 줄. 누르면 USAspending 계약 쪽이 새 창으로 열린다 */
function ContractLine({ r }: { r: ContractRow }) {
  return (
    <tr className="row" onClick={() => openAward(r.usaspendingUrl)}>
      <td className="ct">
        {r.category && (
          <div className="rc-tc">
            <CategoryTag category={r.category} />
            {r.oneTimeConstruction && <span className="rc-con">{C.oneTime}</span>}
          </div>
        )}
        <b>{r.description || r.piid}</b>
        <div className="rc-meta">
          <span className="mono">{r.piid}</span>
          <span>{C.type[r.awardType]}</span>
          {r.recipientName && <span>{r.recipientName}</span>}
          {r.naicsCode && <span>{C.naicsLabel} <span className="mono">{r.naicsCode}</span> {r.naicsName}</span>}
        </div>
        {r.category && (
          <div className={`rc-why ${r.category}`}>
            <RcIcon name="flag" />
            <span>{whyText(r)}</span>
          </div>
        )}
        <div className="rc-lk">
          <AwardLink row={r} />
          {r.solicitationNumber && <span>{C.solicitation} <span className="mono">{r.solicitationNumber}</span></span>}
          {r.publicNoticeId && (
            <Link to={`/search/${r.publicNoticeId}`} onClick={(e) => e.stopPropagation()}>{C.original}<Icon name="cr" /></Link>
          )}
        </div>
      </td>
      <td className="r">{r.amountUsd != null ? <span className="amt">{money(r.amountUsd)}</span> : '-'}</td>
      <td>
        <div className="rc-dt">
          <DateLine label={C.date.start} date={r.startDate} basis={false} />
          <DateLine label={C.kind.CURRENT_END} date={r.endDate} basis={r.basisDateKind === 'CURRENT_END'} />
          <DateLine label={C.kind.POTENTIAL_END} date={r.potentialEndDate} basis={r.basisDateKind === 'POTENTIAL_END'} />
        </div>
      </td>
    </tr>
  )
}

/** 계약 목록 표. 비었으면 표 안에 한 줄로 알린다 */
export function ContractTable({ rows, empty }: { rows: ContractRow[]; empty: ReactNode }) {
  return (
    <table className="tbl rc-list">
      <thead>
        <tr>
          <th>{C.head.contract}</th>
          <th style={{ width: 130, textAlign: 'right' }}>{C.head.amount}</th>
          <th style={{ width: 200 }}>{C.head.dates}</th>
        </tr>
      </thead>
      <tbody>
        {rows.length > 0 ? rows.map((r) => <ContractLine key={r.id} r={r} />) : (
          <tr><td colSpan={3}><div className="empty2"><b>{empty}</b></div></td></tr>
        )}
      </tbody>
    </table>
  )
}

/** 목록 위 건수. "재발주 후보 4건" 이나 "64건 중 1-20" */
export function ListCount({ label, total, page, size }: { label: string | null; total: number; page: number; size: number }) {
  const from = page * size + 1
  const to = Math.min(total, (page + 1) * size)
  return <span className="cnt"><Rich text={C.count(label, total, from, to)} /></span>
}

export function SortSeg({ value, onChange }: { value: RecompeteSort; onChange: (s: RecompeteSort) => void }) {
  return (
    <div className="seg">
      {(['END', 'AMOUNT'] as RecompeteSort[]).map((s) => (
        <Button key={s} className={`wj-reset${value === s ? ' on' : ''}`} aria-pressed={value === s} onClick={() => onChange(s)}>{C.sort[s]}</Button>
      ))}
    </div>
  )
}

/** 구분 규칙 칸. 처음엔 접혀 있다 */
export function RuleBox({ defaultOpen = false }: { defaultOpen?: boolean }) {
  const [open, setOpen] = useState(defaultOpen)
  const t = RULE_TEXT
  return (
    <section className="cbx rc-rule" id="rule">
      <Button className="wj-reset hd2" aria-expanded={open} onClick={() => setOpen(!open)}>
        <RcIcon name="flag" />{t.title}<small>{t.sub}</small>
        <span className="tg">{open ? t.close : t.open}<Icon name={open ? 'cu' : 'cd'} /></span>
      </Button>
      {open && (
        <>
          <ol>
            {t.rules.map((r, i) => (
              <li key={r.category}>
                <em>{i + 1}</em>
                <span><CategoryTag category={r.category} /></span>
                <span>{r.text}</span>
              </li>
            ))}
          </ol>
          <div className="ft2">{t.foot}</div>
        </>
      )}
    </section>
  )
}

const NAICS_DELAY_MS = 250

/**
 * 업종 고르기. 코드나 이름을 넣으면 서버에서 찾는다. value 가 비면 업종 전체.
 * extra 는 맨 위에 덧붙이는 고르기 줄(낙찰가 범위의 공고번호 찾기)이다.
 */
export function NaicsPick({ value, label, onChange, large, placeholder, hint, extra }: {
  value: string | null
  label?: ReactNode
  onChange: (code: string | null) => void
  large?: boolean
  placeholder?: string
  hint?: string
  extra?: (text: string, close: () => void) => ReactNode
}) {
  const [open, setOpen] = useState(false)
  const [text, setText] = useState('')
  const [items, setItems] = useState<NaicsOption[] | null>(null)

  useEffect(() => {
    if (!open) return
    let alive = true
    const timer = window.setTimeout(() => {
      federalApi.naics(text.trim() || undefined)
        .then((r) => { if (alive) setItems(r.items) })
        .catch(() => { if (alive) setItems([]) })
    }, NAICS_DELAY_MS)
    return () => {
      alive = false
      window.clearTimeout(timer)
    }
  }, [open, text])

  const close = () => setOpen(false)
  const pick = (code: string | null) => {
    onChange(code)
    close()
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button className={`wj-reset rc-sel${large ? ' lg' : ''}`} aria-label={C.naics.hint}>
          <span>{label ?? (placeholder ? <small>{placeholder}</small> : <>{C.naics.all}<small>{hint ?? C.naics.hint}</small></>)}</span>
          <Icon name="cd" />
        </Button>
      </PopoverTrigger>
      {/* 떠 있는 창은 공통 틀 밖에 그려져 .wj 규칙이 닿지 않는다. 그래서 ds-ui 모양 그대로 쓴다 */}
      <PopoverContent className="wj-npop" align="start">
        <Command shouldFilter={false}>
          <CommandInput value={text} onValueChange={setText} placeholder={C.naics.search} />
          <CommandList>
            {extra?.(text, close)}
            {value && !large && <CommandItem value="__all" onSelect={() => pick(null)}>{C.naics.clear}</CommandItem>}
            {items !== null && items.length === 0 && <CommandEmpty>{C.naics.none}</CommandEmpty>}
            {(items ?? []).map((o) => (
              <CommandItem key={o.code} value={o.code} onSelect={() => pick(o.code)}>
                <span className="nm">
                  <span className="cd">{o.code}</span>
                  <span className="tx">{o.name ?? ''}</span>
                  <span className="ct">{C.naics.count(o.count)}</span>
                </span>
              </CommandItem>
            ))}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  )
}

/** 아래 띠. 단추는 하나나 둘이고, 마지막 단추만 채운다 */
export function RecompeteBand({ icon, title, desc, links }: {
  icon: RcIconName
  title: string
  desc: string
  links: { label: string; to: string }[]
}) {
  return (
    <div className="apply">
      <div className="icn"><RcIcon name={icon} /></div>
      <div className="xtx">
        <b>{title}</b>
        <span>{desc}</span>
      </div>
      <div className="bbs">
        {links.map((l, i) => (
          <Link key={l.to} className={`bigb${i < links.length - 1 ? ' o' : ''}`} to={l.to}>{l.label}</Link>
        ))}
      </div>
    </div>
  )
}

/** 비로그인 안내. 로그인을 마치면 이 화면으로 돌아온다 */
export function GuestGate({ from }: { from: string }) {
  const g = C.guest
  return (
    <section className="cbx rc-guest">
      <div className="empty2"><div className="icn"><RcIcon name="user" /></div></div>
      <b>{g.title}</b>
      <span>{g.desc}</span>
      <div className="bb">
        <Link className="bigb" to="/login" state={{ from }}>{g.login}</Link>
        <Link className="bigb o" to="/register">{g.register}</Link>
      </div>
    </section>
  )
}
