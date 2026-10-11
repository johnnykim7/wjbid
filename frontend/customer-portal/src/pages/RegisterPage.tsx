import { useRef, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { register, authErrorOf, AUTH_ERROR, EMAIL_ALREADY_EXISTS } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { JoinIntro } from '../components/auth/JoinIntro'
import { SnsButtons } from '../components/auth/SnsButtons'
import { EmailCodeField } from '../components/auth/EmailCodeField'
import { Fld, CompanyPart, AgreePart, EmailTaken } from '../components/auth/AuthFields'
import {
  REQUIRED, EMAIL_PATTERN, COUNT_WORD, COMPANY_KEYS, AGREE_REQUIRED,
  type Agree, type CompanyKey,
} from '../components/auth/authRules'
import { IMAGES } from '../data/homeContent'
import '../wjbid-auth.css'

type FieldKey = 'email' | 'password' | 'passwordConfirm' | CompanyKey
type ErrorKey = FieldKey | 'agree'

// 서버 가입 검증(RegisterRequest)과 같은 값이다
const PASSWORD_MIN = 8
// 화면에서 위에서 아래로 놓인 순서. 첫 오류 칸으로 옮길 때 쓴다
const ERROR_ORDER: ErrorKey[] = ['email', 'password', 'passwordConfirm', ...COMPANY_KEYS, 'agree']

/**
 * 회원가입 (CR-131). 화면 정본은 docs/기록/토론_WJbid홈페이지방향/자료/시안/AIDA_색상/로그인_회원가입_시안.html #reg
 * 이메일은 인증번호 확인을 마쳐야 가입된다 (CR-146 T3-4 시나리오 5).
 */
export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<Record<FieldKey, string>>({
    email: '',
    password: '',
    passwordConfirm: '',
    companyName: '',
    businessRegistrationNumber: '',
    contactPerson: '',
    phone: '',
  })
  const [agree, setAgree] = useState<Agree>({ terms: false, privacy: false, marketing: false })
  const [errors, setErrors] = useState<Partial<Record<ErrorKey, string>>>({})
  const [emailVerified, setEmailVerified] = useState(false)
  const [emailTaken, setEmailTaken] = useState(false)
  const [serverFailed, setServerFailed] = useState(false)
  const [loading, setLoading] = useState(false)
  const refs = useRef<Partial<Record<ErrorKey, HTMLElement | null>>>({})

  const moveTo = (key: ErrorKey) => {
    const el = refs.current[key]
    if (!el) return
    el.focus({ preventScroll: true })
    el.scrollIntoView({ block: 'center' })
  }

  const update = (key: FieldKey, value: string) => {
    setForm((f) => ({ ...f, [key]: value }))
    setErrors((e) => ({ ...e, [key]: undefined }))
    if (key === 'email') setEmailTaken(false)
  }

  const setAgreeAll = (next: Agree) => {
    setAgree(next)
    if (next.terms && next.privacy) setErrors((e) => ({ ...e, agree: undefined }))
  }

  const validate = () => {
    const e: Partial<Record<ErrorKey, string>> = {}
    const email = form.email.trim()
    if (!email) e.email = REQUIRED
    else if (!EMAIL_PATTERN.test(email)) e.email = '이메일 형식이 맞지 않습니다'
    else if (!emailVerified) e.email = '이메일 인증을 마쳐 주세요'
    if (form.password.length < PASSWORD_MIN) e.password = `${PASSWORD_MIN}자 이상 넣어 주세요`
    if (!form.passwordConfirm) e.passwordConfirm = REQUIRED
    else if (form.passwordConfirm !== form.password) e.passwordConfirm = '비밀번호가 같지 않습니다'
    COMPANY_KEYS.forEach((k) => {
      if (!form[k].trim()) e[k] = REQUIRED
    })
    if (!agree.terms || !agree.privacy) e.agree = AGREE_REQUIRED
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
    try {
      await register({
        email: form.email.trim(),
        password: form.password,
        companyName: form.companyName.trim(),
        businessRegistrationNumber: form.businessRegistrationNumber.trim(),
        contactPerson: form.contactPerson.trim(),
        phone: form.phone.trim(),
        agreeTerms: agree.terms,
        agreePrivacy: agree.privacy,
        agreeMarketing: agree.marketing,
      })
      navigate('/login', { state: { registered: true } })
    } catch (err: unknown) {
      // 서버 문장을 그대로 보이지 않고 오류 코드만 보고 안내를 고른다
      const { status, body } = authErrorOf(err)
      if (status === 409 && body.error === EMAIL_ALREADY_EXISTS) {
        setEmailTaken(true)
        moveTo('email')
      } else if (body.error === AUTH_ERROR.EMAIL_NOT_VERIFIED) {
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

  const errorCount = ERROR_ORDER.filter((k) => errors[k]).length
  const emailError = emailTaken ? <EmailTaken /> : errors.email

  return (
    <div className="wj au-pg">
      <div className="au r">
        <form className="card reg" onSubmit={handleSubmit} noValidate>
          <h1>회원가입</h1>
          <p className="lead">무료 회원으로 가입하면 마감 전 공고를 한글로 보고 관심 공고를 저장할 수 있습니다</p>
          <SnsButtons place="register" />

          <div className="part">
            <h2><span className="no">1</span>계정</h2>
            <EmailCodeField
              id="reg-email"
              email={form.email}
              onEmailChange={(v) => update('email', v)}
              verified={emailVerified}
              onVerifiedChange={(v) => { setEmailVerified(v); if (v) setErrors((e) => ({ ...e, email: undefined })) }}
              onSent={() => setErrors((e) => ({ ...e, email: undefined }))}
              error={emailError}
              inputRef={(el) => { refs.current.email = el }}
            />
            <div className="cols2">
              <Fld
                ref={(el: HTMLInputElement | null) => { refs.current.password = el }}
                id="reg-password" label="비밀번호" type="password" autoComplete="new-password" placeholder={`${PASSWORD_MIN}자 이상`}
                value={form.password} onChange={(e) => update('password', e.target.value)}
                error={errors.password} hint={`${PASSWORD_MIN}자 이상`}
              />
              <Fld
                ref={(el: HTMLInputElement | null) => { refs.current.passwordConfirm = el }}
                id="reg-password-confirm" label="비밀번호 확인" type="password" autoComplete="new-password" placeholder="한 번 더 입력"
                value={form.passwordConfirm} onChange={(e) => update('passwordConfirm', e.target.value)} error={errors.passwordConfirm}
              />
            </div>
          </div>

          <CompanyPart
            no={2} idPrefix="reg" values={form} errors={errors}
            onChange={update} setRef={(k, el) => { refs.current[k] = el }}
          />
          <AgreePart
            no={3} agree={agree} onChange={setAgreeAll} error={errors.agree}
            setRef={(el) => { refs.current.agree = el }}
          />

          {errorCount > 0 && (
            <div className="msg err" role="alert"><Icon name="alert" /><span>빨간 칸 {COUNT_WORD[errorCount]} 곳을 고쳐 주세요.</span></div>
          )}
          {serverFailed && (
            <div className="msg err" role="alert"><Icon name="alert" /><span>가입을 마치지 못했습니다. 잠시 뒤 다시 시도해 주세요.</span></div>
          )}
          <Button type="submit" className="wj-reset bigb w" disabled={loading}>
            {loading ? '가입하는 중' : '무료로 가입하기'}
          </Button>
        </form>

        <JoinIntro image={IMAGES.register} tag="가입하면 바로" title={<>가입하면 바로<br />이렇게 쓸 수 있습니다</>}>
          <div className="lg">이미 회원이신가요?<Link to="/login">로그인</Link></div>
        </JoinIntro>
      </div>
    </div>
  )
}
