import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate, useNavigationType } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { getMyPreCheck } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { ApplyBand, PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { useAuth } from '../context/AuthContext'
import { PRE_CHECK_TEXT, SEARCH_PATH, preCheckHref, reviewHref } from '../data/preCheckContent'
import { STATE_LABEL } from '../types'
import type { PreCheckRow } from '../types/preCheck'
import { dotDate } from '../utils/eligibilityFormat'
import { daysLeftKst } from '../utils/kstDate'
import '../wjbid-eligibility.css'
import '../wjbid-precheck.css'

const PLACE = findMenuItem('documents', 'pre-check')
const T = PRE_CHECK_TEXT
/** 입찰 건별 검사에서 뒤로 오면 보던 줄로 돌아가도록 떠날 때의 스크롤 위치를 둔다 */
const SCROLL_KEY = 'wjbid.preCheck.scrollY'

type Load = { status: 'loading' } | { status: 'error' } | { status: 'ok'; rows: PreCheckRow[] }

/** 마감은 날짜만 믿을 수 있어(CR-137) 한국 날짜로 지났는지만 본다 */
const isPast = (deadline: string | null) => {
  const ymd = deadline?.slice(0, 10)
  return !!ymd && daysLeftKst(ymd) < 0
}

/**
 * 제출 전 검사 첫 화면 (CR-141, T3-3 「제출 전 검사」, 화면 정본 list, states).
 * 회원의 입찰 건마다 필수 서류를 몇 개 올렸는지, 빠짐, 어긋남, 사람 확인 수를 서버가 센 대로 보인다.
 */
export default function PreCheckPage() {
  const { isAuthenticated } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const navigationType = useNavigationType()
  const [load, setLoad] = useState<Load>({ status: 'loading' })

  const fetchRows = useCallback(() => {
    getMyPreCheck()
      .then((res) => setLoad({ status: 'ok', rows: res.data }))
      .catch(() => setLoad({ status: 'error' }))
  }, [])
  const retry = () => {
    setLoad({ status: 'loading' })
    fetchRows()
  }

  useEffect(() => {
    if (isAuthenticated) fetchRows()
  }, [isAuthenticated, fetchRows])

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

  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: pathname }} />
  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE

  const rememberScroll = () => {
    try {
      sessionStorage.setItem(SCROLL_KEY, String(window.scrollY))
    } catch { /* 저장소를 못 쓰면 돌아올 때 맨 위에서 보인다 */ }
  }
  const openRow = (bidRequestId: string) => {
    rememberScroll()
    navigate(preCheckHref(bidRequestId))
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

      {load.status === 'loading' ? (
        <section className="cbx pad"><LoadingSpinner fullPage /></section>
      ) : load.status === 'error' ? (
        <section className="cbx">
          <div className="empty2" style={{ borderBottom: 0 }}>
            <div className="icn"><Icon name="alert" /></div>
            <b>{T.loadError}</b>
            <Button className="eq-retry" variant="outline" size="lg" onClick={retry}>{T.retry}</Button>
          </div>
        </section>
      ) : load.rows.length === 0 ? (
        <section className="cbx">
          <div className="empty2" style={{ borderBottom: 0, padding: '44px 20px' }}>
            <div className="icn"><Icon name="file" /></div>
            <b>{T.empty.title}</b>
            <span>{T.empty.desc}</span>
            <div style={{ marginTop: 18 }}><Link className="eq-obtn" to={SEARCH_PATH}>{T.empty.action}</Link></div>
          </div>
        </section>
      ) : (
        <section className="cbx pad">
          <h2 className="h2">{T.listTitle(load.rows.length)}<small>{T.listDesc}</small></h2>
          <RowTable rows={load.rows} onOpen={openRow} onLink={rememberScroll} />
          <p className="pc-cap">{T.cap}</p>
        </section>
      )}

      <ApplyBand icon="headset" title={T.review.title} desc={T.review.desc} action={{ label: T.review.action, to: reviewHref() }} />
    </SubPageFrame>
  )
}

function RowTable({ rows, onOpen, onLink }: { rows: PreCheckRow[]; onOpen: (id: string) => void; onLink: () => void }) {
  return (
    <table className="tbl pc-tbl">
      <thead>
        <tr>
          <th>{T.cols.notice}</th>
          <th className="eq-c" style={{ width: 150 }}>{T.cols.deadline}</th>
          <th className="eq-c" style={{ width: 110 }}>{T.cols.state}</th>
          <th className="eq-c" style={{ width: 110 }}>{T.cols.uploaded}</th>
          <th className="eq-c" style={{ width: 70 }}>{T.cols.missing}</th>
          <th className="eq-c" style={{ width: 90 }}>{T.cols.mismatch}</th>
          <th className="eq-c" style={{ width: 80 }}>{T.cols.needsHuman}</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.bidRequestId} className="eq-lnk" onClick={() => onOpen(r.bidRequestId)}>
            <td>
              <Link to={preCheckHref(r.bidRequestId)} onClick={(e) => { e.stopPropagation(); onLink() }}>
                {r.titleKo ?? r.title ?? r.solicitationNumber}
              </Link>
              {r.solicitationNumber && <span className="eng mono">{r.solicitationNumber}</span>}
            </td>
            <td className="eq-c">
              {dotDate(r.responseDeadline) || '-'}
              {isPast(r.responseDeadline) && <span className="eq-tag eq-past">{T.past}</span>}
            </td>
            <td className="eq-c"><span className="pc-st">{STATE_LABEL[r.state] ?? r.state}</span></td>
            {r.analysisReady && r.summary ? (
              <>
                <td className="eq-c"><span className="pc-n">{r.summary.uploaded}<small>/ {r.summary.required}</small></span></td>
                <td className="eq-c"><Count n={r.summary.missing} tone="pc-r" /></td>
                <td className="eq-c"><Count n={r.summary.mismatch} tone="pc-a" /></td>
                <td className="eq-c"><Count n={r.summary.needsHuman} tone="pc-h" /></td>
              </>
            ) : (
              <td colSpan={4} className="pc-na">{T.notReady}</td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function Count({ n, tone }: { n: number; tone: string }) {
  return <span className={n > 0 ? tone : 'pc-z'}>{n}</span>
}
