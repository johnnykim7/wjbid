/** 날짜만 다루는 칸의 변환 (CR-136). 날짜만 다루므로 시간대를 옮기지 않는다 */
export const toDate = (ymd: string): Date => {
  const [y, m, d] = ymd.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export const toYmd = (d: Date): string =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
