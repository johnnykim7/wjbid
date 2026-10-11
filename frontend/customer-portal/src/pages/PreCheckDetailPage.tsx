import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useParams } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { Button } from '@therecommerce-develop/ds-ui'
import { getPreCheck } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PreCheckBoard } from '../components/precheck/PreCheckBoard'
import { SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { useAuth } from '../context/AuthContext'
import { PRE_CHECK_DETAIL_TEXT, PRE_CHECK_PATH, PRE_CHECK_TEXT, bidRequestHref } from '../data/preCheckContent'
import { STATE_LABEL } from '../types'
import type { PreCheckDetail } from '../types/preCheck'
import { dotDate } from '../utils/eligibilityFormat'
import { daysLeftKst } from '../utils/kstDate'
import '../wjbid-eligibility.css'
import '../wjbid-precheck.css'

const PLACE = findMenuItem('documents', 'pre-check')
const T = PRE_CHECK_DETAIL_TEXT

type Load = { status: 'loading' } | { status: 'notFound' } | { status: 'error' } | { status: 'ok'; detail: PreCheckDetail }

/**
 * 입찰 건별 제출 전 검사 (CR-141, T3-3 「입찰 건별 제출 전 검사」, 화면 정본 detail, states).
 * 공고 머리 아래에 공용 검사 목록을 둔다. 남의 입찰 건과 없는 입찰 건은 같은 찾을 수 없음으로 보인다.
 */
export default function PreCheckDetailPage() {
  const { bidRequestId } = useParams()
  const { pathname } = useLocation()
  const { isAuthenticated } = useAuth()
  const [load, setLoad] = useState<Load>({ status: 'loading' })

  const fetchDetail = useCallback((): Promise<void> => {
    if (!bidRequestId) return Promise.resolve()
    return getPreCheck(bidRequestId)
      .then(({ data }) => setLoad({ status: 'ok', detail: data }))
      .catch((e) => {
        // 주소가 입찰 건 번호 모양이 아니면 400, 남의 것이거나 없으면 404 라 둘 다 찾을 수 없는 입찰 건으로 보인다
        const code = isAxiosError(e) ? e.response?.status : undefined
        setLoad({ status: code === 404 || code === 400 ? 'notFound' : 'error' })
      })
  }, [bidRequestId])

  useEffect(() => {
    if (isAuthenticated) fetchDetail()
  }, [isAuthenticated, fetchDetail])
  const retry = () => {
    setLoad({ status: 'loading' })
    fetchDetail()
  }

  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: pathname }} />
  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE

  return (
    <SubPageFrame group={group} item={item}>
      {load.status === 'loading' ? (
        <section className="cbx pad"><LoadingSpinner fullPage /></section>
      ) : load.status === 'notFound' ? (
        <section className="cbx">
          <div className="empty2" style={{ borderBottom: 0, padding: '44px 20px' }}>
            <div className="icn"><Icon name="file" /></div>
            <b>{T.notFound.title}</b>
            <span>{T.notFound.desc}</span>
            <div style={{ marginTop: 18 }}><Link className="eq-obtn" to={PRE_CHECK_PATH}>{T.notFound.back}</Link></div>
          </div>
        </section>
      ) : load.status === 'error' ? (
        <section className="cbx">
          <div className="empty2" style={{ borderBottom: 0 }}>
            <div className="icn"><Icon name="alert" /></div>
            <b>{PRE_CHECK_TEXT.loadError}</b>
            <Button className="eq-retry" variant="outline" size="lg" onClick={retry}>{PRE_CHECK_TEXT.retry}</Button>
          </div>
        </section>
      ) : (
        <>
          <Header detail={load.detail} />
          <PreCheckBoard detail={load.detail} reload={fetchDetail} onSubmitted={fetchDetail} />
        </>
      )}
    </SubPageFrame>
  )
}

function Header({ detail }: { detail: PreCheckDetail }) {
  const ymd = detail.responseDeadline?.slice(0, 10)
  const past = !!ymd && daysLeftKst(ymd) < 0
  return (
    <section className="cbx">
      <div className="eq-nhd">
        <div>
          {past && <><span className="eq-tag eq-past">{PRE_CHECK_TEXT.past}</span> </>}
          <span className="pc-st">{STATE_LABEL[detail.state] ?? detail.state}</span>
          <h2>{detail.titleKo ?? detail.title ?? detail.solicitationNumber}</h2>
          <div className="eq-meta">
            {detail.solicitationNumber && <span><small>{T.meta.number}</small><b className="mono">{detail.solicitationNumber}</b></span>}
            {detail.responseDeadline && <span><small>{T.meta.deadline}</small><b>{dotDate(detail.responseDeadline)}</b></span>}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {detail.noticeId && <Link className="eq-obtn" to={`/search/${detail.noticeId}`}>{T.noticeLink}<Icon name="cr" /></Link>}
          <Link className="eq-obtn" to={bidRequestHref(detail.bidRequestId)}>{T.bidLink}<Icon name="cr" /></Link>
        </div>
      </div>
    </section>
  )
}
