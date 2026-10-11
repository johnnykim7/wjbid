import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useParams } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { Button, ToggleGroup, ToggleGroupItem } from '@therecommerce-develop/ds-ui'
import { getEligibilityDetail, saveEligibilityAnswer } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { useAuth } from '../context/AuthContext'
import { ANSWER_OPTIONS, ELIGIBILITY_DETAIL_TEXT, ELIGIBILITY_PATH, ELIGIBILITY_TEXT, expertHref } from '../data/eligibilityContent'
import type { EligibilityAnswerValue, EligibilityDetail, EligibilityItem } from '../types'
import { dotDate, monthDayTime, officeName } from '../utils/eligibilityFormat'
import '../wjbid-eligibility.css'

const PLACE = findMenuItem('fit', 'eligibility')
const T = ELIGIBILITY_DETAIL_TEXT

type Load = { status: 'loading' } | { status: 'notFound' } | { status: 'error' } | { status: 'ok'; detail: EligibilityDetail }

/**
 * 공고별 진단 (CR-137, T3-3 「공고별 진단」, 화면 정본 detail, states).
 * 자격마다 회원이 있음, 없음, 확인 필요 중 하나를 고르면 바로 저장하고, 통과 조건 자격의 답 개수만 센다.
 * 서류함 서류는 증빙 서류 글과 서류 종류 이름이 맞을 때만 붙인다(BIZ-032).
 */
export default function EligibilityDetailPage() {
  const { noticeId } = useParams()
  const { pathname } = useLocation()
  const { isAuthenticated } = useAuth()
  const [load, setLoad] = useState<Load>({ status: 'loading' })
  const [saving, setSaving] = useState<Record<number, boolean>>({})
  const [failed, setFailed] = useState<Record<number, boolean>>({})

  const fetchDetail = useCallback(() => {
    if (!noticeId) return
    getEligibilityDetail(noticeId)
      .then((res) => setLoad({ status: 'ok', detail: res.data }))
      .catch((e) => {
        // 주소가 공고문 번호 모양이 아니면 400, 숨김이나 분석 전이면 404 라 둘 다 진단할 수 없는 공고로 보인다
        const code = isAxiosError(e) ? e.response?.status : undefined
        setLoad({ status: code === 404 || code === 400 ? 'notFound' : 'error' })
      })
  }, [noticeId])

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

  const putItem = (next: EligibilityItem, summary?: EligibilityDetail['summary']) =>
    setLoad((prev) => prev.status !== 'ok' ? prev : {
      status: 'ok',
      detail: {
        ...prev.detail,
        items: prev.detail.items.map((i) => (i.itemNo === next.itemNo ? next : i)),
        summary: summary ?? prev.detail.summary,
      },
    })

  const answer = async (target: EligibilityItem, raw: string) => {
    // 채운 단추를 다시 누르면 빈 값이 온다. 답 지우기는 두지 않는다(T3-4 시나리오 2-4)
    if (!raw || !noticeId || saving[target.itemNo] || raw === target.answer) return
    const value = raw as EligibilityAnswerValue
    const no = target.itemNo
    putItem({ ...target, answer: value, stale: false })
    setSaving((s) => ({ ...s, [no]: true }))
    setFailed((f) => ({ ...f, [no]: false }))
    try {
      const { data } = await saveEligibilityAnswer(noticeId, no, value)
      putItem(data.item, data.summary)
    } catch {
      putItem(target)
      setFailed((f) => ({ ...f, [no]: true }))
    } finally {
      setSaving((s) => ({ ...s, [no]: false }))
    }
  }

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
            <div style={{ marginTop: 18 }}><Link className="eq-obtn" to={ELIGIBILITY_PATH}>{T.notFound.back}</Link></div>
          </div>
        </section>
      ) : load.status === 'error' ? (
        <section className="cbx">
          <div className="empty2" style={{ borderBottom: 0 }}>
            <div className="icn"><Icon name="alert" /></div>
            <b>{ELIGIBILITY_TEXT.loadError}</b>
            <Button className="eq-retry" variant="outline" size="lg" onClick={retry}>{ELIGIBILITY_TEXT.retry}</Button>
          </div>
        </section>
      ) : (
        <Diagnosis detail={load.detail} saving={saving} failed={failed} onAnswer={answer} />
      )}
    </SubPageFrame>
  )
}

function Diagnosis({ detail, saving, failed, onAnswer }: {
  detail: EligibilityDetail
  saving: Record<number, boolean>
  failed: Record<number, boolean>
  onAnswer: (item: EligibilityItem, raw: string) => void
}) {
  const { notice, items, summary } = detail
  const office = officeName(notice.organizationName)
  const need = items.filter((i) => i.answer === 'LACK' || i.answer === 'UNSURE')
  const lack = need.filter((i) => i.answer === 'LACK')
  const unsure = need.filter((i) => i.answer === 'UNSURE')

  return (
    <>
      <section className="cbx">
        <div className="eq-nhd">
          <div>
            <span className={notice.open ? 'eq-tag eq-open' : 'eq-tag eq-past'}>{notice.open ? T.open : T.past}</span>
            <h2>{notice.koreanTitle ?? notice.solicitationNumber}</h2>
            <div className="eq-meta">
              <span><small>{T.meta.number}</small><b className="mono">{notice.solicitationNumber}</b></span>
              {notice.responseDeadline && <span><small>{T.meta.deadline}</small><b>{dotDate(notice.responseDeadline)}</b></span>}
              {office && <span><small>{T.meta.office}</small><b>{office}</b></span>}
            </div>
          </div>
          <Link className="eq-obtn" to={`/search/${notice.noticeId}`}>{T.noticeLink}<Icon name="cr" /></Link>
        </div>
      </section>

      {!notice.open && (
        <div className="xnote" style={{ margin: '16px 0 24px' }}>
          <Icon name="alert" />
          <div>{T.pastNote}</div>
        </div>
      )}

      {summary.gateCount > 0 ? (
        <>
          <div className="eq-sumcap" style={notice.open ? { marginTop: 24 } : undefined}>
            <span><b>{T.sumTitle(summary.gateCount)}</b>{T.sumSuffix}</span>
            <span>{T.sumNote}</span>
          </div>
          <div className="eq-nums">
            <div className="eq-k1"><span><i />{T.nums.have}</span><b>{summary.have}<small>{T.unit}</small></b></div>
            <div className="eq-k2"><span><i />{T.nums.lack}</span><b>{summary.lack}<small>{T.unit}</small></b></div>
            <div className="eq-k3"><span><i />{T.nums.unsure}</span><b>{summary.unsure}<small>{T.unit}</small></b></div>
            <div className="eq-k4"><span><i />{T.nums.unanswered}</span><b>{summary.unanswered}<small>{T.unit}</small></b></div>
          </div>
        </>
      ) : (
        <div className="eq-nogate" style={notice.open ? { marginTop: 24 } : undefined}>
          <div className="eq-sumcap" style={{ margin: 0 }}><span><b>{T.sumTitle(0)}</b></span></div>
          <p style={{ margin: '10px 0 0' }}>{T.noGate}</p>
        </div>
      )}

      <section className="cbx pad">
        <h2 className="h2">{T.listTitle(summary.allCount)}<small>{T.listDesc}</small></h2>
        <ul className="eq-elist">
          {items.map((i) => (
            <li key={i.itemNo}>
              <span className="eq-no">{i.itemNo + 1}</span>
              <div>
                {i.stale && <div className="eq-stl"><Icon name="alert" />{T.stale}</div>}
                <div className="eq-tt">
                  <b>{i.title}</b>
                  {i.gate && <span className="eq-tag eq-gate">{T.gate}</span>}
                  {i.mandatory && <span className="eq-tag">{T.mandatory}</span>}
                </div>
                {i.description && <p className="eq-ds">{i.description}</p>}
                {(i.sourceRef || i.evidenceBy) && (
                  <dl className="eq-kv">
                    {i.sourceRef && <><dt>{T.sourceRef}</dt><dd className="mono">{i.sourceRef}</dd></>}
                    {i.evidenceBy && <><dt>{T.evidenceBy}</dt><dd>{i.evidenceBy}</dd></>}
                  </dl>
                )}
                {i.vaultCategory && <VaultRow item={i} />}
              </div>
              <div className="eq-answ">
                <ToggleGroup
                  type="single"
                  className="eq-ans"
                  value={i.answer ?? ''}
                  onValueChange={(v) => onAnswer(i, v)}
                  disabled={!!saving[i.itemNo]}
                  aria-label={i.title}
                >
                  {ANSWER_OPTIONS.map((o) => (
                    <ToggleGroupItem key={o.value} value={o.value} className={o.tone}>
                      {i.answer === o.value && <Icon name="ok" />}{o.label}
                    </ToggleGroupItem>
                  ))}
                </ToggleGroup>
                {failed[i.itemNo] ? (
                  <small className="eq-err" role="alert">{T.saveError}</small>
                ) : saving[i.itemNo] ? (
                  <small>{T.saving}</small>
                ) : (
                  i.answeredAt && <small>{T.saved(monthDayTime(i.answeredAt))}</small>
                )}
              </div>
            </li>
          ))}
        </ul>
        <div className="eq-notj">
          <Icon name="alert" />
          <div>{T.notJudged}</div>
        </div>
      </section>

      {need.length > 0 && (
        <div className="apply">
          <div className="icn"><Icon name="headset" /></div>
          <div className="xtx">
            <b>{T.expertTitle(lack.length, unsure.length)}</b>
            <span>{T.expertDesc}</span>
            <div className="eq-need">{need.map((i) => <em key={i.itemNo}>{i.title}</em>)}</div>
          </div>
          <Link
            className="bigb"
            to={expertHref(notice.solicitationNumber, [
              { label: T.nums.lack, titles: lack.map((i) => i.title) },
              { label: T.nums.unsure, titles: unsure.map((i) => i.title) },
            ])}
          >
            {ELIGIBILITY_TEXT.expert.action}
          </Link>
        </div>
      )}
    </>
  )
}

function VaultRow({ item }: { item: EligibilityItem }) {
  const category = item.vaultCategoryName ?? ''
  const [first, ...rest] = item.vaultDocuments
  if (!first) {
    const t = T.vaultNone(category)
    return (
      <div className="eq-vlt eq-none">
        <Icon name="folder" />
        <span>{t.before}<b>{t.name}</b>{t.after}</span>
        <Link to="/documents">{T.vaultLink}</Link>
      </div>
    )
  }
  return (
    <div className="eq-vlt">
      <Icon name="folder" />
      <span>
        {T.vaultHave} <b>{first.name}</b>{rest.length > 0 && ` ${T.vaultMore(rest.length)}`}{' '}
        <small>{T.vaultUploaded(category, dotDate(first.uploadedAt))}</small>
      </span>
      <Link to="/documents">{T.vaultLink}</Link>
    </div>
  )
}
