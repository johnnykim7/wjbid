import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { login } from '../api/client'

export default function LoginPage() {
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await login(email, password)
      signIn(data.email ?? email, data.accessToken, data.refreshToken)
      navigate('/search', { replace: true })
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex" style={{ background: '#f5f7fa' }}>
      {/* 좌측 브랜드 패널 */}
      <div
        className="hidden lg:flex flex-col justify-between w-[460px] flex-shrink-0 p-12 text-white"
        style={{ background: 'linear-gradient(160deg, #0c1e3d 0%, #1a3a6c 100%)' }}
      >
        <div className="flex items-center gap-2.5">
          <div
            className="w-8 h-8 rounded-md flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.12)' }}
          >
            <i className="fa-solid fa-file-signature text-white text-sm" />
          </div>
          <span className="font-bold text-lg text-white">WJbid.com</span>
        </div>

        <div>
          <h2 className="text-3xl font-bold leading-snug mb-4">
            미 연방정부 입찰의<br />새로운 기준
          </h2>
          <p className="text-sm leading-relaxed mb-10" style={{ color: 'rgba(255,255,255,0.6)' }}>
            SAM.gov 공고 검색부터 AI 제안서 자동 생성까지.<br />
            입찰 준비의 모든 과정을 자동화합니다.
          </p>
          <div className="space-y-3">
            {[
              { icon: 'fa-solid fa-bolt',          text: 'SAM.gov 실시간 공고 연동' },
              { icon: 'fa-solid fa-brain',          text: 'Claude AI 기반 제안서 자동 생성' },
              { icon: 'fa-solid fa-shield-halved',  text: '제출 규격 자동 검증' },
            ].map((f) => (
              <div key={f.text} className="flex items-center gap-3">
                <div
                  className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
                  style={{ background: 'rgba(255,255,255,0.1)' }}
                >
                  <i className={`${f.icon} text-xs`} style={{ color: 'rgba(255,255,255,0.75)' }} />
                </div>
                <span className="text-sm" style={{ color: 'rgba(255,255,255,0.65)' }}>{f.text}</span>
              </div>
            ))}
          </div>
        </div>

        <p className="text-xs" style={{ color: 'rgba(255,255,255,0.3)' }}>© 2025 WJbid.com</p>
      </div>

      {/* 우측 로그인 폼 */}
      <div className="flex-1 flex items-center justify-center px-6 py-12 relative overflow-hidden">
        <div className="w-full max-w-[400px]">
          {/* 모바일 로고 */}
          <div className="lg:hidden flex items-center gap-2.5 mb-8">
            <div className="w-8 h-8 rounded-md flex items-center justify-center" style={{ background: '#0c1e3d' }}>
              <i className="fa-solid fa-file-signature text-white text-sm" />
            </div>
            <span className="font-bold text-[17px]" style={{ color: '#0c1e3d' }}>
              WJbid<span className="font-normal text-sm" style={{ color: '#1a56db' }}>.com</span>
            </span>
          </div>

          <div className="mb-8">
            <h1 className="text-2xl font-bold text-[#0c1e3d] mb-1">로그인</h1>
            <p className="text-sm text-gray-500">계정에 로그인하여 서비스를 이용하세요</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">이메일</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="name@company.com"
                className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:border-[#1a56db] transition-colors"
              />
            </div>
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-sm font-medium text-gray-700">비밀번호</label>
                <a href="#" className="text-xs text-[#1a56db] hover:underline">비밀번호 찾기</a>
              </div>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
                className="w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:border-[#1a56db] transition-colors"
              />
            </div>

            {error && (
              <div className="flex items-center gap-2 p-3 rounded-lg bg-red-50 border border-red-200">
                <i className="fa-solid fa-circle-exclamation text-red-500 text-sm flex-shrink-0" />
                <p className="text-sm text-red-700">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 text-sm font-semibold text-white rounded-lg transition-opacity disabled:opacity-60 mt-2"
              style={{ background: '#0c1e3d' }}
              onMouseEnter={(e) => !loading && (e.currentTarget.style.background = '#162d57')}
              onMouseLeave={(e) => !loading && (e.currentTarget.style.background = '#0c1e3d')}
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <i className="fa-solid fa-spinner fa-spin text-xs" /> 로그인 중...
                </span>
              ) : '로그인'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              아직 계정이 없으신가요?{' '}
              <Link to="/register" className="text-[#1a56db] font-medium hover:underline">
                회원가입
              </Link>
            </p>
          </div>

          <div className="mt-4 pt-4 border-t border-gray-200">
            <Link
              to="/"
              className="flex items-center gap-1.5 text-xs text-gray-400 hover:text-gray-600 transition-colors"
            >
              <i className="fa-solid fa-arrow-left text-[10px]" />
              홈으로 돌아가기
            </Link>
          </div>
        </div>

        {/* 미군 독수리 엠블럼 — 우측 하단 장식 */}
        <div
          className="absolute bottom-0 right-0 pointer-events-none select-none"
          style={{ width: 400, height: 360 }}
        >
          <svg viewBox="0 0 420 380" xmlns="http://www.w3.org/2000/svg" width="400" height="360">
            <defs>
              <radialGradient id="eagleGlow" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stopColor="#0c1e3d" stopOpacity="0.22" />
                <stop offset="100%" stopColor="#0c1e3d" stopOpacity="0.06" />
              </radialGradient>
            </defs>

            {/* 외곽 씰 링 */}
            <circle cx="210" cy="195" r="168" fill="none" stroke="rgba(12,30,61,0.09)" strokeWidth="1.5"/>
            <circle cx="210" cy="195" r="155" fill="none" stroke="rgba(12,30,61,0.06)" strokeWidth="0.8"/>

            {/* 방사형 글로우 */}
            <circle cx="210" cy="195" r="140" fill="url(#eagleGlow)"/>

            {/* ── 성조기 별 13개 (원형 배열) ── */}
            {Array.from({ length: 13 }).map((_, i) => {
              const a = ((i * 360) / 13 - 90) * (Math.PI / 180)
              const rx = 148, ry = 148
              const cx = 210 + rx * Math.cos(a)
              const cy = 195 + ry * Math.sin(a)
              const R = 5.5, r = 2.3
              const pts = Array.from({ length: 5 }).map((__, j) => {
                const ao = (j * 72 - 90) * (Math.PI / 180)
                const ai = ((j * 72 + 36) - 90) * (Math.PI / 180)
                return `${cx + R * Math.cos(ao)},${cy + R * Math.sin(ao)} ${cx + r * Math.cos(ai)},${cy + r * Math.sin(ai)}`
              }).join(' ')
              return <polygon key={i} points={pts} fill="rgba(12,30,61,0.18)" />
            })}

            {/* ── 별 5개 (독수리 머리 위 호) ── */}
            {[
              [134, 58], [159, 43], [185, 37], [211, 43], [236, 58],
            ].map(([sx, sy], i) => {
              const R = 8, r = 3.5
              const pts = Array.from({ length: 5 }).map((_, j) => {
                const ao = (j * 72 - 90) * (Math.PI / 180)
                const ai = ((j * 72 + 36) - 90) * (Math.PI / 180)
                return `${sx + R * Math.cos(ao)},${sy + R * Math.sin(ao)} ${sx + r * Math.cos(ai)},${sy + r * Math.sin(ai)}`
              }).join(' ')
              return <polygon key={i} points={pts} fill="rgba(12,30,61,0.28)" />
            })}

            {/* ── 왼쪽 날개 (상면) ── */}
            <path
              d="M 178,180 C 150,152 98,112 30,84 C 22,81 12,80 4,83 C 24,94 62,116 102,142 C 138,165 166,182 178,188 Z"
              fill="rgba(12,30,61,0.22)"
            />
            {/* 왼쪽 날개 하면 (깃털 층) */}
            <path
              d="M 178,185 C 158,176 128,158 98,140 C 65,120 34,100 12,86 C 30,96 60,114 90,134 C 124,157 158,176 175,188 Z"
              fill="rgba(12,30,61,0.12)"
            />

            {/* ── 오른쪽 날개 (상면) ── */}
            <path
              d="M 240,180 C 268,152 320,112 388,84 C 396,81 406,80 414,83 C 394,94 356,116 316,142 C 280,165 252,182 240,188 Z"
              fill="rgba(12,30,61,0.22)"
            />
            {/* 오른쪽 날개 하면 (깃털 층) */}
            <path
              d="M 240,185 C 260,176 290,158 320,140 C 353,120 384,100 406,86 C 388,96 358,114 328,134 C 294,157 260,176 243,188 Z"
              fill="rgba(12,30,61,0.12)"
            />

            {/* ── 몸통 ── */}
            <ellipse cx="209" cy="218" rx="32" ry="58" fill="rgba(12,30,61,0.20)"/>

            {/* ── 목 ── */}
            <path
              d="M 193,178 C 193,164 198,154 209,148 C 220,154 226,164 226,178 Z"
              fill="rgba(12,30,61,0.22)"
            />

            {/* ── 머리 (흰독수리) ── */}
            <circle cx="218" cy="132" r="28" fill="rgba(12,30,61,0.22)"/>

            {/* 머리 흰색 부분 하이라이트 */}
            <circle cx="215" cy="128" r="22" fill="rgba(12,30,61,0.08)"/>

            {/* ── 부리 (오른쪽 향함) ── */}
            <path
              d="M 242,123 C 258,128 266,136 261,144 C 256,142 248,134 242,128 Z"
              fill="rgba(12,30,61,0.35)"
            />
            {/* 부리 아랫 부분 */}
            <path
              d="M 244,138 C 254,140 261,144 258,148 C 253,147 246,143 244,140 Z"
              fill="rgba(12,30,61,0.28)"
            />

            {/* 눈 */}
            <circle cx="228" cy="127" r="5" fill="rgba(12,30,61,0.15)"/>
            <circle cx="228" cy="127" r="3" fill="rgba(12,30,61,0.35)"/>

            {/* ── 가슴 방패 (Shield) ── */}
            <path
              d="M 197,200 L 221,200 L 221,230 Q 209,246 197,230 Z"
              fill="rgba(12,30,61,0.16)"
              stroke="rgba(12,30,61,0.1)" strokeWidth="0.5"
            />
            {/* 방패 세로줄 */}
            <line x1="209" y1="200" x2="209" y2="238" stroke="rgba(12,30,61,0.1)" strokeWidth="0.8"/>
            <line x1="203" y1="201" x2="203" y2="234" stroke="rgba(12,30,61,0.08)" strokeWidth="0.8"/>
            <line x1="215" y1="201" x2="215" y2="234" stroke="rgba(12,30,61,0.08)" strokeWidth="0.8"/>

            {/* ── 꼬리깃 (7개) ── */}
            {[
              [-26, -8], [-16, -3], [-7, 0], [0, 1], [7, 0], [16, -3], [26, -8],
            ].map(([ox, oy], i) => (
              <path
                key={i}
                d={`M ${209 + ox * 0.6},272 C ${209 + ox * 0.8 + oy},290 ${209 + ox},308 ${209 + ox * 1.1},328 L ${213 + ox * 1.1},327 C ${213 + ox},307 ${213 + ox * 0.8 + oy},289 ${213 + ox * 0.6},272 Z`}
                fill="rgba(12,30,61,0.18)"
              />
            ))}

            {/* ── 발톱 (양쪽) ── */}
            {/* 왼쪽 발 */}
            <path d="M 193,270 C 187,278 182,286 178,292" stroke="rgba(12,30,61,0.18)" strokeWidth="3" strokeLinecap="round" fill="none"/>
            <path d="M 178,292 C 172,293 166,292 162,295 M 178,292 C 175,298 172,304 170,308 M 178,292 C 178,298 179,305 179,310 M 178,292 C 182,296 184,301 185,306" stroke="rgba(12,30,61,0.16)" strokeWidth="2.5" strokeLinecap="round" fill="none"/>
            {/* 오른쪽 발 */}
            <path d="M 225,270 C 231,278 236,286 240,292" stroke="rgba(12,30,61,0.18)" strokeWidth="3" strokeLinecap="round" fill="none"/>
            <path d="M 240,292 C 246,293 252,292 256,295 M 240,292 C 243,298 246,304 248,308 M 240,292 C 240,298 239,305 239,310 M 240,292 C 236,296 234,301 233,306" stroke="rgba(12,30,61,0.16)" strokeWidth="2.5" strokeLinecap="round" fill="none"/>

            {/* ── 올리브 가지 (왼쪽 발) ── */}
            <path d="M 162,296 C 152,300 142,304 134,310" stroke="rgba(12,30,61,0.14)" strokeWidth="1.5" strokeLinecap="round" fill="none"/>
            {[150, 143, 136].map((x, i) => (
              <ellipse key={i} cx={x} cy={300 + i * 4} rx="4" ry="2.5"
                fill="rgba(12,30,61,0.14)"
                transform={`rotate(${-30 + i * 10} ${x} ${300 + i * 4})`}/>
            ))}

            {/* ── 화살 (오른쪽 발) ── */}
            <line x1="256" y1="294" x2="276" y2="308" stroke="rgba(12,30,61,0.16)" strokeWidth="1.5" strokeLinecap="round"/>
            <path d="M 274,305 L 280,310 L 272,312 Z" fill="rgba(12,30,61,0.18)"/>
            <line x1="265" y1="297" x2="285" y2="311" stroke="rgba(12,30,61,0.12)" strokeWidth="1.5" strokeLinecap="round"/>
            <line x1="258" y1="300" x2="278" y2="314" stroke="rgba(12,30,61,0.12)" strokeWidth="1.5" strokeLinecap="round"/>

            {/* E PLURIBUS UNUM 텍스트 */}
            <path id="sealArc" d="M 62,195 A 148,148 0 0,1 358,195" fill="none"/>
            <text fontSize="10" fill="rgba(12,30,61,0.20)" letterSpacing="3" fontFamily="serif">
              <textPath href="#sealArc" startOffset="18%">E · PLURIBUS · UNUM</textPath>
            </text>
          </svg>
        </div>
      </div>
    </div>
  )
}
