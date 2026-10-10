import { Check } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge, Button, Table, TableBody, TableRow, TableCell } from '@therecommerce-develop/ds-ui'
import { useAuth } from '../context/AuthContext'

/**
 * CR-126 — 요금제 (무료 / Pro / 컨설팅 3플랜)
 * 메트로닉 plans 디자인(카드 헤더 + 기능 비교 표) 차용.
 *
 * 핵심 결정(메모 cr126-pricing-feature-lineup):
 *  - AI 제안서 자동작성/편집기 = 요금제 전면 제외(미군 제안서 최종 책임=고객, 면책 충돌).
 *  - 컨설팅 = 정액 아닌 "문의하기"(/support). 성공보수형 과금은 법무 검토 후 출시 → 표기 최소.
 *  - 표현은 "대행"❌ "컨설팅"✅.
 *  - 컨설팅 항목(SAM 등록 등)·수수료율 실값 = 운영(사업) 정보 미정 → placeholder.
 */

interface PlanInfo {
  key: 'free' | 'pro' | 'consulting'
  title: string
  description: string
  price: string
  period: string
  cta: string
  ctaTo?: string
  highlight?: boolean
  current?: boolean
}

interface FeatureRow {
  title: string
  free: string | boolean
  pro: string | boolean
  consulting: string | boolean
}

const PLANS: PlanInfo[] = [
  {
    key: 'free',
    title: '무료',
    description: '입찰을 시작하는 개인·소규모 기업',
    price: '₩0',
    period: '',
    cta: '무료로 시작',
    current: true,
  },
  {
    key: 'pro',
    title: 'Pro',
    description: '본격적으로 미군 입찰을 준비하는 기업',
    price: '₩20,000',
    period: '/ 월',
    cta: '업그레이드',
    highlight: true,
  },
  {
    key: 'consulting',
    title: '컨설팅',
    description: '등록·인증·입찰까지 전문가 자문이 필요한 기업',
    price: '별도 문의',
    period: '',
    cta: '문의하기',
    ctaTo: '/support',
  },
]

const FEATURES: FeatureRow[] = [
  { title: 'SAM.gov 공고 검색', free: true, pro: true, consulting: true },
  { title: '공고 한글 번역', free: '월 5건', pro: '무제한', consulting: '무제한' },
  { title: '관심 공고 저장', free: true, pro: true, consulting: true },
  { title: '마감 임박 알림', free: false, pro: true, consulting: true },
  { title: 'AI 적격성 분석', free: '월 1건', pro: '무제한', consulting: '무제한' },
  { title: '공고 분석 리포트 (요약·필수서류·자격요건)', free: false, pro: true, consulting: true },
  { title: '첨부 다운로드', free: false, pro: true, consulting: true },
  { title: '제출 규격 자동 검증', free: false, pro: true, consulting: true },
  { title: '공통 서류함', free: '기본', pro: '전체', consulting: '전체' },
  { title: '전문가 컨설팅 (등록·인증·입찰)', free: false, pro: false, consulting: true },
  { title: '고객 지원', free: '이메일', pro: '우선 지원', consulting: '전담 매니저' },
]

// 컨설팅 항목 — 실제 제공 가능 항목·세부는 운영 정보 미정(placeholder). 법무 검토 후 확정.
const CONSULTING_ITEMS: { title: string; desc: string }[] = [
  { title: 'SAM.gov 등록 지원', desc: '사업자 등록부터 UEI·CAGE 발급까지 절차 자문' },
  { title: 'PIEE 등록 지원', desc: 'PIEE 계정·역할 신청 및 연동 자문' },
  { title: '입찰서 작성 컨설팅', desc: '공고 요건 분석·제안서 구성 검토·보조 (최종 제출·서명은 고객)' },
  { title: '인증 취득 자문', desc: 'CMMC 등 미군 조달 관련 인증 취득 절차 안내' },
  { title: '계약 관리 지원', desc: '낙찰 이후 인보이스·납품·청구(WAWF) 절차 자문' },
]

function featureCell(v: string | boolean) {
  if (typeof v === 'boolean') {
    return v ? <Check className="inline text-emerald-500 w-4 h-4" /> : <span className="text-slate-300">—</span>
  }
  return <span className="text-sm text-slate-700">{v}</span>
}

export default function PricingPage() {
  const { isAuthenticated } = useAuth()
  return (
    <div className="max-w-5xl mx-auto">
      {/* 헤더 */}
      <div className="text-center mb-10">
        <h1 className="text-2xl font-bold text-[#0c1e3d]">요금 안내</h1>
        <p className="mt-2 text-slate-500">무료로 시작하고, 필요에 맞춰 Pro 또는 전문가 컨설팅을 선택하세요. 약정 없이 언제든 해지 가능합니다.</p>
      </div>

      {/* 플랜 비교 표 (메트로닉 plans 디자인) — 패널로 감싸 배경과 대비 확보 */}
      <div className="rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 p-4 sm:p-6 overflow-x-auto pt-8 sm:pt-10">
        <Table className="table-fixed border-separate border-spacing-0 min-w-[760px] w-full">
          <TableBody>
            {/* 플랜 헤더 행 */}
            <TableRow className="hover:bg-transparent">
              <TableCell className="align-bottom p-5 w-[34%] border-t border-l border-slate-200 rounded-tl-xl" />
              {PLANS.map((p) => (
                <TableCell
                  key={p.key}
                  className={
                    'relative align-bottom px-5 pb-5 pt-8 border-t border-l border-r rounded-t-xl ' +
                    (p.highlight ? 'bg-[#eef3fb] border-[#c7d6ef]' : 'bg-slate-50 border-slate-200')
                  }
                >
                  {p.current && isAuthenticated && (
                    <Badge variant="success" appearance="light"
                      className="absolute top-2.5 left-1/2 -translate-x-1/2">현재 플랜</Badge>
                  )}
                  {p.highlight && (
                    <Badge variant="primary"
                      className="absolute top-2.5 left-1/2 -translate-x-1/2 bg-[#1b3a72] text-white">추천</Badge>
                  )}
                  <h3 className="text-lg font-semibold text-[#0c1e3d] pb-1">{p.title}</h3>
                  <p className="text-sm text-slate-500 min-h-[40px]">{p.description}</p>
                  <div className="py-4 flex items-end gap-1.5">
                    <span className="text-2xl font-semibold text-[#0c1e3d] leading-none">{p.price}</span>
                    {p.period && <span className="text-slate-400 text-xs">{p.period}</span>}
                  </div>
                  {p.ctaTo ? (
                    <Button asChild variant={p.highlight ? 'primary' : 'outline'} className="w-full justify-center">
                      <Link to={p.ctaTo}>{p.cta}</Link>
                    </Button>
                  ) : (
                    <Button variant={p.highlight ? 'primary' : 'outline'} className="w-full justify-center">
                      {p.cta}
                    </Button>
                  )}
                </TableCell>
              ))}
            </TableRow>

            {/* 기능 비교 행 */}
            {FEATURES.map((f, i) => (
              <TableRow key={i} className="hover:bg-transparent">
                <TableCell className="border-l border-b border-slate-200 px-5 py-3.5">
                  <span className="text-sm font-medium text-[#0c1e3d]">{f.title}</span>
                </TableCell>
                <TableCell className="border-l border-b border-slate-200 px-5 py-3.5 text-center">
                  {featureCell(f.free)}
                </TableCell>
                <TableCell className={'border-l border-b border-slate-200 px-5 py-3.5 text-center ' + 'bg-[#eef3fb]/40'}>
                  {featureCell(f.pro)}
                </TableCell>
                <TableCell className="border-l border-r border-b border-slate-200 px-5 py-3.5 text-center">
                  {featureCell(f.consulting)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* 컨설팅 서비스 항목 (보조 영역) — 항목·요율 실값은 운영 정보 미정(placeholder) */}
      <div className="mt-14">
        <div className="text-center mb-6">
          <h2 className="text-xl font-bold text-[#0c1e3d]">전문가 컨설팅 서비스</h2>
          <p className="mt-2 text-slate-500">
            등록·인증·입찰 준비를 전문가가 함께합니다. 공고와 상황에 따라 범위·비용이 달라 별도 상담으로 안내드립니다.
          </p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {CONSULTING_ITEMS.map((item, i) => (
            <div key={i} className="rounded-xl border border-slate-200 bg-white p-5">
              <h3 className="text-sm font-semibold text-[#0c1e3d]">{item.title}</h3>
              <p className="mt-1.5 text-sm text-slate-500 leading-relaxed">{item.desc}</p>
            </div>
          ))}
          <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-5 flex flex-col justify-center items-start">
            <p className="text-sm text-slate-600">필요한 컨설팅이 목록에 없나요?</p>
            <Button asChild variant="outline" className="mt-3">
              <Link to="/support">상담 문의하기</Link>
            </Button>
          </div>
        </div>
      </div>

      <p className="text-xs text-slate-400 mt-10 text-center">
        모든 금액은 부가세 별도입니다. Pro는 월 단위 결제이며 약정 없이 해지할 수 있습니다.
        컨설팅 비용은 상담 후 별도 견적으로 안내됩니다.
      </p>
    </div>
  )
}
