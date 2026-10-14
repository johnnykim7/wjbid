/**
 * 홈 화면 정의 (CR-127). 홈의 문구, 서비스 목록, 예시를 이 한 곳에 둔다.
 * 자주 묻는 질문과 공지는 고객센터 화면과 같은 목록(data/supportContent)을 읽는다(CR-133).
 * 서비스 목록은 발주사가 서비스 후보를 고른 뒤 이 파일에서 바꾼다.
 * 회원 금액은 여기 두지 않는다. 요금 출처(hooks/usePricing)에서 읽는다.
 * 화면이 아직 없는 곳은 메뉴 항목 key 로 항목별 준비 안내에 보낸다(CR-133).
 */
import { comingSoonHref, serviceHref } from '../components/layout/menu'
import type { SitePhotoSlot } from '../hooks/useSitePhoto'

/** 첫 화면 공고 목록 줄 수와 "N일 안 마감" 기준 */
export const LIST_ROWS = 7
export const SOON_DAYS = 7

/** 사진 칸의 자리 키. 사진은 관리자 화면에서 자리마다 올린 묶음에서 방문마다 고른다(CR-153) */
export const IMAGES = {
  empty: 'HOME_EMPTY',
  intro: 'HOME_INTRO',
  login: 'LOGIN',
  register: 'REGISTER',
} satisfies Record<string, SitePhotoSlot>

/** 공고 0건일 때 보여 주는 마감된 공고 예시 */
export const SAMPLE_NOTICE = {
  caption: '마감된 공고 정리 예시',
  title: '부산지역 쓰레기 수거 및 처리 (USAG-대구)',
  number: 'W90VN926QA046',
  facts: [
    { label: '계약 방식', value: '확정고정가격' },
    { label: '계약 기간', value: '기초 1년 + 옵션 4년' },
    { label: '평가 방식', value: '최저가 (LPTA)' },
    { label: '참가 자격', value: '7가지 · 제출 서류 10건' },
  ],
}

/** 대표 문장 아래 사이트 자료 규모 한 줄. 숫자는 서버가 센 값만 쓴다 (CR-162, BIZ-082) */
export const SITE_STATS_ITEMS = (s: { federalContracts: number; notices: number; openNotices: number }) => [
  `한국 내 미군 계약 ${s.federalContracts.toLocaleString()}건`,
  `모은 공고 ${s.notices.toLocaleString()}건`,
  `마감 전 공고 ${s.openNotices.toLocaleString()}건`,
]

export const TODAY_LABEL = '오늘 확인할 것'

export const FREE_BENEFITS = ['마감 전 공고 한글로 보기', '관심 공고 저장', '입찰 신청']

export const FLOW = [
  { no: 1, name: '찾기', desc: '마감 전 공고, 알림' },
  { no: 2, name: '판단', desc: '상세 번역, 입찰 자격 진단' },
  { no: 3, name: '준비', desc: '등록 대행, 서류함, 서류 영문 번역' },
  { no: 4, name: '제출', desc: '제안서 자문·검토, 제출 전 검사' },
  { no: 5, name: '낙찰 뒤', desc: '대금 청구 대행' },
]

/** 서비스 등급 꼬리표. f 무료, m 회원, c 건별, k 컨설팅 */
export type Tier = 'f' | 'm' | 'c' | 'k'
export const TIER_LABEL: Record<Tier, string> = { f: '무료', m: '회원', c: '건별', k: '컨설팅' }
/** 범례 설명. 회원 설명은 금액이라 화면에서 요금 출처로 채운다 */
export const TIER_NOTE: Record<Exclude<Tier, 'm'>, string> = { f: '누구나', c: '건마다 견적', k: '상담 후 결정' }

export const SERVICE_MAP: { no: number; name: string; desc: string; items: { name: string; tiers: Tier[] }[] }[] = [
  {
    no: 1, name: '찾기', desc: '지금 입찰할 수 있는 공고와 곧 나올 공고를 미리 확인합니다',
    items: [
      { name: '마감 전 공고 목록', tiers: ['f'] },
      { name: '기지·업종 맞춤 새 공고 알림', tiers: ['m'] },
      { name: '카카오톡 알림', tiers: ['m'] },
      { name: '사전 공고·수요 조사 알림', tiers: ['m'] },
      { name: '변경 공고 모아 보기', tiers: ['m'] },
    ],
  },
  {
    no: 2, name: '판단', desc: '우리 회사가 이 공고에 참여할 수 있는지 판단합니다',
    items: [
      { name: '공고문 상세 번역', tiers: ['m'] },
      { name: '참가 자격 확인', tiers: ['m'] },
      { name: '공고별 판정', tiers: ['m'] },
      { name: '비슷한 공고 낙찰가 범위', tiers: ['m'] },
      { name: '기지별 발주 동향', tiers: ['f'] },
    ],
  },
  {
    no: 3, name: '준비', desc: '등록과 서류를 빠짐없이 갖춥니다',
    items: [
      { name: 'SAM.gov·UEI·NCAGE·PIEE 등록 대행', tiers: ['c'] },
      { name: 'SAM 등록 만료 알림과 갱신', tiers: ['m', 'c'] },
      { name: '회사 공통 서류함', tiers: ['m'] },
      { name: '회사 서류 영문 번역', tiers: ['c'] },
      { name: '팀 구성 협력사 찾기', tiers: ['m'] },
    ],
  },
  {
    no: 4, name: '제출', desc: '제출 전에 빠진 서류와 항목을 확인합니다',
    items: [
      { name: '제안서 자문·검토', tiers: ['c'] },
      { name: '가격 제안서 자문', tiers: ['c'] },
      { name: '제출 전 검사', tiers: ['m'] },
      { name: '계약관 영문 메일·질의 대행', tiers: ['c'] },
      { name: '현장 설명회 일정 알림', tiers: ['m'] },
    ],
  },
  {
    no: 5, name: '낙찰 뒤', desc: '계약 서류와 대금 청구를 대행합니다',
    items: [
      { name: '계약 서류·대금 청구 대행', tiers: ['c'] },
    ],
  },
]

/** 회원 혜택 카드 세 장의 예시 그림 내용. 실제 판정·예측 값이 아니다 */
export const BENEFIT_TRANSLATE = {
  title: '공고문 상세 번역',
  desc: '첨부 문서를 포함해 작업 범위, 참가 자격, 제출 서류, 평가 방식을 한글로 정리합니다.',
  free: { caption: '무료 · 기본 번역', title: '부산지역 쓰레기 수거 및 처리 (USAG-대구)', sub: '제목과 기본 정보' },
  member: { caption: '회원 · 상세 번역', items: ['작업 범위', '참가 자격 7가지', '제출 서류 10건', '평가 방식 LPTA'] },
  link: { label: '공고문 예시 보기' },
}

export const BENEFIT_FIT = {
  title: '입찰 자격 진단',
  desc: '참가 자격 확인에 남긴 답과 서류함 서류로 공고마다 참여 가능 여부와 부족한 항목을 확인할 수 있습니다.',
  company: ['폐기물 수집운반업', '유사 실적 3년', '부산'],
  rows: [
    { title: '부산지역 쓰레기 수거 및 처리', fit: 'ok', label: '참여 가능' },
    { title: '대구지역 쓰레기 수거 및 처리', fit: 'lack', label: '서류 1건 부족' },
    { title: '막사 P-6821 수리', fit: 'no', label: '면허 없음' },
  ],
  note: '화면 예시입니다',
  link: { label: '회사 정보 등록', to: '/profile' },
}

export const PLAN_FREE = {
  name: '무료',
  who: '처음 둘러보는 회사',
  items: ['마감 전 공고 한글로 보기 (제목과 기본 정보)', '관심 공고 저장', '입찰 신청', '입찰 교육 자료'],
  cta: { label: '무료로 시작', to: '/register' },
}
export const PLAN_MEMBER = {
  name: '회원',
  who: '입찰을 꾸준히 하는 회사',
  items: ['공고문 상세 번역 (첨부 포함)', '참가 자격 확인, 공고별 판정', '새 공고·카카오톡 알림', '서류함, 제출 전 검사'],
  cta: { label: '회원으로 시작', to: '/pricing' },
}
export const PLAN_CONSULT = {
  name: '대행·컨설팅',
  price: '상담 후 결정',
  who: '맡기고 싶은 일이 있는 회사',
  items: ['SAM.gov·PIEE 등록 대행', '제안서 자문·검토', '현장 설명회 동행, 기지 출입 지원', '대금 청구 대행, 서류 영문 번역'],
  cta: { label: '상담 신청', to: '/support' },
}

export const PROMOS: { tone: string; photo: SitePhotoSlot; small: string; title: string; desc: string; to: string }[] = [
  { tone: 'a', photo: 'HOME_PROMO_REGISTRATION', small: '등록 대행', title: 'SAM.gov·PIEE 등록을 대행합니다', desc: '처음 등록하는 회사도 순서대로', to: serviceHref('sam-registration') },
  { tone: 'b', photo: 'HOME_PROMO_SITE_VISIT', small: '현장 지원', title: '설명회 동행과 기지 출입', desc: '출입 절차부터 통역까지', to: serviceHref('site-visit') },
]

export const HELP_LINKS = [
  { icon: 'headset', title: '1:1 문의', desc: '궁금한 점을 남겨 주시면 답변 드립니다', to: '/support' },
  { icon: 'chat', title: '카카오톡 알림 받기', desc: '새 공고와 마감 3일 전을 카카오톡으로', to: comingSoonHref('kakao') },
  { icon: 'book', title: '이용 안내', desc: '가입부터 입찰 신청까지 순서대로', to: '/guide' },
] as const

export const RELATED_SITES = [
  { label: 'SAM.gov', href: 'https://sam.gov' },
  { label: 'PIEE', href: 'https://piee.eb.mil' },
  { label: 'DLA', href: 'https://www.dla.mil' },
  { label: 'USAspending', href: 'https://www.usaspending.gov' },
  { label: 'Acquisition.gov (FAR)', href: 'https://www.acquisition.gov/far' },
  { label: '주한미군 USFK', href: 'https://www.usfk.mil' },
]
