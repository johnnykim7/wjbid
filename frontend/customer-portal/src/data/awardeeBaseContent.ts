/** 낙찰 업체, 기지별 지도 문구 (CR-136, T3-3 「낙찰 업체」 「기지별 지도」). "**굵게**" 는 Rich 가 굵게 그린다 */

export const AWARDEE_TEXT = {
  title: '낙찰 업체',
  desc: '미군 낙찰 공지에 이름이 오른 업체입니다. 어느 업체가 어떤 일을 따 갔는지 보세요',
  basis: (from: string, to: string, total: number, named: number) =>
    `공지일 **${from} ~ ${to}** 낙찰 공지 **${total}건** 중 업체 이름이 있는 **${named}건**을 업체별로 셉니다. `
    + (total > named ? `업체 이름이 오지 않은 낙찰 공지 ${total - named}건은 세지 않습니다. ` : '')
    + '금액은 금액이 공개된 공지만 더합니다.',
  search: '업체 이름으로 찾기',
  searchGo: '찾기',
  clear: '찾기 풀기',
  sort: { COUNT: '낙찰 건수순', AMOUNT: '금액순' },
  head: { rank: '순위', name: '업체 이름', count: '낙찰', sum: '금액 합계', last: '최근 공지일' },
  unit: '건',
  amountPart: (known: number) => `금액 공개 ${known}건`,
  open: '펼치기',
  close: '접기',
  uei: (v: string) => `UEI ${v}`,
  goNotice: '공고 상세',
  emptyTitle: '낙찰 업체 자료를 모으고 있습니다',
  emptyDesc: 'SAM.gov 낙찰 공지에 업체 이름이 오는 대로 이곳에 업체별로 모아 보여 드립니다',
  emptyAction: '최근 낙찰 보기',
  noMatch: (q: string) => `"${q}" 이(가) 들어간 업체가 없습니다`,
  footnote: '업체 이름은 SAM.gov 에 적힌 그대로입니다. 같은 업체는 SAM 고유번호(UEI)로 묶습니다.',
  band: { title: '낙찰 업체가 낸 공고를 미리 받아 보세요', desc: '무료로 가입하면 마감 전 공고와 알림을 받을 수 있습니다' },
}

export const BASEMAP_TEXT = {
  title: '기지별 지도',
  desc: '주한미군 기지마다 공고와 낙찰이 얼마나 나왔는지 보여 드립니다',
  basis: (from: string, to: string, total: number, unmatched: number) =>
    `게시일 **${from} ~ ${to}** 공고번호 **${total}개**. 공고 제목에 기지 이름이 있는 공고만 셉니다. `
    + `기지를 알 수 없는 공고 **${unmatched}개**. 공고 수는 공고번호 기준이고 제목에 기지가 둘이면 둘 다에 셉니다.`,
  mapTitle: '지도',
  mapAria: '기지별 공고 지도',
  legend: { few: '공고 적음', many: '공고 많음', dmz: '점선: 휴전선' },
  tableTitle: (n: number) => `기지 ${n}곳`,
  head: { name: '기지', region: '지역', opp: '공고', award: '낙찰', open: '마감 전' },
  noPosition: '위치 없음',
  dotLabel: (name: string, n: number) => `${name} ${n}`,
  dotTitle: (name: string, n: number) => `${name} 공고 ${n}`,
  recentTitle: (name: string) => `${name} 최근 공고`,
  recentSub: (n: number) => `공고번호마다 마지막 판, ${n}건`,
  recentHead: { type: '유형', title: '제목 · 공고번호', posted: '게시일', deadline: '마감일' },
  recentEmpty: '이 기지 공고가 없습니다',
  emptyTitle: '기지 목록을 준비하고 있습니다',
  emptyDesc: '기지 이름과 위치를 정리하는 대로 이곳에 지도와 기지별 공고 수를 보여 드립니다',
  emptyAction: '마감 전 공고 보기',
  band: { title: '관심 기지의 새 공고를 놓치지 마세요', desc: '무료로 가입하면 마감 전 공고와 알림을 받을 수 있습니다' },
}
