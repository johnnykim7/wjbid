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
            SAM.gov 공고 검색부터 전문가 제안서 작성까지.<br />
            입찰 준비의 모든 과정을 자동화합니다.
          </p>
          <div className="space-y-3">
            {[
              { icon: 'fa-solid fa-bolt',          text: 'SAM.gov 실시간 공고 연동' },
              { icon: 'fa-solid fa-brain',          text: '전문가 기반 제안서 작성' },
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
      <div className="flex-1 flex items-center justify-center px-6 py-12">
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
      </div>
    </div>
  )
}
