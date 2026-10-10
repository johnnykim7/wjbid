import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Input, Label, Button } from '@therecommerce-develop/ds-ui'
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
    <div className="min-h-screen flex bg-surface">
      {/* 좌측 브랜드 패널 */}
      <div
        className="hidden lg:flex flex-col justify-between w-[460px] flex-shrink-0 p-12 text-white bg-brand-deep"
      >
        <div className="flex items-center gap-2.5">
          <div
            className="w-8 h-8 rounded-md flex items-center justify-center bg-white/12"
          >
            <i className="fa-solid fa-file-signature text-white text-sm" />
          </div>
          <span className="font-bold text-lg text-white">WJbid.com</span>
        </div>

        <div>
          <h2 className="text-3xl font-bold leading-snug mb-4">
            미 연방정부 입찰의<br />새로운 기준
          </h2>
          <p className="text-sm leading-relaxed mb-10 text-white/60">
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
                  className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 bg-white/10"
                >
                  <i className={`${f.icon} text-xs text-white/75`} />
                </div>
                <span className="text-sm text-white/65">{f.text}</span>
              </div>
            ))}
          </div>
        </div>

        <p className="text-xs text-white/30">© 2025 WJbid.com</p>
      </div>

      {/* 우측 로그인 폼 */}
      <div className="flex-1 flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-[400px]">
          {/* 모바일 로고 */}
          <div className="lg:hidden flex items-center gap-2.5 mb-8">
            <div className="w-8 h-8 rounded-md flex items-center justify-center bg-brand-deep">
              <i className="fa-solid fa-file-signature text-white text-sm" />
            </div>
            <span className="font-bold text-[17px] text-brand-deep">
              WJbid<span className="font-normal text-sm text-brand">.com</span>
            </span>
          </div>

          <div className="mb-8">
            <h1 className="text-2xl font-bold text-ink mb-1">로그인</h1>
            <p className="text-sm text-ink-mute">계정에 로그인하여 서비스를 이용하세요</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label>이메일</Label>
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                required placeholder="name@company.com" />
            </div>
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label>비밀번호</Label>
                <a href="#" className="text-xs text-brand hover:underline">비밀번호 찾기</a>
              </div>
              <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                required placeholder="••••••••" />
            </div>

            {error && (
              <div className="flex items-center gap-2 p-3 rounded-lg bg-danger-soft border border-danger/30">
                <i className="fa-solid fa-circle-exclamation text-danger text-sm flex-shrink-0" />
                <p className="text-sm text-danger">{error}</p>
              </div>
            )}

            <Button type="submit" disabled={loading} className="w-full mt-2" size="lg">
              {loading ? '로그인 중...' : '로그인'}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-ink-mute">
              아직 계정이 없으신가요?{' '}
              <Link to="/register" className="text-brand font-medium hover:underline">
                회원가입
              </Link>
            </p>
          </div>

          <div className="mt-4 pt-4 border-t border-line">
            <Link
              to="/"
              className="flex items-center gap-1.5 text-xs text-ink-mute hover:text-ink-sub transition-colors"
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
