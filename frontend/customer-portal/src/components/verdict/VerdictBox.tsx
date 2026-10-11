import { Link } from 'react-router-dom'
import { Icon } from '../layout/Icon'
import { ANSWER_OPTIONS, eligibilityHref } from '../../data/eligibilityContent'
import { EVIDENCE_TEXT, VERDICT_BOX_TEXT as T, VERDICT_TAG } from '../../data/verdictContent'
import type { EligibilityBlocker, EligibilityDetail, EligibilityItem } from '../../types'
import { dotDate, monthDayTime } from '../../utils/eligibilityFormat'
import { VerdictTag, VerdictWhy } from './VerdictTag'
// 공고 상세는 속페이지 틀 밖이라 상자가 쓰는 속페이지 규칙을 직접 싣는다
import '../../wjbid-sub.css'
import '../../wjbid-eligibility.css'
import '../../wjbid-verdict.css'

/**
 * 공고 상세의 우리 회사 판정 상자 (CR-140, T3-3 「공고 상세」, 화면 정본 notice, states).
 * 참여 가능이면 통과 조건 자격마다 붙인 서류를, 그 밖은 판정을 정한 자격만 보인다.
 */
export function VerdictBox({ detail }: { detail: EligibilityDetail }) {
  const { verdict, blockers, items, summary, notice } = detail
  const byNo = new Map(items.map((i) => [i.itemNo, i]))
  const rows: { item: EligibilityItem; blocker?: EligibilityBlocker }[] = verdict === 'ELIGIBLE'
    ? items.filter((i) => i.gate).map((item) => ({ item }))
    : blockers.flatMap((b) => {
      const item = byNo.get(b.itemNo)
      return item ? [{ item, blocker: b }] : []
    })
  return (
    <div className="wj vd-host">
      <section className={`cbx vd-box vd-b-${VERDICT_TAG[verdict].tone}`}>
        <div className="vd-bhd">
          <div>
            <div className="vd-lb">{T.label}</div>
            <div className="vd-ln">
              <VerdictTag verdict={verdict} />
              <p><VerdictWhy verdict={verdict} blockers={blockers} gateCount={summary.gateCount} /></p>
            </div>
          </div>
          <Link className="eq-obtn" to={eligibilityHref(notice.noticeId)}>{T.answer}<Icon name="cr" /></Link>
        </div>
        {rows.length > 0 && (
          <table className="vd-bt">
            <thead>
              <tr>
                <th>{verdict === 'ELIGIBLE' ? T.colAll : T.colBlocked}</th>
                <th style={{ width: 270 }}>{T.colSource}</th>
                <th style={{ width: 190 }}>{T.colAnswer}</th>
                <th style={{ width: 270 }}>{T.colDocument}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(({ item, blocker }) => (
                <tr key={item.itemNo}>
                  <td><b>{item.title}</b></td>
                  <td className="mono">{item.sourceRef}</td>
                  <td><AnswerCell item={item} blocker={blocker} /></td>
                  <td><DocumentCell item={item} blocker={blocker} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <p className="vd-foot">{T.foot}</p>
      </section>
    </div>
  )
}

function AnswerCell({ item, blocker }: { item: EligibilityItem; blocker?: EligibilityBlocker }) {
  const option = ANSWER_OPTIONS.find((o) => o.value === item.answer)
  if (!option) {
    const label = (blocker && T.problemAnswer[blocker.problem]) ?? T.problemAnswer.UNANSWERED
    return <span className="vd-a n">{label}</span>
  }
  const at = monthDayTime(item.answeredAt)
  return <><span className={`vd-a ${option.tone}`}>{option.label}</span>{at && <small>{at}</small>}</>
}

function DocumentCell({ item, blocker }: { item: EligibilityItem; blocker?: EligibilityBlocker }) {
  if (blocker?.problem === 'NO_DOCUMENT') return <span className="vd-xp">{T.noDocument}</span>
  if (blocker?.problem === 'EXPIRED') {
    return (
      <>
        <span className="vd-dn"><Icon name="folder" />{blocker.documentName}</span>
        <small className="vd-xp">{T.expired(dotDate(blocker.expiresOn))}</small>
      </>
    )
  }
  const d = item.attachedDocument
  if (!d) return <span className="vd-dash">-</span>
  return (
    <>
      <span className="vd-dn"><Icon name="folder" />{d.name}</span>
      <small>{d.expiresOn ? EVIDENCE_TEXT.expiresOn(dotDate(d.expiresOn)) : EVIDENCE_TEXT.noExpiry}</small>
    </>
  )
}
