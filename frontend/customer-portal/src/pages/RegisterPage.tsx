import { useRef, useState, type ComponentProps, type ReactNode } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Input, Label, Button, Checkbox } from '@therecommerce-develop/ds-ui'
import { register, EMAIL_ALREADY_EXISTS } from '../api/client'
import { Icon } from '../components/layout/Icon'
import { JoinIntro } from '../components/auth/JoinIntro'
import { IMAGES } from '../data/homeContent'
import '../wjbid-auth.css'

type FieldKey = 'email' | 'password' | 'passwordConfirm' | 'companyName' | 'businessRegistrationNumber' | 'contactPerson' | 'phone'
type ErrorKey = FieldKey | 'agree'

// 서버 가입 검증(RegisterRequest)과 같은 값이다
const PASSWORD_MIN = 8
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const REQUIRED = '꼭 넣어 주세요'
// 화면에서 위에서 아래로 놓인 순서. 첫 오류 칸으로 옮길 때 쓴다
const ERROR_ORDER: ErrorKey[] = ['email', 'password', 'passwordConfirm', 'companyName', 'businessRegistrationNumber', 'contactPerson', 'phone', 'agree']
const COUNT_WORD = ['', '한', '두', '세', '네', '다섯', '여섯', '일곱', '여덟']

function Fld({ id, label, error, hint, ...input }: ComponentProps<typeof Input> & { id: string; label: string; error?: ReactNode; hint?: string }) {
  const hintId = `${id}-hint`
  return (
    <div className="fld">
      <Label htmlFor={id} className="wj-reset fl">{label} <span className="rq">*</span></Label>
      <Input
        id={id}
        className="wj-reset in"
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? hintId : undefined}
        {...input}
      />
      {error ? (
        <div id={hintId} className="hint bad"><Icon name="alert" />{error}</div>
      ) : hint ? (
        <div id={hintId} className="hint">{hint}</div>
      ) : null}
    </div>
  )
}

/** 회원가입 (CR-131). 화면 정본은 docs/기록/토론_WJbid홈페이지방향/자료/시안/AIDA_색상/로그인_회원가입_시안.html #reg */
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
  // 가입 동의 (CR-124). 약관·개인정보 필수, 소식 받기 선택
  const [agree, setAgree] = useState({ terms: false, privacy: false, marketing: false })
  const [errors, setErrors] = useState<Partial<Record<ErrorKey, string>>>({})
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

  const setAgreeAll = (next: typeof agree) => {
    setAgree(next)
    if (next.terms && next.privacy) setErrors((e) => ({ ...e, agree: undefined }))
  }
  const allAgreed = agree.terms && agree.privacy && agree.marketing
  const toggleAll = () => setAgreeAll({ terms: !allAgreed, privacy: !allAgreed, marketing: !allAgreed })

  const validate = () => {
    const e: Partial<Record<ErrorKey, string>> = {}
    const email = form.email.trim()
    if (!email) e.email = REQUIRED
    else if (!EMAIL_PATTERN.test(email)) e.email = '이메일 형식이 맞지 않습니다'
    if (form.password.length < PASSWORD_MIN) e.password = `${PASSWORD_MIN}자 이상 넣어 주세요`
    if (!form.passwordConfirm) e.passwordConfirm = REQUIRED
    else if (form.passwordConfirm !== form.password) e.passwordConfirm = '비밀번호가 같지 않습니다'
    const company: FieldKey[] = ['companyName', 'businessRegistrationNumber', 'contactPerson', 'phone']
    company.forEach((k) => {
      if (!form[k].trim()) e[k] = REQUIRED
    })
    if (!agree.terms || !agree.privacy) e.agree = '필수 약관 두 가지에 동의해 주세요'
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
      const res = (err as { response?: { status?: number; data?: { error?: string } } }).response
      if (res?.status === 409 && res.data?.error === EMAIL_ALREADY_EXISTS) {
        setEmailTaken(true)
        moveTo('email')
      } else {
        setServerFailed(true)
      }
    } finally {
      setLoading(false)
    }
  }

  const errorCount = ERROR_ORDER.filter((k) => errors[k]).length
  const emailError = emailTaken ? <>이미 가입된 이메일입니다.<Link to="/login">로그인하기</Link></> : errors.email

  return (
    <div className="wj au-pg">
      <div className="au r">
        <form className="card reg" onSubmit={handleSubmit} noValidate>
          <h1>회원가입</h1>
          <p className="lead">무료 회원으로 가입하면 마감 전 공고를 한글로 보고 관심 공고를 저장할 수 있습니다</p>

          <div className="part">
            <h2><span className="no">1</span>계정</h2>
            <Fld
              ref={(el: HTMLInputElement | null) => { refs.current.email = el }}
              id="reg-email" label="이메일" type="email" autoComplete="email" placeholder="name@company.com"
              value={form.email} onChange={(e) => update('email', e.target.value)} error={emailError}
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

          <div className="part">
            <h2><span className="no">2</span>회사 정보</h2>
            <div className="cols2">
              <Fld
                ref={(el: HTMLInputElement | null) => { refs.current.companyName = el }}
                id="reg-company" label="회사명" autoComplete="organization" placeholder="예: 한빛환경"
                value={form.companyName} onChange={(e) => update('companyName', e.target.value)} error={errors.companyName}
              />
              <Fld
                ref={(el: HTMLInputElement | null) => { refs.current.businessRegistrationNumber = el }}
                id="reg-brn" label="사업자등록번호" inputMode="numeric" placeholder="123-45-67890"
                value={form.businessRegistrationNumber} onChange={(e) => update('businessRegistrationNumber', e.target.value)}
                error={errors.businessRegistrationNumber}
              />
            </div>
            <div className="cols2">
              <Fld
                ref={(el: HTMLInputElement | null) => { refs.current.contactPerson = el }}
                id="reg-contact" label="담당자명" autoComplete="name" placeholder="예: 김민수"
                value={form.contactPerson} onChange={(e) => update('contactPerson', e.target.value)} error={errors.contactPerson}
              />
              <Fld
                ref={(el: HTMLInputElement | null) => { refs.current.phone = el }}
                id="reg-phone" label="전화번호" type="tel" autoComplete="tel" placeholder="010-1234-5678"
                value={form.phone} onChange={(e) => update('phone', e.target.value)} error={errors.phone}
              />
            </div>
          </div>

          <div className="part">
            <h2><span className="no">3</span>약관 동의</h2>
            <div className={`agr${errors.agree ? ' bad' : ''}`}>
              <div className="arow aall">
                <Checkbox id="agree-all" className="wj-reset ck" checked={allAgreed} onCheckedChange={toggleAll} />
                <Label htmlFor="agree-all" className="wj-reset atx">전체 동의</Label>
              </div>
              <div className="arow">
                <Checkbox
                  ref={(el: HTMLButtonElement | null) => { refs.current.agree = el }}
                  id="agree-terms" className="wj-reset ck" checked={agree.terms}
                  onCheckedChange={(c) => setAgreeAll({ ...agree, terms: c === true })}
                />
                <Label htmlFor="agree-terms" className="wj-reset atx"><em className="req">[필수]</em>이용약관에 동의합니다</Label>
                <Link to="/terms" target="_blank" rel="noopener">보기<Icon name="cr" /></Link>
              </div>
              <div className="arow">
                <Checkbox
                  id="agree-privacy" className="wj-reset ck" checked={agree.privacy}
                  onCheckedChange={(c) => setAgreeAll({ ...agree, privacy: c === true })}
                />
                <Label htmlFor="agree-privacy" className="wj-reset atx"><em className="req">[필수]</em>개인정보처리방침에 동의합니다</Label>
                <Link to="/privacy" target="_blank" rel="noopener">보기<Icon name="cr" /></Link>
              </div>
              <div className="arow">
                <Checkbox
                  id="agree-marketing" className="wj-reset ck" checked={agree.marketing}
                  onCheckedChange={(c) => setAgreeAll({ ...agree, marketing: c === true })}
                />
                <Label htmlFor="agree-marketing" className="wj-reset atx"><em className="opt">[선택]</em>새 공고·서비스 소식 받기</Label>
              </div>
            </div>
            {errors.agree && <div className="hint bad"><Icon name="alert" />{errors.agree}</div>}
          </div>

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
