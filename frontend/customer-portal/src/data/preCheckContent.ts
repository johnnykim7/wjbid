/**
 * 제출 전 검사 화면 문구 (CR-141, T3-3 「제출 전 검사」, 「입찰 건별 제출 전 검사」).
 * 판정은 서버가 낸 값만 보이고, 화면은 판정 이름과 근거 문장만 고른다.
 */
import type { FileFormatName, PreCheckVerdict } from '../types/preCheck'
import { INQUIRY_TYPE, inquiryHref } from './subpageContent'

export const PRE_CHECK_PATH = '/documents/pre-check'
export const preCheckHref = (bidRequestId: string) => `${PRE_CHECK_PATH}/${bidRequestId}`
export const bidRequestHref = (bidRequestId: string) => `/proposals/${bidRequestId}`
export const SEARCH_PATH = '/search'
export const VAULT_PATH = '/documents'
const REVIEW_TITLE = '제출 전 서류 검토'

/** 입찰 건에서 열면 공고번호를 제목에, 빠짐·어긋남·사람 확인 서류 이름을 문의 내용에 채워 둔다 */
export const reviewHref = (solicitationNumber?: string | null, need: { label: string; titles: string[] }[] = []) => {
  const lines = need.filter((n) => n.titles.length > 0).map((n) => `${n.label}: ${n.titles.join(', ')}`)
  return inquiryHref(INQUIRY_TYPE.consulting,
    solicitationNumber ? `${REVIEW_TITLE} ${solicitationNumber}` : REVIEW_TITLE,
    lines.length > 0 ? lines.join('\n') : undefined)
}

export const FORMAT_LABEL: Record<FileFormatName, string> = {
  PDF: 'PDF',
  WORD: 'Word',
  EXCEL: 'Excel',
  HWP: 'HWP',
  DWG: 'DWG',
}

/** 판정에 쓰는 형식 표에 있으면 그 이름, 없으면 확장자를 대문자로 */
export const extensionLabel = (extension?: string | null) => {
  if (!extension) return ''
  const known: Record<string, FileFormatName> = {
    pdf: 'PDF', doc: 'WORD', docx: 'WORD', xls: 'EXCEL', xlsx: 'EXCEL', hwp: 'HWP', dwg: 'DWG',
  }
  const name = known[extension.toLowerCase()]
  return name ? FORMAT_LABEL[name] : extension.toUpperCase()
}

export const PRE_CHECK_TEXT = {
  title: '제출 전 검사',
  desc: '공고가 요구하는 서류와 올린 서류를 맞춰 빠진 것, 파일 형식, PDF 쪽수를 봅니다. 서류 내용은 사람이 봅니다',
  steps: [
    { name: '입찰 건 고르기', desc: '입찰 신청한 공고마다 요구 서류를 모아 둡니다' },
    { name: '빠진 서류 올리기', desc: '파일을 올리거나 공통 서류함에서 고르면 형식과 쪽수를 바로 봅니다' },
    { name: '내용은 검토 신청', desc: '서류 내용이 조건에 맞는지는 담당자가 봅니다' },
  ],
  listTitle: (n: number) => `내 입찰 건 ${n}건`,
  listDesc: '마감일 가까운 순. 줄을 누르면 서류마다 판정을 봅니다',
  cols: {
    notice: '공고',
    deadline: '마감일',
    state: '진행 단계',
    uploaded: '필수 서류 올림',
    missing: '빠짐',
    mismatch: '형식·쪽수 어긋남',
    needsHuman: '사람 확인',
  },
  past: '마감 지남',
  notReady: '공고 분석 전이라 요구 서류를 알 수 없습니다',
  cap: '어긋남과 사람 확인은 올린 서류만 셉니다. 필수 서류가 빠지면 서류 접수로 전환할 수 없습니다.',
  empty: {
    title: '입찰 신청한 공고가 없습니다',
    desc: '입찰 검색에서 공고를 골라 신청하면 여기에 요구 서류가 모입니다',
    action: '입찰 검색으로',
  },
  review: {
    title: '서류 내용이 조건에 맞는지 모르겠다면',
    desc: '공고번호를 남겨 주시면 담당자가 올린 서류를 공고 조건과 함께 봐 드립니다. 신청은 1:1 문의로 접수됩니다',
    action: '제출 전 서류 검토 신청',
  },
  loadError: '불러오지 못했습니다',
  retry: '다시 시도',
}

export const PRE_CHECK_DETAIL_TEXT = {
  noticeLink: '공고 보기',
  bidLink: '입찰 건 상세',
  meta: { number: '공고번호', deadline: '마감' },
  nums: { required: '필수 서류', uploaded: '올림', missing: '빠짐', mismatch: '형식·쪽수 어긋남', needsHuman: '사람 확인' },
  unit: '개',
  listTitle: (n: number) => `올릴 서류 ${n}개`,
  listDesc: '공고 분석이 원문에서 뽑은 고객 제출 서류입니다. 원문 위치로 공고문에서 확인하세요',
  mandatory: '필수',
  optional: '선택',
  sourceRef: '원문 위치',
  condition: '공고 조건',
  formatWord: '형식',
  pagesWord: '쪽수',
  noCondition: '조건 없음',
  more: '공고 설명',
  notes: '분석 메모',
  file: {
    empty: '아직 올리지 않았습니다',
    limit: (mb: number) => `${mb}MB 이하`,
    noPages: '쪽수 세지 않음',
    pages: (n: number) => `${n}쪽`,
    uploadedOn: (d: string) => `${d} 올림`,
  },
  actions: {
    upload: '파일 올리기',
    vault: '서류함에서 고르기',
    replace: '바꾸기',
    remove: '빼기',
  },
  tooBig: (mb: number) => `파일 크기가 ${mb}MB를 넘습니다`,
  actionError: '처리하지 못했습니다. 잠시 뒤 다시 시도해 주세요',
  label: { presence: '서류', format: '형식', pages: '쪽수' },
  presence: { UPLOADED: '올림', MISSING: '빠짐' },
  verdict: {
    MATCH: '맞음',
    MISMATCH: '어긋남',
    OVER: '넘음',
    NEEDS_HUMAN: '사람 확인',
    NO_CONDITION: '조건 없음',
    NOT_UPLOADED: '올리기 전',
  } satisfies Record<PreCheckVerdict, string>,
  why: {
    missingMandatory: '필수 서류입니다. 올리기 전에는 형식과 쪽수를 보지 않습니다',
    missingOptional: '선택 서류입니다. 올리기 전에는 형식과 쪽수를 보지 않습니다',
    formatMatch: (allowed: string) => `원문에서 읽은 형식: ${allowed}`,
    formatMismatch: (allowed: string, uploaded: string) => `원문에서 읽은 형식은 ${allowed}, 올린 파일은 ${uploaded}`,
    formatHuman: '이 형식 이름으로 읽히지 않아 사람이 봅니다',
    formatNone: '공고 분석에 형식 조건이 없습니다',
    pagesMatch: (count: number, limit: number) => `${count}쪽. 원문에서 읽은 한도는 ${limit}쪽`,
    pagesOver: (count: number, limit: number) => `${count}쪽. 원문에서 읽은 한도 ${limit}쪽을 넘습니다`,
    pagesNotSingle: '이 숫자 하나와 쪽 단위로만 되어 있지 않아 사람이 봅니다',
    pagesNotCounted: 'PDF 가 아니거나 쪽수를 세지 못해 사람이 봅니다',
    pagesNoNumber: '원문에 숫자가 없어 쪽수 조건이 없습니다',
    pagesNone: '공고 분석에 쪽수 조건이 없습니다',
  },
  notJudged: '이 화면은 공고 분석이 뽑은 조건 원문과 올린 파일의 형식, PDF 쪽수만 맞춰 봅니다. 조건 원문이 형식 이름이나 쪽수 하나로 읽히지 않으면 사람 확인으로 둡니다. 서류 내용이 조건에 맞는지는 판정하지 않습니다.',
  noClientItems: '이 공고는 고객이 올릴 서류가 없습니다. 아래 플랫폼이 만드는 서류를 확인하세요.',
  autoFill: {
    action: '서류함에서 자동 채우기',
    done: (n: number) => `서류함에서 ${n}개를 이었습니다`,
    none: '서류함에서 이을 수 있는 서류가 없습니다. 서류 이름이 서류함 분류와 맞을 때만 채웁니다',
  },
  platform: {
    title: '플랫폼이 만드는 서류',
    desc: '올리지 않아도 됩니다',
    empty: '이 공고는 공고 분석이 제출 서류를 모두 고객 제출로 표시했습니다. 플랫폼 작성이나 정부 양식 항목이 있으면 여기에 이름과 원문 위치가 나옵니다.',
    party: { PLATFORM_GENERATED: '플랫폼 작성', SYSTEM_FORM: '정부 양식' } as Record<string, string>,
  },
  submit: {
    title: '서류 제출 완료',
    blocked: (n: number, names: string) => `필수 서류 ${n}개가 빠져 서류 접수로 전환할 수 없습니다: ${names}`,
    warn: (n: number) => `형식·쪽수가 공고 조건과 다르거나 사람이 볼 서류가 ${n}개 있습니다. 이대로 제출하면 담당자가 확인합니다`,
    ready: '필수 서류가 모두 있습니다. 제출하면 담당자가 서류를 받습니다',
    notReady: '요구 서류를 알 수 없어 서류를 맞춰 보지 못했습니다. 제출하면 담당자가 확인합니다',
    action: '서류 제출 완료',
    notes: '고객 서류 제출 완료',
    error: '제출하지 못했습니다. 잠시 뒤 다시 시도해 주세요',
  },
  need: {
    part: (label: string, n: number) => `${label} ${n}개`,
    title: (parts: string[]) => `${parts.join(', ')}가 남았습니다`,
    desc: '담당자가 올린 서류를 공고 조건과 함께 봐 드립니다. 신청 내용에 아래 서류 이름이 미리 채워집니다',
    more: (title: string, n: number) => `${title} 외 ${n}`,
  },
  notReady: {
    title: '공고 분석 전이라 요구 서류를 알 수 없습니다',
    desc: '분석이 끝나면 요구 서류가 여기에 나옵니다. 급하면 검토 신청을 남겨 주세요',
  },
  notFound: {
    title: '입찰 건을 찾을 수 없습니다',
    desc: '주소를 확인하거나 제출 전 검사 목록에서 다시 골라 주세요',
    back: '제출 전 검사 목록으로',
  },
  tabNote: '제출 전 검사와 같은 목록입니다. 공고 분석의 요구 서류 하나하나에 파일을 잇습니다.',
  tabLink: '제출 전 검사 보기',
  picker: {
    title: '서류함에서 고르기',
    desc: (title: string) => `${title} 에 이을 서류`,
    note: '고른 서류는 이 입찰 건 서류로 복사됩니다. 서류함의 서류는 그대로 남습니다.',
    empty: '공통 서류함에 올린 서류가 없습니다',
    toVault: '공통 서류함으로',
    noExpiry: '만료일 없음',
    expiresOn: (d: string) => `${d} 만료`,
    expired: (d: string) => `${d} 만료됨`,
    ok: '이 서류 잇기',
    cancel: '닫기',
  },
}
