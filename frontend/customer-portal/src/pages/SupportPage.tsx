/**
 * CR-124 — 고객센터(Support) 페이지.
 * 상단: FAQ 요약 아코디언(3~4개) + 하단: 1:1 문의 폼.
 * BE 미연동 — FAQ_SUMMARY는 더미, 폼 제출은 콘솔 출력 + alert 플레이스홀더(추후 API 연동).
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
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

const INQUIRY_TYPES = ['서비스 이용 문의', 'SAM.gov 등록 문의', '제안서 작성 문의', '결제·환불 문의', '기타']

export default function SupportPage() {
  const [form, setForm] = useState({
    name: '',
    email: '',
    type: INQUIRY_TYPES[0],
    title: '',
    content: '',
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // TODO: BE 미연동 — 추후 1:1 문의 등록 API로 교체
    console.log('[SupportPage] 1:1 문의 제출 (placeholder):', form)
    alert('문의가 접수되었습니다. 빠른 시일 내에 답변드리겠습니다.')
    setForm({ name: '', email: '', type: INQUIRY_TYPES[0], title: '', content: '' })
  }

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [k]: e.target.value }))

  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-[#0c1e3d]">고객센터</h1>
        <p className="mt-2 text-slate-500">
          자주 묻는 질문을 먼저 확인하시고, 해결되지 않으면 1:1 문의를 남겨 주세요.
        </p>
      </div>

      <div className="max-w-3xl mx-auto px-6 py-12 space-y-12">
        {/* FAQ 요약 */}
        <section>
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-lg font-extrabold text-gray-900">자주 묻는 질문</h2>
            <Link to="/faq" className="text-sm text-[#1a56db] font-medium hover:underline">
              전체 FAQ 보기 <i className="fa-solid fa-arrow-right text-xs ml-0.5" />
            </Link>
          </div>
          <Accordion type="single" collapsible className="space-y-4">
            {FAQ_SUMMARY.map((faq, idx) => (
              <AccordionItem
                key={idx}
                value={`sfaq-${idx}`}
                className="bg-white rounded-xl border border-gray-200 px-5"
              >
                <AccordionTrigger className="text-sm font-bold text-gray-900 gap-2">
                  <span className="flex items-start gap-2 text-left">
                    <i className="fa-solid fa-circle-question text-[#1a56db] mt-0.5" />
                    {faq.q}
                  </span>
                </AccordionTrigger>
                <AccordionContent className="text-sm text-gray-600 ml-6 leading-relaxed">
                  {faq.a}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </section>

        {/* 1:1 문의 폼 */}
        <section>
          <h2 className="text-lg font-extrabold text-gray-900 mb-5">1:1 문의</h2>
          <form
            onSubmit={handleSubmit}
            className="bg-white rounded-xl border border-gray-200 p-6 space-y-5"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="space-y-1.5">
                <Label htmlFor="name">이름</Label>
                <Input id="name" value={form.name} onChange={set('name')} placeholder="홍길동" required />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="email">이메일</Label>
                <Input
                  id="email"
                  type="email"
                  value={form.email}
                  onChange={set('email')}
                  placeholder="you@example.com"
                  required
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="type">문의 유형</Label>
              <Select
                value={form.type}
                onValueChange={(v) => setForm((prev) => ({ ...prev, type: v }))}
              >
                <SelectTrigger id="type" className="w-full">
                  <SelectValue placeholder="문의 유형 선택" />
                </SelectTrigger>
                <SelectContent>
                  {INQUIRY_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t}
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
                required
              />
            </div>

            <div className="flex justify-end">
              <Button type="submit" variant="accent">
                <i className="fa-solid fa-paper-plane mr-2" /> 문의 제출
              </Button>
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}
