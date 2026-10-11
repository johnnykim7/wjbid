/**
 * 적격성 진단 화면 문구 (CR-137, T3-3 「적격성 진단」, 「공고별 진단」).
 * 화면은 회원이 남긴 답을 셀 뿐 참여할 수 있는지 판정하지 않으므로 "참여 가능" 같은 판정 말은 두지 않는다(BIZ-032).
 */
import type { EligibilityAnswerValue } from '../types'
import { INQUIRY_TYPE, inquiryHref } from './subpageContent'

export const ELIGIBILITY_PATH = '/fit/eligibility'
export const eligibilityHref = (noticeId: string) => `${ELIGIBILITY_PATH}/${noticeId}`
const EXPERT_TITLE = '적격성 진단 상담'

/** 공고에서 열면 공고번호를 제목에, 없음과 확인 필요로 답한 자격 이름을 문의 내용에 채워 넘긴다 */
export const expertHref = (solicitationNumber?: string, need: { label: string; titles: string[] }[] = []) => {
  const lines = need.filter((n) => n.titles.length > 0).map((n) => `${n.label}: ${n.titles.join(', ')}`)
  return inquiryHref(INQUIRY_TYPE.consulting,
    solicitationNumber ? `${EXPERT_TITLE} ${solicitationNumber}` : EXPERT_TITLE,
    lines.length > 0 ? lines.join('\n') : undefined)
}

export const ELIGIBILITY_TEXT = {
  title: '적격성 진단',
  desc: '공고가 요구하는 참가 자격을 하나씩 짚어 보고, 우리 회사에 있는지 직접 답해 두는 곳입니다',
  steps: [
    { name: '공고 고르기', desc: '공고 분석이 끝난 공고를 고릅니다' },
    { name: '자격마다 답하기', desc: '있음, 없음, 확인 필요 중 하나를 누르면 바로 저장됩니다' },
    { name: '모자란 자격은 전문가에게', desc: '없음, 확인 필요로 남은 자격을 전문가가 함께 봐 드립니다' },
  ],
  open: { title: '마감 전 공고', desc: '공고 분석이 끝나 진단할 수 있는 공고' },
  past: { title: (n: number) => `지난 공고 ${n}건`, desc: '마감이 지난 공고. 미리 해 보기용입니다' },
  empty: {
    title: '진단할 수 있는 마감 전 공고가 아직 없습니다',
    desc: '마감 전 공고는 분석이 끝나는 대로 여기에 올라옵니다',
    pastHint: '그동안 아래 지난 공고로 미리 해 보세요',
  },
  cols: { notice: '공고', deadline: '마감일', items: '참가 자격', mine: '내 답' },
  count: (n: number) => `${n}개`,
  notStarted: '아직 안 함',
  loadError: '불러오지 못했습니다',
  retry: '다시 시도',
  guest: {
    title: '로그인하면 진단할 공고가 보입니다',
    desc: '답은 회원마다 저장되어 다음에 와도 이어서 할 수 있습니다',
    login: '로그인',
    register: '회원가입',
  },
  expert: {
    title: '어느 자격이 걸리는지 모르겠다면',
    desc: '공고번호를 남겨 주시면 전문가가 참가 자격을 함께 봐 드립니다. 상담은 1:1 문의로 접수됩니다',
    action: '전문가 진단 신청',
  },
}

export const ELIGIBILITY_DETAIL_TEXT = {
  past: '마감 지남',
  open: '마감 전',
  meta: { number: '공고번호', deadline: '마감', office: '발주처' },
  noticeLink: '공고 상세 보기',
  pastNote: '마감이 지난 공고입니다. 진단 방법을 미리 익혀 두는 용도로 쓰세요.',
  sumTitle: (n: number) => `통과 조건 ${n}개`,
  sumSuffix: ' 중 내 답',
  sumNote: '통과 조건은 공고 분석이 "못 갖추면 평가에서 빠진다" 고 표시한 자격입니다',
  noGate: '통과 조건으로 표시된 자격이 없습니다. 아래 자격을 확인해 주세요.',
  nums: { have: '있음', lack: '없음', unsure: '확인 필요', unanswered: '아직 답 안 함' },
  unit: '개',
  listTitle: (n: number) => `참가 자격 ${n}개`,
  listDesc: '공고 분석이 원문에서 뽑은 자격입니다. 원문 위치로 공고문에서 확인하세요',
  gate: '통과 조건',
  mandatory: '필수',
  sourceRef: '원문 위치',
  evidenceBy: '증빙 서류',
  stale: '공고 분석이 바뀌었습니다. 다시 답해 주세요',
  vaultHave: '서류함에 맞을 수 있는 서류',
  vaultMore: (n: number) => `외 ${n}건`,
  vaultUploaded: (category: string, date: string) => `${category} · ${date} 올림`,
  vaultNone: (category: string) => ({ before: '서류함에 ', name: category, after: ' 서류가 아직 없습니다' }),
  vaultLink: '서류함으로',
  saved: (at: string) => `${at} 저장`,
  saving: '저장 중',
  saveError: '저장하지 못했습니다. 다시 눌러 주세요',
  notJudged: '이 화면은 회원님이 남긴 답을 세어 보여 줄 뿐, 입찰에 참여할 수 있는지 판정하지 않습니다. 서류함 서류는 증빙 서류 글과 서류 종류 이름이 맞아 붙인 것이니 내용은 직접 확인하세요.',
  expertTitle: (lack: number, unsure: number) =>
    [lack > 0 && `없음 ${lack}개`, unsure > 0 && `확인 필요 ${unsure}개`].filter(Boolean).join(', ') + '가 남았습니다',
  expertDesc: '전문가가 이 공고의 자격을 함께 봐 드립니다',
  notFound: { title: '진단할 수 없는 공고입니다', desc: '공고 분석이 끝난 공고만 진단할 수 있습니다', back: '적격성 진단 목록으로' },
}

/** 답 단추 순서와 이름. 채운 색은 단추마다 다르다(화면 정본 detail) */
export const ANSWER_OPTIONS: { value: EligibilityAnswerValue; label: string; tone: string }[] = [
  { value: 'HAVE', label: '있음', tone: 'h' },
  { value: 'LACK', label: '없음', tone: 'l' },
  { value: 'UNSURE', label: '확인 필요', tone: 'u' },
]
