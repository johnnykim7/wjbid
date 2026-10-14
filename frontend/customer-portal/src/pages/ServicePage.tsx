import type { CSSProperties } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { Icon } from '../components/layout/Icon'
import { SitePhoto } from '../components/SitePhoto'
import { comingSoonHref, findMenuItem, menuHref } from '../components/layout/menu'
import { COMPANY } from '../components/layout/siteContent'
import { SubIcon } from '../components/subpage/SubIcon'
import { ApplyBand, Lines, SubPageFrame } from '../components/subpage/SubPageFrame'
import { TIER_LABEL } from '../data/homeContent'
import { SERVICE_TEXT, TIER_TAG_CLASS, consultTitle, findService, inquiryHref } from '../data/subpageContent'

const GROUP_KEY = 'services'

/**
 * 등록·컨설팅 서비스 안내 (CR-133, T3-3 「등록·컨설팅 서비스 안내」, 화면 정본 sam). 서비스 7개가 이 한 틀에 내용만 바꾼다.
 * 발주사가 설명을 간략하게 해 달라고 해 진행 순서는 단계 이름만, 비용은 한 줄만 둔다(CR-162, 원문 7장)
 */
export default function ServicePage() {
  const { key = '' } = useParams()
  const service = findService(key)
  const found = findMenuItem(GROUP_KEY, key)
  if (!service || !found) return <Navigate to={comingSoonHref(key)} replace />

  const { group, item } = found
  const apply = inquiryHref(service.inquiryType, consultTitle(item.label))
  const related = service.related.flatMap((k) => {
    const info = findService(k)
    const menu = findMenuItem(GROUP_KEY, k)
    return info && menu ? [{ info, rel: menu.item }] : []
  })

  return (
    <SubPageFrame group={group} item={item}>
      <section className="cbx hero">
        <div className="tx">
          <div className="tg">
            <span className={TIER_TAG_CLASS[service.tier]}>{TIER_LABEL[service.tier]}</span>
            <span>{group.label}</span>
          </div>
          <h1 className="h1">{item.label}</h1>
          <p className="ld"><Lines lines={service.lead} /></p>
          <div className="acts">
            <Link className="bigb" to={apply}>{SERVICE_TEXT.primary}</Link>
            <a className="bigb o" href={`mailto:${COMPANY.email}`}>{COMPANY.email}</a>
          </div>
        </div>
        <div className="im"><SitePhoto slot={service.photo} /></div>
      </section>

      <section className="cbx pad">
        <h2 className="h2">{SERVICE_TEXT.whoTitle}</h2>
        <div className="wh">
          {service.who.map((w) => (
            <div key={w.lines.join()}>
              <SubIcon name={w.icon} />
              <b><Lines lines={w.lines} /></b>
            </div>
          ))}
        </div>
        <h2 className="h2 sech">{SERVICE_TEXT.stepsTitle}</h2>
        <ol className="stp" style={{ '--n': service.steps.length } as CSSProperties}>
          {service.steps.map((name, i) => (
            <li key={name}><i>{i + 1}</i><b>{name}</b></li>
          ))}
        </ol>
      </section>

      <section className="cbx pad cost">
        <h2 className="h2">{SERVICE_TEXT.costTitle}</h2>
        <b className="big">{SERVICE_TEXT.costBig}</b>
        <p>{SERVICE_TEXT.costDesc}</p>
        <Link className="lnk" to="/pricing">{SERVICE_TEXT.pricingLink}<Icon name="cr" /></Link>
      </section>

      <ApplyBand
        icon="headset"
        title={consultTitle(item.label)}
        desc={<>{SERVICE_TEXT.applyDesc} <a href={`mailto:${COMPANY.email}`}>{COMPANY.email}</a></>}
        action={{ label: SERVICE_TEXT.primary, to: apply }}
      />

      <section className="sech">
        <h2 className="h2">{SERVICE_TEXT.relatedTitle}</h2>
        <div className="rel3">
          {related.map(({ info, rel }) => (
            <Link key={info.key} to={menuHref(rel)}>
              <small>{TIER_LABEL[info.tier]}</small>
              <b>{rel.label}</b>
              <span>{info.summary}</span>
              <em>{SERVICE_TEXT.relatedMore}<Icon name="cr" /></em>
            </Link>
          ))}
        </div>
      </section>
    </SubPageFrame>
  )
}
