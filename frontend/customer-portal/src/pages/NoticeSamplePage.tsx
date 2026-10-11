import { Link, Navigate } from 'react-router-dom'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { LoadState, MarketApplyBand } from '../components/market/MarketParts'
import { fmtDate, useNow } from '../components/market/marketUtils'
import { marketApi } from '../api/market'
import { useMarketLoad } from '../hooks/useMarketLoad'
import { SAMPLE_TEXT } from '../data/marketContent'

const PLACE = findMenuItem('bids', 'notice-sample')
/** 발주처 경로("DEPT OF DEFENSE.….411TH CSB.0906 AQ CO …")에서 보이는 마지막 단계 수 */
const AGENCY_TAIL = 2

const lines = (text: string | undefined) => (text ?? '').split('\n').map((l) => l.trim()).filter(Boolean)
const agencyOf = (path: string | undefined) =>
  path ? path.split('.').slice(-AGENCY_TAIL).map((p) => p.replace(/\s+/g, ' ').trim()).join(' · ') : '-'

function Paras({ text }: { text: string | undefined }) {
  return <>{lines(text).map((l, i) => <p key={i}>{l}</p>)}</>
}

/** 공고문 예시 (CR-135, BID-MKT-003, T3-3 「공고문 예시」). 설정으로 고른 마감된 공고 한 건의 한글 공고문 */
export default function NoticeSamplePage() {
  const sample = useMarketLoad(() => marketApi.sample(), 'sample')
  const now = useNow()
  if (!PLACE) return <Navigate to="/" replace />
  const t = SAMPLE_TEXT
  const s = sample.data
  const n = s?.notice
  const sum = n?.analysis?.summary
  const docs = n?.analysis?.requiredDocuments?.documents ?? []
  const closed = !!n?.responseDeadline && n.responseDeadline.slice(0, 10) < now.today

  return (
    <SubPageFrame group={PLACE.group} item={PLACE.item}>
      <PageHead title={t.title} desc={t.desc} />
      {!n ? (
        <section className="cbx pad">
          {sample.status === 'done' ? (
            <div className="empty2">
              <div className="icn"><Icon name="file" /></div>
              <b>{t.emptyTitle}</b>
              <span>{t.emptyDesc}</span>
              <Link className="lnk" to="/search">{t.emptyLink}<Icon name="cr" /></Link>
            </div>
          ) : (
            <LoadState state={sample.status === 'error' ? 'error' : 'loading'} onRetry={sample.reload} />
          )}
        </section>
      ) : (
        <section className="cbx pad ndoc">
          <div className="tg">
            {closed && <span>{t.closedTag}</span>}
            {n.type && <span>{n.type}</span>}
          </div>
          <h1 className="h1">{n.title}</h1>
          {s.originalTitle && s.originalTitle !== n.title && <p className="en0">{s.originalTitle}</p>}
          <div className="facts f4">
            <div><small>{t.facts.sol}</small><b className="mono">{n.solicitationNumber || '-'}</b></div>
            <div><small>{t.facts.agency}</small><b>{agencyOf(n.agencyName)}</b></div>
            <div><small>{t.facts.posted}</small><b>{fmtDate(s.firstPostedDate ?? n.postedDate)}</b></div>
            <div><small>{t.facts.deadline}</small><b>{fmtDate(n.responseDeadline)}</b></div>
          </div>
          {s.samUrl && (
            <a className="lnk" href={s.samUrl} target="_blank" rel="noopener noreferrer">{t.samLink}<Icon name="ext" /></a>
          )}
          {sum?.overview && <div className="nsec"><h3>{t.sec.overview}</h3><Paras text={sum.overview} /></div>}
          {sum?.scope && <div className="nsec"><h3>{t.sec.scope}</h3><Paras text={sum.scope} /></div>}
          {(sum?.contractPeriod || (sum?.keyDates?.length ?? 0) > 0) && (
            <div className="nsec two2">
              <div><h3>{t.sec.period}</h3><Paras text={sum?.contractPeriod} /></div>
              <div>
                <h3>{t.sec.keyDates}</h3>
                <ul className="kd">
                  {(sum?.keyDates ?? []).map((k, i) => <li key={i}>{k.date ? `${k.label}: ${k.date}` : k.label}</li>)}
                </ul>
              </div>
            </div>
          )}
          {sum?.eligibility && (
            <div className="nsec">
              <h3>{t.sec.eligibility}</h3>
              <ol className="nl">{lines(sum.eligibility).map((l, i) => <li key={i}>{l.replace(/^(\d+\.|-)\s*/, '')}</li>)}</ol>
            </div>
          )}
          {docs.length > 0 && (
            <div className="nsec">
              <h3>{t.sec.documents} <small>{t.sec.documentsCount(docs.length)}</small></h3>
              <table className="otbl dtbl">
                <thead>
                  <tr><th>{t.dhead.name}</th><th style={{ width: 80 }}>{t.dhead.mandatory}</th><th style={{ width: 110 }}>{t.dhead.format}</th></tr>
                </thead>
                <tbody>
                  {docs.map((doc, i) => (
                    <tr key={i}>
                      <td><b>{doc.name}</b>{doc.description && <span className="en">{doc.description}</span>}</td>
                      {doc.mandatory ? <td><span className="tag2">{t.mandatory}</span></td> : <td className="dt">{t.optional}</td>}
                      <td className="dt">{doc.format || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {sum?.evaluationCriteria && <div className="nsec"><h3>{t.sec.evaluation}</h3><Paras text={sum.evaluationCriteria} /></div>}
          {sum?.budgetInfo && <div className="nsec"><h3>{t.sec.budget}</h3><Paras text={sum.budgetInfo} /></div>}
          {(sum?.specialNotes?.length ?? 0) > 0 && (
            <div className="nsec"><h3>{t.sec.notes}</h3><ul className="bl">{sum?.specialNotes?.map((x, i) => <li key={i}>{x}</li>)}</ul></div>
          )}
        </section>
      )}
      <MarketApplyBand title={t.bandTitle} desc={t.bandDesc} />
    </SubPageFrame>
  )
}
