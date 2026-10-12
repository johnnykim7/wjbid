import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useNavigationType } from 'react-router-dom'
import { Button, ToggleGroup, ToggleGroupItem } from '@therecommerce-develop/ds-ui'
import { getEligibilityNotices } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { SubIcon } from '../components/subpage/SubIcon'
import { ApplyBand, PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { VerdictTag, VerdictWhy } from '../components/verdict/VerdictTag'
import { useAuth } from '../context/AuthContext'
import { MemberOnlyBox } from '../components/membership/MemberOnlyBox'
import { MEMBERSHIP_TEXT } from '../data/membershipContent'
import { ELIGIBILITY_TEXT, eligibilityHref } from '../data/eligibilityContent'
import { VERDICT_ORDER, VERDICT_PATH, VERDICT_TAG, VERDICT_TEXT as T, verdictExpertHref } from '../data/verdictContent'
import type { EligibilityNoticeRow, EligibilityVerdict } from '../types'
import { dotDate } from '../utils/eligibilityFormat'
import '../wjbid-eligibility.css'
import '../wjbid-verdict.css'

const PLACE = findMenuItem('fit', 'opportunity-fit')
/** 공고별 진단에서 뒤로 오면 보던 줄과 거른 판정으로 돌아가도록 떠날 때 둔다 */
const BACK_KEY = 'wjbid.verdicts.back'

type Load = { status: 'loading' } | { status: 'error' } | { status: 'ok'; rows: EligibilityNoticeRow[] }

type Back = { y: number; filter: EligibilityVerdict | '' }

function readBack(): Back | null {
  try {
    const raw = sessionStorage.getItem(BACK_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/**
 * 공고별 판정 (CR-140, T3-3 「공고별 판정」, 화면 정본 list, guest, states).
 * 판정은 서버가 적격성 진단의 답과 붙인 서류로 낸 값을 그대로 보인다(BIZ-040). 개수는 판정할 수 있는 공고 전체 기준이다.
 */
export default function VerdictPage() {
  const { isAuthenticated, membership } = useAuth()
  const navigate = useNavigate()
  const navigationType = useNavigationType()
  const [load, setLoad] = useState<Load>({ status: 'loading' })
  const [back] = useState(() => (navigationType === 'POP' ? readBack() : null))
  const [filter, setFilter] = useState<EligibilityVerdict | ''>(back?.filter ?? '')

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
    try {
      sessionStorage.removeItem(BACK_KEY)
    } catch { /* 저장소를 못 쓰면 지울 것도 없다 */ }
    // 부드럽게 굴리면 맨 위에서 내려오는 것이 보여 바로 옮긴다
    if (back) window.scrollTo({ top: back.y, behavior: 'instant' })
  }, [load.status, back])

  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE

  const remember = () => {
    try {
      sessionStorage.setItem(BACK_KEY, JSON.stringify({ y: window.scrollY, filter }))
    } catch { /* 저장소를 못 쓰면 돌아올 때 맨 위에서 다 보인다 */ }
  }
  const openNotice = (noticeId: string) => {
    remember()
    navigate(eligibilityHref(noticeId))
  }

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={T.title} desc={T.desc} />
      <div className="vd-legend">
        {VERDICT_ORDER.map((v) => (
          <div key={v}>
            <VerdictTag verdict={v} />
            <p>{T.legend[v]}</p>
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
              <Link className="eq-obtn eq-p" to="/login" state={{ from: VERDICT_PATH }}>{T.guest.login}</Link>
              <Link className="eq-obtn" to="/register">{T.guest.register}</Link>
            </div>
          </div>
        </section>
      ) : membership === 'free' ? (
        <MemberOnlyBox title={MEMBERSHIP_TEXT.verdict.title} desc={MEMBERSHIP_TEXT.verdict.desc} />
      ) : load.status === 'loading' ? (
        <section className="cbx pad"><LoadingSpinner fullPage /></section>
      ) : load.status === 'error' ? (
        <section className="cbx">
          <div className="empty2" style={{ borderBottom: 0 }}>
            <div className="icn"><Icon name="alert" /></div>
            <b>{ELIGIBILITY_TEXT.loadError}</b>
            <Button className="eq-retry" variant="outline" size="lg" onClick={retry}>{ELIGIBILITY_TEXT.retry}</Button>
          </div>
        </section>
      ) : (
        <Verdicts rows={load.rows} filter={filter} onFilter={setFilter} onOpen={openNotice} onLink={remember} />
      )}

      <ApplyBand
        icon="headset"
        title={ELIGIBILITY_TEXT.expert.title}
        desc={ELIGIBILITY_TEXT.expert.desc}
        action={{ label: ELIGIBILITY_TEXT.expert.action, to: verdictExpertHref() }}
      />
    </SubPageFrame>
  )
}

function Verdicts({ rows, filter, onFilter, onOpen, onLink }: {
  rows: EligibilityNoticeRow[]
  filter: EligibilityVerdict | ''
  onFilter: (v: EligibilityVerdict | '') => void
  onOpen: (id: string) => void
  onLink: () => void
}) {
  const count = (v: EligibilityVerdict) => rows.filter((r) => r.verdict === v).length
  const shown = filter ? rows.filter((r) => r.verdict === filter) : rows
  const open = rows.filter((r) => r.open)
  const past = rows.filter((r) => !r.open)
  const openShown = shown.filter((r) => r.open)
  const pastShown = shown.filter((r) => !r.open)
  const notJudged = (
    <div className="eq-notj">
      <Icon name="alert" />
      <div>{T.notJudged}</div>
    </div>
  )
  return (
    <>
      <ToggleGroup
        type="single"
        className="vd-nums"
        value={filter}
        onValueChange={(v) => onFilter(v as EligibilityVerdict | '')}
        aria-label={T.title}
      >
        {VERDICT_ORDER.map((v) => (
          <ToggleGroupItem key={v} value={v} className={`vd-k-${VERDICT_TAG[v].tone}`}>
            <span><i />{VERDICT_TAG[v].label}</span>
            <b>{count(v)}<small>{T.unit}</small></b>
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p className="vd-ncap">{T.countCap(rows.length)}</p>

      <section className="cbx pad">
        <h2 className="h2">{T.open.title}<small>{T.open.desc}</small></h2>
        {open.length > 0 ? (
          <VerdictTable rows={openShown} onOpen={onOpen} onLink={onLink} />
        ) : (
          <div className="empty2" style={{ borderBottom: 0, padding: '48px 20px 40px' }}>
            <div className="icn"><SubIcon name="hour" /></div>
            <b>{T.empty.title}</b>
            <span>{past.length > 0 ? `${T.empty.desc}. ${T.empty.pastHint}` : T.empty.desc}</span>
          </div>
        )}
        {past.length === 0 && notJudged}
      </section>
      {past.length > 0 && (
        <section className="cbx pad">
          <h2 className="h2">{T.past.title(pastShown.length)}<small>{T.past.desc}</small></h2>
          <VerdictTable rows={pastShown} onOpen={onOpen} onLink={onLink} />
          {notJudged}
        </section>
      )}
    </>
  )
}

function VerdictTable({ rows, onOpen, onLink }: { rows: EligibilityNoticeRow[]; onOpen: (id: string) => void; onLink: () => void }) {
  return (
    <table className="tbl">
      <thead>
        <tr>
          <th style={{ width: 132 }}>{T.cols.verdict}</th>
          <th>{T.cols.notice}</th>
          <th style={{ width: 340 }}>{T.cols.why}</th>
          <th className="eq-c" style={{ width: 120 }}>{T.cols.deadline}</th>
        </tr>
      </thead>
      <tbody>
        {rows.length === 0 ? (
          <tr className="vd-none"><td colSpan={4}>{T.noneOfVerdict}</td></tr>
        ) : rows.map((r) => (
          <tr key={r.noticeId} className="eq-lnk" onClick={() => onOpen(r.noticeId)}>
            <td className="vd-tc"><VerdictTag verdict={r.verdict} /></td>
            <td>
              <Link to={eligibilityHref(r.noticeId)} onClick={(e) => { e.stopPropagation(); onLink() }}>
                {r.koreanTitle ?? r.solicitationNumber}
              </Link>
              <span className="eng mono">{r.solicitationNumber}</span>
            </td>
            <td><span className="vd-why"><VerdictWhy verdict={r.verdict} blockers={r.blockers} gateCount={r.gateCount} /></span></td>
            <td className="eq-c">{dotDate(r.responseDeadline)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
