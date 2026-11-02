import { useRef, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { resetPassword, authErrorOf, AUTH_ERROR } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { EmailCodeField } from '../components/auth/EmailCodeField'
import { Fld } from '../components/auth/AuthFields'
import { REQUIRED, EMAIL_PATTERN, PASSWORD_MIN } from '../components/auth/authRules'
import '../wjbid-auth.css'

type FieldKey = 'email' | 'password' | 'passwordConfirm'

// 화면에서 위에서 아래로 놓인 순서. 첫 오류 칸으로 옮길 때 쓴다
const ERROR_ORDER: FieldKey[] = ['email', 'password', 'passwordConfirm']

/**
 * 비밀번호 재설정 (CR-186, BID-AUTH-009). 가입한 이메일의 재설정 인증번호를 확인해야 새 비밀번호 칸이 열린다
 * (T3-4 로그인·회원가입 시나리오 9).
 */
export default function PasswordResetPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<Record<FieldKey, string>>({ email: '', password: '', passwordConfirm: '' })
  const [errors, setErrors] = useState<Partial<Record<FieldKey, string>>>({})
  const [emailVerified, setEmailVerified] = useState(false)
  const [serverFailed, setServerFailed] = useState(false)
  const [loading, setLoading] = useState(false)
  const refs = useRef<Partial<Record<FieldKey, HTMLElement | null>>>({})

  const moveTo = (key: FieldKey) => {
    const el = refs.current[key]
    if (!el) return
    el.focus({ preventScroll: true })
    el.scrollIntoView({ block: 'center' })
  }

  const update = (key: FieldKey, value: string) => {
    setForm((f) => ({ ...f, [key]: value }))
    setErrors((e) => ({ ...e, [key]: undefined }))
  }

  const validate = () => {
    const e: Partial<Record<FieldKey, string>> = {}
    const email = form.email.trim()
    if (!email) e.email = REQUIRED
    else if (!EMAIL_PATTERN.test(email)) e.email = '이메일 형식이 맞지 않습니다'
    else if (!emailVerified) e.email = '이메일 인증을 마쳐 주세요'
    if (form.password.length < PASSWORD_MIN) e.password = `${PASSWORD_MIN}자 이상 넣어 주세요`
    if (!form.passwordConfirm) e.passwordConfirm = REQUIRED
    else if (form.passwordConfirm !== form.password) e.passwordConfirm = '비밀번호가 같지 않습니다'
    return e
  }

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault()
    setServerFailed(false)
    const found = validate()
    setErrors(found)
    const first = ERROR_ORDER.find((k) => found[k])
    if (first) {
      moveTo(first)
      return
    }

    setLoading(true)
    const email = form.email.trim()
    try {
      await resetPassword(email, form.password)
      navigate('/login', { state: { passwordReset: true, email } })
    } catch (err: unknown) {
      // 서버 문장을 그대로 보이지 않고 오류 코드만 보고 안내를 고른다
      const { body } = authErrorOf(err)
      if (body.error === AUTH_ERROR.EMAIL_NOT_VERIFIED) {
        setEmailVerified(false)
        setErrors((e) => ({ ...e, email: '이메일 인증을 다시 해 주세요' }))
        moveTo('email')
      } else {
        setServerFailed(true)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="wj au-pg">
      <div className="au one">
        <form className="card lgn" onSubmit={handleSubmit} noValidate>
          <h1>비밀번호 재설정</h1>
          <p className="lead">가입한 이메일로 인증번호를 받아 새 비밀번호를 정합니다</p>

          <div className="part">
            <h2><span className="no">1</span>이메일 확인</h2>
            <EmailCodeField
              id="reset-email"
              purpose="PASSWORD_RESET"
              email={form.email}
              onEmailChange={(v) => update('email', v)}
              verified={emailVerified}
              onVerifiedChange={(v) => { setEmailVerified(v); if (v) setErrors((e) => ({ ...e, email: undefined })) }}
              onSent={() => setErrors((e) => ({ ...e, email: undefined }))}
              error={errors.email}
              inputRef={(el) => { refs.current.email = el }}
            />
          </div>

          <div className="part">
            <h2><span className="no">2</span>새 비밀번호</h2>
            <Fld
              ref={(el: HTMLInputElement | null) => { refs.current.password = el }}
              id="reset-password" label="새 비밀번호" type="password" autoComplete="new-password" placeholder={`${PASSWORD_MIN}자 이상`}
              disabled={!emailVerified}
              value={form.password} onChange={(e) => update('password', e.target.value)}
              error={errors.password} hint={`${PASSWORD_MIN}자 이상`}
            />
            <Fld
              ref={(el: HTMLInputElement | null) => { refs.current.passwordConfirm = el }}
              id="reset-password-confirm" label="새 비밀번호 확인" type="password" autoComplete="new-password" placeholder="한 번 더 입력"
              disabled={!emailVerified}
              value={form.passwordConfirm} onChange={(e) => update('passwordConfirm', e.target.value)} error={errors.passwordConfirm}
            />
          </div>

          {serverFailed && (
            <div className="msg err" role="alert"><Icon name="alert" /><span>비밀번호를 바꾸지 못했습니다. 잠시 뒤 다시 시도해 주세요.</span></div>
          )}
          <Button type="submit" className="wj-reset bigb w" disabled={loading}>
            {loading ? '바꾸는 중' : '비밀번호 바꾸기'}
          </Button>
          <div className="forgot">
            <div className="bx2"><Link to="/login">로그인으로 돌아가기</Link></div>
          </div>
        </form>
      </div>
    </div>
  )
}
