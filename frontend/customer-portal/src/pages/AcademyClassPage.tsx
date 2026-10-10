import { Link, Navigate } from 'react-router-dom'
import { findMenuItem } from '../components/layout/menu'
import { ApplyBand, Lines, SubPageFrame } from '../components/subpage/SubPageFrame'
import { CLASS_INFO } from '../data/academyContent'
import { TIER_LABEL } from '../data/homeContent'
import { ACADEMY_PHOTO, INQUIRY_TYPE, TIER_TAG_CLASS, inquiryHref } from '../data/subpageContent'

const PLACE = findMenuItem('academy', 'class')

/** 무료 입찰 교실 (CR-133, T3-3 「무료 입찰 교실」, 화면 정본 class). 일정은 정해진 것만 보이고 참가 신청은 1:1 문의로 받는다 */
export default function AcademyClassPage() {
  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE
  const apply = inquiryHref(INQUIRY_TYPE.consulting, CLASS_INFO.inquiryTitle)
  const { facts, schedule } = CLASS_INFO

  return (
    <SubPageFrame group={group} item={item}>
      <section className="cbx hero">
        <div className="tx">
          <div className="tg">
            <span className={TIER_TAG_CLASS.f}>{TIER_LABEL.f}</span>
            <span>{group.label}</span>
          </div>
          <h1 className="h1">{item.label}</h1>
          <p className="ld"><Lines lines={CLASS_INFO.lead} /></p>
          <div className="acts">
            <Link className="bigb" to={apply}>{CLASS_INFO.primary}</Link>
            <Link className="bigb o" to={CLASS_INFO.secondary.to}>{CLASS_INFO.secondary.label}</Link>
          </div>
        </div>
        <div className="im"><img src={ACADEMY_PHOTO} alt="" /></div>
      </section>

      <section className="cbx pad">
        <h2 className="h2">{CLASS_INFO.factsTitle}</h2>
        <div className="facts">
          <div><small>{facts.method.label}</small><b>{facts.method.value}</b></div>
          <div><small>{facts.cost.label}</small><b>{facts.cost.value}</b></div>
          <div><small>{schedule.label}</small><b>{schedule.value ?? schedule.pending}</b></div>
        </div>
        <h2 className="h2 sech">{CLASS_INFO.lessonsTitle}</h2>
        <ul className="lesson">
          {CLASS_INFO.lessons.map((l, i) => (
            <li key={l.title}><i>{i + 1}</i><div><b>{l.title}</b><span>{l.desc}</span></div></li>
          ))}
        </ul>
      </section>

      <ApplyBand icon="cal" title={CLASS_INFO.inquiryTitle} desc={CLASS_INFO.applyDesc} action={{ label: CLASS_INFO.primary, to: apply }} />
    </SubPageFrame>
  )
}
