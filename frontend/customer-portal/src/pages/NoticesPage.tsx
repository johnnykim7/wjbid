import { Navigate } from 'react-router-dom'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { NOTICES, NOTICE_TEXT } from '../data/supportContent'

const PLACE = findMenuItem('support', 'notices')

/** 공지사항 (CR-133, T3-3 「공지사항」, 화면 정본 notice). 공지를 내려 주는 기능이 없어 화면 정의 파일의 목록을 보인다 */
export default function NoticesPage() {
  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE
  const t = NOTICE_TEXT

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        <table className="tbl ntbl">
          <thead><tr><th className="no">{t.head.no}</th><th>{t.head.title}</th><th className="dt">{t.head.date}</th></tr></thead>
          {NOTICES.length > 0 && (
            <tbody>
              {NOTICES.map((n) => (
                <tr key={n.id}><td className="no">{n.id}</td><td className="xtx">{n.title}</td><td className="dt">{n.date}</td></tr>
              ))}
            </tbody>
          )}
        </table>
        {NOTICES.length === 0 && (
          <div className="empty2">
            <div className="icn"><Icon name="bell" /></div>
            <b>{t.emptyTitle}</b>
            <span>{t.emptyDesc}</span>
          </div>
        )}
      </section>
    </SubPageFrame>
  )
}
