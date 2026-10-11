import type { ContractDateKind, FederalAwardType, RangeGroup, RecompeteCategory, RecompeteSort } from '../types/federal'

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
  predict: '예측',
  oneTime: '한 번짜리 공사일 수 있음',
  category: {
    RECOMPETE: '재발주 후보',
    EXTENDABLE: '연장 가능',
    IN_PARENT: '상위 계약 안 주문',
    UNCONFIRMED: '옵션 끝날짜 확인 전',
  } satisfies Record<RecompeteCategory, string>,
  type: {
    PURCHASE_ORDER: '구매 주문',
    DEFINITIVE_CONTRACT: '단독 계약',
    DELIVERY_ORDER: '주문',
    BPA_CALL: 'BPA 주문',
  } satisfies Record<FederalAwardType, string>,
  kind: {
    CURRENT_END: '지금 끝날짜',
    POTENTIAL_END: '옵션 끝날짜',
  } satisfies Record<ContractDateKind, string>,
  why: {
    RECOMPETE: (potential: string, type: string) => `옵션 끝날짜 ${potential} 에 옵션이 모두 끝나는 ${type}`,
    EXTENDABLE: (end: string, potential: string) => `지금 끝날짜 ${end}, 옵션 끝날짜 ${potential} 까지 연장할 수 있음`,
    IN_PARENT: (parent: string | null, type: string) => (parent ? `상위 계약 ${parent} 안의 ${type}` : `상위 계약 안의 ${type}`),
    UNCONFIRMED: '옵션 끝날짜를 아직 확인하지 않았습니다',
  },
  date: { start: '시작일', unknown: '확인 전' },
  awardPage: 'USAspending 계약 쪽',
  solicitation: '원래 공고번호',
  original: '원래 공고',
  naicsLabel: '업종',
  head: { contract: '계약', amount: '금액', dates: '날짜' },
  sort: { END: '끝날짜순', AMOUNT: '금액순' } satisfies Record<RecompeteSort, string>,
  count: (label: string | null, total: number, from: number, to: number) =>
    `${label ? label + ' ' : ''}**${total.toLocaleString('en-US')}**건${total > to - from + 1 ? ` 중 ${from}-${to}` : ''}`,
  emptyMonth: '이 달에 끝나는 계약이 없습니다',
  emptyCategory: '이 구분의 계약이 없습니다',
  listNote: '줄을 누르면 USAspending 계약 쪽이 새 창으로 열립니다. 원래 공고번호가 우리 공고에 있으면 "원래 공고" 로 공고 상세에 갑니다.',
  monthTitle: (ym: string) => `${ym.slice(0, 4)}년 ${Number(ym.slice(5, 7))}월`,
  endingIn: (label: string) => `${label}에 끝나는 계약`,
  endingSub: '그 달에 걸린 날짜 순',
  daysLeft: (n: number) => (n < 0 ? '끝남' : `D-${n}`),
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
    desc: '미 연방 계약 자료로 낸 예측과 범위, 우리 회사 계약 알림은 회원에게 보여 드립니다',
    login: '로그인',
    register: '회원가입',
  },
}

export const RULE_TEXT = {
  title: '구분은 이렇게 냅니다',
  sub: '지난 계약 자료로 낸 예측입니다',
  open: '펼치기',
  close: '접기',
  rules: [
    { category: 'IN_PARENT', text: '계약 종류가 주문이나 BPA 주문이면 여기에 둡니다. 상위 계약을 가진 업체끼리 다시 나누는 일이 많아 공개 공고로 나오지 않을 수 있습니다' },
    { category: 'UNCONFIRMED', text: '구매 주문이나 단독 계약인데 옵션 끝날짜를 아직 받지 못했습니다' },
    { category: 'RECOMPETE', text: '구매 주문이나 단독 계약의 옵션 끝날짜가 그 달입니다. 옵션을 다 썼으니 다시 공고될 수 있습니다' },
    { category: 'EXTENDABLE', text: '지금 끝날짜가 그 달이고 옵션 끝날짜가 더 뒤입니다. 공고 대신 옵션으로 연장될 수 있습니다' },
  ] satisfies { category: RecompeteCategory; text: string }[],
  foot: '위에서부터 처음 맞는 구분을 붙입니다. 공사 업종은 재발주 후보여도 "한 번짜리 공사일 수 있음" 을 함께 붙입니다. 처음 몇 달은 실제 공고와 대조해 규칙을 고칩니다.',
}

export const NEXT_MONTH_TEXT = {
  title: '다음 달 공고',
  desc: '다음 달 끝나는 미군 계약 중 다시 공고될 것으로 보이는 계약입니다. 지난 계약 자료로 낸 예측입니다',
  nums: {
    RECOMPETE: '옵션이 모두 끝나 다시 공고될 수 있는 계약',
    EXTENDABLE: '공고 대신 옵션으로 연장될 수 있는 계약',
    IN_PARENT: '상위 계약 업체끼리 나눌 수 있어 공고로 안 나올 수 있음',
  },
  unit: '건',
  unconfirmed: (label: string, n: number) => `${label}. 옵션 끝날짜 확인 전 **${n}건**은 목록에만 보입니다. 숫자 칸을 누르면 그 구분만 봅니다.`,
  empty: '다음 달 끝나는 계약이 없습니다',
  band: {
    title: '1년 치를 달마다 보세요',
    desc: '재발주 달력은 이번 달부터 12달을 한눈에 보여 드립니다. 우리 회사 계약은 옵션 연장 알림에 넣어 두세요',
  },
  syncingDesc: '자료를 받는 대로 이곳에 다음 달 끝나는 계약이 보입니다',
  syncingBand: {
    title: '우리 회사 계약은 지금 넣어 둘 수 있습니다',
    desc: '공개 자료에 없으면 끝날짜를 직접 넣어 알림을 받으세요',
  },
}

export const CALENDAR_TEXT = {
  title: '재발주 달력',
  desc: '이번 달부터 12달 동안 끝나는 미군 계약을 달마다 모았습니다. 지난 계약 자료로 낸 예측입니다',
  thisMonth: '이번 달',
  unconfirmedShort: '확인 전',
  syncingDesc: '자료를 받는 대로 이곳에 달마다 끝나는 계약이 보입니다',
  band: {
    title: '우리 회사 계약도 끝나기 전에 알려 드립니다',
    desc: '계약번호를 넣어 두면 지금 끝날짜와 옵션 끝날짜 전에 이메일로 알려 드립니다',
  },
}

export const OPTION_ALERT_TEXT = {
  title: '옵션 연장 알림',
  desc: (days: number[]) =>
    days.length > 0
      ? `우리 회사 계약번호를 넣으면 끝날짜와 옵션 끝날짜를 찾아 보이고, 지금 끝날짜와 옵션 끝날짜 ${days.map((d) => `${d}일 전`).join('과 ')} 오전 9시(한국 시각)에 이메일로 알려 드립니다`
      : '우리 회사 계약번호를 넣으면 끝날짜와 옵션 끝날짜를 찾아 보입니다',
  off: '지금은 계약 끝 메일을 보내지 않습니다',
  receiver: '받는 이메일',
  look: {
    label: '계약번호',
    placeholder: '예: W90VN6-24-C-0002',
    find: '찾기',
    hint: '하이픈과 빈칸은 빼고 찾습니다. 공개 자료에 없으면 끝날짜를 직접 넣을 수 있습니다.',
  },
  found: {
    title: '공개 자료에서 찾았습니다',
    basis: (date: string) => `공개 자료 ${date} 기준`,
    amount: '금액',
    plan: (items: string[]) => `알림 날 ${items.join(', ')}. 오전 9시(한국 시각)`,
    planItem: (kind: string, days: number, date: string, past: boolean) => `${kind} ${days}일 전 **${date}**${past ? '(지남)' : ''}`,
    register: '이 계약 알림 받기',
    already: '이미 넣은 계약입니다',
  },
  miss: {
    title: '공개 자료에서 찾지 못했습니다',
    desc: (lagDays: number | null) =>
      (lagDays != null ? `국방부 계약은 약 ${lagDays}일 늦게 공개됩니다. ` : '') +
      '계약서에 적힌 끝날짜를 직접 넣어 주시면 그 날짜로 알려 드립니다. 직접 넣은 날짜는 언제든 고칠 수 있습니다.',
    failTitle: '지금 공개 자료를 찾지 못했습니다',
    failDesc: '끝날짜를 직접 넣거나 잠시 뒤 다시 찾아 주세요',
    retry: '다시 찾기',
    end: '지금 끝날짜',
    potential: '옵션 끝날짜',
    optional: '선택',
    endPlaceholder: '날짜 고르기',
    potentialPlaceholder: '모르면 비워 두세요',
    submit: '직접 넣고 알림 받기',
  },
  my: {
    title: '내 계약',
    count: (n: number) => `${n}건`,
    sub: '지금 끝날짜가 가까운 순',
    source: { PUBLIC: '공개 자료', MANUAL: '직접 넣음' },
    edit: '날짜 고치기',
    editDone: '다 고침',
    clearPotential: '옵션 끝날짜 지우기',
    remove: '지우기',
    removeAsk: '지우면 알림이 멈춥니다',
    removeYes: '지우기',
    removeNo: '그대로 두기',
    next: '다음 알림',
    last: '마지막 알림',
    unknown: '모름',
    noNext: '남은 알림 없음',
    noLast: '아직 없음',
    nextLabel: (kind: string, days: number) => `${kind} ${days}일 전`,
    changed: (date: string) => `공개 자료에서 날짜가 바뀌었습니다(${date}). 바뀐 날짜로 알려 드립니다.`,
    empty: '넣은 계약이 없습니다',
    note: '공개 자료로 찾은 계약은 매일 받는 자료에 따라 날짜가 바뀌고, 바뀌면 그 날을 줄 아래에 적습니다. 날짜를 고칠 수 있는 것은 직접 넣은 계약뿐입니다. 지우면 그 계약의 알림이 멈춥니다.',
  },
  error: {
    CONTRACT_PIID_INVALID: '계약번호를 넣어 주세요',
    MEMBER_CONTRACT_DUPLICATE: '이미 넣은 계약입니다',
    CONTRACT_END_DATE_REQUIRED: '지금 끝날짜를 골라 주세요',
    CONTRACT_DATES_INVALID: '옵션 끝날짜는 지금 끝날짜와 같거나 그 뒤여야 합니다',
    MEMBER_CONTRACT_PUBLIC_DATES: '공개 자료에서 찾은 계약의 날짜는 고칠 수 없습니다',
    fallback: '저장하지 못했습니다. 잠시 뒤 다시 해 주세요',
  } as Record<string, string>,
  band: {
    title: '다음 달 끝나는 미군 계약도 보세요',
    desc: '다시 공고될 것으로 보이는 계약을 근거와 함께 보여 드립니다',
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
  nextMonth: '다음 달 공고',
  calendar: '재발주 달력',
  optionAlert: '옵션 연장 알림',
  openNotices: '마감 전 공고',
}
