import { Check } from 'lucide-react'
import { Badge, Button, Table, TableBody, TableRow, TableCell } from '@therecommerce-develop/ds-ui'

/**
 * CR-124 — 요금제 (무료 + 월 2만원 2플랜)
 * 메트로닉 plans 디자인(카드 헤더 + 기능 비교 표) 차용. 우리는 2플랜·월 구독.
 */

interface PlanInfo {
  key: 'free' | 'pro'
  title: string
  description: string
  price: string
  period: string
  cta: string
  highlight?: boolean
  current?: boolean
}

interface FeatureRow {
  title: string
  free: string | boolean
  pro: string | boolean
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
]

const FEATURES: FeatureRow[] = [
  { title: 'SAM.gov 공고 검색', free: true, pro: true },
  { title: '공고 한글 번역', free: '월 5건', pro: '무제한' },
  { title: '관심 공고 저장', free: true, pro: true },
  { title: '마감 임박 알림', free: false, pro: true },
  { title: 'AI 적격성 분석', free: '월 1건', pro: '무제한' },
  { title: 'AI 제안서 작성', free: false, pro: true },
  { title: '제안서 편집기', free: false, pro: true },
  { title: '제출 규격 자동 검증', free: false, pro: true },
  { title: '서류함 (제출 슬롯)', free: '기본', pro: '전체' },
  { title: '고객 지원', free: '이메일', pro: '우선 지원' },
]

function featureCell(v: string | boolean) {
  if (typeof v === 'boolean') {
    return v ? <Check className="inline text-emerald-500 w-4 h-4" /> : <span className="text-slate-300">—</span>
  }
  return <span className="text-sm text-slate-700">{v}</span>
}

export default function PricingPage() {
  return (
    <div className="max-w-4xl mx-auto">
      {/* 헤더 */}
      <div className="text-center mb-10">
        <h1 className="text-2xl font-bold text-[#0c1e3d]">요금 안내</h1>
        <p className="mt-2 text-slate-500">무료로 시작하고, 필요할 때 Pro로 업그레이드하세요. 약정 없이 언제든 해지 가능합니다.</p>
      </div>

      {/* 플랜 비교 표 (메트로닉 plans 디자인) — 뱃지가 카드 위로 떠서 상단 여백 확보 */}
      <div className="overflow-x-auto pt-4">
        <Table className="table-fixed border-separate border-spacing-0 min-w-[640px] w-full">
          <TableBody>
            {/* 플랜 헤더 행 */}
            <TableRow className="hover:bg-transparent">
              <TableCell className="align-bottom p-5 w-[40%]" />
              {PLANS.map((p) => (
                <TableCell
                  key={p.key}
                  className={
                    'relative align-bottom px-5 pb-5 pt-8 border-t border-l border-slate-200 ' +
                    (p.highlight ? 'bg-[#eef3fb] rounded-t-xl border-[#1b3a72]' : 'rounded-t-xl') +
                    (p.key === 'pro' ? ' border-r' : '')
                  }
                >
                  {p.current && (
                    <Badge variant="success" appearance="light"
                      className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2">현재 플랜</Badge>
                  )}
                  {p.highlight && (
                    <Badge variant="primary"
                      className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-[#1b3a72]">추천</Badge>
                  )}
                  <h3 className="text-lg font-semibold text-[#0c1e3d] pb-1">{p.title}</h3>
                  <p className="text-sm text-slate-500 min-h-[40px]">{p.description}</p>
                  <div className="py-4 flex items-end gap-1.5">
                    <span className="text-2xl font-semibold text-[#0c1e3d] leading-none">{p.price}</span>
                    {p.period && <span className="text-slate-400 text-xs">{p.period}</span>}
                  </div>
                  <Button variant={p.highlight ? 'primary' : 'outline'} className="w-full justify-center">
                    {p.cta}
                  </Button>
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
                <TableCell className={'border-l border-r border-b border-slate-200 px-5 py-3.5 text-center ' + 'bg-[#eef3fb]/40'}>
                  {featureCell(f.pro)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <p className="text-xs text-slate-400 mt-6 text-center">
        모든 금액은 부가세 별도입니다. Pro는 월 단위 결제이며 약정 없이 해지할 수 있습니다.
      </p>
    </div>
  )
}
