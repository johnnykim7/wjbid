/**
 * CR-124 — 고객센터(Support) 페이지.
 * 상단: FAQ 요약 아코디언(3~4개) + 하단: 1:1 문의 폼.
 * FAQ_SUMMARY는 더미. 1:1 문의는 서버에 접수한다(CR-132).
 * 다른 화면이 /support?type=<유형코드>&title=<제목> 으로 유형과 제목을 채워 열 수 있다(CR-133).
 */
import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  Accordion,
  AccordionItem,
  AccordionTrigger,
  AccordionContent,
  Input,
  Textarea,
  Label,
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@therecommerce-develop/ds-ui'
import { Button } from '../components/ui/button'
import { useAuth } from '../context/AuthContext'
import { getInquiryTypes, getMyProfile, submitInquiry, type InquiryTypeItem } from '../api/client'

// 더미 데이터 (BE 미연동)
const FAQ_SUMMARY = [
  {
    q: 'SAM.gov 등록은 어떻게 하나요?',
    a: 'SAM.gov에서 사업자 등록(Entity Registration)을 진행해야 입찰에 참여할 수 있습니다. UEI·CAGE 코드가 필요하며 WJbid가 절차를 안내합니다.',
  },
  {
    q: '제안서 작성에는 얼마나 걸리나요?',
    a: '분석·초안 작성 1~2일, 전문가 검토 1일이 소요됩니다. 마감 D-3 이전 신청을 권장합니다.',
  },
  {
    q: '환불은 가능한가요?',
    a: '제안서 작성 시작 전에는 전액 환불, 작성 진행 후에는 단계별 부분 환불이 적용됩니다.',
  },
  {
    q: '입찰 결과는 어떻게 확인하나요?',
    a: '제출 이후 진행 상황과 낙찰·미낙찰 결과를 마이페이지와 알림으로 실시간 확인할 수 있습니다.',
  },
]

const TITLE_MAX = 200

const EMPTY_FORM = { name: '', email: '', typeCode: '', title: '', content: '' }

export default function SupportPage() {
  const { isAuthenticated } = useAuth()
  const [params] = useSearchParams()
  const formRef = useRef<HTMLElement>(null)
  const [types, setTypes] = useState<InquiryTypeItem[]>([])
  const [typesFailed, setTypesFailed] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
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
    getInquiryTypes()
      .then((res) => {
        setTypes(res.data)
        setForm((prev) => ({
          ...prev,
          typeCode: defaultTypeCode(res.data),
          title: wantedTitle ? wantedTitle.slice(0, TITLE_MAX) : prev.title,
        }))
        if (res.data.some((t) => t.code === wantedType) || wantedTitle) {
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
    setForm((prev) => ({ ...EMPTY_FORM, name: prev.name, email: prev.email, typeCode: types[0]?.code ?? '' }))
  }

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [k]: e.target.value }))

  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-ink">고객센터</h1>
        <p className="mt-2 text-ink-mute">
          자주 묻는 질문을 먼저 확인하시고, 해결되지 않으면 1:1 문의를 남겨 주세요.
        </p>
      </div>

      <div className="max-w-3xl mx-auto px-6 py-12 space-y-12">
        {/* FAQ 요약 */}
        <section>
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-lg font-extrabold text-ink">자주 묻는 질문</h2>
            <Link to="/faq" className="text-sm text-brand font-medium hover:underline">
              전체 FAQ 보기 <i className="fa-solid fa-arrow-right text-xs ml-0.5" />
            </Link>
          </div>
          <Accordion type="single" collapsible className="space-y-4">
            {FAQ_SUMMARY.map((faq, idx) => (
              <AccordionItem
                key={idx}
                value={`sfaq-${idx}`}
                className="bg-white rounded-xl border border-line px-5"
              >
                <AccordionTrigger className="text-sm font-bold text-ink gap-2">
                  <span className="flex items-start gap-2 text-left">
                    <i className="fa-solid fa-circle-question text-brand mt-0.5" />
                    {faq.q}
                  </span>
                </AccordionTrigger>
                <AccordionContent className="text-sm text-ink-sub ml-6 leading-relaxed">
                  {faq.a}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </section>

        {/* 1:1 문의 폼 */}
        <section ref={formRef} className="scroll-mt-4">
          <h2 className="text-lg font-extrabold text-ink mb-5">1:1 문의</h2>
          {sentTo ? (
            <div className="bg-white rounded-xl border border-line p-8 text-center space-y-2">
              <i className="fa-solid fa-circle-check text-3xl text-success" />
              <p className="text-base font-bold text-ink">문의가 접수되었습니다.</p>
              <p className="text-sm text-ink-sub">답변은 {sentTo} 로 보내 드립니다.</p>
              <div className="pt-3">
                <Button type="button" variant="outline" onClick={startNew}>
                  새 문의 쓰기
                </Button>
              </div>
            </div>
          ) : (
          <form
            onSubmit={handleSubmit}
            className="bg-white rounded-xl border border-line p-6 space-y-5"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="space-y-1.5">
                <Label htmlFor="name">이름</Label>
                <Input id="name" value={form.name} onChange={set('name')} placeholder="홍길동" maxLength={100} required />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="email">이메일</Label>
                <Input
                  id="email"
                  type="email"
                  value={form.email}
                  onChange={set('email')}
                  placeholder="you@example.com"
                  maxLength={255}
                  required
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="type">문의 유형</Label>
              <Select
                value={form.typeCode}
                onValueChange={(v) => setForm((prev) => ({ ...prev, typeCode: v }))}
                disabled={types.length === 0}
              >
                <SelectTrigger id="type" className="w-full">
                  <SelectValue placeholder="문의 유형 선택" />
                </SelectTrigger>
                <SelectContent>
                  {types.map((t) => (
                    <SelectItem key={t.code} value={t.code}>
                      {t.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="title">제목</Label>
              <Input
                id="title"
                value={form.title}
                onChange={set('title')}
                placeholder="문의 제목을 입력해 주세요"
                maxLength={TITLE_MAX}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="content">내용</Label>
              <Textarea
                id="content"
                value={form.content}
                onChange={set('content')}
                rows={6}
                placeholder="문의 내용을 자세히 작성해 주세요"
                maxLength={5000}
                required
              />
            </div>

            {typesFailed && (
              <p className="text-sm text-danger">문의 유형을 불러오지 못했습니다. 잠시 뒤 다시 열어 주세요.</p>
            )}
            {sendFailed && (
              <p className="text-sm text-danger">문의를 보내지 못했습니다. 잠시 뒤 다시 보내 주세요.</p>
            )}

            <div className="flex justify-end">
              <Button type="submit" variant="accent" disabled={sending || !form.typeCode}>
                <i className="fa-solid fa-paper-plane mr-2" /> {sending ? '보내는 중' : '문의 보내기'}
              </Button>
            </div>
          </form>
          )}
        </section>
      </div>
    </div>
  )
}
