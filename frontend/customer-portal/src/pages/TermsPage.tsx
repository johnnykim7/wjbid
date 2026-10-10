{/* ⚠️ 법무 검토 필요 — 플레이스홀더 문구 */}
import { Link } from 'react-router-dom'

const ARTICLES = [
  {
    no: '제1조',
    title: '목적',
    body: '본 약관은 WJbid(이하 "회사")가 제공하는 미군(USFK)·연방 정부 조달 입찰 대행 서비스(이하 "서비스")의 이용과 관련하여 회사와 이용자 간의 권리·의무 및 책임사항을 규정함을 목적으로 합니다.',
  },
  {
    no: '제2조',
    title: '정의',
    body: '"서비스"란 SAM.gov 공고 수집·분석, 제안서 작성 지원 등 회사가 제공하는 일체의 서비스를 말합니다. "이용자"란 본 약관에 동의하고 회사와 이용계약을 체결한 개인 또는 사업자를 말합니다. "회원 월 요금"이란 회원 기능을 월 단위로 이용하는 요금을, "건별 요금"이란 대행·컨설팅 작업마다 상담 후 견적으로 정하는 요금을 말합니다.',
  },
  {
    no: '제3조',
    title: '서비스 내용',
    body: '회사는 SAM.gov 등 공개 조달 정보의 수집·한글화·요약, 공고 요구사항 분석, 제안서 초안 작성 지원, 제출 서류 안내 등의 서비스를 제공합니다. 회사는 서비스의 내용을 사전 고지 후 변경할 수 있습니다.',
  },
  {
    no: '제4조',
    title: '이용계약',
    body: '이용계약은 이용자가 회사가 정한 절차에 따라 회원가입을 신청하고 회사가 이를 승낙함으로써 체결됩니다. 회사는 허위 정보 기재, 타인 명의 도용 등의 사유가 있는 경우 승낙을 거부하거나 사후 계약을 해지할 수 있습니다.',
  },
  {
    no: '제5조',
    title: '요금 및 결제',
    body: '서비스 요금은 무료 이용, 회원 월 요금, 대행·컨설팅 건별 요금으로 나뉩니다. 회원 월 요금은 요금 안내 화면에 안내된 금액을 월 단위로 결제하며, 약정 없이 해지할 수 있습니다. 대행·컨설팅 요금은 상담 후 작업 착수 전에 견적으로 안내하며, 이용자가 결제를 완료한 시점에 작업이 개시됩니다. 환불 기준은 별도의 환불 정책에 따릅니다.',
  },
  {
    no: '제6조',
    title: '이용자 의무',
    body: '이용자는 정확한 정보를 제공하여야 하며, 제출하는 자료가 제3자의 권리를 침해하지 않음을 보증합니다. 이용자는 서비스를 통해 산출된 문서의 최종 검토·제출·서명에 대한 책임을 부담합니다.',
  },
  {
    no: '제7조',
    title: '면책',
    body: '회사는 서비스를 통해 입찰의 낙찰을 보장하지 않습니다. SAM.gov 등 외부 정보의 오류, 천재지변, 이용자의 귀책사유로 인한 손해에 대하여 회사는 책임을 지지 않습니다. 상세 면책 사항은 면책 고지를 따릅니다.',
  },
]

export default function TermsPage() {
  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-ink">이용약관</h1>
        <p className="mt-2 text-ink-mute">
          WJbid 미군·연방 조달 입찰 대행 서비스 이용약관입니다.
        </p>
      </div>

      <div className="max-w-3xl mx-auto px-6 py-16">
        <div className="space-y-6">
          {ARTICLES.map((a) => (
            <div key={a.no} className="bg-white rounded-xl border border-line p-6">
              <h2 className="text-base font-bold text-ink mb-2">
                {a.no} ({a.title})
              </h2>
              <p className="text-sm text-ink-sub leading-relaxed">{a.body}</p>
            </div>
          ))}
        </div>

        <div className="text-center mt-12">
          <Link to="/" className="text-sm font-semibold text-brand hover:underline">
            ← 홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
