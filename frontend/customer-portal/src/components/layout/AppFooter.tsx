import { useNavigate } from 'react-router-dom'

/**
 * CR-124 — 공통 푸터 (4컬럼 가지런히)
 * 브랜드 | 서비스 | 약관·정책 | 고객센터
 * 사업자정보(전상법 표기)는 하단 바. 미확정 플레이스홀더 "(...)"는 렌더 생략.
 */

// "(대표자명)" 같은 미정 플레이스홀더면 표시하지 않음
const filled = (v?: string) => (v && !/^\(.*\)$/.test(v.trim()) ? v.trim() : undefined)

const COMPANY = {
  name: '우진USbid',
  ceo: '(대표자명)',
  businessNumber: '(사업자등록번호)',
  address: '(주소)',
  email: 'support@wjbid.com',
  phone: '(고객센터 번호)',
  hours: '평일 09:00 ~ 18:00',
}

export function AppFooter() {
  const navigate = useNavigate()

  const SERVICE = [
    { label: '이용 안내', to: '/guide' },
    { label: '요금 안내', to: '/pricing' },
    { label: '공지사항', to: '/notices' },
    { label: '고객센터', to: '/support' },
  ]
  const POLICY = [
    { label: '이용약관', to: '/terms' },
    { label: '개인정보처리방침', to: '/privacy' },
    { label: '환불 정책', to: '/refund' },
    { label: '면책 고지', to: '/disclaimer' },
  ]

  // 하단 바 사업자정보: 값이 채워진 항목만
  const bizItems = [
    filled(COMPANY.name) && `우진USbid`,
    filled(COMPANY.ceo) && `대표: ${filled(COMPANY.ceo)}`,
    filled(COMPANY.businessNumber) && `사업자등록번호: ${filled(COMPANY.businessNumber)}`,
    filled(COMPANY.address) && `주소: ${filled(COMPANY.address)}`,
    filled(COMPANY.email) && `이메일: ${filled(COMPANY.email)}`,
  ].filter(Boolean) as string[]

  return (
    <footer className="bg-[#13294f] text-white">
      <div className="max-w-[1600px] mx-auto px-6 lg:px-10 py-14">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-x-8 gap-y-10">
          {/* 브랜드 */}
          <div>
            <div className="flex items-center gap-2 mb-3">
              <span className="w-7 h-7 rounded bg-white text-[#13294f] grid place-items-center text-xs font-bold">
                W
              </span>
              <span className="font-bold text-white">WJbid</span>
            </div>
            <p className="text-sm text-white/55 leading-relaxed">
              미군·연방 조달 입찰을
              <br />
              AI로 대행하는 서비스
            </p>
          </div>

          {/* 서비스 */}
          <div>
            <h4 className="text-sm font-semibold text-white mb-4">서비스</h4>
            <ul className="space-y-3">
              {SERVICE.map((l) => (
                <li key={l.to}>
                  <button
                    onClick={() => navigate(l.to)}
                    className="text-sm text-white/65 hover:text-white transition-colors"
                  >
                    {l.label}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          {/* 약관·정책 */}
          <div>
            <h4 className="text-sm font-semibold text-white mb-4">약관·정책</h4>
            <ul className="space-y-3">
              {POLICY.map((l) => (
                <li key={l.to}>
                  <button
                    onClick={() => navigate(l.to)}
                    className="text-sm text-white/65 hover:text-white transition-colors"
                  >
                    {l.label}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          {/* 고객센터 */}
          <div>
            <h4 className="text-sm font-semibold text-white mb-4">고객센터</h4>
            {filled(COMPANY.phone) && (
              <p className="text-lg font-bold text-white mb-1">{filled(COMPANY.phone)}</p>
            )}
            <p className="text-sm text-white/65">{COMPANY.hours}</p>
            {filled(COMPANY.email) && (
              <a
                href={`mailto:${filled(COMPANY.email)}`}
                className="text-sm text-white/65 hover:text-white transition-colors block mt-1"
              >
                {filled(COMPANY.email)}
              </a>
            )}
          </div>
        </div>

        {/* 하단 바 */}
        <div className="mt-12 pt-6 border-t border-white/15">
          {bizItems.length > 0 && (
            <p className="text-xs text-white/55 flex flex-wrap gap-x-4 gap-y-1">
              {bizItems.map((t) => (
                <span key={t}>{t}</span>
              ))}
            </p>
          )}
          <p className="text-xs text-white/40 mt-2 leading-relaxed">
            © 2026 WJbid · 우진USbid · 미군·연방 조달 입찰 대행 서비스. 본 서비스는 낙찰을 보장하지
            않으며 입찰서 최종 제출·서명은 고객 책임입니다.
          </p>
        </div>
      </div>
    </footer>
  )
}
