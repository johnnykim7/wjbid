import { useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/button'

const STEPS = [
  {
    no: 1,
    title: '공고 검색',
    desc: 'SAM.gov에서 자동 수집된 USFK 관련 공고를 키워드, NAICS 코드, 마감일 등으로 검색하세요.',
    icon: 'fa-solid fa-magnifying-glass',
    color: 'bg-blue-50 text-blue-500',
  },
  {
    no: 2,
    title: '제안서 작성 신청',
    desc: '관심 있는 공고를 선택하고 "제안서 작성" 버튼을 클릭하면 입찰 프로세스가 시작됩니다.',
    icon: 'fa-solid fa-wand-magic-sparkles',
    color: 'bg-purple-50 text-purple-500',
  },
  {
    no: 3,
    title: '필요 서류 제출',
    desc: '과거 수행 실적, 회사 프로필 등 필요한 서류를 업로드하세요. 제안서 작성에 활용됩니다.',
    icon: 'fa-solid fa-cloud-arrow-up',
    color: 'bg-teal-50 text-teal-500',
  },
  {
    no: 4,
    title: '요구사항 분석',
    desc: '공고 요구사항을 추출하고 자격 요건 충족 여부를 검토합니다.',
    icon: 'fa-solid fa-robot',
    color: 'bg-indigo-50 text-indigo-500',
  },
  {
    no: 5,
    title: '제안서 초안 작성',
    desc: 'Cover Letter, Technical Proposal, Past Performance 등 필요한 문서의 초안을 작성합니다.',
    icon: 'fa-solid fa-file-lines',
    color: 'bg-orange-50 text-orange-500',
  },
  {
    no: 6,
    title: '전문가 검토 & 제출',
    desc: '입찰 전문가가 최종 검토 후 SAM.gov에 제출합니다. 진행 상황은 실시간으로 확인 가능합니다.',
    icon: 'fa-solid fa-paper-plane',
    color: 'bg-emerald-50 text-emerald-500',
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
    a: 'Basic($299), Professional($599), Enterprise($999) 3개 플랜이 있습니다. 가격 페이지에서 상세 내용을 확인하세요.',
  },
]

export default function GuidePage() {
  const navigate = useNavigate()

  return (
    <div className="flex-1 overflow-y-auto">
      {/* Hero */}
      <div className="bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white px-6 py-16 text-center">
        <h1 className="text-3xl md:text-4xl font-extrabold mb-4">
          입찰 대행 서비스 가이드
        </h1>
        <p className="text-gray-300 max-w-2xl mx-auto text-lg leading-relaxed">
          SAM.gov 미군(USFK) 정부 조달 입찰을 전문가가 분석하고<br className="hidden md:block" />
          제안서를 작성해드립니다.
        </p>
        <div className="flex justify-center gap-3 mt-8">
          <Button variant="accent" onClick={() => navigate('/search')}>
            <i className="fa-solid fa-magnifying-glass mr-2" /> 공고 검색하기
          </Button>
          <Button variant="outline" onClick={() => navigate('/pricing')} className="!text-white !border-gray-600 hover:!bg-gray-700">
            요금제 보기
          </Button>
        </div>
      </div>

      {/* Steps */}
      <div className="max-w-5xl mx-auto px-6 py-16">
        <h2 className="text-2xl font-extrabold text-gray-900 text-center mb-2">이용 절차</h2>
        <p className="text-gray-500 text-center mb-12">6단계로 간편하게 입찰에 참여하세요.</p>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {STEPS.map(step => (
            <div key={step.no} className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-md transition">
              <div className="flex items-center gap-3 mb-4">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center ${step.color}`}>
                  <i className={`${step.icon} text-lg`} />
                </div>
                <span className="text-xs font-bold text-gray-400 uppercase">Step {step.no}</span>
              </div>
              <h3 className="text-base font-bold text-gray-900 mb-2">{step.title}</h3>
              <p className="text-sm text-gray-500 leading-relaxed">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* FAQ */}
      <div className="bg-gray-50 px-6 py-16">
        <div className="max-w-3xl mx-auto">
          <h2 className="text-2xl font-extrabold text-gray-900 text-center mb-10">자주 묻는 질문</h2>
          <div className="space-y-4">
            {FAQS.map((faq, idx) => (
              <div key={idx} className="bg-white rounded-xl border border-gray-200 p-5">
                <h3 className="text-sm font-bold text-gray-900 flex items-start gap-2">
                  <i className="fa-solid fa-circle-question text-secondary mt-0.5" />
                  {faq.q}
                </h3>
                <p className="text-sm text-gray-600 mt-2 ml-6 leading-relaxed">{faq.a}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* CTA */}
      <div className="text-center px-6 py-16">
        <h2 className="text-xl font-bold text-gray-900 mb-3">지금 시작하세요</h2>
        <p className="text-gray-500 mb-6">전문가가 입찰 제안서를 작성하는 동안, 본업에 집중하세요.</p>
        <Button variant="accent" size="lg" onClick={() => navigate('/search')}>
          <i className="fa-solid fa-rocket mr-2" /> 공고 검색 시작
        </Button>
      </div>
    </div>
  )
}
