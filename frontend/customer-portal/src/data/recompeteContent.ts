import type { FederalAwardType, RangeGroup } from '../types/federal'

/**
 * 미 연방 계약 자료로 여는 화면 문구 (CR-142, T3-3 「미 연방 계약 자료로 여는 화면 공통」, 화면 정본 속페이지_시안_4묶음_재발주.html).
 * 자료 기준 줄은 **굵게** 표시를 쓰고, 건수와 날짜, 설정값은 화면이 응답에서 넣는다.
 */

/** 늦게 공개되는 기간. 30일 이상이면 달로 읽는다 */
const lagSpan = (days: number) => (days >= 30 ? `최근 ${Math.round(days / 30)}개월` : `최근 ${days}일`)

export const FEDERAL_COMMON = {
  source: (total: string, from: string, last: string, lagDays: number | null) =>
    `미 연방 계약 공개 자료(USAspending.gov). 수행 장소와 업체가 모두 한국인 국방부 계약 **${total}건**(${from} 부터). 마지막으로 받은 때 **${last}**(한국 시각).` +
    (lagDays != null ? ` 국방부 계약 자료는 약 ${lagDays}일 늦게 공개되어 ${lagSpan(lagDays)} 계약과 옵션 연장은 아직 보이지 않을 수 있습니다.` : ''),
  syncing: '미 연방 계약 자료를 받고 있습니다',
  type: {
    PURCHASE_ORDER: '구매 주문',
    DEFINITIVE_CONTRACT: '단독 계약',
    DELIVERY_ORDER: '주문',
    BPA_CALL: 'BPA 주문',
  } satisfies Record<FederalAwardType, string>,
  awardPage: 'USAspending 계약 쪽',
  naics: {
    all: '업종 전체',
    hint: '코드나 이름으로 찾기',
    search: '업종 코드나 이름',
    none: '맞는 업종이 없습니다',
    clear: '업종 전체로',
    count: (n: number) => `${n.toLocaleString('en-US')}건`,
  },
  guest: {
    title: '로그인하면 볼 수 있습니다',
    desc: '미 연방 계약 자료로 낸 낙찰가 범위는 회원에게 보여 드립니다',
    login: '로그인',
    register: '회원가입',
  },
}

export const AWARD_RANGE_TEXT = {
  title: '낙찰가 범위',
  desc: '같은 업종의 지난 한국 내 미군 계약 금액을 범위로 보여 드립니다. 가격을 정할 때 참고하는 자료입니다',
  excluded: (n: number) => ` 금액은 계약에 지금까지 확정된 금액(달러)입니다. 0 이하인 계약 **${n.toLocaleString('en-US')}건**은 뺐습니다.`,
  target: {
    label: '공고번호 또는 업종',
    placeholder: '공고번호를 넣거나 업종을 고르세요',
    search: '공고번호, 업종 코드나 이름',
    byNotice: (no: string) => `공고번호 ${no} 의 업종으로 찾기`,
    notice: (no: string) => `공고 ${no}`,
  },
  word: { label: '작업 설명 낱말', optional: '선택', placeholder: '예: roof' },
  find: '찾기',
  noticeLine: (no: string, title: string | null) => `공고 ${no}${title ? ` ${title}` : ''}`,
  sub: (q: string | null, n: number) =>
    `${q ? `작업 설명에 "${q}" 가 든 계약 ` : '근거 계약 '}${n.toLocaleString('en-US')}건. 막대의 상자는 아래 4분의 1부터 위 4분의 1까지, 굵은 선은 가운데입니다. 금액 눈금은 10배씩 늘어납니다.`,
  group: {
    STANDALONE: ['상위 계약 없는 계약', '구매 주문, 단독 계약'],
    ORDER: ['상위 계약 안 주문', '주문, BPA 주문'],
  } satisfies Record<RangeGroup, [string, string]>,
  count: (n: number) => `${n.toLocaleString('en-US')}건`,
  few: (n: number, min: number) => `**${n}건**이라 범위를 내지 않습니다. ${min}건 이상일 때 범위를 보여 드립니다.`,
  stats: { min: '가장 작은 금액', p25: '아래 4분의 1', p50: '가운데', p75: '위 4분의 1', max: '가장 큰 금액' },
  evidence: {
    title: '근거 계약',
    sub: (n: number) => `최근 시작순 ${n}건`,
    head: { start: '시작일', desc: '작업 설명', type: '종류', amount: '금액' },
    note: '줄을 누르면 USAspending 계약 쪽이 새 창으로 열립니다. 우리 낙찰 공지 금액과는 섞지 않습니다.',
  },
  syncingDesc: '자료를 받는 대로 이곳에 업종별 금액 범위가 보입니다',
  empty: {
    pick: '공고번호나 업종을 넣어 주세요',
    none: '이 업종의 한국 내 계약이 없습니다',
    noNaics: '이 공고에는 업종 코드가 없습니다. 업종을 골라 주세요',
    noNotice: '이 공고번호를 찾지 못했습니다. 업종을 골라 주세요',
  },
  band: {
    title: '이 업종의 마감 전 공고를 보세요',
    desc: '범위를 보고 가격을 정했다면 지금 열린 공고에 바로 신청하세요',
  },
}

/** 아래 띠 이동 단추 */
export const FEDERAL_LINKS = {
  openNotices: '마감 전 공고',
}
