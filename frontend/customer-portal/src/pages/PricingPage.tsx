import { Check, Minus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge, Button, Table, TableBody, TableRow, TableCell } from '@therecommerce-develop/ds-ui'
import { useAuth } from '../context/AuthContext'
import { MEMBER_PRICE_PENDING_LABEL, formatWon, usePricing } from '../hooks/usePricing'

/**
 * 요금 안내 (CR-129, T3-3)
 * 월 구독은 무료와 회원 두 단계, 대행·컨설팅은 건별 견적으로 따로 안내한다.
 * 회원 월 금액은 요금 조회가 설정값에서 내주므로 화면에 쓰지 않는다(POL-010).
 * 단계 이름, 대상, 포함 항목은 홈 요금 구역(CR-127)과 같은 말을 쓴다.
 */

interface PlanInfo {
  key: 'free' | 'member'
  title: string
  audience: string
  cta: string
  ctaTo: string
  highlight?: boolean
}

interface FeatureRow {
  title: string
  free: boolean
  member: boolean
}

// 결제 연동 전이라 회원 신청은 1:1 문의로 받는다
const PLANS: PlanInfo[] = [
  { key: 'free', title: '무료', audience: '처음 둘러보는 회사', cta: '무료로 시작', ctaTo: '/register' },
  { key: 'member', title: '회원', audience: '입찰을 꾸준히 하는 회사', cta: '회원으로 시작', ctaTo: '/support', highlight: true },
]

const FEATURES: FeatureRow[] = [
  { title: 'SAM.gov 공고 검색', free: true, member: true },
  { title: '공고 한글로 보기 (SAM 번역 수준)', free: true, member: true },
  { title: '관심 공고 저장', free: true, member: true },
  { title: '입찰 신청', free: true, member: true },
  { title: '입찰 교육 자료, 무료 입찰 교실', free: true, member: true },
  { title: '공고문 상세 번역 (첨부 포함)', free: false, member: true },
  { title: '적격성 진단 (우리 회사 맞춤 판정)', free: false, member: true },
  { title: '재발주 예측 달력', free: false, member: true },
  { title: '새 공고 알림', free: false, member: true },
  { title: '서류함, 제출 전 검사', free: false, member: true },
]

const SERVICE_ITEMS: { title: string; desc: string }[] = [
  { title: 'SAM.gov·PIEE 등록 대행', desc: '처음 등록하는 회사도 순서대로. UEI·CAGE 발급부터 PIEE 계정까지' },
  { title: '제안서 자문·검토', desc: '공고 요건 분석과 제안서 구성 검토. 최종 제출과 서명은 고객이 합니다' },
  { title: '현장 설명회 동행, 기지 출입 지원', desc: '출입 절차부터 통역까지' },
  { title: '대금 청구 대행', desc: '낙찰 이후 인보이스와 청구(WAWF) 절차' },
  { title: '서류 영문 번역', desc: '제출 서류와 회사 소개 자료의 영문 번역' },
]

function featureCell(included: boolean) {
  return included
    ? <Check className="inline text-brand w-4 h-4" />
    : <Minus className="inline text-ink-faint w-4 h-4" />
}

function MemberPrice() {
  const { memberMonthlyPrice, loading } = usePricing()
  if (loading) {
    return <span className="inline-block h-6 w-28 rounded bg-line-soft animate-pulse" />
  }
  if (memberMonthlyPrice === null) {
    return <span className="text-lg font-semibold text-ink-sub leading-none">{MEMBER_PRICE_PENDING_LABEL}</span>
  }
  return (
    <>
      <span className="text-2xl font-semibold text-ink leading-none">{formatWon(memberMonthlyPrice)}</span>
      <span className="text-ink-mute text-xs">/ 월</span>
    </>
  )
}

export default function PricingPage() {
  const { isAuthenticated } = useAuth()
  return (
    <div className="max-w-4xl mx-auto">
      {/* 머리 */}
      <div className="text-center mb-10">
        <h1 className="text-2xl font-bold text-ink">요금 안내</h1>
        <p className="mt-2 text-ink-mute">
          무료로 시작하고, 필요할 때 회원으로 넓히세요. 회원은 월 단위, 대행과 컨설팅은 건마다 상담합니다.
        </p>
      </div>

      {/* 월 구독 비교 표 */}
      <div className="rounded-2xl bg-white shadow-sm ring-1 ring-line p-4 sm:p-6 overflow-x-auto pt-8 sm:pt-10">
        <Table className="table-fixed border-separate border-spacing-0 min-w-[600px] w-full">
          <TableBody>
            <TableRow className="hover:bg-transparent">
              <TableCell className="align-bottom p-5 w-[40%] border-t border-l border-line rounded-tl-xl" />
              {PLANS.map((p) => {
                const isCurrent = p.key === 'free' && isAuthenticated
                return (
                  <TableCell
                    key={p.key}
                    className={
                      'relative align-bottom px-5 pb-5 pt-8 border-t border-l border-r rounded-t-xl ' +
                      (p.highlight ? 'bg-brand-soft border-brand-line' : 'bg-surface border-line')
                    }
                  >
                    {isCurrent && (
                      <Badge variant="success" appearance="light"
                        className="absolute top-2.5 left-1/2 -translate-x-1/2">현재 플랜</Badge>
                    )}
                    {p.highlight && (
                      <Badge variant="primary"
                        className="absolute top-2.5 left-1/2 -translate-x-1/2 bg-brand-deep text-white">추천</Badge>
                    )}
                    <h3 className="text-lg font-semibold text-ink pb-1">{p.title}</h3>
                    <p className="text-sm text-ink-mute min-h-[20px]">{p.audience}</p>
                    <div className="py-4 flex items-end gap-1.5 min-h-[56px]">
                      {p.key === 'free'
                        ? <span className="text-2xl font-semibold text-ink leading-none">0원</span>
                        : <MemberPrice />}
                    </div>
                    {!isCurrent && (
                      <Button asChild variant={p.highlight ? 'primary' : 'outline'} className="w-full justify-center">
                        <Link to={p.ctaTo}>{p.cta}</Link>
                      </Button>
                    )}
                  </TableCell>
                )
              })}
            </TableRow>

            {FEATURES.map((f) => (
              <TableRow key={f.title} className="hover:bg-transparent">
                <TableCell className="border-l border-b border-line px-5 py-3.5">
                  <span className="text-sm font-medium text-ink">{f.title}</span>
                </TableCell>
                <TableCell className="border-l border-b border-line px-5 py-3.5 text-center">
                  {featureCell(f.free)}
                </TableCell>
                <TableCell className="border-l border-r border-b border-line px-5 py-3.5 text-center bg-brand-softer">
                  {featureCell(f.member)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* 대행·컨설팅 (건별 견적) */}
      <div className="mt-14">
        <div className="text-center mb-6">
          <h2 className="text-xl font-bold text-ink">대행·컨설팅</h2>
          <p className="mt-2 text-ink-mute">
            맡기고 싶은 일이 있는 회사. 일마다 범위와 비용이 달라 상담 후 견적으로 안내합니다.
          </p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {SERVICE_ITEMS.map((item) => (
            <div key={item.title} className="rounded-xl border border-line bg-white p-5">
              <h3 className="text-sm font-semibold text-ink">{item.title}</h3>
              <p className="mt-1.5 text-sm text-ink-mute leading-relaxed">{item.desc}</p>
            </div>
          ))}
          <div className="rounded-xl border border-dashed border-line-strong bg-surface p-5 flex flex-col justify-center items-start">
            <p className="text-sm text-ink-sub">맡기고 싶은 일을 알려 주세요.</p>
            <Button asChild variant="outline" className="mt-3">
              <Link to="/support">상담 신청</Link>
            </Button>
          </div>
        </div>
      </div>

      <p className="text-xs text-ink-mute mt-10 text-center">
        금액은 부가세 별도입니다. 회원은 월 단위로 결제하며 약정 없이 해지할 수 있습니다.
        대행·컨설팅은 상담 후 견적으로 안내합니다.
      </p>
    </div>
  )
}
