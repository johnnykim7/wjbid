/** 공통 틀(유틸 띠, 머리말, 꼬리말) 문구 (CR-127). 문구를 바꿀 때는 이 파일만 고친다. */

export const SERVICE_LINE = '미군·연방 조달 입찰 정보 서비스'
export const LOGO_SUB = '미군·연방 조달 입찰 대행'

export const searchHref = (keyword?: string) =>
  keyword ? `/search?keyword=${encodeURIComponent(keyword)}` : '/search'

/** 검색 구분. 공고 검색은 지금 제목으로만 찾으므로 공고번호·기지명은 검색이 그 항목을 지원할 때 넣는다 */
export const SEARCH_FIELDS = [{ value: 'title', label: '공고명' }]
export const SEARCH_PLACEHOLDER = '공고명, 공고번호, 기지명을 입력하세요'
export const RECOMMENDED_KEYWORDS = ['미화 용역', '시설 유지관리', '오산', '험프리스', '차량 정비']

export const FOOTER_LINKS = [
  { label: '이용약관', to: '/terms' },
  { label: '개인정보처리방침', to: '/privacy' },
  { label: '면책 고지', to: '/disclaimer' },
  { label: '환불 정책', to: '/refund' },
  { label: '고객센터', to: '/support' },
  { label: '제휴 문의', to: '/support' },
]

/** 사업자 정보. "(대표자명)" 처럼 괄호로 둘러싼 값은 아직 정해지지 않은 것이라 꼬리말에 내지 않는다 */
export const COMPANY = {
  name: '우진USbid',
  ceo: '(대표자명)',
  businessNumber: '(사업자등록번호)',
  address: '(주소)',
  email: 'support@wjbid.com',
}

export const COPYRIGHT = '© 2026 WJbid · 우진USbid · 미군·연방 조달 입찰 대행 서비스'
