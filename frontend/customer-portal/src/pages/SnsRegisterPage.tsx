import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { getSnsSignup, snsRegister, authErrorOf, AUTH_ERROR, EMAIL_ALREADY_EXISTS } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { Icon } from '../components/layout/Icon'
import { JoinIntro } from '../components/auth/JoinIntro'
import { EmailCodeField } from '../components/auth/EmailCodeField'
import { CompanyPart, AgreePart, EmailTaken } from '../components/auth/AuthFields'
import {
  REQUIRED, COUNT_WORD, COMPANY_KEYS, AGREE_REQUIRED, SNS_NAME,
  type Agree, type CompanyKey,
} from '../components/auth/authRules'
import { IMAGES } from '../data/homeContent'
import '../wjbid-auth.css'

type ErrorKey = 'email' | CompanyKey | 'agree'
const ERROR_ORDER: ErrorKey[] = ['email', ...COMPANY_KEYS, 'agree']

type Signup = { provider: string; email: string | null; emailVerifiedByProvider: boolean }

/**
 * SNS 로 처음 가입 (CR-146 T3-3 「SNS 가입」, T3-4 시나리오 6).
 * SNS 가 확인한 이메일이면 인증번호 없이, 아니면 인증번호로 이메일을 확인한다. 비밀번호는 받지 않는다.
 */
export default function SnsRegisterPage() {
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const [params] = useSearchParams()
  const ticket = params.get('ticket') ?? ''

  const [signup, setSignup] = useState<Signup | null>(null)
  const [closed, setClosed] = useState<'expired' | 'failed' | null>(ticket ? null : 'expired')
  const [useOtherEmail, setUseOtherEmail] = useState(false)
  const [email, setEmail] = useState('')
  const [emailVerified, setEmailVerified] = useState(false)
  const [company, setCompany] = useState<Record<CompanyKey, string>>({
    companyName: '', businessRegistrationNumber: '', contactPerson: '', phone: '',
  })
  const [agree, setAgree] = useState<Agree>({ terms: false, privacy: false, marketing: false })
  const [errors, setErrors] = useState<Partial<Record<ErrorKey, React.ReactNode>>>({})
  const [serverFailed, setServerFailed] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const refs = useRef<Partial<Record<ErrorKey, HTMLElement | null>>>({})

  useEffect(() => {
    if (!ticket) return
    let alive = true
    getSnsSignup(ticket)
      .then(({ data }) => {
        if (!alive) return
        setSignup(data)
        setEmail(data.email ?? '')
      })
      .catch((err) => {
        if (!alive) return
        setClosed(authErrorOf(err).body.error === AUTH_ERROR.SNS_TICKET_EXPIRED ? 'expired' : 'failed')
      })
    return () => { alive = false }
  }, [ticket])

  const providerVerified = !!signup?.emailVerifiedByProvider && !!signup.email && !useOtherEmail

  const moveTo = (key: ErrorKey) => {
    const el = refs.current[key]
    if (!el) return
    el.focus({ preventScroll: true })
    el.scrollIntoView({ block: 'center' })
  }

  const updateCompany = (key: CompanyKey, value: string) => {
    setCompany((c) => ({ ...c, [key]: value }))
    setErrors((e) => ({ ...e, [key]: undefined }))
  }

  const setAgreeAll = (next: Agree) => {
    setAgree(next)
    if (next.terms && next.privacy) setErrors((e) => ({ ...e, agree: undefined }))
  }

  const switchToOtherEmail = () => {
    setUseOtherEmail(true)
    setEmail('')
    setEmailVerified(false)
  }

  const validate = () => {
    const e: Partial<Record<ErrorKey, React.ReactNode>> = {}
    if (!providerVerified) {
      if (!email.trim()) e.email = REQUIRED
      else if (!emailVerified) e.email = '이메일 인증을 마쳐 주세요'
    }
    COMPANY_KEYS.forEach((k) => {
      if (!company[k].trim()) e[k] = REQUIRED
    })
    if (!agree.terms || !agree.privacy) e.agree = AGREE_REQUIRED
    return e
  }

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault()
    setServerFailed(null)
    const found = validate()
    setErrors(found)
    const first = ERROR_ORDER.find((k) => found[k])
    if (first) {
      moveTo(first)
      return
    }

    setLoading(true)
    try {
      const { data } = await snsRegister({
        ticket,
        email: email.trim(),
        companyName: company.companyName.trim(),
        businessRegistrationNumber: company.businessRegistrationNumber.trim(),
        contactPerson: company.contactPerson.trim(),
        phone: company.phone.trim(),
        agreeTerms: agree.terms,
        agreePrivacy: agree.privacy,
        agreeMarketing: agree.marketing,
      })
      signIn(data.email ?? email.trim(), data.accessToken, data.refreshToken)
      navigate('/search', { replace: true })
    } catch (err) {
      const { status, body } = authErrorOf(err)
      if (body.error === AUTH_ERROR.SNS_TICKET_EXPIRED) {
        setClosed('expired')
      } else if (status === 409 && body.error === EMAIL_ALREADY_EXISTS) {
        setErrors((e) => ({ ...e, email: <EmailTaken /> }))
        moveTo('email')
      } else if (body.error === AUTH_ERROR.EMAIL_NOT_VERIFIED) {
        setEmailVerified(false)
        setErrors((e) => ({ ...e, email: '이메일 인증을 다시 해 주세요' }))
        moveTo('email')
      } else if (body.error === AUTH_ERROR.SNS_ACCOUNT_IN_USE) {
        setServerFailed('이 SNS 계정은 이미 가입되어 있습니다. 로그인 화면에서 SNS 로 로그인해 주세요.')
      } else {
        setServerFailed('가입을 마치지 못했습니다. 잠시 뒤 다시 시도해 주세요.')
      }
    } finally {
      setLoading(false)
    }
  }

  const intro = (
    <JoinIntro image={IMAGES.register} tag="가입하면 바로" title={<>가입하면 바로<br />이렇게 쓸 수 있습니다</>}>
      <div className="lg">이미 회원이신가요?<Link to="/login">로그인</Link></div>
    </JoinIntro>
  )

  if (closed) {
    return (
      <div className="wj au-pg">
        <div className="au r">
          <div className="card reg">
            <h1>SNS 로 가입</h1>
            <div className="msg err" role="alert">
              <Icon name="alert" />
              <span>{closed === 'expired'
                ? '다시 SNS 로 시작해 주세요.'
                : '가입 화면을 열지 못했습니다. 잠시 뒤 다시 SNS 로 시작해 주세요.'}</span>
            </div>
            <Button asChild className="wj-reset bigb w"><Link to="/login">로그인 화면으로</Link></Button>
          </div>
          {intro}
        </div>
      </div>
    )
  }

  if (!signup) {
    return (
      <div className="wj au-pg">
        <div className="au r">
          <div className="card reg"><p className="lead" role="status">가입 화면을 여는 중</p></div>
          {intro}
        </div>
      </div>
    )
  }

  const errorCount = ERROR_ORDER.filter((k) => errors[k]).length
  const providerName = SNS_NAME[signup.provider] ?? signup.provider

  return (
    <div className="wj au-pg">
      <div className="au r">
        <form className="card reg" onSubmit={handleSubmit} noValidate>
          <h1>{providerName}로 가입</h1>
          <p className="lead">회사 정보만 넣으면 가입이 끝납니다</p>

          <div className="part">
            <h2><span className="no">1</span>이메일</h2>
            {providerVerified ? (
              <div className="fld">
                <div className="fl">이메일</div>
                <div className="fixed" ref={(el) => { refs.current.email = el }} tabIndex={-1}>
                  <span className="v">{signup.email}</span>
                  <span className="by"><Icon name="okc" />{providerName}에서 확인된 이메일</span>
                  <Button type="button" variant="ghost" className="wj-reset tlnk" onClick={switchToOtherEmail}>다른 이메일 쓰기</Button>
                </div>
              </div>
            ) : (
              <EmailCodeField
                id="sns-email"
                email={email}
                onEmailChange={(v) => { setEmail(v); setErrors((e) => ({ ...e, email: undefined })) }}
                verified={emailVerified}
                onVerifiedChange={(v) => { setEmailVerified(v); if (v) setErrors((e) => ({ ...e, email: undefined })) }}
                onSent={() => setErrors((e) => ({ ...e, email: undefined }))}
                error={errors.email}
                inputRef={(el) => { refs.current.email = el }}
              />
            )}
          </div>

          <CompanyPart
            no={2} idPrefix="sns" values={company} errors={errors as Partial<Record<CompanyKey, string>>}
            onChange={updateCompany} setRef={(k, el) => { refs.current[k] = el }}
          />
          <AgreePart
            no={3} agree={agree} onChange={setAgreeAll} error={errors.agree as string | undefined}
            setRef={(el) => { refs.current.agree = el }}
          />

          {errorCount > 0 && (
            <div className="msg err" role="alert"><Icon name="alert" /><span>빨간 칸 {COUNT_WORD[errorCount]} 곳을 고쳐 주세요.</span></div>
          )}
          {serverFailed && (
            <div className="msg err" role="alert"><Icon name="alert" /><span>{serverFailed}</span></div>
          )}
          <Button type="submit" className="wj-reset bigb w" disabled={loading}>
            {loading ? '가입 처리 중' : '가입하고 시작하기'}
          </Button>
        </form>
        {intro}
      </div>
    </div>
  )
}
