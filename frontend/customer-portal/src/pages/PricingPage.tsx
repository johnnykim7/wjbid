export default function PricingPage() {
  return (
    <div className="p-6">
      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-10">
          <h2 className="text-2xl font-bold text-gray-900">요금제 업그레이드 및 크레딧 충전</h2>
          <p className="mt-2 text-gray-500">더 많은 AI 제안서 작성이 필요하신가요?</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Pro Plan */}
          <div className="bg-white rounded-2xl shadow-sm border-2 border-secondary p-8 relative">
            <div className="absolute top-0 right-0 bg-secondary text-white px-4 py-1 rounded-bl-xl rounded-tr-xl text-xs font-bold">
              현재 이용중
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-2">Pro Plan</h3>
            <div className="my-4">
              <span className="text-3xl font-extrabold text-gray-900">390,000원</span>
              <span className="text-gray-500">/월</span>
            </div>
            <ul className="space-y-3 mb-8 text-sm text-gray-600">
              <li className="flex items-center">
                <i className="fa-solid fa-check text-accent mr-2" />
                SAM.gov 입찰 무제한 검색 및 알림
              </li>
              <li className="flex items-center font-bold text-secondary">
                <i className="fa-solid fa-check text-accent mr-2" />
                월 10회 AI 제안서 작성 (현재 5회 남음)
              </li>
              <li className="flex items-center">
                <i className="fa-solid fa-check text-accent mr-2" />
                과거 실적(Past Performance) 자동 매칭
              </li>
              <li className="flex items-center">
                <i className="fa-solid fa-check text-accent mr-2" />
                이메일 마감일 알림 서비스
              </li>
            </ul>
            <button className="w-full bg-gray-100 text-gray-700 py-2.5 rounded-lg font-bold hover:bg-gray-200 transition">
              결제 관리
            </button>
          </div>

          {/* Enterprise Plan */}
          <div className="bg-gradient-to-br from-primary to-slate-800 text-white rounded-2xl shadow-xl border border-gray-700 p-8">
            <h3 className="text-xl font-bold mb-2">Enterprise Plan</h3>
            <div className="my-4">
              <span className="text-3xl font-extrabold">별도 문의</span>
            </div>
            <ul className="space-y-3 mb-8 text-sm text-gray-300">
              <li className="flex items-center">
                <i className="fa-solid fa-check text-accent mr-2" />
                <strong className="text-white">무제한</strong>&nbsp;AI 제안서 작성
              </li>
              <li className="flex items-center">
                <i className="fa-solid fa-check text-accent mr-2" />
                입찰 전문가 1:1 휴먼 리뷰 (Human-in-the-loop)
              </li>
              <li className="flex items-center">
                <i className="fa-solid fa-check text-accent mr-2" />
                회사 전용 AI 모델 파인튜닝 (보안 강화)
              </li>
              <li className="flex items-center">
                <i className="fa-solid fa-check text-accent mr-2" />
                전담 계정 매니저 배정
              </li>
            </ul>
            <button className="w-full bg-white text-primary py-2.5 rounded-lg font-bold hover:bg-gray-100 transition shadow-md">
              영업팀에 문의하기
            </button>
          </div>
        </div>

        {/* Credit Top-up */}
        <div className="mt-8 bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
          <h3 className="text-lg font-bold text-gray-800 mb-4">
            <i className="fa-solid fa-bolt text-yellow-400 mr-2" />
            AI 크레딧 추가 충전
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[
              { count: 5, price: '150,000원' },
              { count: 10, price: '280,000원', popular: true },
              { count: 20, price: '500,000원' },
            ].map((pkg) => (
              <div
                key={pkg.count}
                className={`relative border rounded-xl p-5 text-center cursor-pointer hover:border-secondary transition ${
                  pkg.popular ? 'border-secondary bg-blue-50' : 'border-gray-200'
                }`}
              >
                {pkg.popular && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-secondary text-white text-xs font-bold px-3 py-0.5 rounded-full">
                    인기
                  </div>
                )}
                <div className="text-2xl font-bold text-gray-800 mb-1">{pkg.count}회</div>
                <div className="text-sm text-gray-500 mb-3">AI 크레딧</div>
                <div className="text-lg font-bold text-secondary">{pkg.price}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
