import { useRef, useState } from 'react'
import { useNavigate, useLocation, useSearchParams, Link } from 'react-router-dom'
import { Input, Label, Button } from '@therecommerce-develop/ds-ui'
import { useAuth } from '../context/AuthContext'
import { login } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { COMPANY } from '../components/layout/siteContent'
import { JoinIntro } from '../components/auth/JoinIntro'
import { SnsButtons } from '../components/auth/SnsButtons'
import { IMAGES } from '../data/homeContent'
import '../wjbid-auth.css'

// SNS 에서 돌아온 결과가 로그인, 가입이 아닐 때 서버가 붙이는 값 (CR-146 시나리오 8)
const SNS_NOTICE: Record<string, { tone: 'err' | 'info'; text: string }> = {
  EMAIL_IN_USE: { tone: 'info', text: '이 이메일은 이미 가입되어 있습니다. 이메일과 비밀번호로 로그인해 주세요.' },
  CANCELLED: { tone: 'info', text: 'SNS 로그인을 취소했습니다.' },
  FAILED: { tone: 'err', text: 'SNS 로그인을 마치지 못했습니다. 잠시 뒤 다시 시도해 주세요.' },
}

/** 로그인 (CR-131). 화면 정본은 docs/기록/토론_WJbid홈페이지방향/자료/시안/AIDA_색상/로그인_회원가입_시안.html #login */
export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { signIn } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const passwordRef = useRef<HTMLInputElement>(null)
  // 회원가입을 마치고 넘어오면 가입 완료 안내를 띄운다
  const registered = (location.state as { registered?: boolean } | null)?.registered === true
  // 로그인해야 쓰는 화면에서 넘어오면 로그인 뒤 그 화면으로 돌려보낸다(CR-137)
  const from = (location.state as { from?: string } | null)?.from
  const [params] = useSearchParams()
  const snsNotice = SNS_NOTICE[params.get('sns') ?? '']

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim() || !password) {
      setError('이메일과 비밀번호를 넣어 주세요.')
      return
    }
    setError('')
    setLoading(true)
    try {
      const { data } = await login(email.trim(), password)
      signIn(data.email ?? email.trim(), data.accessToken, data.refreshToken)
      navigate(from?.startsWith('/') ? from : '/search', { replace: true })
    } catch {
      // 이메일은 남기고 비밀번호만 다시 넣게 한다
      setError('이메일 또는 비밀번호가 맞지 않습니다. 다시 확인해 주세요.')
      setPassword('')
      passwordRef.current?.focus()
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="wj au-pg">
      <div className="au l">
        <form className="card lgn" onSubmit={handleSubmit} noValidate>
          <h1>로그인</h1>
          <p className="lead">가입한 이메일과 비밀번호를 넣어 주세요</p>
          {snsNotice && !error && (
            <div className={`msg ${snsNotice.tone}`} role={snsNotice.tone === 'err' ? 'alert' : 'status'}>
              <Icon name={snsNotice.tone === 'err' ? 'alert' : 'okc'} /><span>{snsNotice.text}</span>
            </div>
          )}
          {registered && !error && (
            <div className="msg good" role="status"><Icon name="okc" />가입이 끝났습니다. 가입한 이메일로 로그인해 주세요.</div>
          )}
          <div className="fld">
            <Label htmlFor="login-email" className="wj-reset fl">이메일</Label>
            <Input
              id="login-email"
              className="wj-reset in"
              type="email"
              autoComplete="username"
              placeholder="name@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="fld">
            <Label htmlFor="login-password" className="wj-reset fl">비밀번호</Label>
            <Input
              ref={passwordRef}
              id="login-password"
              className="wj-reset in"
              type="password"
              autoComplete="current-password"
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          {error && (
            <div className="msg err" role="alert"><Icon name="alert" /><span>{error}</span></div>
          )}
          <Button type="submit" className="wj-reset bigb w" disabled={loading}>
            {loading ? '로그인 중' : '로그인'}
          </Button>
          <SnsButtons place="login" returnTo={from} />
          <div className="forgot">
            <div className="bx2">
              <b>비밀번호를 잊으셨나요?</b>
              가입한 이메일을 적어 <a href={`mailto:${COMPANY.email}`}>{COMPANY.email}</a> 으로 보내 주시면 확인 뒤 다시 설정해 드립니다.
            </div>
          </div>
        </form>

        <JoinIntro image={IMAGES.login} tag="USFK 입찰 첫걸음" title={<>아직 회원이 아니신가요?<br />가입은 무료입니다</>}>
          <Link className="bigb o" to="/register">무료 회원가입</Link>
        </JoinIntro>
      </div>
    </div>
  )
}
