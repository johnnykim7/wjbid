import type { ComponentProps, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Input, Label, Checkbox } from '@therecommerce-develop/ds-ui'
import { Icon } from '../layout/Icon'
import type { Agree, CompanyKey } from './authRules'

/** 이메일 가입과 SNS 가입이 같이 쓰는 칸들 (CR-131, CR-146) */

export function EmailTaken() {
  return <>이미 가입된 이메일입니다.<Link to="/login">로그인하기</Link></>
}

export function Fld({ id, label, error, hint, ...input }: ComponentProps<typeof Input> & { id: string; label: string; error?: ReactNode; hint?: string }) {
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

export function CompanyPart({ no, idPrefix, values, errors, onChange, setRef }: {
  no: number
  idPrefix: string
  values: Record<CompanyKey, string>
  errors: Partial<Record<CompanyKey, string>>
  onChange: (key: CompanyKey, value: string) => void
  setRef: (key: CompanyKey, el: HTMLElement | null) => void
}) {
  return (
    <div className="part">
      <h2><span className="no">{no}</span>회사 정보</h2>
      <div className="cols2">
        <Fld
          ref={(el: HTMLInputElement | null) => setRef('companyName', el)}
          id={`${idPrefix}-company`} label="회사명" autoComplete="organization" placeholder="예: 한빛환경"
          value={values.companyName} onChange={(e) => onChange('companyName', e.target.value)} error={errors.companyName}
        />
        <Fld
          ref={(el: HTMLInputElement | null) => setRef('businessRegistrationNumber', el)}
          id={`${idPrefix}-brn`} label="사업자등록번호" inputMode="numeric" placeholder="123-45-67890"
          value={values.businessRegistrationNumber} onChange={(e) => onChange('businessRegistrationNumber', e.target.value)}
          error={errors.businessRegistrationNumber}
        />
      </div>
      <div className="cols2">
        <Fld
          ref={(el: HTMLInputElement | null) => setRef('contactPerson', el)}
          id={`${idPrefix}-contact`} label="담당자명" autoComplete="name" placeholder="예: 김민수"
          value={values.contactPerson} onChange={(e) => onChange('contactPerson', e.target.value)} error={errors.contactPerson}
        />
        <Fld
          ref={(el: HTMLInputElement | null) => setRef('phone', el)}
          id={`${idPrefix}-phone`} label="전화번호" type="tel" autoComplete="tel" placeholder="010-1234-5678"
          value={values.phone} onChange={(e) => onChange('phone', e.target.value)} error={errors.phone}
        />
      </div>
    </div>
  )
}

/** 가입 동의 (CR-124). 약관·개인정보 필수, 소식 받기 선택 */
export function AgreePart({ no, agree, onChange, error, setRef }: {
  no: number
  agree: Agree
  onChange: (next: Agree) => void
  error?: string
  setRef: (el: HTMLElement | null) => void
}) {
  const allAgreed = agree.terms && agree.privacy && agree.marketing
  const toggleAll = () => onChange({ terms: !allAgreed, privacy: !allAgreed, marketing: !allAgreed })
  return (
    <div className="part">
      <h2><span className="no">{no}</span>약관 동의</h2>
      <div className={`agr${error ? ' bad' : ''}`}>
        <div className="arow aall">
          <Checkbox id="agree-all" className="wj-reset ck" checked={allAgreed} onCheckedChange={toggleAll} />
          <Label htmlFor="agree-all" className="wj-reset atx">전체 동의</Label>
        </div>
        <div className="arow">
          <Checkbox
            ref={(el: HTMLButtonElement | null) => setRef(el)}
            id="agree-terms" className="wj-reset ck" checked={agree.terms}
            onCheckedChange={(c) => onChange({ ...agree, terms: c === true })}
          />
          <Label htmlFor="agree-terms" className="wj-reset atx"><em className="req">[필수]</em>이용약관에 동의합니다</Label>
          <Link to="/terms" target="_blank" rel="noopener">보기<Icon name="cr" /></Link>
        </div>
        <div className="arow">
          <Checkbox
            id="agree-privacy" className="wj-reset ck" checked={agree.privacy}
            onCheckedChange={(c) => onChange({ ...agree, privacy: c === true })}
          />
          <Label htmlFor="agree-privacy" className="wj-reset atx"><em className="req">[필수]</em>개인정보처리방침에 동의합니다</Label>
          <Link to="/privacy" target="_blank" rel="noopener">보기<Icon name="cr" /></Link>
        </div>
        <div className="arow">
          <Checkbox
            id="agree-marketing" className="wj-reset ck" checked={agree.marketing}
            onCheckedChange={(c) => onChange({ ...agree, marketing: c === true })}
          />
          <Label htmlFor="agree-marketing" className="wj-reset atx"><em className="opt">[선택]</em>새 공고·서비스 소식 받기</Label>
        </div>
      </div>
      {error && <div className="hint bad"><Icon name="alert" />{error}</div>}
    </div>
  )
}
