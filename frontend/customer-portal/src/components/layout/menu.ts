/**
 * 메뉴 정의 (CR-127). 메뉴 띠, 전체메뉴, 속페이지 묶음 메뉴(CR-133)가 이 한 곳을 읽는다.
 * 서비스 후보는 발주사 선택 대기 중이라 메뉴를 바꿀 때는 이 파일만 고친다.
 * to 가 없는 항목은 아직 화면이 없어 항목별 준비 안내로 보낸다(T3-3 「메뉴 목적지」).
 * 화면은 한글 이름이 아니라 key 로 항목을 찾는다.
 */
export interface MenuItem {
  key: string
  label: string
  to?: string
}

export interface MenuGroup extends MenuItem {
  items: MenuItem[]
}

export const COMING_SOON_PATH = '/coming-soon'

export const comingSoonHref = (key: string) => `${COMING_SOON_PATH}/${key}`
export const serviceHref = (key: string) => `/services/${key}`
export const academyHref = (key: string) => `/academy/${key}`

/** 목적지가 없는 묶음은 첫 항목으로, 목적지가 없는 항목은 그 항목의 준비 안내로 간다 */
export const menuHref = (item: MenuItem | MenuGroup): string => {
  if (item.to) return item.to
  if ('items' in item && item.items.length > 0) return menuHref(item.items[0])
  return comingSoonHref(item.key)
}

export const isReady = (item: MenuItem) => item.to !== undefined

export const MENU: MenuGroup[] = [
  {
    key: 'bids',
    label: '입찰공고',
    to: '/search',
    items: [
      { key: 'open', label: '마감 전 공고', to: '/search' },
      { key: 'pre-solicitation', label: '사전 공고', to: '/bids/pre-solicitation' },
      { key: 'amendment', label: '변경 공고', to: '/bids/amendments' },
      { key: 'bookmarks', label: '관심 공고', to: '/bookmarks' },
      { key: 'notice-sample', label: '공고문 예시', to: '/bids/sample' },
    ],
  },
  {
    key: 'awards',
    label: '낙찰정보',
    items: [
      { key: 'recent-awards', label: '최근 낙찰', to: '/awards/recent' },
      { key: 'awardees', label: '낙찰 업체', to: '/awards/awardees' },
      { key: 'award-range', label: '낙찰가 범위', to: '/awards/range' },
    ],
  },
  {
    key: 'analysis',
    label: '발주분석',
    items: [
      { key: 'base-map', label: '기지별 지도', to: '/analysis/base-map' },
      { key: 'monthly-trend', label: '월별 동향', to: '/analysis/monthly' },
      { key: 'naics-trend', label: '업종별 발주', to: '/analysis/naics' },
    ],
  },
  {
    key: 'documents',
    label: '서류함',
    to: '/documents',
    items: [
      { key: 'vault', label: '공통 서류함', to: '/documents' },
      { key: 'proposals', label: '공고별 서류', to: '/proposals' },
      { key: 'expiry-alert', label: '유효기간 알림', to: '/documents/expiry' },
      { key: 'pre-check', label: '제출 전 검사', to: '/documents/pre-check' },
    ],
  },
  {
    key: 'services',
    label: '등록·컨설팅',
    items: [
      { key: 'sam-registration', label: 'SAM 등록 대행', to: serviceHref('sam-registration') },
      { key: 'piee-registration', label: 'PIEE 등록 대행', to: serviceHref('piee-registration') },
      { key: 'sam-renewal', label: 'SAM 갱신', to: serviceHref('sam-renewal') },
      // 법무 확인 전까지 준비 안내로 둔다(사용자 10/11 06:06)
      { key: 'proposal-review', label: '제안서 자문' },
      { key: 'site-visit', label: '현장 설명회', to: serviceHref('site-visit') },
      { key: 'invoicing', label: '대금 청구', to: serviceHref('invoicing') },
      { key: 'translation', label: '서류 영문 번역', to: serviceHref('translation') },
      { key: 'teaming', label: '팀 구성 협력사 찾기', to: serviceHref('teaming') },
    ],
  },
  {
    key: 'support',
    label: '고객센터',
    to: '/support',
    items: [
      { key: 'notices', label: '공지사항', to: '/notices' },
      { key: 'faq', label: '자주 묻는 질문', to: '/faq' },
      { key: 'inquiry', label: '1:1 문의', to: '/support' },
      // 입찰 교실 묶음을 걷어내고 자료 세 화면만 고객센터로 옮겼다(CR-147). 주소는 그대로 둔다
      { key: 'materials', label: '교육 자료', to: academyHref('materials') },
      { key: 'glossary', label: '용어 사전', to: academyHref('glossary') },
      { key: 'forms', label: '서식 모음', to: academyHref('forms') },
      { key: 'pricing', label: '요금 안내', to: '/pricing' },
      { key: 'kakao', label: '카카오톡 알림' },
    ],
  },
  // 발주사가 무엇인지 알기 어렵다며 맨 끝으로 보내 달라고 했다(CR-162, 원문 6장 1)
  {
    key: 'fit',
    label: '입찰 자격 진단',
    items: [
      { key: 'profile', label: '회사 정보', to: '/profile' },
      { key: 'eligibility', label: '참가 자격 확인', to: '/fit/eligibility' },
      { key: 'opportunity-fit', label: '공고별 판정', to: '/fit/verdicts' },
    ],
  },
]

/** 묶음 key 와 항목 key 로 묶음과 항목을 찾는다 */
export const findMenuItem = (groupKey: string, itemKey: string) => {
  const group = MENU.find((g) => g.key === groupKey)
  const item = group?.items.find((i) => i.key === itemKey)
  return group && item ? { group, item } : undefined
}

/** 준비 안내 항목 key 로 묶음을 찾는다 */
export const findPendingItem = (itemKey: string) => {
  for (const group of MENU) {
    const item = group.items.find((i) => i.key === itemKey && !isReady(i))
    if (item) return { group, item }
  }
  return undefined
}
