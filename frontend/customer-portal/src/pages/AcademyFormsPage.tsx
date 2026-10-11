import { Link, Navigate } from 'react-router-dom'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { BRING, FORMS, FORMS_TEXT } from '../data/academyContent'

const PLACE = findMenuItem('support', 'forms')

/** 서식 모음 (CR-133, T3-3 「서식 모음」, 화면 정본 forms). 서식 원본은 GSA 서식 자료실을 새 창으로 연다 */
export default function AcademyFormsPage() {
  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE
  const t = FORMS_TEXT

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        <h2 className="h2">{t.formsTitle} <small>{t.formsNote}</small></h2>
        <table className="tbl frm">
          <thead><tr>{t.head.map((h) => <th key={h}>{h}</th>)}</tr></thead>
          <tbody>
            {FORMS.map((f) => (
              <tr key={f.no}>
                <td><b className="mono">{f.no}</b></td>
                <td>{f.ko}<span className="eng">{f.en}</span></td>
                <td>{f.when}</td>
                <td><a href={t.sourceUrl} target="_blank" rel="noopener noreferrer">{t.sourceLabel}<Icon name="ext" /></a></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      <section className="cbx pad">
        <h2 className="h2">{t.bringTitle}</h2>
        <ul className="chk">
          {BRING.map((b) => (
            <li key={b.name}><Icon name="ok" /><div>{b.name}{b.desc && <span>{b.desc}</span>}</div></li>
          ))}
        </ul>
        <div className="xnote">
          <Icon name="alert" />
          <span>{t.bringNote} <Link to={t.bringLink.to}>{t.bringLink.label}</Link></span>
        </div>
      </section>
    </SubPageFrame>
  )
}
