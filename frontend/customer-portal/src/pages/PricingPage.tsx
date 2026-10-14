import { Link, Navigate } from 'react-router-dom'
import { Icon } from '../components/layout/Icon'
import { comingSoonHref, findMenuItem, serviceHref } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { useAuth } from '../context/AuthContext'
import { MEMBER_PRICE_PENDING_LABEL, formatWon, usePricing } from '../hooks/usePricing'
import { SUB_FRAME } from '../data/subpageContent'

/**
 * 요금 안내 (CR-129, T3-3)
 * 월 구독은 무료와 회원 두 단계, 대행·컨설팅은 건별 견적으로 따로 안내한다.
 * 회원 월 금액은 요금 조회가 설정값에서 내주므로 화면에 쓰지 않는다(POL-010).
 * 단계 이름, 대상, 포함 항목은 홈 요금 구역(CR-127)과 같은 말을 쓴다.
 * 고객센터 속페이지 틀 안에 두고, 대행 항목은 각 서비스 안내로 보낸다(CR-133, 화면 정본 pricing).
 */

const PLACE = findMenuItem('support', 'pricing')

interface PlanInfo {
  key: 'free' | 'member'
  title: string
  audience: string
  cta: string
  ctaTo: string
  highlight?: string
}

interface FeatureRow {
  title: string
  free: boolean
  member: boolean
}

const HEAD = {
  title: '요금 안내',
  desc: '무료로 시작하고, 필요할 때 회원으로 넓히세요. 회원은 월 단위, 대행과 컨설팅은 건마다 상담합니다',
}

// 결제 연동 전이라 회원 신청은 1:1 문의로 받는다
const PLANS: PlanInfo[] = [
  { key: 'free', title: '무료', audience: '처음 둘러보는 회사', cta: '무료로 시작', ctaTo: '/register' },
  { key: 'member', title: '회원', audience: '입찰을 꾸준히 하는 회사', cta: '회원으로 시작', ctaTo: '/support', highlight: '추천' },
]
const FREE_PRICE = '0원'
const CURRENT_PLAN = '현재 플랜'
const PER_MONTH = '/ 월'

const COMPARE = { title: '무엇이 다른가요', head: ['서비스', '무료', '회원'] }

const FEATURES: FeatureRow[] = [
  { title: 'SAM.gov 공고 검색', free: true, member: true },
  { title: '공고 한글로 보기 (SAM 번역 수준)', free: true, member: true },
  { title: '관심 공고 저장', free: true, member: true },
  { title: '서류함, 제출 전 검사', free: true, member: true },
  { title: '입찰 교육 자료', free: true, member: true },
  { title: '새 공고 알림', free: false, member: true },
  { title: '공고문 상세 번역 (첨부 포함)', free: false, member: true },
  { title: '적격성 진단 (우리 회사 맞춤 판정)', free: false, member: true },
  { title: '입찰 신청', free: false, member: true },
]

const SERVICES_HEAD = { title: '대행·컨설팅', note: '건마다 상담 뒤 견적을 드립니다' }

const SERVICE_ITEMS: { title: string; desc: string; to: string; pending?: boolean }[] = [
  { title: 'SAM.gov·PIEE 등록 대행', desc: '처음 등록하는 회사도 순서대로', to: serviceHref('sam-registration') },
  { title: '제안서 자문·검토', desc: '고객이 쓴 제안서를 전문가가 검토하고 보완점 안내', to: comingSoonHref('proposal-review'), pending: true },
  { title: '현장 설명회 동행, 기지 출입 지원', desc: '출입 절차부터 통역까지', to: serviceHref('site-visit') },
  { title: '대금 청구 대행', desc: '계약 서류 정리와 PIEE 대금 청구', to: serviceHref('invoicing') },
  { title: '서류 영문 번역', desc: '등기부등본, 재무제표, 실적 증명을 영문으로', to: serviceHref('translation') },
]

const FOOTNOTE = '금액은 부가세 별도입니다. 회원은 월 단위로 결제하며 약정 없이 해지할 수 있습니다. 대행·컨설팅은 상담 후 견적으로 안내합니다.'

function featureCell(included: boolean) {
  return included ? <Icon name="ok" /> : <span className="nn">-</span>
}

function MemberPrice() {
  const { memberMonthlyPrice, loading } = usePricing()
  if (loading) {
    return <div className="prc2"><span className="inline-block h-9 w-36 rounded bg-line-soft animate-pulse align-middle" /></div>
  }
  if (memberMonthlyPrice === null) {
    return <div className="prc2 pend">{MEMBER_PRICE_PENDING_LABEL}</div>
  }
  return <div className="prc2">{formatWon(memberMonthlyPrice)}<small>{PER_MONTH}</small></div>
}

export default function PricingPage() {
  const { isAuthenticated, membership } = useAuth()
  // 로그인한 고객은 회원 구분에 맞는 단계에 지금 단계를 단다(CR-157)
  const currentPlan = !isAuthenticated || membership === 'loading' ? null : membership === 'member' ? 'member' : 'free'
  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={HEAD.title} desc={HEAD.desc} />
      <div className="pls">
        {PLANS.map((p) => {
          const isCurrent = p.key === currentPlan
          return (
            <div key={p.key} className={p.highlight ? 'plan rcm' : 'plan'}>
              <div className="nm">{p.title}{p.highlight && <span>{p.highlight}</span>}</div>
              <div className="au2">{p.audience}</div>
              {p.key === 'free' ? <div className="prc2">{FREE_PRICE}</div> : <MemberPrice />}
              {isCurrent
                ? <span className="cur">{CURRENT_PLAN}</span>
                : <Link className={p.highlight ? 'bigb' : 'bigb o'} to={p.ctaTo}>{p.cta}</Link>}
            </div>
          )
        })}
      </div>

      <section className="cbx pad">
        <h2 className="h2">{COMPARE.title}</h2>
        <table className="cmpt">
          <thead><tr>{COMPARE.head.map((h) => <th key={h}>{h}</th>)}</tr></thead>
          <tbody>
            {FEATURES.map((f) => (
              <tr key={f.title}>
                <td>{f.title}</td>
                <td>{featureCell(f.free)}</td>
                <td className="mm">{featureCell(f.member)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="cbx pad">
        <h2 className="h2">{SERVICES_HEAD.title} <small>{SERVICES_HEAD.note}</small></h2>
        <ul className="svc">
          {SERVICE_ITEMS.map((s) => (
            <li key={s.title}>
              <Link to={s.to}>
                <b>{s.title}{s.pending && <small>{SUB_FRAME.pendingTag}</small>}</b>
                <span>{s.desc}</span>
              </Link>
            </li>
          ))}
        </ul>
        <p className="prnote">{FOOTNOTE}</p>
      </section>
    </SubPageFrame>
  )
}
