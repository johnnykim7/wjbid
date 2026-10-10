/**
 * 고객센터 화면 정의 (CR-133). 공지사항과 자주 묻는 질문을 이 한 곳에 두고 홈 도움 구역도 같은 목록을 읽는다.
 * 공지를 내려 주는 기능이 아직 없어 공지는 여기에 쓴다. 견본 공지 6건은 지웠다(사용자 10/11 06:06).
 * 자주 묻는 질문의 답은 지금 서비스가 하는 일과 공식 안내로 확인한 것만 쓴다.
 */
import { academyHref, serviceHref } from '../components/layout/menu'

export interface NoticeItem {
  id: number
  title: string
  /** YYYY-MM-DD */
  date: string
}

export const NOTICES: NoticeItem[] = []

export const NOTICE_TEXT = {
  title: '공지사항',
  desc: 'WJbid 서비스 소식과 점검 안내를 알려 드립니다',
  head: { no: '번호', title: '제목', date: '날짜' },
  emptyTitle: '아직 공지가 없습니다',
  emptyDesc: '새 소식이 생기면 이곳에 올립니다',
}

export type FaqCategory = 'join' | 'bids' | 'registration'

export const FAQ_CATEGORIES: { key: FaqCategory | 'all'; label: string }[] = [
  { key: 'all', label: '전체' },
  { key: 'join', label: '가입·요금' },
  { key: 'bids', label: '공고 보기' },
  { key: 'registration', label: '등록·서류' },
]

/** 답의 한 조각. 글자이거나 화면 링크다 */
export type AnswerPart = string | { label: string; to: string }

export interface FaqItem {
  id: string
  category: FaqCategory
  q: string
  a: AnswerPart[]
}

export const FAQ_TEXT = {
  title: '자주 묻는 질문',
  desc: '찾는 답이 없으면 1:1 문의로 남겨 주세요',
}

export const FAQS: FaqItem[] = [
  {
    id: 'sam-required',
    category: 'registration',
    q: 'SAM.gov 등록이 꼭 있어야 입찰할 수 있나요?',
    a: [
      '대부분의 미군 공고는 견적을 내거나 계약하기 전에 SAM.gov 등록을 요구합니다. 공고문의 제출 안내에서 확인하세요. 등록이 없으면 ',
      { label: 'SAM 등록 대행', to: serviceHref('sam-registration') },
      '을 신청할 수 있습니다.',
    ],
  },
  {
    id: 'uei-cage',
    category: 'registration',
    q: 'UEI 와 CAGE 코드는 무엇인가요?',
    a: [
      'UEI 는 SAM.gov 가 등록한 회사에 주는 열두 자리 번호입니다. CAGE 는 업체마다 주는 다섯 자리 코드로, 미국 밖 회사는 NCAGE 를 SAM.gov 등록 전에 받습니다. 다른 말은 ',
      { label: '용어 사전', to: academyHref('glossary') },
      '에서 찾아보세요.',
    ],
  },
  {
    id: 'sam-fee',
    category: 'registration',
    q: 'SAM.gov 등록에 비용이 드나요?',
    a: [
      'SAM.gov 등록과 갱신 자체는 무료입니다. 등록은 해마다 갱신해야 합니다. 등록이나 갱신을 맡기시면 건별 견적으로 안내합니다. ',
      { label: 'SAM 갱신', to: serviceHref('sam-renewal') },
    ],
  },
  {
    id: 'free-vs-member',
    category: 'join',
    q: '무료와 회원은 무엇이 다른가요?',
    a: [
      '무료로는 마감 전 공고를 한글로 보고, 관심 공고를 저장하고, 입찰을 신청할 수 있습니다. 회원은 공고문 상세 번역, 맞춤 판정, 알림 같은 기능을 더 씁니다. 일부는 준비하고 있습니다. 자세한 비교는 ',
      { label: '요금 안내', to: '/pricing' },
      '에서 보세요.',
    ],
  },
  {
    id: 'source',
    category: 'bids',
    q: '공고는 어디서 모아 오나요?',
    a: [
      'SAM.gov 에 올라온 주한미군 육군 계약 부서(411th CSB)의 공고를 모아 한글로 정리합니다. 공고 유형과 찾는 법은 ',
      { label: '교육 자료', to: academyHref('materials') },
      '에 있습니다.',
    ],
  },
  {
    id: 'refund',
    category: 'join',
    q: '환불은 어떻게 되나요?',
    a: ['환불 기준은 ', { label: '환불 정책', to: '/refund' }, '에 적어 두었습니다. 궁금한 점은 1:1 문의로 남겨 주세요.'],
  },
]

/** 홈 도움 구역에 보이는 질문 수 */
export const HOME_FAQ_COUNT = 6
