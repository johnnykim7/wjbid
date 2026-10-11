/**
 * 1:1 문의. 문의는 서버에 접수한다(CR-132).
 * 다른 화면이 /support?type=<유형코드>&title=<제목> 으로 유형과 제목을 채워 열 수 있다.
 * 고객센터 속페이지 틀 안에 두고 오른쪽에 메일 안내와 바로가기를 둔다(CR-133, T3-3, 화면 정본 support).
 */
import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import {
  Button,
  Input,
  Textarea,
  Label,
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { COMPANY } from '../components/layout/siteContent'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { SubIcon } from '../components/subpage/SubIcon'
import { useAuth } from '../context/AuthContext'
import { getInquiryTypes, getMyProfile, submitInquiry, type InquiryTypeItem } from '../api/client'
import { INQUIRY_MAX, INQUIRY_TEXT } from '../data/supportContent'

const PLACE = findMenuItem('support', 'inquiry')

const EMPTY_FORM = { name: '', email: '', typeCode: '', title: '', content: '' }

/** 주소로 채운 값. 고객이 고치면 그 칸은 채운 표시를 거둔다 */
interface Prefilled {
  typeCode?: string
  title?: string
  content?: string
}

function FieldLabel({ htmlFor, children }: { htmlFor: string; children: string }) {
  return <Label htmlFor={htmlFor} className="wj-reset fl">{children}<span className="rq">*</span></Label>
}

export default function SupportPage() {
  const { isAuthenticated } = useAuth()
  const [params] = useSearchParams()
  const formRef = useRef<HTMLElement>(null)
  const [types, setTypes] = useState<InquiryTypeItem[]>([])
  const [typesFailed, setTypesFailed] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [prefilled, setPrefilled] = useState<Prefilled | null>(null)
  const [sending, setSending] = useState(false)
  const [sendFailed, setSendFailed] = useState(false)
  const [sentTo, setSentTo] = useState<string | null>(null)

  // 주소로 넘어온 유형이 쓰는 유형이면 고르고, 아니면 목록 첫 번째를 고른다
  const defaultTypeCode = (list: InquiryTypeItem[]) => {
    const wanted = params.get('type')
    return list.find((t) => t.code === wanted)?.code ?? list[0]?.code ?? ''
  }

  useEffect(() => {
    const wantedType = params.get('type')
    const wantedTitle = params.get('title')
    const wantedContent = params.get('content')
    getInquiryTypes()
      .then((res) => {
        const title = wantedTitle ? wantedTitle.slice(0, INQUIRY_MAX.title) : undefined
        const content = wantedContent ? wantedContent.slice(0, INQUIRY_MAX.content) : undefined
        const typeCode = res.data.some((t) => t.code === wantedType) ? wantedType ?? undefined : undefined
        setTypes(res.data)
        setForm((prev) => ({ ...prev, typeCode: defaultTypeCode(res.data), title: title ?? prev.title, content: content ?? prev.content }))
        if (typeCode || title || content) {
          setPrefilled({ typeCode, title, content })
          formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      })
      .catch(() => setTypesFailed(true))
    // 주소 값은 처음 열 때 한 번만 채운다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!isAuthenticated) return
    getMyProfile()
      .then((res) => {
        const me = res.data as { email?: string; contactPerson?: string; companyName?: string }
        setForm((prev) => ({
          ...prev,
          name: prev.name || me.contactPerson || me.companyName || '',
          email: prev.email || me.email || '',
        }))
      })
      .catch(() => undefined)
  }, [isAuthenticated])

  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE
  const t = INQUIRY_TEXT

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setSending(true)
    setSendFailed(false)
    submitInquiry(form)
      .then(() => setSentTo(form.email))
      .catch(() => setSendFailed(true))
      .finally(() => setSending(false))
  }

  const startNew = () => {
    setSentTo(null)
    setPrefilled(null)
    setForm((prev) => ({ ...EMPTY_FORM, name: prev.name, email: prev.email, typeCode: types[0]?.code ?? '' }))
  }

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [k]: e.target.value }))

  const filled = (k: keyof Prefilled) =>
    prefilled?.[k] !== undefined && prefilled[k] === form[k] ? ' fill' : ''

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={t.title} desc={t.desc} />
      <div className="sup">
        <section ref={formRef} className="cbx pad">
          {sentTo ? (
            <div className="done" role="status">
              <div className="icn"><Icon name="okc" /></div>
              <b>{t.doneTitle}</b>
              <span>{t.doneDesc(sentTo)}</span>
              <Button type="button" className="wj-reset bigb o" onClick={startNew}>{t.again}</Button>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              {prefilled && (
                <div className="msg good" role="status"><Icon name="okc" />{t.prefilled}</div>
              )}
              <div className="cols2">
                <div className="fld">
                  <FieldLabel htmlFor="inq-name">{t.label.name}</FieldLabel>
                  <Input id="inq-name" className="wj-reset in" value={form.name} onChange={set('name')} placeholder={t.placeholder.name} maxLength={INQUIRY_MAX.name} required />
                </div>
                <div className="fld">
                  <FieldLabel htmlFor="inq-email">{t.label.email}</FieldLabel>
                  <Input id="inq-email" className="wj-reset in" type="email" value={form.email} onChange={set('email')} placeholder={t.placeholder.email} maxLength={INQUIRY_MAX.email} required />
                </div>
              </div>
              <div className="fld">
                <FieldLabel htmlFor="inq-type">{t.label.type}</FieldLabel>
                {/* 폼 안의 고르기 칸은 숨은 select 에 값을 먼저 넣는데, 그때 유형 목록이 아직 없으면 빈 값으로 되돌린다.
                    목록이 온 뒤 새로 그려 값과 목록을 한 번에 받게 한다 */}
                <Select
                  key={types.length > 0 ? 'loaded' : 'loading'}
                  value={form.typeCode}
                  onValueChange={(v) => setForm((prev) => ({ ...prev, typeCode: v }))}
                  disabled={types.length === 0}
                >
                  <SelectTrigger id="inq-type" className={`wj-reset in sel2${filled('typeCode')}`}>
                    <SelectValue placeholder={t.placeholder.type} />
                  </SelectTrigger>
                  <SelectContent>
                    {types.map((ty) => <SelectItem key={ty.code} value={ty.code}>{ty.label}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="fld">
                <FieldLabel htmlFor="inq-title">{t.label.title}</FieldLabel>
                <Input id="inq-title" className={`wj-reset in${filled('title')}`} value={form.title} onChange={set('title')} placeholder={t.placeholder.title} maxLength={INQUIRY_MAX.title} required />
              </div>
              <div className="fld">
                <FieldLabel htmlFor="inq-content">{t.label.content}</FieldLabel>
                <Textarea id="inq-content" className={`wj-reset in ta${filled('content')}`} value={form.content} onChange={set('content')} placeholder={t.placeholder.content} maxLength={INQUIRY_MAX.content} required />
              </div>
              {typesFailed && <div className="msg err" role="alert"><Icon name="alert" /><span>{t.typesFailed}</span></div>}
              {sendFailed && <div className="msg err" role="alert"><Icon name="alert" /><span>{t.sendFailed}</span></div>}
              <Button type="submit" className="wj-reset bigb w" disabled={sending || !form.typeCode}>
                {sending ? t.sending : t.submit}
              </Button>
            </form>
          )}
        </section>
        <aside className="sd">
          <div className="cbx">
            <b className="xtx">{t.mailTitle}</b>
            <p>{t.mailDesc}</p>
            <a className="ml" href={`mailto:${COMPANY.email}`}><SubIcon name="mail" />{COMPANY.email}</a>
          </div>
          <div className="cbx">
            <b className="xtx">{t.shortcutTitle}</b>
            <ul>
              {t.shortcuts.map((s) => (
                <li key={s.to}><Link to={s.to}>{s.label}<Icon name="cr" /></Link></li>
              ))}
            </ul>
          </div>
        </aside>
      </div>
    </SubPageFrame>
  )
}
