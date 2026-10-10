{/* ⚠️ 법무 검토 필요 — 플레이스홀더 문구 */}
import { Link } from 'react-router-dom'

const SECTIONS = [
  {
    no: '1',
    title: '건당 과금 환불 원칙',
    body: 'WJbid는 공고 분석·제안서 작성 등 개별 작업 단위로 요금을 부과하는 건당 과금 방식을 운영합니다. 환불 가능 여부와 환불 비율은 각 작업의 착수 여부 및 진행률에 따라 결정됩니다.',
  },
  {
    no: '2',
    title: '작업 착수 전 — 전액 환불',
    body: '결제 후 회사가 해당 작업(공고 분석 또는 제안서 작성)에 아직 착수하지 않은 경우, 이용자는 전액 환불을 요청할 수 있습니다.',
  },
  {
    no: '3',
    title: '작업 착수 후 — 진행률 기준 부분 환불',
    body: '회사가 작업에 착수한 이후에는 진행 단계에 따라 환불이 제한됩니다. 공고 분석 단계가 완료된 경우 분석 비용은 환불되지 않으며, 제안서 작성이 진행 중인 경우 이미 수행된 작업 비율만큼을 공제한 후 환불합니다.',
  },
  {
    no: '4',
    title: '환불 불가 시점',
    body: '제안서 초안이 완성되어 이용자에게 전달된 경우, 또는 산출물이 이용자에 의해 다운로드·제출된 경우에는 해당 작업 비용의 환불이 불가합니다.',
  },
  {
    no: '5',
    title: '환불 절차',
    body: '환불은 고객센터를 통해 신청할 수 있으며, 회사는 환불 사유와 작업 진행률을 확인한 후 결제 수단으로 환불을 진행합니다. 결제 대행사의 정책에 따라 환불 처리에 일정 기간이 소요될 수 있습니다.',
  },
]

export default function RefundPage() {
  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-[#0c1e3d]">환불 정책</h1>
        <p className="mt-2 text-slate-500">
          건당 과금 기준에 따른 단계별 환불 가능·불가 시점을 안내합니다.
        </p>
      </div>

      <div className="max-w-3xl mx-auto px-6 py-16">
        <div className="space-y-6">
          {SECTIONS.map((s) => (
            <div key={s.no} className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="text-base font-bold text-[#0c1e3d] mb-2">
                {s.no}. {s.title}
              </h2>
              <p className="text-sm text-gray-600 leading-relaxed">{s.body}</p>
            </div>
          ))}
        </div>

        <div className="text-center mt-12">
          <Link to="/" className="text-sm font-semibold text-[#1a56db] hover:underline">
            ← 홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
