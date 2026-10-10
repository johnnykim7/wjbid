/**
 * CR-132 — 메뉴의 미답변 숫자를 다시 읽게 알린다. 답변을 보낸 화면과 메뉴가 서로 떨어져 있어 창 이벤트로 잇는다.
 */
export const INQUIRY_COUNTS_CHANGED = 'inquiry-counts-changed'

export const notifyInquiryCountsChanged = () => window.dispatchEvent(new Event(INQUIRY_COUNTS_CHANGED))
