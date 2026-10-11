import { Fragment, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../ui/button'
import { Icon, type IconName } from '../layout/Icon'
import { useAuth } from '../../context/AuthContext'
import { MARKET_COMMON } from '../../data/marketContent'
import { daysBetween, fmtDate, useNow, type GoTarget } from './marketUtils'
import '../../wjbid-market.css'

/**
 * 메뉴 속 화면 2묶음 공통 부품 (CR-135, T3-3 「메뉴 속 화면 2묶음」). 속페이지 틀 안에서 쓴다.
 * 낙찰 업체, 기지별 지도 화면(CR-136)도 같은 부품을 쓴다.
 */

/** 남은 날이 이 안이면 마감 임박(빨강) */
const URGENT_DAYS = 7

const EXTRA = {
  info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5.5M12 7.5v.01" /></>,
  badge: <><path d="M12 2.5 14.4 4l2.8-.2.9 2.6 2.3 1.6-.9 2.6.9 2.6-2.3 1.6-.9 2.6-2.8-.2L12 19.5 9.6 18l-2.8.2-.9-2.6-2.3-1.6.9-2.6-.9-2.6 2.3-1.6.9-2.6 2.8.2Z" /><path d="m9 11 2 2 4-4" /></>,
  cl: <path d="m15 18-6-6 6-6" />,
}

export type MarketIconName = IconName | keyof typeof EXTRA

export function MarketIcon({ name }: { name: MarketIconName }) {
  if (!(name in EXTRA)) return <Icon name={name as IconName} />
  return (
    <svg className="i" viewBox="0 0 24 24" aria-hidden="true">
      {EXTRA[name as keyof typeof EXTRA]}
    </svg>
  )
}

/** "**굵게**" 표시가 있는 글을 그린다 */
export function Rich({ text }: { text: string }) {
  return <>{text.split('**').map((part, i) => (i % 2 === 1 ? <b key={i}>{part}</b> : <Fragment key={i}>{part}</Fragment>))}</>
}

/** 자료 기준 줄. 기간과 건수, 빈 칸이 무엇인지 밝힌다 */
export function BasisNote({ text }: { text: string }) {
  return (
    <div className="basis">
      <MarketIcon name="info" />
      <p><Rich text={text} /></p>
    </div>
  )
}

export function TypeTag({ label }: { label: string | null }) {
  return <span className="tag2">{label ?? '-'}</span>
}

/** 제목 칸. 한글 제목이 없으면 원문 제목만 보인다 */
export function TitleCell({ title, titleKo, sol, children }: { title: string | null; titleKo: string | null; sol: string | null; children?: ReactNode }) {
  return (
    <td className="xtt">
      {titleKo ? (<><b>{titleKo}</b><span className="en">{title}</span></>) : <b>{title}</b>}
      <div className="meta"><span className="no">{sol || '-'}</span>{children}</div>
    </td>
  )
}

/** 마감일 칸. 남은 날은 KST 날짜끼리 센다 */
export function DeadlineCell({ deadline }: { deadline: string | null }) {
  const now = useNow()
  if (!deadline) return <td className="dl over">{MARKET_COMMON.noDeadline}</td>
  if (new Date(deadline).getTime() <= now.ms) {
    return <td className="dl over">{MARKET_COMMON.closed}<span className="d">{fmtDate(deadline)}</span></td>
  }
  const left = daysBetween(now.today, deadline)
  return <td className={`dl${left <= URGENT_DAYS ? ' urg' : ''}`}>D-{left}<span className="d">{fmtDate(deadline)}</span></td>
}

/** 이동 고리. 줄 누름과 같은 곳으로 가며, 글쇠로 고를 수 있게 한다 */
export function GoLink({ target, children, className }: { target: GoTarget | null; children: ReactNode; className?: string }) {
  if (!target) return null
  const stop = (e: React.MouseEvent) => e.stopPropagation()
  return <Link className={className} to={target} onClick={stop} aria-label={MARKET_COMMON.goNotice}>{children}</Link>
}

export function GoCell({ target }: { target: GoTarget | null }) {
  return (
    <td className="go" title={MARKET_COMMON.goNotice}>
      <GoLink target={target}><Icon name="cr" /></GoLink>
    </td>
  )
}

export function GoLegend() {
  return (
    <p className="golg">
      <span><Icon name="cr" />{MARKET_COMMON.legend}</span>
    </p>
  )
}

const PAGE_WINDOW = 10

/** 쪽 넘김. 쪽은 0 부터 센다 */
export function MarketPager({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) {
    return <div className="pgn"><Button className="wj-reset on" disabled>1</Button></div>
  }
  const start = Math.floor(page / PAGE_WINDOW) * PAGE_WINDOW
  const pages = Array.from({ length: Math.min(PAGE_WINDOW, totalPages - start) }, (_, i) => start + i)
  return (
    <div className="pgn">
      <Button className="wj-reset ar" disabled={page === 0} onClick={() => onChange(page - 1)} aria-label={MARKET_COMMON.pagePrev}><MarketIcon name="cl" /></Button>
      {pages.map((p) => (
        <Button key={p} className={`wj-reset${p === page ? ' on' : ''}`} onClick={() => onChange(p)} aria-current={p === page ? 'page' : undefined}>{p + 1}</Button>
      ))}
      <Button className="wj-reset ar" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} aria-label={MARKET_COMMON.pageNext}><Icon name="cr" /></Button>
    </div>
  )
}

/** 아래 신청 띠. 로그인했으면 마감 전 공고로, 아니면 가입으로 */
export function MarketApplyBand({ title, desc }: { title: string; desc: string }) {
  const { isAuthenticated } = useAuth()
  const b = MARKET_COMMON.band
  return (
    <div className="apply">
      <div className="icn"><MarketIcon name="badge" /></div>
      <div className="xtx">
        <b>{title}</b>
        <span>{desc}</span>
      </div>
      <Link className="bigb" to={isAuthenticated ? '/search' : '/register'}>{isAuthenticated ? b.open : b.join}</Link>
    </div>
  )
}

/** 불러오는 중, 못 불러옴(다시 시도), 비었음 */
export function LoadState({ state, icon = 'search', onRetry }: { state: 'loading' | 'error' | 'empty'; icon?: IconName; onRetry?: () => void }) {
  const c = MARKET_COMMON
  if (state === 'loading') return <div className="mload" role="status">{c.loading}</div>
  return (
    <div className="empty2">
      <div className="icn"><Icon name={state === 'error' ? 'alert' : icon} /></div>
      <b>{state === 'error' ? c.loadFailTitle : c.emptyTitle}</b>
      {state === 'error' && onRetry && <Button className="wj-reset bigb o retry" onClick={onRetry}>{c.retry}</Button>}
    </div>
  )
}
