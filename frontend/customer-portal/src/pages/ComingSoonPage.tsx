import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { findPendingItem } from '../components/layout/menu'
import { SubIcon } from '../components/subpage/SubIcon'
import { ApplyBand, Lines, SubPageFrame } from '../components/subpage/SubPageFrame'
import { TIER_LABEL } from '../data/homeContent'
import { COMING_SOON_GENERIC, PENDING_TEXT, TIER_TAG_CLASS, findPending } from '../data/subpageContent'

/**
 * 항목별 준비 안내 (CR-133, T3-3 「항목별 준비 안내」, 화면 정본 soon).
 * 화면이 아직 없는 메뉴 항목마다 무엇을 해 줄지와 지금 대신 쓸 화면을 보인다. 여는 날짜는 약속하지 않는다.
 * 모르는 키와 키 없는 주소는 일반 준비 중 안내를 틀 없이 보인다(CR-127).
 */
export default function ComingSoonPage() {
  const { key } = useParams()
  const [params] = useSearchParams()
  const pending = key ? findPending(key) : undefined
  const found = key ? findPendingItem(key) : undefined

  if (!pending || !found) return <GenericComingSoon name={found?.item.label ?? params.get('name')} />

  const { group, item } = found
  return (
    <SubPageFrame group={group} item={item}>
      <section className="cbx hero soon">
        <div className="tx">
          <div className="tg">
            {pending.tier && <span className={TIER_TAG_CLASS[pending.tier]}>{TIER_LABEL[pending.tier]}</span>}
            <span>{group.label}</span>
          </div>
          <h1 className="h1">{item.label}</h1>
          <p className="ld"><Lines lines={pending.desc} /></p>
          <span className="st"><SubIcon name="hour" />{PENDING_TEXT.status}</span>
        </div>
        <div className="im"><img src={pending.photo} alt="" /></div>
      </section>

      <section className="cbx pad">
        <h2 className="h2">{PENDING_TEXT.altTitle}</h2>
        <div className="alt3">
          {pending.alts.map((a) => (
            <Link key={a.name} to={a.to}>
              <span className="icn"><SubIcon name={a.icon} /></span>
              <div><b>{a.name}</b><span>{a.desc}</span></div>
            </Link>
          ))}
        </div>
      </section>

      <ApplyBand
        icon="headset"
        title={PENDING_TEXT.askTitle}
        desc={PENDING_TEXT.askDesc}
        action={{ label: PENDING_TEXT.askAction, to: '/support' }}
      />
    </SubPageFrame>
  )
}

function GenericComingSoon({ name }: { name: string | null }) {
  const t = COMING_SOON_GENERIC
  return (
    <div className="bg-white border border-line rounded-[14px] px-10 py-16 text-center">
      <span className="inline-flex items-center h-7 px-3 rounded bg-brand-deep text-white text-[13px] font-semibold">{t.tag}</span>
      <h1 className="mt-4 text-[28px] text-ink">{name ? t.titleWith(name) : t.title}</h1>
      <p className="mt-2 text-[16px] text-ink-sub">{t.desc}</p>
      <div className="mt-8 flex justify-center gap-2.5">
        <Button asChild variant="outline" size="lg"><Link to="/">{t.actions.home}</Link></Button>
        <Button asChild variant="outline" size="lg"><Link to="/search">{t.actions.open}</Link></Button>
        <Button asChild variant="primary" size="lg" className="bg-brand hover:bg-brand-strong"><Link to="/support">{t.actions.inquiry}</Link></Button>
      </div>
    </div>
  )
}
