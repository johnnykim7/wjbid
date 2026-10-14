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
    desc: '관심 있는 공고를 선택하고 "제안서 작성" 버튼을 누르면 입찰 준비가 시작됩니다.',
    icon: 'fa-solid fa-file-pen',
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
    desc: '공고 요구사항을 정리하고 자격 요건을 갖췄는지 검토합니다.',
    icon: 'fa-solid fa-list-check',
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
    title: '최종 검토와 제출',
    desc: '최종 검토 후 SAM.gov에 제출합니다. 진행 상황은 내 제안서 화면에서 확인할 수 있습니다.',
    icon: 'fa-solid fa-paper-plane',
    color: 'bg-brand-soft text-brand',
  },
]

const FAQS = [
  {
    q: '제안서의 품질은 어떤가요?',
    a: '초안은 SAM.gov 공고 요구사항에 맞춰 작성하고, 제출 전에 검토와 보완을 거칩니다.',
  },
  {
    q: '어떤 서류를 준비해야 하나요?',
    a: '공고마다 다르지만, 일반적으로 회사 프로필, 과거 수행 실적(Past Performance), 관련 자격증/인증서가 필요합니다. 공고마다 필요한 서류는 공고 상세 화면에서 안내합니다.',
  },
  {
    q: '마감일이 임박한 공고도 신청 가능한가요?',
    a: '마감 3일 전까지 신청하시기 바랍니다. 서류 작성에 1~2일, 담당자 검토에 1일이 걸립니다.',
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
          SAM.gov 에 올라온 주한미군(USFK) 조달 입찰의 공고 검토와 제안서 작성을 돕습니다.
        </p>
        <div className="flex justify-center gap-3 mt-6">
          <Button onClick={() => navigate('/search')}>
            <i className="fa-solid fa-magnifying-glass mr-2" /> 공고 검색하기
          </Button>
          <Button variant="outline" onClick={() => navigate('/pricing')}>
            요금 안내
          </Button>
        </div>
      </div>

      {/* Steps */}
      <div className="max-w-5xl mx-auto px-6 py-16">
        <h2 className="text-2xl font-extrabold text-ink text-center mb-2">이용 절차</h2>
        <p className="text-ink-mute text-center mb-12">입찰은 아래 6단계로 진행합니다.</p>

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
        <p className="text-ink-mute mb-6">관심 있는 공고를 찾는 것부터 시작합니다.</p>
        <Button variant="accent" size="lg" onClick={() => navigate('/search')}>
          <i className="fa-solid fa-magnifying-glass mr-2" /> 공고 검색 시작
        </Button>
      </div>
    </div>
  )
}
