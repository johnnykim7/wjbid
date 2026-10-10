/**
 * CR-124 — FAQ 페이지.
 * 미군(USFK)·연방 조달 입찰 관련 자주 묻는 질문 아코디언.
 * BE 미연동 — 아래 FAQS는 더미 데이터(플레이스홀더).
 */
import { Link } from 'react-router-dom'
import {
  Accordion,
  AccordionItem,
  AccordionTrigger,
  AccordionContent,
} from '@therecommerce-develop/ds-ui'

// 더미 데이터 (BE 미연동)
const FAQS = [
  {
    q: 'SAM.gov 등록은 어떻게 하나요?',
    a: 'SAM.gov(System for Award Management)에서 사업자 등록(Entity Registration)을 진행해야 미 연방 조달 입찰에 참여할 수 있습니다. 등록에는 UEI와 CAGE 코드가 필요하며, WJbid에서 등록 절차를 단계별로 안내해 드립니다. 신규 등록은 보통 7~14일이 소요됩니다.',
  },
  {
    q: 'UEI와 CAGE 코드는 무엇인가요?',
    a: 'UEI(Unique Entity Identifier)는 SAM.gov에서 발급하는 12자리 고유 사업자 식별자로 DUNS 번호를 대체합니다. CAGE(Commercial and Government Entity) 코드는 미 국방 조달에 필요한 5자리 코드입니다. 두 코드 모두 입찰 참여 전 발급이 완료되어야 합니다.',
  },
  {
    q: 'NAICS 코드는 왜 중요한가요?',
    a: 'NAICS(North American Industry Classification System) 코드는 사업 분야를 분류하는 코드로, 공고가 요구하는 NAICS와 회사 등록 NAICS가 일치해야 입찰 자격이 인정됩니다. WJbid는 프로필의 NAICS를 기준으로 적합한 공고를 추천합니다.',
  },
  {
    q: '제안서 작성에는 얼마나 걸리나요?',
    a: '요구사항 분석과 초안 작성에 약 1~2일, 입찰 전문가 검토에 1일이 소요됩니다. 마감일 임박 공고는 작업이 어려울 수 있으니, 최소 마감 D-3 이전 신청을 권장합니다.',
  },
  {
    q: '어떤 서류를 준비해야 하나요?',
    a: '공고마다 다르지만 일반적으로 회사 프로필, 과거 수행 실적(Past Performance), 관련 자격증·인증서가 필요합니다. 공고별 필수 서류는 시스템이 자동 추출하여 안내하며, 고객이 업로드해야 하는 항목만 별도로 표시됩니다.',
  },
  {
    q: '제안서 품질은 보장되나요?',
    a: '초안은 SAM.gov 공고 요구사항(PWS/Instructions)을 정밀 분석하여 작성됩니다. 이후 입찰 전문가가 내용을 검토·보완하여 제출 전 최종 품질을 확인합니다.',
  },
  {
    q: '환불 정책은 어떻게 되나요?',
    a: '제안서 작성이 시작되기 전(요구사항 분석 단계 진입 전)에는 전액 환불이 가능합니다. 작성이 진행된 이후에는 진행 단계에 따라 부분 환불이 적용됩니다. 자세한 내용은 고객센터로 문의해 주세요.',
  },
  {
    q: '입찰 결과는 어떻게 확인하나요?',
    a: '제출 이후 진행 상황과 낙찰(AWARDED)·미낙찰(NOT_AWARDED) 결과를 마이페이지와 알림으로 실시간 확인할 수 있습니다. 결과 통보 시 이메일 알림도 함께 발송됩니다.',
  },
]

export default function FaqPage() {
  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-ink">자주 묻는 질문</h1>
        <p className="mt-2 text-ink-mute">
          미군(USFK)·연방 조달 입찰에 대해 자주 묻는 질문을 모았습니다.
        </p>
      </div>

      {/* FAQ 아코디언 */}
      <div className="max-w-3xl mx-auto px-6 py-12">
        <Accordion type="single" collapsible className="space-y-4">
          {FAQS.map((faq, idx) => (
            <AccordionItem
              key={idx}
              value={`faq-${idx}`}
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

        <p className="text-center text-xs text-ink-mute mt-8">
          원하는 답을 찾지 못하셨나요?{' '}
          <Link to="/support" className="text-brand font-medium hover:underline">
            1:1 문의하기
          </Link>
        </p>
      </div>
    </div>
  )
}
