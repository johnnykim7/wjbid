{/* ⚠️ 법무 검토 필요 — 플레이스홀더 문구 */}
import { Link } from 'react-router-dom'

const SECTIONS = [
  {
    no: '1',
    title: '환불 원칙',
    body: 'WJbid의 유료 서비스는 회원 월 요금과 대행·컨설팅 건별 요금으로 나뉘며, 각각 아래 기준에 따라 환불합니다.',
  },
  {
    no: '2',
    title: '회원 월 요금, 결제 후 7일 이내',
    body: '회원 월 요금을 결제한 뒤 7일 이내이고 회원 기능을 이용하지 않았다면 전액 환불을 요청할 수 있습니다.',
  },
  {
    no: '3',
    title: '회원 월 요금, 해지',
    body: '해지하면 다음 결제일부터 요금이 청구되지 않으며, 이미 결제한 달은 남은 기간 동안 회원 기능을 그대로 이용할 수 있습니다. 이미 결제한 달의 요금은 나누어 환불하지 않습니다.',
  },
  {
    no: '4',
    title: '대행·컨설팅, 작업 착수 전',
    body: '결제 후 회사가 해당 작업에 아직 착수하지 않은 경우 전액 환불을 요청할 수 있습니다.',
  },
  {
    no: '5',
    title: '대행·컨설팅, 작업 착수 후',
    body: '착수 이후에는 이미 수행한 작업 비율만큼을 공제한 뒤 환불합니다.',
  },
  {
    no: '6',
    title: '환불 불가 시점',
    body: '대행·컨설팅 결과물(등록 완료, 자문 의견, 번역본 등)이 이용자에게 전달된 경우 해당 작업 요금은 환불되지 않습니다.',
  },
  {
    no: '7',
    title: '환불 절차',
    body: '환불은 고객센터를 통해 신청할 수 있으며, 회사는 환불 사유와 작업 진행률을 확인한 후 결제 수단으로 환불을 진행합니다. 결제 대행사의 정책에 따라 환불 처리에 일정 기간이 소요될 수 있습니다.',
  },
]

export default function RefundPage() {
  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-ink">환불 정책</h1>
        <p className="mt-2 text-ink-mute">
          회원 월 요금과 대행·컨설팅 건별 요금의 환불 기준을 안내합니다.
        </p>
      </div>

      <div className="max-w-3xl mx-auto px-6 py-16">
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
            ← 홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
