/** 유효기간 알림 화면과 서류함 만료일 칸의 문구 (CR-136, T3-3 「유효기간 알림」) */

/** 알림 시점 [30, 7] 을 "30일 전과 7일 전" 으로 */
const daysPhrase = (days: number[]) => days.map((d) => (d === 0 ? '만료일 당일' : `${d}일 전`)).join('과 ')

export const EXPIRY_TEXT = {
  title: '유효기간 알림',
  // 알림 시각은 백엔드 설정 app.doc-expiry.cron 의 기본값(한국 시각 9시)과 맞춘다
  desc: (alertDays: number[]) => `만료일을 넣어 두면 만료 ${daysPhrase(alertDays)} 오전 9시(한국 시각)에 이메일로 알려 드립니다`,
  descOff: '지금은 만료 메일을 보내지 않습니다. 만료일과 남은 날은 그대로 볼 수 있습니다',
  receiver: '받는 이메일',
  cards: {
    expired: { label: '만료됨', hint: '새 서류로 바꿔 올리세요' },
    soon: { label: (soonDays: number | null) => (soonDays === null ? '곧 만료' : `곧 만료 (${soonDays}일 안)`), hint: '메일로 한 번 더 알려 드립니다', hintOff: '만료일이 가까운 서류입니다' },
    noDate: { label: '만료일 없음', hint: '만료일을 넣어야 알림이 갑니다' },
  },
  unit: '건',
  listTitle: (n: number) => `서류 ${n}건`,
  listSub: '공통 서류함에 올린 서류',
  filtered: (label: string) => `${label}만 보는 중`,
  showAll: '전체 보기',
  noDateYet: '만료일을 넣은 서류가 없습니다. 서류마다 만료일을 넣으면 만료 전에 메일로 알려 드립니다',
  cols: { name: '서류 이름', kind: '종류', expiresOn: '만료일', left: '남은 날', lastAlert: '마지막 알림' },
  remove: '지우기',
  removeAria: (name: string) => `${name} 만료일 지우기`,
  pickAria: (name: string) => `${name} 만료일`,
  expired: '만료됨',
  today: 'D-day',
  dday: (n: number) => `D-${n}`,
  noDate: '만료일 없음',
  noAlert: '-',
  saveFail: '만료일을 저장하지 못했습니다. 다시 시도해 주세요',
  loadFail: '서류를 불러오지 못했습니다',
  retry: '다시 시도',
  empty: { title: '서류함에 올린 서류가 없습니다', desc: '공통 서류함에 서류를 올리면 이곳에서 만료일을 넣고 알림을 받을 수 있습니다', action: '공통 서류함' },
  note: { before: '만료일은 서류에 적힌 날을 넣으세요. 만료일을 고치면 알림도 새 날짜에 맞춰 다시 보냅니다. 서류 파일을 바꾸려면 ', link: '공통 서류함', after: '에서 올리세요.' },
  band: { title: '새 서류를 받으셨나요?', desc: '공통 서류함에서 바꿔 올리면 공고마다 다시 올리지 않아도 됩니다', action: '공통 서류함' },
}

export const DATE_PICK_TEXT = {
  add: '만료일 넣기',
  close: '닫기',
  save: '저장',
  saveWith: (ymd: string) => `${ymd} 저장`,
}

/** 서류함 카드와 줄의 만료일 한 줄 */
export const VAULT_EXPIRY_TEXT = {
  label: '만료일',
  none: '만료일 없음',
  setOnExpiryPage: '유효기간 알림에서 넣기',
  shortcut: '유효기간 알림',
  field: '만료일',
  fieldHint: '선택. 넣어 두면 만료 전에 메일로 알려 드립니다',
  clear: '비우기',
}
