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
import { BasisNote } from '../market/MarketParts'
import { federalApi } from '../../api/federal'
import { FEDERAL_COMMON as C } from '../../data/recompeteContent'
import type { FederalSource, NaicsOption } from '../../types/federal'
import '../../wjbid-market.css'
import '../../wjbid-recompete.css'

/**
 * 미 연방 계약 자료로 여는 화면 공통 부품 (CR-142, T3-3 「미 연방 계약 자료로 여는 화면 공통」).
 * 속페이지 틀과 메뉴 속 화면 2묶음 부품(자료 기준, 빈 상태, 쪽 넘김) 위에 얹는다.
 */

const EXTRA = {
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
