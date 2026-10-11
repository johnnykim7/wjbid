/**
 * 메뉴 속 화면 2묶음 문구 (CR-135, T3-3 「메뉴 속 화면 2묶음」, 화면 정본 속페이지_시안_2묶음.html).
 * 자료 기준 줄은 **굵게** 표시를 쓰고, 숫자와 기간은 화면이 응답에서 넣는다.
 */

export const MARKET_COMMON = {
  legend: { notice: 'WJbid 한글 공고문으로 갑니다', sam: 'SAM.gov 원문이 새 창으로 열립니다' },
  goNotice: 'WJbid 공고 상세',
  goSam: 'SAM.gov 원문 새 창',
  loading: '불러오고 있습니다',
  loadFailTitle: '불러오지 못했습니다',
  retry: '다시 시도',
  emptyTitle: '조건에 맞는 공고가 없습니다',
  noDeadline: '기한 없음',
  closed: '마감',
  notDisclosed: '공개 안 됨',
  band: {
    title: '마감 전 공고는 한글 공고문으로 정리해 드립니다',
    desc: '무료 회원가입 후 관심 공고를 담아 두고 다시 볼 수 있습니다',
    join: '무료로 시작',
    open: '마감 전 공고',
  },
  month: (m: number, year?: string) => (year ? `${year}.${m}월` : `${m}월`),
  pagePrev: '앞 쪽',
  pageNext: '다음 쪽',
}

export const PRE_TEXT = {
  title: '사전 공고',
  desc: '본 공고보다 먼저 올라온 공고입니다. 미리 준비할 것을 챙기세요',
  basis: (from: string, to: string, pre: number, sources: number) =>
    `게시일 **${from} ~ ${to}** 사이 모은 공고 중 사전 공고 **${pre}건**, 사전 수요 조사 **${sources}건**입니다. 한글 제목이 없는 공고는 원문 제목으로 보입니다.`,
  all: '전체',
  presol: '사전 공고',
  sources: '사전 수요 조사',
  openOnly: '답변 기한 남은 것만',
  head: { type: '유형', title: '제목 · 공고번호', posted: '게시일', deadline: '답변 기한' },
  mainNotice: '본 공고 나옴',
  howTitle: '사전 공고가 보이면 무엇을 하면 되나요',
  how: [
    { icon: 'bell', title: '사전 공고', desc: '무엇을 사려는지 본 공고 전에 미리 알리는 공고입니다. 본 공고는 대개 같은 공고번호로 올라옵니다.' },
    { icon: 'chat', title: '사전 수요 조사', desc: '이 일을 할 수 있는 업체가 있는지 알아보는 시장 조사입니다. 입찰이 아니며 회사 소개와 실적으로 답할 수 있습니다.' },
    { icon: 'check', title: '미리 할 일', desc: 'SAM.gov 등록 상태와 비슷한 실적을 정리해 두면 본 공고가 나왔을 때 서두르지 않아도 됩니다.' },
  ] as const,
  howLink: '교육 자료에서 공고 유형 더 보기',
}

export const AMEND_TEXT = {
  title: '변경 공고',
  desc: '같은 공고번호로 다시 올라온 공고입니다. 마감일과 조건이 바뀌었는지 확인하세요',
  basis: (from: string, to: string, groups: number, versions: number) =>
    `게시일 **${from} ~ ${to}** 사이 같은 공고번호로 두 번 이상 올라온 공고 **${groups}건**(올라온 판 ${versions}개)입니다. 낙찰 공지는 세지 않습니다. 판끼리 바뀐 곳을 나란히 비교하는 기능은 준비하고 있습니다.`,
  open: '진행 중',
  all: '전체',
  note: '진행 중은 마지막 판의 마감일이 지나지 않은 공고입니다',
  solOnly: (sol: string) => `공고번호 ${sol} 만 보고 있습니다`,
  solClear: '모두 보기',
  head: { title: '제목 · 공고번호', count: '올라온 횟수', first: '처음 게시', last: '마지막 게시', deadline: '마감일' },
  count: (n: number) => `${n}번`,
  deadlineChanged: '마감일 바뀜',
  awarded: '낙찰',
  expand: '판 목록 펼치기',
  collapse: '판 목록 접기',
  vhead: { no: '판', posted: '게시일', type: '유형', deadline: '마감일' },
  version: (n: number) => `${n}판`,
  changed: '바뀜',
  goNotice: '공고 상세',
  goSam: '원문',
}

export const SAMPLE_TEXT = {
  title: '공고문 예시',
  desc: '마감된 실제 공고 한 건을 WJbid 가 한글로 정리한 모습입니다',
  closedTag: '마감된 공고',
  facts: { sol: '공고번호', agency: '발주처', posted: '처음 게시', deadline: '제안 마감' },
  samLink: 'SAM.gov 원문 보기',
  sec: {
    overview: '공고 요약',
    scope: '업무 범위',
    period: '계약 기간',
    keyDates: '주요 일정',
    eligibility: '참여 자격',
    documents: '필요 서류',
    documentsCount: (n: number) => `${n}가지`,
    evaluation: '평가 방식',
    budget: '예산과 수량',
    notes: '꼭 볼 것',
  },
  dhead: { name: '서류', mandatory: '필수', format: '형식' },
  mandatory: '필수',
  optional: '선택',
  bandTitle: '마감 전 공고도 이렇게 정리해 드립니다',
  bandDesc: '무료 회원가입 후 마감 전 공고의 한글 공고문을 볼 수 있습니다',
  emptyTitle: '예시를 준비하고 있습니다',
  emptyDesc: '마감 전 공고에서 실제 공고를 둘러보세요',
  emptyLink: '마감 전 공고 보기',
}

export const AWARD_TEXT = {
  title: '최근 낙찰',
  desc: '미군이 낙찰을 알린 계약입니다',
  basis: (from: string, to: string, total: number, amountKnown: number, awardeeKnown: number) =>
    `공지일 **${from} ~ ${to}** 사이 낙찰 공지 **${total}건**, 그중 금액이 적힌 공지 **${amountKnown}건**입니다. `
    + (awardeeKnown > 0 ? `낙찰 업체 이름이 적힌 공지는 ${awardeeKnown}건입니다.` : '낙찰 업체 이름은 아직 모으지 않습니다.')
    + ' 한글 제목이 없는 공지는 원문 제목으로 보입니다.',
  nums: { count: '낙찰 공지', sum: (n: number) => `금액 공개 ${n}건 합계`, max: '가장 큰 금액', unit: '건' },
  all: '전체',
  awardee: (name: string) => `낙찰 업체 ${name}`,
  sort: { LATEST: '최신순', AMOUNT: '금액순' },
  head: { date: '공지일', title: '제목 · 공고번호', amount: '낙찰 금액', original: '원래 공고' },
  original: '원래 공고',
}

export const MONTHLY_TEXT = {
  title: '월별 동향',
  desc: '달마다 공고가 몇 건 올라왔는지 유형별로 보여 드립니다',
  basis: (from: string, to: string, total: number, emptyMonths: string, current: string) =>
    `게시일 **${from} ~ ${to}** 사이 모은 공고 **${total}건**입니다.`
    + (emptyMonths ? ` ${emptyMonths}은 모은 공고가 없습니다.` : '')
    + ` ${current}`,
  currentMonth: (month: number, day: number) => `${month}월은 오늘(${month}.${day})까지입니다.`,
  chartTitle: '달마다 공고 수',
  chartNote: '막대를 누르면 그 달 공고를 아래에 보여 드립니다',
  groups: { BID: '입찰 공고', PRE: '사전 공고', AWARD: '낙찰 공지', OTHER: '그 밖' },
  tip: (month: number, total: number) => `${month}월 합계 ${total}건`,
  head: { type: '유형', sum: '합계' },
  total: '합계',
  monthTitle: (month: string) => `${month} 공고`,
  monthSub: (n: number) => `${n}건, 게시일 최신순`,
  listHead: { type: '유형', title: '제목 · 공고번호', posted: '게시일' },
}

export const NAICS_TEXT = {
  title: '업종별 발주',
  desc: '업종(NAICS) 대분류마다 공고가 얼마나 나왔는지 보여 드립니다',
  basis: (from: string, to: string, total: number, coded: number, none: number) =>
    `게시일 **${from} ~ ${to}** 사이 모은 공고 ${total}건 중 업종 코드가 있는 **${coded}건**을 업종 대분류로 묶었습니다. 업종 코드가 없는 ${none}건은 맨 아래 ‘업종 표시 없음’에 둡니다.`,
  chartTitle: '업종 대분류별 공고 수',
  chartSub: '많은 순',
  none: '업종 표시 없음',
  tip: (name: string, n: number, pct: number) => `${name} ${n}건 (${pct}%)`,
  count: (n: number) => `${n}건`,
  subTitle: '세부 업종 코드별',
  subNote: '코드는 NAICS 여섯 자리입니다',
  recentTitle: '최근 공고',
  recentSize: 5,
  head: { title: '제목 · 공고번호', posted: '게시일' },
}
