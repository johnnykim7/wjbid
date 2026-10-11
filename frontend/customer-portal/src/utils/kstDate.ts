/** 날짜만 다루는 칸(만료일 등)의 변환 (CR-136) */

/** YYYY-MM-DD 를 그 날 0시(브라우저 시간대)로. 날짜만 다루므로 시간대를 옮기지 않는다 */
export const toDate = (ymd: string): Date => {
  const [y, m, d] = ymd.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export const toYmd = (d: Date): string =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

const SERVICE_ZONE = 'Asia/Seoul'

/** 오늘(한국 날짜)부터 그 날까지 남은 날. 지난 날이면 음수 */
export const daysLeftKst = (ymd: string): number => {
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: SERVICE_ZONE }).format(new Date())
  const utc = (s: string) => {
    const [y, m, d] = s.split('-').map(Number)
    return Date.UTC(y, m - 1, d)
  }
  return Math.round((utc(ymd) - utc(today)) / 86_400_000)
}
