import { useNavigate } from 'react-router-dom'
import { Accordion, AccordionItem, AccordionTrigger, AccordionContent } from '@therecommerce-develop/ds-ui'
import { Button } from '../components/ui/button'

const STEPS = [
  {
    no: 1,
    title: '공고 검색',
    desc: 'SAM.gov에서 자동 수집된 USFK 관련 공고를 키워드, NAICS 코드, 마감일 등으로 검색하세요.',
    icon: 'fa-solid fa-magnifying-glass',
    color: 'bg-brand-soft text-brand',
  },
  {
    no: 2,
    title: '제안서 작성 신청',
    desc: '관심 있는 공고를 선택하고 "제안서 작성" 버튼을 클릭하면 입찰 프로세스가 시작됩니다.',
    icon: 'fa-solid fa-wand-magic-sparkles',
    color: 'bg-brand-soft text-brand',
  },
  {
    no: 3,
    title: '필요 서류 제출',
    desc: '과거 수행 실적, 회사 프로필 등 필요한 서류를 업로드하세요. 제안서 작성에 활용됩니다.',
    icon: 'fa-solid fa-cloud-arrow-up',
    color: 'bg-brand-soft text-brand',
  },
  {
    no: 4,
    title: '요구사항 분석',
    desc: '공고 요구사항을 추출하고 자격 요건 충족 여부를 검토합니다.',
    icon: 'fa-solid fa-robot',
    color: 'bg-brand-soft text-brand',
  },
  {
    no: 5,
    title: '제안서 초안 작성',
    desc: 'Cover Letter, Technical Proposal, Past Performance 등 필요한 문서의 초안을 작성합니다.',
    icon: 'fa-solid fa-file-lines',
    color: 'bg-brand-soft text-brand',
  },
  {
    no: 6,
    title: '전문가 검토 & 제출',
    desc: '입찰 전문가가 최종 검토 후 SAM.gov에 제출합니다. 진행 상황은 실시간으로 확인 가능합니다.',
    icon: 'fa-solid fa-paper-plane',
    color: 'bg-brand-soft text-brand',
  },
]

const FAQS = [
  {
    q: '제안서의 품질은 어떤가요?',
    a: '초안은 SAM.gov 공고 요구사항을 정밀 분석하여 작성됩니다. 이후 입찰 전문가가 내용을 검토·보완하여 최종 품질을 보장합니다.',
  },
  {
    q: '어떤 서류를 준비해야 하나요?',
    a: '공고마다 다르지만, 일반적으로 회사 프로필, 과거 수행 실적(Past Performance), 관련 자격증/인증서가 필요합니다. 시스템이 필요 서류를 안내합니다.',
  },
  {
    q: '마감일이 임박한 공고도 신청 가능한가요?',
    a: '최소 D-3 이전 신청을 권장합니다. 요구사항 분석과 문서 작성에 약 1-2일, 전문가 검토에 1일이 소요됩니다.',
  },
  {
    q: '비용은 어떻게 되나요?',
    a: '무료와 회원 두 단계가 있고, 회원은 월 단위로 결제합니다. 등록 대행·제안서 자문 같은 대행·컨설팅은 상담 후 건별 견적으로 안내합니다. 자세한 내용은 요금 안내 화면에서 확인하세요.',
  },
]

export default function GuidePage() {
  const navigate = useNavigate()

  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-8 pt-2">
        <h1 className="text-2xl font-bold text-ink">입찰 대행 서비스 가이드</h1>
        <p className="mt-2 text-ink-mute">
          SAM.gov 미군(USFK) 정부 조달 입찰을 전문가가 분석하고 제안서를 작성해드립니다.
        </p>
        <div className="flex justify-center gap-3 mt-6">
          <Button onClick={() => navigate('/search')}>
            <i className="fa-solid fa-magnifying-glass mr-2" /> 공고 검색하기
          </Button>
          <Button variant="outline" onClick={() => navigate('/pricing')}>
            요금제 보기
          </Button>
        </div>
      </div>

      {/* Steps */}
      <div className="max-w-5xl mx-auto px-6 py-16">
        <h2 className="text-2xl font-extrabold text-ink text-center mb-2">이용 절차</h2>
        <p className="text-ink-mute text-center mb-12">6단계로 간편하게 입찰에 참여하세요.</p>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {STEPS.map(step => (
            <div key={step.no} className="bg-white rounded-xl border border-line p-6 hover:shadow-md transition">
              <div className="flex items-center gap-3 mb-4">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center ${step.color}`}>
                  <i className={`${step.icon} text-lg`} />
                </div>
                <span className="text-xs font-bold text-ink-mute uppercase">Step {step.no}</span>
              </div>
              <h3 className="text-base font-bold text-ink mb-2">{step.title}</h3>
              <p className="text-sm text-ink-mute leading-relaxed">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* FAQ */}
      <div className="bg-surface px-6 py-16">
        <div className="max-w-3xl mx-auto">
          <h2 className="text-2xl font-extrabold text-ink text-center mb-10">자주 묻는 질문</h2>
          <Accordion type="single" collapsible className="space-y-4">
            {FAQS.map((faq, idx) => (
              <AccordionItem key={idx} value={`faq-${idx}`} className="bg-white rounded-xl border border-line px-5">
                <AccordionTrigger className="text-sm font-bold text-ink gap-2">
                  <span className="flex items-start gap-2 text-left">
                    <i className="fa-solid fa-circle-question text-brand mt-0.5" />
                    {faq.q}
                  </span>
                </AccordionTrigger>
                <AccordionContent className="text-sm text-ink-sub ml-6 leading-relaxed">{faq.a}</AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </div>
      </div>

      {/* CTA */}
      <div className="text-center px-6 py-16">
        <h2 className="text-xl font-bold text-ink mb-3">지금 시작하세요</h2>
        <p className="text-ink-mute mb-6">전문가가 입찰 제안서를 작성하는 동안, 본업에 집중하세요.</p>
        <Button variant="accent" size="lg" onClick={() => navigate('/search')}>
          <i className="fa-solid fa-rocket mr-2" /> 공고 검색 시작
        </Button>
      </div>
    </div>
  )
}
