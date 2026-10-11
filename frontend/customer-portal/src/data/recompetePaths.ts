import { findMenuItem, menuHref } from '../components/layout/menu'

/** 미 연방 계약 자료로 여는 화면의 주소 (CR-142). 메뉴 정의가 정본이라 거기서 읽는다 */
const hrefOf = (groupKey: string, itemKey: string) => {
  const found = findMenuItem(groupKey, itemKey)
  return found ? menuHref(found.item) : '/'
}

export const RECOMPETE_PATHS = {
  nextMonth: hrefOf('recompete', 'next-month'),
  calendar: hrefOf('recompete', 'recompete-calendar'),
  optionAlert: hrefOf('recompete', 'option-alert'),
  awardRange: hrefOf('awards', 'award-range'),
}
