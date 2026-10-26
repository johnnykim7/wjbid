// 입찰 상태 이름. 화면마다 따로 두면 이름이 갈라져서 관리자 화면은 이 한 곳에서 읽는다.
// 이름은 docs/AI-SDLC/T1-10_용어표.md 를 따르고 고객 포털 types/index.ts 의 STATE_LABEL 과 같게 둔다.
export const STATE_LABELS: Record<string, string> = {
  CREATED: '신청 접수',
  DOCS_PENDING: '문서 대기',
  DOCS_RECEIVED: '문서 접수 완료',
  ANALYZING: '분석 중',
  GENERATING: '문서 작성 중',
  REVIEW: '관리자 검토',
  CONFIRMED: '확정',
  SUBMITTED: '제출 완료',
  AWARDED: '낙찰',
  NOT_AWARDED: '탈락',
  CLOSED: '종료',
}

export type StateTone = 'gray' | 'info' | 'warning' | 'success' | 'destructive'

// 상태 배지 색. 진행 단계, 사람 손 기다림, 끝남, 탈락, 처음과 닫힘으로 묶는다(CR-182 4.4)
export const STATE_TONE: Record<string, StateTone> = {
  CREATED: 'gray',
  DOCS_PENDING: 'warning',
  DOCS_RECEIVED: 'info',
  ANALYZING: 'info',
  GENERATING: 'info',
  REVIEW: 'warning',
  CONFIRMED: 'success',
  SUBMITTED: 'success',
  AWARDED: 'success',
  NOT_AWARDED: 'destructive',
  CLOSED: 'gray',
}
