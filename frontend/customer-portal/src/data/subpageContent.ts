/**
 * 메뉴 속 화면 정의 (CR-133). 속페이지 틀 문구, 등록·컨설팅 서비스 안내 7개, 항목별 준비 안내 18개를 이 한 곳에 둔다.
 * 안내 글은 서비스 후보 문구와 공식 안내(SAM.gov, NSPA, PIEE, FAR·DFARS)로 확인한 것만 쓰고, 발주사 확인은 배포 뒤 받아 고친다(사용자 10/11 06:06).
 * 금액, 기간, 일정은 정해진 값이 없어 쓰지 않는다.
 * 화면 이름은 메뉴 정의(components/layout/menu.ts)의 이름을 그대로 쓰고, 여기서는 항목 key 로 찾는다.
 */
import type { SubIconName } from '../components/subpage/SubIcon'
import { academyHref, serviceHref } from '../components/layout/menu'
import type { Tier } from './homeContent'

export const SUB_FRAME = {
  pendingTag: '준비 중',
  call: { title: '상담이 필요하세요?', desc: '궁금한 점을 남겨 주시면 이메일로 답변 드립니다', link: '1:1 문의' },
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

const PHOTO = {
  desk: '/img/army_71dae7c5.jpg',
  piee: '/img/army_17a23e72.jpg',
  meeting: '/img/army_5decc7b0.jpg',
  hall: '/img/army_d68e5213.jpg',
}
export const ACADEMY_PHOTO = PHOTO.hall

/* 서비스 안내 */

export interface ServiceInfo {
  key: string
  tier: Tier
  lead: string[]
  photo: string
  /** 함께 보는 서비스 카드의 한 줄 */
  summary: string
  who: { icon: SubIconName; lines: string[] }[]
  steps: { basis: string; items: { name: string; desc: string }[] }
  note: { text: string; link?: { label: string; to: string } }
  prepare: { items: { name: string; desc?: string }[]; vault: boolean }
  cost: { big: string; desc: string; fact?: { strong: string; rest?: string } }
  inquiryType: InquiryType
  related: string[]
}

export const SERVICE_TEXT = {
  primary: '상담 신청',
  whoTitle: '이런 회사에 필요합니다',
  stepsTitle: '진행 순서',
  prepareTitle: '준비할 것',
  vaultLink: '공통 서류함에 올려 두기',
  costTitle: '비용',
  pricingLink: '요금 안내',
  applyDesc: '남겨 주시면 이메일로 답변 드립니다. 메일로도 받습니다',
  relatedTitle: '함께 보는 서비스',
  relatedMore: '자세히',
}

const QUOTE_COST = { big: '건별 견적', desc: '회사 상황과 필요한 일을 듣고 상담 뒤에 정합니다.' }

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
    steps: {
      basis: '미국 밖 회사의 SAM.gov 등록 순서입니다',
      items: [
        { name: '상담과 서류 확인', desc: '회사 상황을 듣고 필요한 서류를 알려 드립니다' },
        { name: 'NCAGE 코드 받기', desc: '미국 밖 회사는 SAM 등록 전에 받아야 합니다' },
        { name: 'Login.gov 계정', desc: 'SAM.gov 에 들어갈 때 쓰는 계정입니다' },
        { name: 'SAM.gov 등록 신청', desc: '회사 정보, 대금 받을 계좌, 담당자를 넣고 UEI 를 받습니다' },
        { name: '공증 서한 내고 활성', desc: '대표 관리자를 정하는 공증 서한을 내면 심사 뒤 활성이 됩니다' },
      ],
    },
    note: {
      text: '계정 본인 인증과 공증 서명은 회사 담당자가 직접 해야 합니다. 그 밖의 입력과 확인은 WJbid 가 대신합니다. 등록은 해마다 갱신해야 합니다.',
      link: { label: 'SAM 갱신 보기', to: serviceHref('sam-renewal') },
    },
    prepare: {
      items: [
        { name: '사업자등록증, 법인 등기부등본' },
        { name: '회사 영문 이름과 영문 주소', desc: 'NCAGE 와 SAM 에 넣는 이름과 주소가 서로 같아야 합니다' },
        { name: '대금 받을 은행 계좌 정보' },
        { name: '담당자 이름, 이메일, 전화번호' },
      ],
      vault: true,
    },
    cost: { ...QUOTE_COST, fact: { strong: '영문 입력, 본인 인증, 공증 서한까지 혼자 하기 어려운 등록을 대신 처리합니다.', rest: '비용은 상담 뒤 건별로 정합니다.' } },
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
    steps: {
      basis: 'PIEE 업체 등록 순서입니다',
      items: [
        { name: '상담과 등록 확인', desc: 'SAM.gov 등록이 활성인지, CAGE(NCAGE) 코드가 있는지 확인합니다' },
        { name: '전자 거래 담당자 확인', desc: 'SAM.gov 에 적힌 전자 거래 담당자(EB POC)가 업체 관리자를 승인합니다' },
        { name: '업체 관리자 계정', desc: '업체 관리자(CAM) 계정을 만들고 회사 정보를 넣습니다' },
        { name: '담당자 계정과 역할', desc: '견적을 내고 대금을 청구할 담당자 계정과 역할을 신청합니다' },
        { name: '승인 뒤 사용', desc: '관리자가 승인하면 PIEE 를 쓸 수 있습니다' },
      ],
    },
    note: {
      text: '계정 본인 인증과 승인은 회사 담당자가 직접 해야 합니다. 그 밖의 입력과 확인은 WJbid 가 도와 드립니다. SAM.gov 등록이 먼저입니다.',
      link: { label: 'SAM 등록 대행 보기', to: serviceHref('sam-registration') },
    },
    prepare: {
      items: [
        { name: 'SAM.gov 등록 정보', desc: 'UEI 와 CAGE(NCAGE) 코드' },
        { name: '전자 거래 담당자 이름과 이메일', desc: 'SAM.gov 에 적힌 것과 같아야 합니다' },
        { name: 'PIEE 를 쓸 담당자 이름, 이메일, 전화번호' },
      ],
      vault: false,
    },
    cost: QUOTE_COST,
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
    steps: {
      basis: 'SAM.gov 등록 갱신 순서입니다',
      items: [
        { name: '상담과 만료일 확인', desc: '등록 상태와 만료일을 함께 확인합니다' },
        { name: '바뀐 정보 정리', desc: '주소, 계좌, 담당자처럼 바뀐 것을 정리합니다' },
        { name: '갱신 신청', desc: '등록 정보를 다시 확인하고 갱신을 신청합니다' },
        { name: '심사와 활성', desc: '심사가 끝나면 다음 만료일이 새로 정해집니다' },
      ],
    },
    note: {
      text: 'Login.gov 본인 인증은 회사 담당자가 직접 해야 합니다. 갱신은 만료 전에 시작하는 것이 안전합니다.',
      link: { label: '처음 등록이면 SAM 등록 대행 보기', to: serviceHref('sam-registration') },
    },
    prepare: {
      items: [
        { name: '지금 SAM.gov 등록 정보', desc: 'UEI 와 만료일' },
        { name: '바뀐 회사 정보', desc: '주소, 대금 받을 계좌, 담당자' },
      ],
      vault: false,
    },
    cost: { ...QUOTE_COST, fact: { strong: '영문 입력과 확인까지 혼자 하기 어려운 갱신을 대신 처리합니다.', rest: '비용은 상담 뒤 건별로 정합니다.' } },
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
    steps: {
      basis: '공고에 현장 설명회가 있을 때 순서입니다',
      items: [
        { name: '상담과 공고 확인', desc: '공고문에서 설명회 날짜, 장소, 출입 신청 기한을 찾습니다' },
        { name: '출입 신청 준비', desc: '공고가 요구하는 참석자 정보와 서류를 기한 안에 냅니다' },
        { name: '설명회 동행', desc: '설명회에 함께 가서 통역하고 내용을 기록합니다' },
        { name: '정리와 질의', desc: '들은 내용을 정리하고, 궁금한 점은 공고에 적힌 방법으로 묻습니다' },
      ],
    },
    note: {
      text: '기지 출입 허가는 미군이 정합니다. 공고문에 적힌 출입 신청 기한이 지나면 참석할 수 없습니다.',
      link: { label: '교육 자료 2. 판단 보기', to: `${academyHref('materials')}?step=2` },
    },
    prepare: {
      items: [
        { name: '참석자 이름과 신분 정보', desc: '공고가 요구하는 대로' },
        { name: '회사 정보와 공고 번호' },
        { name: '설명회 때 물을 질문' },
      ],
      vault: false,
    },
    cost: { big: '상담 뒤 견적', desc: QUOTE_COST.desc },
    inquiryType: INQUIRY_TYPE.consulting,
    related: ['translation', 'teaming', 'invoicing'],
  },
  {
    key: 'invoicing',
    tier: 'c',
    lead: ['미 국방부 계약 대금은 PIEE 의 전자 청구(WAWF)로 청구합니다.', '계약 서류를 정리하고 청구서를 내는 일을 도와 드립니다.'],
    photo: PHOTO.piee,
    summary: '계약 서류 정리와 PIEE 대금 청구를 도와 드립니다',
    who: [
      { icon: 'flag', lines: ['미군 계약을 따내고', '첫 대금을 청구하는 회사'] },
      { icon: 'globe', lines: ['청구 서류를 영문으로', '맞추기 어려운 회사'] },
      { icon: 'alert', lines: ['청구가 돌아와', '다시 내야 하는 회사'] },
    ],
    steps: {
      basis: '낙찰 뒤 대금을 청구하는 순서입니다',
      items: [
        { name: '상담과 계약 확인', desc: '계약서에서 청구 방법, 청구 서류 종류, 받는 부서를 확인합니다' },
        { name: 'PIEE 역할 확인', desc: '청구를 낼 담당자 계정과 역할이 있는지 확인합니다' },
        { name: '청구서 준비', desc: '납품하거나 수행한 만큼 청구서와 증빙을 준비합니다' },
        { name: '청구와 확인', desc: 'PIEE 에서 청구를 내고 처리 상태를 확인합니다' },
      ],
    },
    note: {
      text: '청구는 회사 계정으로 회사가 냅니다. WJbid 는 서류 정리와 입력을 도와 드립니다. PIEE 계정이 없으면 PIEE 등록부터 합니다.',
      link: { label: 'PIEE 등록 대행 보기', to: serviceHref('piee-registration') },
    },
    prepare: {
      items: [
        { name: '계약서와 수정 계약서' },
        { name: '납품·수행 증빙' },
        { name: '대금 받을 계좌 정보', desc: 'SAM.gov 에 적힌 계좌' },
        { name: 'PIEE 계정 정보' },
      ],
      vault: false,
    },
    cost: { ...QUOTE_COST, fact: { strong: '미 국방부 계약 대금은 전자로 청구합니다.', rest: '계약서의 DFARS 252.232-7006 조항에 청구 방법이 적혀 있습니다.' } },
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
    steps: {
      basis: '번역을 맡기는 순서입니다',
      items: [
        { name: '상담과 서류 확인', desc: '번역할 서류와 쓰일 곳(등록, 공고 제출)을 확인합니다' },
        { name: '견적', desc: '서류 분량을 보고 비용과 기간을 알려 드립니다' },
        { name: '번역', desc: '회사 영문 이름과 주소를 등록 정보와 같게 맞춰 옮깁니다' },
        { name: '확인과 전달', desc: '회사가 내용을 확인한 뒤 최종본을 드립니다' },
      ],
    },
    note: { text: '공증이나 번역 확인서가 필요한지는 쓰일 곳에 따라 다릅니다. 상담 때 함께 확인합니다.' },
    prepare: {
      items: [
        { name: '번역할 서류 원본', desc: '등기부등본, 재무제표, 실적 증명 등' },
        { name: '회사 영문 이름과 영문 주소' },
        { name: '쓰일 곳과 제출 기한' },
      ],
      vault: true,
    },
    cost: QUOTE_COST,
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
    steps: {
      basis: '상담으로 짝 회사를 찾는 순서입니다',
      items: [
        { name: '상담과 공고 확인', desc: '함께 내려는 공고와 우리 회사가 맡을 일을 확인합니다' },
        { name: '모자란 것 정리', desc: '모자란 업종, 실적, 허가를 정리합니다' },
        { name: '짝 회사 찾기', desc: '조건에 맞는 회사를 찾아 연결을 돕습니다' },
        { name: '역할 나누기', desc: '누가 주계약자가 되고 누가 하도급을 맡을지 정합니다' },
      ],
    },
    note: { text: '팀 구성 방식은 공고가 허용하는 범위 안에서 정합니다. 팀 계약 문서는 두 회사가 직접 맺습니다.' },
    prepare: {
      items: [
        { name: '우리 회사 업종과 실적 소개' },
        { name: '함께 내려는 공고 번호' },
        { name: '짝 회사에 바라는 조건' },
      ],
      vault: false,
    },
    cost: { big: '회원 서비스', desc: '화면을 만드는 동안에는 상담으로 받습니다. 회원 요금은 요금 안내에서 봅니다.' },
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
  photo: string
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
    desc: ['공고가 요구하는 서류와 올린 서류를 맞춰 보고,', '빠진 것과 쪽수, 서식을 확인해 드립니다.'],
    alts: [ALT.vault, ALT.proposals, ALT.forms],
  },
  {
    key: 'proposal-review', tier: 'c', photo: PHOTO.piee,
    desc: ['고객이 직접 쓴 제안서를 전문가가 검토하고 보완할 점을 알려 드리는', '제안서 자문·검토 서비스입니다.'],
    alts: [ALT.forms, ALT.materials, ALT.glossary],
  },
  {
    key: 'kakao', tier: 'm', photo: PHOTO.desk,
    desc: ['새 공고와 마감이 다가온 공고를 카카오톡으로 알려 드립니다.'],
    alts: [ALT.bookmarks, ALT.open, ALT.notices],
  },
]

export const findPending = (key: string) => PENDING.find((p) => p.key === key)
