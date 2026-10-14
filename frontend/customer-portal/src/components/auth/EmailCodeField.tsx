import { useEffect, useState, type ReactNode } from 'react'
import { Input, Label, Button } from '@therecommerce-develop/ds-ui'
import { sendEmailCode, verifyEmailCode, authErrorOf, AUTH_ERROR, EMAIL_ALREADY_EXISTS } from '../../api/client'
import { Icon } from '../layout/Icon'
import { EMAIL_PATTERN, REQUIRED } from './authRules'
import { EmailTaken } from './AuthFields'

const CODE_PATTERN = /^\d{6}$/

const clock = (seconds: number) => `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`

/**
 * 이메일 칸과 인증번호 받기, 확인 (CR-146 T3-4 시나리오 5).
 * 남은 시간과 다시 받기 간격은 서버가 준 초에서 줄여 보인다. 정책값을 화면에 두지 않는다.
 */
export function EmailCodeField({ id, email, onEmailChange, verified, onVerifiedChange, onSent, error, inputRef }: {
  id: string
  email: string
  onEmailChange: (value: string) => void
  verified: boolean
  onVerifiedChange: (value: boolean) => void
  /** 번호를 보냈을 때. 화면이 "인증을 마쳐 주세요" 같은 앞 안내를 지울 때 쓴다 */
  onSent?: () => void
  error?: ReactNode
  inputRef?: (el: HTMLInputElement | null) => void
}) {
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [expiresAt, setExpiresAt] = useState(0)
  const [resendAt, setResendAt] = useState(0)
  const [now, setNow] = useState(() => Date.now())
  const [code, setCode] = useState('')
  const [sendError, setSendError] = useState<ReactNode>(null)
  const [codeError, setCodeError] = useState('')
  const [locked, setLocked] = useState(false)
  const [sending, setSending] = useState(false)
  const [checking, setChecking] = useState(false)

  const ticking = !verified && (sentTo !== null || resendAt > now)
  useEffect(() => {
    if (!ticking) return
    const t = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(t)
  }, [ticking])

  const remain = Math.max(0, Math.ceil((expiresAt - now) / 1000))
  const resendIn = Math.max(0, Math.ceil((resendAt - now) / 1000))
  const expired = sentTo !== null && remain === 0

  const reset = () => {
    setSentTo(null)
    setCode('')
    setCodeError('')
    setLocked(false)
  }

  const changeEmail = (value: string) => {
    onEmailChange(value)
    setSendError(null)
    if (sentTo !== null) reset()
  }

  const send = async () => {
    const target = email.trim()
    if (!target) return setSendError(REQUIRED)
    if (!EMAIL_PATTERN.test(target)) return setSendError('이메일 형식이 맞지 않습니다')
    setSending(true)
    setSendError(null)
    try {
      const { data } = await sendEmailCode(target)
      const at = Date.now()
      setSentTo(target)
      setExpiresAt(at + data.expiresInSeconds * 1000)
      setResendAt(at + data.resendAfterSeconds * 1000)
      setNow(at)
      setCode('')
      setCodeError('')
      setLocked(false)
      onSent?.()
    } catch (err) {
      const { status, body } = authErrorOf(err)
      if (status === 409 && body.error === EMAIL_ALREADY_EXISTS) setSendError(<EmailTaken />)
      else if (body.error === AUTH_ERROR.EMAIL_CODE_TOO_SOON) {
        setSendError('잠시 뒤 다시 받을 수 있습니다')
        const at = Date.now()
        setResendAt(at + (body.retryAfterSeconds ?? 0) * 1000)
        setNow(at)
      } else if (body.error === AUTH_ERROR.EMAIL_CODE_DAILY_LIMIT) {
        setSendError('오늘 받을 수 있는 횟수를 넘었습니다. 내일 다시 시도해 주세요.')
      } else {
        setSendError('인증번호를 보내지 못했습니다. 잠시 뒤 다시 시도해 주세요.')
      }
    } finally {
      setSending(false)
    }
  }

  const check = async () => {
    if (sentTo === null) return
    setChecking(true)
    setCodeError('')
    try {
      await verifyEmailCode(sentTo, code)
      onVerifiedChange(true)
    } catch (err) {
      const { body } = authErrorOf(err)
      if (body.error === AUTH_ERROR.EMAIL_CODE_MISMATCH) {
        setCodeError(`인증번호가 맞지 않습니다. ${body.remainingAttempts ?? 0}번 더 넣을 수 있습니다`)
      } else if (body.error === AUTH_ERROR.EMAIL_CODE_EXPIRED) {
        setExpiresAt(Date.now())
      } else if (body.error === AUTH_ERROR.EMAIL_CODE_LOCKED) {
        setLocked(true)
      } else {
        setCodeError('인증번호를 확인하지 못했습니다. 잠시 뒤 다시 시도해 주세요.')
      }
    } finally {
      setChecking(false)
    }
  }

  const unlock = () => {
    onVerifiedChange(false)
    reset()
    setResendAt(0)
  }

  const emailError = error ?? sendError
  const hintId = `${id}-hint`
  const codeId = `${id}-code`
  const codeMessage = locked
    ? '틀린 횟수를 넘었습니다. 인증번호를 다시 받아 주세요'
    : expired ? '인증 시간이 지났습니다. 다시 받아 주세요' : codeError

  return (
    <>
      <div className="fld">
        <Label htmlFor={id} className="wj-reset fl">이메일 <span className="rq">*</span></Label>
        <div className="inrow">
          <Input
            ref={inputRef}
            id={id}
            className="wj-reset in"
            type="email"
            autoComplete="email"
            placeholder="name@company.com"
            value={email}
            disabled={verified}
            onChange={(e) => changeEmail(e.target.value)}
            aria-invalid={emailError ? true : undefined}
            aria-describedby={emailError || verified ? hintId : undefined}
          />
          {!verified && sentTo === null && (
            <Button type="button" variant="outline" className="wj-reset subb" onClick={send} disabled={sending || resendIn > 0}>
              {sending ? '보내는 중' : '인증번호 받기'}
            </Button>
          )}
        </div>
        {verified ? (
          <div id={hintId} className="hint good">
            <Icon name="okc" />이메일이 확인되었습니다
            <Button type="button" variant="ghost" className="wj-reset tlnk" onClick={unlock}>이메일 바꾸기</Button>
          </div>
        ) : emailError ? (
          <div id={hintId} className="hint bad"><Icon name="alert" />{emailError}</div>
        ) : null}
      </div>

      {!verified && sentTo !== null && (
        <div className="fld">
          <Label htmlFor={codeId} className="wj-reset fl">인증번호 <span className="rq">*</span></Label>
          <div className="inrow">
            <div className="tmw">
              <Input
                id={codeId}
                className="wj-reset in"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                placeholder="메일로 받은 6자리"
                value={code}
                onChange={(e) => { setCode(e.target.value.replace(/\D/g, '')); setCodeError('') }}
                aria-invalid={codeMessage ? true : undefined}
                aria-describedby={`${codeId}-hint`}
              />
              {!expired && !locked && <span className="tm" aria-label="남은 시간">{clock(remain)}</span>}
            </div>
            <Button
              type="button" variant="outline" className="wj-reset subb" onClick={check}
              disabled={checking || expired || locked || !CODE_PATTERN.test(code)}
            >
              {checking ? '확인 중' : '확인'}
            </Button>
          </div>
          {codeMessage && <div className="hint bad"><Icon name="alert" />{codeMessage}</div>}
          <div id={`${codeId}-hint`} className="hint">
            메일이 오지 않으면 스팸함을 확인해 주세요
            <Button type="button" variant="ghost" className="wj-reset tlnk" onClick={send} disabled={sending || resendIn > 0}>
              인증번호 재요청{resendIn > 0 ? ` (${resendIn}초)` : ''}
            </Button>
          </div>
        </div>
      )}
    </>
  )
}
