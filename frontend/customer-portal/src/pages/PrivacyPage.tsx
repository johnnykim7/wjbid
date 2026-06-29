{/* ⚠️ 법무 검토 필요 — 플레이스홀더 문구 */}
import { Link } from 'react-router-dom'

const SECTIONS = [
  {
    no: '1',
    title: '수집하는 개인정보 항목',
    body: '회사는 서비스 제공을 위해 다음의 항목을 수집합니다: 이메일 주소, 회사명, 사업자등록번호, 담당자 성명, 연락처(전화·이메일), UEI(Unique Entity Identifier), CAGE Code. 서비스 이용 과정에서 입찰 관련 첨부 서류가 추가로 수집될 수 있습니다.',
  },
  {
    no: '2',
    title: '개인정보의 이용 목적',
    body: '수집된 개인정보는 회원 식별 및 본인 확인, 입찰 대행 서비스 제공, 공고 분석·제안서 작성 지원, 요금 청구 및 결제, 서비스 관련 안내·알림 발송, 고객 문의 응대 목적으로만 이용됩니다.',
  },
  {
    no: '3',
    title: '개인정보의 보유 및 이용 기간',
    body: '회사는 이용자의 개인정보를 서비스 이용계약 존속 기간 동안 보유하며, 계약 종료 또는 회원 탈퇴 시 지체 없이 파기합니다. 다만 관계 법령에 따라 보존이 필요한 경우 해당 법령이 정한 기간 동안 보관합니다.',
  },
  {
    no: '4',
    title: '개인정보의 제3자 제공',
    body: '회사는 이용자의 개인정보를 본 방침에 명시한 범위를 초과하여 제3자에게 제공하지 않습니다. 다만 이용자가 사전 동의한 경우, 또는 법령에 의거하거나 수사기관의 적법한 요청이 있는 경우에 한하여 제공할 수 있습니다.',
  },
  {
    no: '5',
    title: '개인정보 처리 위탁',
    body: '회사는 원활한 서비스 제공을 위해 결제 처리, 클라우드 인프라 운영, 알림 발송 등 일부 업무를 외부에 위탁할 수 있으며, 위탁 시 수탁자와 위탁 업무 내용을 본 방침을 통해 고지합니다.',
  },
  {
    no: '6',
    title: '이용자의 권리',
    body: '이용자는 언제든지 자신의 개인정보를 조회·수정할 수 있으며, 개인정보의 처리 정지 및 삭제(회원 탈퇴)를 요청할 수 있습니다. 권리 행사는 고객센터를 통해 요청할 수 있으며, 회사는 지체 없이 조치합니다.',
  },
]

export default function PrivacyPage() {
  return (
    <div className="flex-1 overflow-y-auto">
      {/* Hero */}
      <div className="bg-gradient-to-br from-[#0c1e3d] via-[#0c1e3d] to-[#10264d] text-white px-6 py-16 text-center">
        <h1 className="text-3xl md:text-4xl font-extrabold mb-4">개인정보처리방침</h1>
        <p className="text-gray-300 max-w-2xl mx-auto text-lg leading-relaxed">
          WJbid는 이용자의 개인정보를 소중히 다루며 관계 법령을 준수합니다.
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
