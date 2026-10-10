/**
 * 메뉴 정의 (CR-127). 메뉴 띠와 전체메뉴가 이 한 곳을 읽는다.
 * 서비스 후보는 발주사 선택 대기 중이라 메뉴를 바꿀 때는 이 파일만 고친다.
 * to 가 없는 항목은 아직 화면이 없어 준비 중 안내로 보낸다(T3-3 「메뉴 목적지」).
 */
export interface MenuItem {
  label: string
  to?: string
}

export interface MenuGroup extends MenuItem {
  items: MenuItem[]
}

export const COMING_SOON_PATH = '/coming-soon'

export const comingSoonHref = (label: string) => `${COMING_SOON_PATH}?name=${encodeURIComponent(label)}`

export const menuHref = (item: MenuItem) => item.to ?? comingSoonHref(item.label)

export const MENU: MenuGroup[] = [
  {
    label: '입찰공고',
    to: '/search',
    items: [
      { label: '마감 전 공고', to: '/search' },
      { label: '사전 공고' },
      { label: '변경 공고' },
      { label: '관심 공고', to: '/bookmarks' },
      { label: '공고문 예시' },
    ],
  },
  { label: '재발주 예측', items: [{ label: '다음 달 공고' }, { label: '재발주 달력' }, { label: '옵션 연장 알림' }] },
  { label: '낙찰정보', items: [{ label: '최근 낙찰' }, { label: '낙찰 업체' }, { label: '낙찰가 범위' }] },
  { label: '발주분석', items: [{ label: '기지별 지도' }, { label: '월별 동향' }, { label: '업종별 발주' }] },
  {
    label: '맞춤 판정',
    items: [{ label: '회사 정보', to: '/profile' }, { label: '적격성 진단' }, { label: '공고별 판정' }],
  },
  {
    label: '서류함',
    to: '/documents',
    items: [
      { label: '공통 서류함', to: '/documents' },
      { label: '공고별 서류', to: '/proposals' },
      { label: '유효기간 알림' },
      { label: '제출 전 검사' },
    ],
  },
  {
    label: '등록·컨설팅',
    items: [
      { label: 'SAM 등록 대행' },
      { label: 'PIEE 등록 대행' },
      { label: 'SAM 갱신' },
      { label: '제안서 자문' },
      { label: '현장 설명회' },
      { label: '대금 청구' },
      { label: '서류 영문 번역' },
      { label: '팀 구성 찾기' },
    ],
  },
  { label: '입찰 교실', items: [{ label: '무료 입찰 교실' }, { label: '교육 자료' }, { label: '용어 사전' }, { label: '서식 모음' }] },
  {
    label: '고객센터',
    to: '/support',
    items: [
      { label: '공지사항', to: '/notices' },
      { label: '자주 묻는 질문', to: '/faq' },
      { label: '1:1 문의', to: '/support' },
      { label: '요금 안내', to: '/pricing' },
      { label: '카카오톡 알림' },
    ],
  },
]
