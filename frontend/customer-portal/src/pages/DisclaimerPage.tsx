{/* ⚠️ 법무 검토 필요 — 플레이스홀더 문구 */}
import { Link } from 'react-router-dom'

const SECTIONS = [
  {
    no: '1',
    title: '낙찰 및 제출 책임',
    body: '본 서비스는 낙찰을 보장하지 않으며, 입찰서의 최종 제출 및 서명은 고객의 책임입니다. 회사는 제안서 작성 지원을 제공할 뿐, 입찰 결과(낙찰·탈락)에 대해 어떠한 보증도 하지 않습니다.',
  },
  {
    no: '2',
    title: 'SAM.gov 정보의 정확성',
    body: '회사가 제공하는 공고 정보는 SAM.gov 등 외부 공개 출처에서 수집·가공한 것으로, 회사는 해당 정보의 정확성·완전성·최신성을 보증하지 않습니다. 이용자는 최종 의사결정 전 SAM.gov 원문을 직접 확인할 책임이 있습니다.',
  },
  {
    no: '3',
    title: '한글화·요약·분석의 한계',
    body: '회사가 제공하는 한글 번역, 요약, 요구사항 분석은 참고용이며 원문과 차이가 있을 수 있습니다. 법적·계약적 효력은 항상 SAM.gov의 원문(영문)을 기준으로 합니다.',
  },
  {
    no: '4',
    title: '외부 요인으로 인한 면책',
    body: '천재지변, 정부 정책 변경, 입찰 마감 일정 변경, SAM.gov·PIEE 등 외부 시스템의 장애나 정책 변경 등 회사가 통제할 수 없는 사유로 발생한 손해에 대하여 회사는 책임을 지지 않습니다.',
  },
  {
    no: '5',
    title: '자격 요건 및 적격성',
    body: '입찰 참여를 위한 사업자 자격, 등록(SAM.gov 등록·UEI·CAGE), 인증 요건의 충족 여부는 이용자의 책임이며, 회사는 이용자의 적격성을 보증하지 않습니다.',
  },
]

export default function DisclaimerPage() {
  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-ink">면책 고지</h1>
        <p className="mt-2 text-ink-mute">
          서비스 이용 전 반드시 확인해야 할 책임 및 면책 사항입니다.
        </p>
      </div>

      <div className="max-w-3xl mx-auto px-6 py-16">
        <div className="rounded-xl border border-brand-line bg-brand-softer p-6 mb-8">
          <p className="text-sm font-semibold text-ink leading-relaxed">
            본 서비스는 낙찰을 보장하지 않으며, 입찰서의 최종 제출 및 서명은 고객의 책임입니다.
          </p>
        </div>

        <div className="space-y-6">
          {SECTIONS.map((s) => (
            <div key={s.no} className="bg-white rounded-xl border border-line p-6">
              <h2 className="text-base font-bold text-ink mb-2">
                {s.no}. {s.title}
              </h2>
              <p className="text-sm text-ink-sub leading-relaxed">{s.body}</p>
            </div>
          ))}
        </div>

        <div className="text-center mt-12">
          <Link to="/" className="text-sm font-semibold text-brand hover:underline">
            홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
