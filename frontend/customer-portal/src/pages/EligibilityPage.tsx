import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useNavigationType } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { getEligibilityNotices } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { SubIcon } from '../components/subpage/SubIcon'
import { ApplyBand, PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { useAuth } from '../context/AuthContext'
import { MemberOnlyBox } from '../components/membership/MemberOnlyBox'
import { MEMBERSHIP_TEXT } from '../data/membershipContent'
import { ELIGIBILITY_PATH, ELIGIBILITY_TEXT, eligibilityHref, expertHref } from '../data/eligibilityContent'
import type { EligibilityNoticeRow } from '../types'
import { dotDate } from '../utils/eligibilityFormat'
import '../wjbid-eligibility.css'

const PLACE = findMenuItem('fit', 'eligibility')
const T = ELIGIBILITY_TEXT
/** 공고별 진단에서 뒤로 오면 보던 줄로 돌아가도록 떠날 때의 스크롤 위치를 둔다 */
const SCROLL_KEY = 'wjbid.eligibility.scrollY'

type Load = { status: 'loading' } | { status: 'error' } | { status: 'ok'; rows: EligibilityNoticeRow[] }

/**
 * 적격성 진단 첫 화면 (CR-137, T3-3 「적격성 진단」, 화면 정본 list, guest).
 * 비로그인은 안내와 로그인만 보이고, 로그인하면 분석이 끝난 공개 공고를 마감 전과 지난 공고로 나눠 보인다.
 */
export default function EligibilityPage() {
  const { isAuthenticated, membership } = useAuth()
  const navigate = useNavigate()
  const navigationType = useNavigationType()
  const [load, setLoad] = useState<Load>({ status: 'loading' })

  const fetchRows = useCallback(() => {
    getEligibilityNotices()
      .then((res) => setLoad({ status: 'ok', rows: res.data }))
      .catch(() => setLoad({ status: 'error' }))
  }, [])
  const retry = () => {
    setLoad({ status: 'loading' })
    fetchRows()
  }

  useEffect(() => {
    // 진단과 판정은 회원만 쓴다(CR-157)
    if (isAuthenticated && membership === 'member') fetchRows()
  }, [isAuthenticated, membership, fetchRows])

  useEffect(() => {
    if (load.status !== 'ok') return
    let saved: string | null = null
    try {
      saved = sessionStorage.getItem(SCROLL_KEY)
      sessionStorage.removeItem(SCROLL_KEY)
    } catch { /* 저장소를 못 쓰면 맨 위에서 보인다 */ }
    // 부드럽게 굴리면 맨 위에서 내려오는 것이 보여 바로 옮긴다
    if (navigationType === 'POP' && saved) window.scrollTo({ top: Number(saved), behavior: 'instant' })
  }, [load.status, navigationType])

  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE

  const rememberScroll = () => {
    try {
      sessionStorage.setItem(SCROLL_KEY, String(window.scrollY))
    } catch { /* 저장소를 못 쓰면 돌아올 때 맨 위에서 보인다 */ }
  }
  const openNotice = (noticeId: string) => {
    rememberScroll()
    navigate(eligibilityHref(noticeId))
  }

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={T.title} desc={T.desc} />
      <div className="eq-steps">
        {T.steps.map((s, i) => (
          <div key={s.name}>
            <i>{i + 1}</i>
            <p style={{ margin: 0 }}><b>{s.name}</b><span>{s.desc}</span></p>
          </div>
        ))}
      </div>

      {!isAuthenticated ? (
        <section className="cbx">
          <div className="eq-gbox">
            <div className="empty2"><div className="icn"><SubIcon name="user" /></div></div>
            <b>{T.guest.title}</b>
            <span>{T.guest.desc}</span>
            <div className="eq-bb">
              <Link className="eq-obtn eq-p" to="/login" state={{ from: ELIGIBILITY_PATH }}>{T.guest.login}</Link>
              <Link className="eq-obtn" to="/register">{T.guest.register}</Link>
            </div>
          </div>
        </section>
      ) : membership === 'free' ? (
        <MemberOnlyBox title={MEMBERSHIP_TEXT.eligibility.title} desc={MEMBERSHIP_TEXT.eligibility.desc} />
      ) : load.status === 'loading' ? (
        <section className="cbx pad"><LoadingSpinner fullPage /></section>
      ) : load.status === 'error' ? (
        <section className="cbx">
          <div className="empty2" style={{ borderBottom: 0 }}>
            <div className="icn"><Icon name="alert" /></div>
            <b>{T.loadError}</b>
            <Button className="eq-retry" variant="outline" size="lg" onClick={retry}>{T.retry}</Button>
          </div>
        </section>
      ) : (
        <NoticeSections rows={load.rows} onOpen={openNotice} onLink={rememberScroll} />
      )}

      <ApplyBand icon="headset" title={T.expert.title} desc={T.expert.desc} action={{ label: T.expert.action, to: expertHref() }} />
    </SubPageFrame>
  )
}

function NoticeSections({ rows, onOpen, onLink }: { rows: EligibilityNoticeRow[]; onOpen: (id: string) => void; onLink: () => void }) {
  const open = rows.filter((r) => r.open)
  const past = rows.filter((r) => !r.open)
  return (
    <>
      <section className="cbx pad">
        <h2 className="h2">{T.open.title}<small>{T.open.desc}</small></h2>
        {open.length > 0 ? (
          <NoticeTable rows={open} onOpen={onOpen} onLink={onLink} />
        ) : (
          <div className="empty2" style={{ borderBottom: 0, padding: '48px 20px 40px' }}>
            <div className="icn"><SubIcon name="hour" /></div>
            <b>{T.empty.title}</b>
            <span>{past.length > 0 ? `${T.empty.desc}. ${T.empty.pastHint}` : T.empty.desc}</span>
          </div>
        )}
      </section>
      {past.length > 0 && (
        <section className="cbx pad">
          <h2 className="h2">{T.past.title(past.length)}<small>{T.past.desc}</small></h2>
          <NoticeTable rows={past} onOpen={onOpen} onLink={onLink} />
        </section>
      )}
    </>
  )
}

function NoticeTable({ rows, onOpen, onLink }: { rows: EligibilityNoticeRow[]; onOpen: (id: string) => void; onLink: () => void }) {
  return (
    <table className="tbl">
      <thead>
        <tr>
          <th>{T.cols.notice}</th>
          <th className="eq-c" style={{ width: 130 }}>{T.cols.deadline}</th>
          <th className="eq-c" style={{ width: 110 }}>{T.cols.items}</th>
          <th style={{ width: 170 }}>{T.cols.mine}</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.noticeId} className="eq-lnk" onClick={() => onOpen(r.noticeId)}>
            <td>
              <Link to={eligibilityHref(r.noticeId)} onClick={(e) => { e.stopPropagation(); onLink() }}>
                {r.koreanTitle ?? r.solicitationNumber}
              </Link>
              <span className="eng mono">{r.solicitationNumber}</span>
            </td>
            <td className="eq-c">{dotDate(r.responseDeadline)}</td>
            <td className="eq-c">{T.count(r.itemCount)}</td>
            <td><Progress answered={r.answeredCount} total={r.itemCount} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function Progress({ answered, total }: { answered: number; total: number }) {
  if (answered === 0 || total === 0) return <span className="eq-prog eq-none">{T.notStarted}</span>
  return (
    <span className="eq-prog">
      <span className="eq-bar"><i style={{ width: `${Math.round((answered / total) * 100)}%` }} /></span>
      {answered} / {total}
    </span>
  )
}
