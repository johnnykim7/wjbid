// 적격성 진단 화면의 날짜와 발주처 표기 (CR-137).
// 서버 시각은 존 없는 문자열이라 Date 로 바꾸면 보는 사람 기기 시간대로 해석된다. 글자를 잘라 서버가 준 시각 그대로 보인다.

const ISO = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/

/** 2026.08.28. 마감은 수집 때 시간대가 맞는지 확인되지 않아 시각을 빼고 날짜만 보인다 */
export function dotDate(s?: string | null): string {
  const m = s ? ISO.exec(s) : null
  return m ? `${m[1]}.${m[2]}.${m[3]}` : ''
}

/** 10/11 14:05 */
export function monthDayTime(s?: string | null): string {
  const m = s ? ISO.exec(s) : null
  return m && m[4] ? `${m[2]}/${m[3]} ${m[4]}:${m[5]}` : ''
}

/** 발주 기관 경로(부처.군.사령부.부대.사무소)에서 공고를 낸 끝 두 단계만 보인다 */
export function officeName(org?: string | null): string {
  if (!org) return ''
  const parts = org.split('.').map((p) => p.replace(/\s+/g, ' ').trim()).filter(Boolean)
  return parts.slice(-2).join(' · ')
}
