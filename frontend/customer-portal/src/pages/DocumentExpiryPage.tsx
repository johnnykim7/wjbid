import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { Button, Spinner } from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { SubPageFrame } from '../components/subpage/SubPageFrame'
import { SubIcon } from '../components/subpage/SubIcon'
import { DatePick } from '../components/expiry/DatePick'
import { useAuth } from '../context/AuthContext'
import { changeDocumentExpiry, getDocumentExpiry } from '../api/client'
import { EXPIRY_TEXT as T } from '../data/expiryContent'
import type { ExpiryDocument, ExpiryOverview, ExpiryStatus } from '../types/expiry'
import '../wjbid-expiry.css'

const PLACE = findMenuItem('documents', 'expiry-alert')
const VAULT_PATH = '/documents'

type CardKey = Extract<ExpiryStatus, 'EXPIRED' | 'SOON' | 'NONE'>

function LeftTag({ doc }: { doc: ExpiryDocument }) {
  if (doc.daysLeft === null) return <span className="dleft n">{T.noDate}</span>
  if (doc.daysLeft < 0) return <span className="dleft x">{T.expired}</span>
  const label = doc.daysLeft === 0 ? T.today : T.dday(doc.daysLeft)
  return <span className={`dleft ${doc.status === 'SOON' ? 's' : 'o'}`}>{label}</span>
}

/** 유효기간 알림 (CR-136, T3-3 「유효기간 알림」, 화면 정본 expiry) */
export default function DocumentExpiryPage() {
  const { userEmail } = useAuth()
  const [data, setData] = useState<ExpiryOverview | null>(null)
  const [failed, setFailed] = useState(false)
  const [filter, setFilter] = useState<CardKey | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [rowError, setRowError] = useState<Record<string, string>>({})

  const load = useCallback(async () => {
    try {
      const { data } = await getDocumentExpiry()
      setData(data)
      setFailed(false)
    } catch {
      setFailed(true)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const rows = useMemo(
    () => (data ? data.documents.filter((d) => filter === null || d.status === filter) : []),
    [data, filter],
  )

  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE

  const save = async (doc: ExpiryDocument, expiresOn: string | null) => {
    setBusyId(doc.id)
    setRowError((e) => {
      const next = { ...e }
      delete next[doc.id]
      return next
    })
    try {
      await changeDocumentExpiry(doc.id, expiresOn)
      await load()
    } catch {
      setRowError((e) => ({ ...e, [doc.id]: T.saveFail }))
    } finally {
      setBusyId(null)
    }
  }

  const toggle = (key: CardKey) => setFilter((f) => (f === key ? null : key))
  const alertsOn = !!data && data.alertDays.length > 0

  const cards: { key: CardKey; label: string; count: number; hint: string; red?: boolean }[] = data
    ? [
        { key: 'EXPIRED', label: T.cards.expired.label, count: data.summary.expired, hint: T.cards.expired.hint, red: true },
        { key: 'SOON', label: T.cards.soon.label(data.soonDays), count: data.summary.soon, hint: alertsOn ? T.cards.soon.hint : T.cards.soon.hintOff },
        { key: 'NONE', label: T.cards.noDate.label, count: data.summary.noDate, hint: T.cards.noDate.hint },
      ]
    : []
  const filterLabel = cards.find((c) => c.key === filter)?.label

  return (
    <SubPageFrame group={group} item={item}>
      <div className="exp">
        <div className="ph2">
          <div>
            <h1>{T.title}</h1>
            <p>{data && !alertsOn ? T.descOff : T.desc(data?.alertDays ?? [])}</p>
          </div>
          {userEmail && (
            <div className="rcv">
              {T.receiver}
              <b><SubIcon name="mail" />{userEmail}</b>
            </div>
          )}
        </div>

        {failed && !data && (
          <section className="cbx pad">
            <div className="empty2">
              <div className="icn"><Icon name="alert" /></div>
              <b>{T.loadFail}</b>
              <Button className="wj-reset bigb o retry" onClick={load}>{T.retry}</Button>
            </div>
          </section>
        )}

        {!data && !failed && (
          <section className="cbx pad"><div className="wait"><Spinner /></div></section>
        )}

        {data && data.summary.total === 0 && (
          <section className="cbx pad">
            <div className="empty2">
              <div className="icn"><Icon name="folder" /></div>
              <b>{T.empty.title}</b>
              <span>{T.empty.desc}</span>
              <Link className="bigb o retry" to={VAULT_PATH}>{T.empty.action}</Link>
            </div>
          </section>
        )}

        {data && data.summary.total > 0 && (
          <>
            <div className="xsum">
              {cards.map((c) => (
                <Button
                  key={c.key}
                  className={`wj-reset${filter === c.key ? ' on' : ''}`}
                  aria-pressed={filter === c.key}
                  onClick={() => toggle(c.key)}
                >
                  <span>{c.label}</span>
                  <b className={c.red && c.count > 0 ? 'r' : undefined}>{c.count}<small>{T.unit}</small></b>
                  <em>{c.hint}</em>
                </Button>
              ))}
            </div>

            <section className="cbx pad">
              <h2 className="h2">
                {T.listTitle(data.summary.total)}
                <small>{filterLabel ? T.filtered(filterLabel) : T.listSub}</small>
                {filter && <Button className="wj-reset all" onClick={() => setFilter(null)}>{T.showAll}</Button>}
              </h2>
              {data.summary.noDate === data.summary.total && <p className="nodate">{T.noDateYet}</p>}
              <table className="tbl">
                <thead>
                  <tr>
                    <th>{T.cols.name}</th>
                    <th className="w-kind">{T.cols.kind}</th>
                    <th className="w-date">{T.cols.expiresOn}</th>
                    <th className="c w-left">{T.cols.left}</th>
                    <th className="c w-alert">{T.cols.lastAlert}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((d) => {
                    const name = d.displayName || d.fileName
                    return (
                      <tr key={d.id}>
                        <td><b>{name}</b></td>
                        <td>{d.categoryLabel}</td>
                        <td>
                          <DatePick
                            value={d.expiresOn}
                            busy={busyId === d.id}
                            label={T.pickAria(name)}
                            onSave={(ymd) => save(d, ymd)}
                          />
                          {d.expiresOn && (
                            <Button
                              className="wj-reset rmv"
                              disabled={busyId === d.id}
                              aria-label={T.removeAria(name)}
                              onClick={() => save(d, null)}
                            >
                              {T.remove}
                            </Button>
                          )}
                          {rowError[d.id] && <p className="ferr">{rowError[d.id]}</p>}
                        </td>
                        <td className="c"><LeftTag doc={d} /></td>
                        <td className={`c${d.lastAlertedAt ? '' : ' mute'}`}>{d.lastAlertedAt?.slice(0, 10) ?? T.noAlert}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              <div className="xnote">
                <Icon name="alert" />
                <div>{T.note.before}<Link to={VAULT_PATH}>{T.note.link}</Link>{T.note.after}</div>
              </div>
            </section>
          </>
        )}

        <div className="apply">
          <div className="icn"><Icon name="folder" /></div>
          <div className="xtx">
            <b>{T.band.title}</b>
            <span>{T.band.desc}</span>
          </div>
          <Link className="bigb" to={VAULT_PATH}>{T.band.action}</Link>
        </div>
      </div>
    </SubPageFrame>
  )
}
