/**
 * 메뉴 속 화면 정의 (CR-133). 속페이지 틀 문구, 등록·컨설팅 서비스 안내 7개, 항목별 준비 안내 18개를 이 한 곳에 둔다.
 * 안내 글은 서비스 후보 문구와 공식 안내(SAM.gov, NSPA, PIEE, FAR·DFARS)로 확인한 것만 쓰고, 발주사 확인은 배포 뒤 받아 고친다(사용자 10/11 06:06).
 * 금액, 기간, 일정은 정해진 값이 없어 쓰지 않는다.
 * 화면 이름은 메뉴 정의(components/layout/menu.ts)의 이름을 그대로 쓰고, 여기서는 항목 key 로 찾는다.
 */
import type { SubIconName } from '../components/subpage/SubIcon'
import { academyHref, serviceHref } from '../components/layout/menu'
import type { Tier } from './homeContent'
import type { SitePhotoSlot } from '../hooks/useSitePhoto'

export const SUB_FRAME = {
  pendingTag: '준비 중',
  call: { title: '상담 안내', desc: '궁금한 점을 남겨 주시면 이메일로 답변 드립니다', link: '1:1 문의' },
}

/** 등급 꼬리표 모양. 무료만 초록, 나머지는 파랑(화면 정본 sam, soon, class) */
export const TIER_TAG_CLASS: Record<Tier, string> = { f: 'f', m: 'c', c: 'c', k: 'c' }

/** 1:1 문의 유형 코드 (CR-132 계약, 서버 문의 유형 코드 목록과 같은 값) */
export const INQUIRY_TYPE = {
  consulting: 'CONSULTING',
  samRegistration: 'SAM_REGISTRATION',
  proposal: 'PROPOSAL',
} as const
export type InquiryType = (typeof INQUIRY_TYPE)[keyof typeof INQUIRY_TYPE]

export const inquiryHref = (type: InquiryType, title: string, content?: string) =>
  `/support?type=${type}&title=${encodeURIComponent(title)}${content ? `&content=${encodeURIComponent(content)}` : ''}`
export const consultTitle = (name: string) => `${name} 상담 신청`

/** 머리 사진 칸의 자리 키. 사진은 관리자 화면에서 자리마다 올린 묶음에서 방문마다 고른다(CR-153) */
const PHOTO = {
  desk: 'SERVICE_DESK',
  piee: 'SERVICE_ONLINE',
  meeting: 'SERVICE_MEETING',
  hall: 'SERVICE_SEMINAR',
} satisfies Record<string, SitePhotoSlot>

/* 서비스 안내 */

export interface ServiceInfo {
  key: string
  tier: Tier
  lead: string[]
  photo: SitePhotoSlot
  /** 함께 보는 서비스 카드의 한 줄 */
  summary: string
  who: { icon: SubIconName; lines: string[] }[]
  /** 진행 순서. 단계 이름만 둔다(CR-162) */
  steps: string[]
  inquiryType: InquiryType
  related: string[]
}

export const SERVICE_TEXT = {
  primary: '상담 신청',
  whoTitle: '이런 회사에 필요합니다',
  stepsTitle: '진행 순서',
  costTitle: '비용',
  costBig: '건별 견적',
  costDesc: '상담 뒤에 정합니다.',
  pricingLink: '요금 안내',
  applyDesc: '남겨 주시면 이메일로 답변 드립니다. 메일로도 받습니다',
  relatedTitle: '함께 보는 서비스',
  relatedMore: '자세히',
}

export const SERVICES: ServiceInfo[] = [
  {
    key: 'sam-registration',
    tier: 'c',
    lead: ['미군 공고에 입찰하려면 먼저 SAM.gov 에 회사를 등록해야 합니다.', '처음 등록하는 회사도 순서대로 대신 처리해 드립니다.'],
    photo: PHOTO.desk,
    summary: 'NCAGE 코드부터 SAM.gov 활성까지 순서대로',
    who: [
      { icon: 'flag', lines: ['미군 공고에', '처음 입찰하는 회사'] },
      { icon: 'check', lines: ['공고에 SAM 등록이 필요한데', '아직 등록이 없는 회사'] },
      { icon: 'globe', lines: ['영문 입력과 서류 준비가', '부담스러운 회사'] },
    ],
    steps: [
      '상담과 서류 확인',
      'NCAGE 코드 받기',
      'Login.gov 계정',
      'SAM.gov 등록 신청',
      '공증 서한 내고 활성',
    ],
    inquiryType: INQUIRY_TYPE.samRegistration,
    related: ['piee-registration', 'sam-renewal', 'translation'],
  },
  {
    key: 'piee-registration',
    tier: 'c',
    lead: ['미 국방부 계약의 견적·제안 제출과 대금 청구는 PIEE 에서 합니다.', '업체 관리자 계정을 만들고 필요한 역할을 받는 일을 순서대로 도와 드립니다.'],
    photo: PHOTO.piee,
    summary: '견적·제안 제출과 대금 청구에 쓰는 미 국방부 조달 시스템 등록',
    who: [
      { icon: 'check', lines: ['SAM.gov 등록은 끝났고', 'PIEE 계정이 없는 회사'] },
      { icon: 'file', lines: ['공고가 PIEE 로', '견적을 내라고 하는 회사'] },
      { icon: 'flag', lines: ['낙찰 뒤 대금 청구를', '준비해야 하는 회사'] },
    ],
    steps: [
      '상담과 등록 확인',
      '전자 거래 담당자 확인',
      '업체 관리자 계정',
      '담당자 계정과 역할',
      '승인 뒤 사용',
    ],
    inquiryType: INQUIRY_TYPE.consulting,
    related: ['sam-registration', 'invoicing', 'sam-renewal'],
  },
  {
    key: 'sam-renewal',
    tier: 'c',
    lead: ['SAM.gov 등록은 해마다 갱신해야 합니다.', '기한이 지나면 등록이 비활성이 되어 입찰과 계약에 지장이 생길 수 있어, 갱신을 대신 처리해 드립니다.'],
    photo: PHOTO.desk,
    summary: '해마다 해야 하는 SAM.gov 등록 갱신을 대신 처리',
    who: [
      { icon: 'cal', lines: ['SAM.gov 등록 만료일이', '다가오는 회사'] },
      { icon: 'check', lines: ['회사 주소·계좌·담당자가', '바뀐 회사'] },
      { icon: 'globe', lines: ['지난해 등록을 직접 하지 않아', '절차가 낯선 회사'] },
    ],
    steps: [
      '상담과 만료일 확인',
      '바뀐 정보 정리',
      '갱신 신청',
      '심사와 활성',
    ],
    inquiryType: INQUIRY_TYPE.samRegistration,
    related: ['sam-registration', 'piee-registration', 'translation'],
  },
  {
    key: 'site-visit',
    tier: 'k',
    lead: ['공고에 따라 기지 안에서 현장 설명회를 엽니다.', '출입 절차 준비부터 설명회 동행과 통역까지 도와 드립니다.'],
    photo: PHOTO.meeting,
    summary: '출입 절차부터 통역까지, 설명회에 함께 갑니다',
    who: [
      { icon: 'pin', lines: ['공고에 현장 설명회가', '잡혀 있는 회사'] },
      { icon: 'flag', lines: ['기지 출입 절차가', '처음인 회사'] },
      { icon: 'globe', lines: ['영어 설명을 듣고', '질문하기 어려운 회사'] },
    ],
    steps: [
      '상담과 공고 확인',
      '출입 신청 준비',
      '설명회 동행',
      '정리와 질의',
    ],
    inquiryType: INQUIRY_TYPE.consulting,
    related: ['translation', 'teaming', 'invoicing'],
  },
  {
    key: 'invoicing',
    tier: 'c',
    lead: ['미 국방부 계약 대금은 PIEE 의 전자 청구(WAWF)로 청구합니다.', '계약 서류를 정리하고 청구서를 내는 일을 도와 드립니다.'],
    photo: PHOTO.piee,
    summary: '계약 서류 정리와 PIEE 대금 청구 준비',
    who: [
      { icon: 'flag', lines: ['미군 계약을 따내고', '첫 대금을 청구하는 회사'] },
      { icon: 'globe', lines: ['청구 서류를 영문으로', '맞추기 어려운 회사'] },
      { icon: 'alert', lines: ['청구가 돌아와', '다시 내야 하는 회사'] },
    ],
    steps: [
      '상담과 계약 확인',
      'PIEE 역할 확인',
      '청구서 준비',
      '청구와 확인',
    ],
    inquiryType: INQUIRY_TYPE.consulting,
    related: ['piee-registration', 'translation', 'sam-renewal'],
  },
  {
    key: 'translation',
    tier: 'c',
    lead: ['SAM.gov 등록과 공고 제출에는 영문 서류가 필요할 때가 많습니다.', '회사 서류를 영문으로 옮겨 드립니다.'],
    photo: PHOTO.desk,
    summary: '등기부등본, 재무제표, 실적 증명을 영문으로',
    who: [
      { icon: 'check', lines: ['SAM.gov 등록에 영문', '회사 서류가 필요한 회사'] },
      { icon: 'file', lines: ['공고가 실적 증명과 재무 자료를', '영문으로 요구하는 회사'] },
      { icon: 'globe', lines: ['회사 소개 자료를', '영문으로 갖추려는 회사'] },
    ],
    steps: [
      '상담과 서류 확인',
      '견적',
      '번역',
      '확인과 전달',
    ],
    inquiryType: INQUIRY_TYPE.consulting,
    related: ['sam-registration', 'piee-registration', 'invoicing'],
  },
  {
    key: 'teaming',
    tier: 'm',
    lead: ['혼자 하기 어려운 공고는 다른 회사와 팀을 이뤄 낼 수 있습니다.', '허가를 가진 회사와 실적을 가진 회사를 이어 드립니다. 지금은 상담으로 받습니다.'],
    photo: PHOTO.meeting,
    summary: '허가와 실적을 서로 채울 짝 회사 찾기, 지금은 상담으로',
    who: [
      { icon: 'flag', lines: ['공고 규모나 범위가', '혼자 하기 큰 회사'] },
      { icon: 'check', lines: ['공고가 요구하는 실적이나', '자격이 모자란 회사'] },
      { icon: 'chat', lines: ['미군 계약 경험이 있는 회사와', '함께하고 싶은 회사'] },
    ],
    steps: [
      '상담과 공고 확인',
      '모자란 것 정리',
      '짝 회사 찾기',
      '역할 나누기',
    ],
    inquiryType: INQUIRY_TYPE.consulting,
    related: ['site-visit', 'translation', 'sam-registration'],
  },
]

export const findService = (key: string) => SERVICES.find((s) => s.key === key)

/* 항목별 준비 안내 */

export interface AltCard {
  icon: SubIconName
  name: string
  desc: string
  to: string
}

const ALT: Record<string, AltCard> = {
  open: { icon: 'clock', name: '마감 전 공고', desc: '지금 낼 수 있는 공고를 마감순으로', to: '/search' },
  bookmarks: { icon: 'star', name: '관심 공고', desc: '눈여겨볼 공고를 저장', to: '/bookmarks' },
  materials: { icon: 'book', name: '교육 자료', desc: '공고 유형과 찾는 법', to: academyHref('materials') },
  glossary: { icon: 'book', name: '용어 사전', desc: '공고에 나오는 말을 한글로', to: academyHref('glossary') },
  forms: { icon: 'file', name: '서식 모음', desc: '미 연방 표준 서식과 준비물', to: academyHref('forms') },
  profile: { icon: 'check', name: '회사 정보', desc: '우리 회사 정보를 적어 두기', to: '/profile' },
  vault: { icon: 'folder', name: '공통 서류함', desc: '회사 서류를 한곳에 올려 두기', to: '/documents' },
  proposals: { icon: 'file', name: '공고별 서류', desc: '신청한 공고의 서류 보기', to: '/proposals' },
  notices: { icon: 'bell', name: '공지사항', desc: 'WJbid 서비스 소식', to: '/notices' },
  renewal: { icon: 'cal', name: 'SAM 갱신', desc: '해마다 하는 등록 갱신 대행', to: serviceHref('sam-renewal') },
}

export interface PendingInfo {
  key: string
  tier?: Tier
  desc: string[]
  alts: AltCard[]
  photo: SitePhotoSlot
}

export const PENDING_TEXT = {
  status: '준비하고 있습니다',
  altTitle: '지금 쓸 수 있는 것',
  askTitle: '궁금한 점은 1:1 문의로 남겨 주세요',
  askDesc: '남겨 주시면 이메일로 답변 드립니다',
  askAction: '1:1 문의',
}

/** 키 없는 주소와 모르는 키에 보이는 일반 준비 중 안내 (CR-127) */
export const COMING_SOON_GENERIC = {
  tag: '준비 중',
  title: '준비 중입니다',
  titleWith: (name: string) => `${name}, 준비 중입니다`,
  desc: '화면을 만들고 있습니다. 그동안 아래에서 필요한 것을 찾아보세요.',
  actions: { home: '홈', open: '마감 전 공고', inquiry: '1:1 문의' },
}

export const PENDING: PendingInfo[] = [
  {
    key: 'awardees', photo: PHOTO.meeting,
    desc: ['업체마다 미군 계약을 몇 건, 얼마에 따냈는지 모아 보여 드립니다.'],
    alts: [ALT.open, ALT.bookmarks, ALT.materials],
  },
  {
    key: 'base-map', tier: 'f', photo: PHOTO.hall,
    desc: ['기지마다 공고와 낙찰이 얼마나 나왔는지 지도에 보여 드립니다.'],
    alts: [ALT.open, ALT.bookmarks, ALT.materials],
  },
  {
    key: 'expiry-alert', tier: 'm', photo: PHOTO.piee,
    desc: ['올린 서류마다 만료일을 보여 주고, 만료 전에 알려 드립니다.'],
    alts: [ALT.vault, ALT.proposals, ALT.renewal],
  },
  {
    key: 'pre-check', tier: 'm', photo: PHOTO.piee,
    desc: ['공고가 요구하는 서류와 올린 서류를 맞춰 보고,', '빠진 것과 파일 형식, PDF 쪽수를 확인합니다.'],
    alts: [ALT.vault, ALT.proposals, ALT.forms],
  },
  {
    key: 'proposal-review', tier: 'c', photo: PHOTO.piee,
    desc: ['고객이 직접 쓴 제안서를 검토하고 보완할 점을 알려 드리는', '제안서 자문·검토 서비스입니다.'],
    alts: [ALT.forms, ALT.materials, ALT.glossary],
  },
  {
    key: 'kakao', tier: 'm', photo: PHOTO.desk,
    desc: ['새 공고와 마감이 다가온 공고를 카카오톡으로 알려 드립니다.'],
    alts: [ALT.bookmarks, ALT.open, ALT.notices],
  },
]

export const findPending = (key: string) => PENDING.find((p) => p.key === key)
