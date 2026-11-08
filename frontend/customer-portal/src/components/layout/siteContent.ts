/** 공통 틀(유틸 띠, 머리말, 꼬리말) 문구 (CR-127). 문구를 바꿀 때는 이 파일만 고친다. */

export const SERVICE_LINE = '(주)우진이엔씨가 운영하는 미군·연방 조달 입찰 정보 서비스'
export const LOGO_LABEL = 'WOOJIN 홈'

export const searchHref = (keyword?: string) =>
  keyword ? `/search?keyword=${encodeURIComponent(keyword)}` : '/search'

/** 검색 구분. 공고 검색은 지금 제목으로만 찾으므로 공고번호·기지명은 검색이 그 항목을 지원할 때 넣는다 */
export const SEARCH_FIELDS = [{ value: 'title', label: '공고명' }]
export const SEARCH_PLACEHOLDER = '공고명, 공고번호, 기지명을 입력하세요'
/** 머리말 추천 검색어로 보이는 기지 수 (CR-187) */
export const RECOMMENDED_BASE_COUNT = 5

export const FOOTER_LINKS = [
  { label: '이용약관', to: '/terms' },
  { label: '개인정보처리방침', to: '/privacy' },
  { label: '면책 고지', to: '/disclaimer' },
  { label: '환불 정책', to: '/refund' },
  { label: '고객센터', to: '/support' },
  { label: '제휴 문의', to: '/support' },
]

/**
 * 사업자 정보. "(사업자등록번호)" 처럼 괄호로 둘러싼 값은 아직 정해지지 않은 것이라 꼬리말에 내지 않는다.
 * 주소는 통신판매업 신고증의 소재지와 같아야 해서 평택 본점이고, 서울지사는 소비자 응대 주소로 따로 낸다 (CR-190)
 */
export const COMPANY = {
  name: '(주)우진이엔씨',
  ceo: '한용일',
  businessNumber: '141-81-27181',
  mailOrderNumber: '제2018-경기평택-0288호',
  address: '경기도 평택시 팽성읍 안정순환로 153-16',
  branchAddress: '서울 강서구 화곡로68길 82, 402호(강서IT밸리)',
  tel: '031-651-0433',
  mobile: '010-7156-7707',
  email: 'woojinenc25@gmail.com',
}

export const COPYRIGHT = '© 2026 (주)우진이엔씨 · WJbid 미군·연방 조달 입찰 정보 서비스'
