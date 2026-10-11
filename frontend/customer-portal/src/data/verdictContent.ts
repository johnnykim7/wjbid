/**
 * 공고별 판정 화면 문구 (CR-140, T3-3 「공고별 판정」, 「공고 검색」, 「공고 상세」, 「공고별 진단」).
 * 판정은 서버가 회원의 답과 붙인 서류로 낸 값을 그대로 보이고, 화면은 걸린 곳 한 줄만 글로 엮는다(BIZ-040, BIZ-044).
 */
import type { EligibilityBlocker, EligibilityBlockerProblem, EligibilityVerdict } from '../types'
import { dotDate } from '../utils/eligibilityFormat'
import { INQUIRY_TYPE, inquiryHref } from './subpageContent'

export const VERDICT_PATH = '/fit/verdicts'

/** 꼬리표 이름과 색 이름. 색은 wjbid-verdict.css 의 vd-ok, vd-doc, vd-no, vd-pre */
export const VERDICT_TAG: Record<EligibilityVerdict, { label: string; tone: string }> = {
  ELIGIBLE: { label: '참여 가능', tone: 'ok' },
  DOCS_MISSING: { label: '서류 부족', tone: 'doc' },
  DIFFICULT: { label: '어려움', tone: 'no' },
  UNDECIDED: { label: '판정 전', tone: 'pre' },
}

/** 개수와 뜻을 보이는 순서 */
export const VERDICT_ORDER: EligibilityVerdict[] = ['ELIGIBLE', 'DOCS_MISSING', 'DIFFICULT', 'UNDECIDED']

export const VERDICT_TEXT = {
  title: '공고별 판정',
  desc: '적격성 진단에 남긴 답과 서류함 서류로 공고마다 우리 회사가 참여할 수 있는지 보여 드립니다',
  legend: {
    ELIGIBLE: '통과 조건을 모두 있음으로 답하고 증빙 서류도 붙였습니다',
    DOCS_MISSING: '모두 있음으로 답했지만 붙인 서류가 없거나 만료된 자격이 있습니다',
    DIFFICULT: '없음으로 답한 통과 조건이 있습니다',
    UNDECIDED: '답하지 않았거나 확인 필요로 남은 통과 조건이 있습니다',
  } satisfies Record<EligibilityVerdict, string>,
  unit: '건',
  countCap: (n: number) =>
    `판정할 수 있는 공고 ${n}건(마감 전과 지난 공고) 기준입니다. 누르면 그 판정의 공고만 남고, 다시 누르면 풀립니다`,
  open: { title: '마감 전 공고', desc: '공고 분석이 끝나 판정할 수 있는 공고' },
  past: { title: (n: number) => `지난 공고 ${n}건`, desc: '마감이 지난 공고. 마감일 최근 순' },
  empty: {
    title: '판정할 수 있는 마감 전 공고가 아직 없습니다',
    desc: '마감 전 공고는 분석이 끝나는 대로 여기에 올라옵니다',
    pastHint: '그동안 아래 지난 공고로 미리 보세요',
  },
  noneOfVerdict: '이 판정인 공고가 없습니다',
  cols: { verdict: '우리 회사', notice: '공고', why: '걸린 곳', deadline: '마감일' },
  guest: {
    title: '로그인하면 공고마다 판정이 보입니다',
    desc: '판정은 적격성 진단에 남긴 답으로 냅니다',
    login: '로그인',
    register: '회원가입',
  },
  notJudged: '판정은 회원님이 남긴 답과 붙인 서류로만 냅니다. 서류 내용이 공고 조건에 맞는지는 보지 않습니다.',
  expertTitle: '공고별 판정 상담',
}

export const verdictExpertHref = () => inquiryHref(INQUIRY_TYPE.consulting, VERDICT_TEXT.expertTitle)

/** 공고 상세의 판정 상자 */
export const VERDICT_BOX_TEXT = {
  label: '우리 회사 판정',
  answer: '적격성 진단에서 답하기',
  colAll: '자격',
  colBlocked: '걸린 자격',
  colSource: '원문 위치',
  colAnswer: '내 답',
  colDocument: '증빙 서류',
  foot: '적격성 진단에 남긴 답과 붙인 서류로만 낸 판정입니다. 서류 내용이 공고 조건에 맞는지는 보지 않습니다',
  loadError: '판정을 불러오지 못했습니다',
  noDocument: '붙인 서류 없음',
  expired: (date: string) => `만료 ${date}`,
  /** 답 칸에 보이는 이름. 답이 없거나 다시 답해야 하면 그 까닭을 보인다 */
  problemAnswer: { UNANSWERED: '답 안 함', STALE: '다시 답해야 함' } as Partial<Record<EligibilityBlockerProblem, string>>,
}

/** 공고별 진단의 증빙 서류 줄과 서류 고르기 창 */
export const EVIDENCE_TEXT = {
  verdictLabel: '우리 회사 판정',
  label: '증빙 서류',
  noExpiry: '만료일 없음',
  expiresOn: (date: string) => `만료일 ${date}`,
  expired: (date: string) => `만료됨 ${date}. 새 서류로 바꿔 주세요`,
  change: '다른 서류로 바꾸기',
  detach: '떼기',
  need: '증빙 서류를 붙여 주세요',
  suggestion: (name: string) => `맞을 수 있는 서류: ${name}`,
  noMatch: '서류함에 맞는 종류의 서류가 없습니다. 내 서류에서 고르거나 서류함에 올려 주세요',
  allExpired: '맞는 종류의 서류가 모두 만료됐습니다. 새 서류를 서류함에 올리거나 내 서류에서 골라 주세요',
  attachSuggested: '이 서류 붙이기',
  pick: '서류 고르기',
  upload: '서류함에 올리기',
  attachError: '붙이지 못했습니다. 다시 눌러 주세요',
  saving: '저장 중',
  picker: {
    title: '증빙 서류 고르기',
    desc: (title: string, evidenceBy: string | null) => (evidenceBy ? `${title} · 증빙 서류 글 "${evidenceBy}"` : title),
    matched: '맞을 수 있는 서류',
    others: '그 밖의 내 서류',
    uploaded: (category: string, date: string) => `${category} · ${date} 올림`,
    noExpiry: '만료일 없음',
    expiresOn: (date: string) => `만료일 ${date}`,
    expired: (date: string) => `만료됨 ${date}`,
    note: '맞을 수 있는 서류는 증빙 서류 글이나 자격 이름이 서류 종류 이름과 맞아 앞에 둔 것입니다. 만료된 서류를 붙이면 서류 부족으로 판정합니다.',
    upload: '서류함에 새로 올리기',
    cancel: '취소',
    ok: '붙이기',
    empty: '서류함에 올린 서류가 없습니다',
    toVault: '서류함으로',
    loadError: '서류함을 불러오지 못했습니다',
    retry: '다시 시도',
  },
}

/** 공고 검색의 우리 회사 칸에서 판정할 수 없는 공고 */
export const VERDICT_NONE = '-'

/**
 * 걸린 곳 한 줄 (T3-3 「공고별 판정」). 앞 자격 이름은 굵게 보이도록 따로 돌려준다.
 * 판정 전은 0이 아닌 것만 답 안 함, 확인 필요, 다시 답해야 함 순서로 잇는다.
 */
export function verdictWhy(verdict: EligibilityVerdict, blockers: EligibilityBlocker[], gateCount: number): { name?: string; text: string } {
  const more = blockers.length > 1 ? ` 외 ${blockers.length - 1}개` : ''
  const [first] = blockers
  if (verdict === 'DIFFICULT' && first) {
    return { name: first.title, text: `: 없음으로 답함${first.sourceRef ? `, ${first.sourceRef}` : ''}${more}` }
  }
  if (verdict === 'DOCS_MISSING' && first) {
    const why = first.problem === 'EXPIRED' ? `증빙 서류 만료 ${dotDate(first.expiresOn)}` : '증빙 서류 없음'
    return { name: first.title, text: `: ${why}${more}` }
  }
  if (verdict === 'ELIGIBLE') return { text: `통과 조건 ${gateCount}개 모두 있음, 증빙 서류 붙임` }
  if (gateCount === 0) return { text: '통과 조건으로 표시된 자격이 없습니다' }
  const count = (p: EligibilityBlockerProblem) => blockers.filter((b) => b.problem === p).length
  const parts = [
    [count('UNANSWERED'), '답 안 함'],
    [count('UNSURE'), '확인 필요'],
    [count('STALE'), '다시 답해야 함'],
  ].filter(([n]) => (n as number) > 0).map(([n, label]) => `${label} ${n}개`)
  return { text: `통과 조건 ${gateCount}개 중 ${parts.join(', ')}` }
}
