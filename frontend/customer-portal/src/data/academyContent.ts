/**
 * 입찰 교육 화면 정의 (CR-133). 교육 자료 다섯 단계, 용어 사전, 서식 모음의 글을 이 한 곳에 둔다.
 * 세 화면은 고객센터 묶음 아래에 있다(CR-147).
 * 글은 공식 안내(SAM.gov, NSPA, PIEE, FAR·DFARS, GSA 서식 자료실)로 확인한 것만 쓴다.
 */
import type { SubIconName } from '../components/subpage/SubIcon'
import { academyHref, serviceHref } from '../components/layout/menu'

export interface LinkTo {
  label: string
  to: string
}

/* 교육 자료 */

export interface ArticleTable {
  head: [string, string]
  rows: { name: string; en?: string; desc: string }[]
}

export interface ArticleSection {
  title: string
  body?: string
  table?: ArticleTable
}

export interface Article {
  name: string
  /** 앞 단계의 오른쪽 단추 글 (받침에 따라 조사가 달라 단계마다 둔다) */
  goLabel: string
  title: string
  lead: string
  sections: ArticleSection[]
  note: { icon: SubIconName; text: string; link: LinkTo }
}

export const MATERIALS_HEAD = { title: '교육 자료', desc: '미군 입찰을 다섯 단계로 나눠 차례대로 설명합니다' }
export const MATERIALS_STEP_PARAM = 'step'
/** 첫 단계의 왼쪽 단추와 마지막 단계의 오른쪽 단추 */
export const MATERIALS_EDGE = {
  first: { label: '용어 사전', to: academyHref('glossary') },
  last: { label: '1:1 문의', to: '/support' },
}
export const prevStepLabel = (no: number, name: string) => `${no}. ${name}`

export const ARTICLES: Article[] = [
  {
    name: '찾기',
    goLabel: '1. 찾기로',
    title: '1. 미군 공고가 올라오는 곳',
    lead: '주한미군 공고도 미 연방정부 공고와 같이 SAM.gov 의 계약 기회(Contract Opportunities)에 올라옵니다.',
    sections: [
      {
        title: '공고를 내는 곳',
        body: '주한미군 육군 공고는 411번째 계약지원대대(411th CSB)와 그 아래 계약 부서가 냅니다. WJbid 는 이 부서들의 공고를 모아 한글로 정리합니다.',
      },
      {
        title: '공고 유형',
        table: {
          head: ['유형', '설명'],
          rows: [
            { name: '사전 수요 조사', en: 'Sources Sought', desc: '본 공고를 내기 전에 이 일을 할 수 있는 회사가 있는지 알아봅니다. 답하면 본 공고를 미리 준비할 수 있습니다.' },
            { name: '사전 공고', en: 'Presolicitation', desc: '곧 낼 입찰을 미리 알립니다.' },
            { name: '공고와 입찰 요청 통합', en: 'Combined Synopsis/Solicitation', desc: '공고와 입찰 요청을 한 번에 냅니다. 상용 품목·용역 공고에 많이 씁니다.' },
            { name: '입찰 요청', en: 'Solicitation', desc: '견적서나 제안서를 내 달라는 요청입니다.' },
            { name: '낙찰 공지', en: 'Award Notice', desc: '계약을 누구에게 주었는지 알립니다.' },
          ],
        },
      },
    ],
    note: { icon: 'search', text: 'WJbid 에서는 마감 전 공고를 한글로 봅니다.', link: { label: '마감 전 공고 보기', to: '/search' } },
  },
  {
    name: '판단',
    goLabel: '2. 판단으로',
    title: '2. 우리 회사가 할 수 있는 공고 선택',
    lead: '공고문을 열면 무엇을, 어디서, 언제까지 해야 하는지와 누가 낼 수 있는지를 먼저 찾습니다.',
    sections: [
      {
        title: '작업 범위',
        body: '공고 첨부의 수행 업무 기술서(PWS)나 작업 기술서(SOW)에 해야 할 일과 수준이 적혀 있습니다. 장소와 기간, 인력과 장비 조건을 먼저 봅니다.',
      },
      {
        title: '참가 자격',
        body: '공고마다 업종 코드(NAICS), 필요한 면허나 허가, 실적 요구가 다릅니다. 공고문에 적힌 자격을 하나씩 맞춰 봅니다.',
      },
      {
        title: '평가 방식',
        table: {
          head: ['방식', '설명'],
          rows: [
            { name: '최저가 적격 선정', en: 'LPTA', desc: '기술 기준을 넘은 회사 가운데 가장 낮은 가격을 낸 회사를 고릅니다.' },
            { name: '가격 대비 가치', en: 'Tradeoff', desc: '기술, 실적, 가격을 함께 보고 정부에 가장 이로운 제안을 고릅니다.' },
          ],
        },
      },
      {
        title: '마감과 질의',
        body: '마감 날짜와 시각은 공고에 적힌 시간대 기준입니다. 궁금한 점은 공고가 정한 기한 안에 공고에 적힌 계약 담당자에게 물을 수 있습니다.',
      },
    ],
    note: { icon: 'search', text: '공고 상세에서 참가 자격과 제출 서류를 한글로 볼 수 있습니다.', link: { label: '마감 전 공고 보기', to: '/search' } },
  },
  {
    name: '준비',
    goLabel: '3. 준비로',
    title: '3. 입찰 전에 갖출 것',
    lead: '미군 공고에 견적을 내려면 먼저 회사 등록을 갖춰야 합니다. 등록에 시간이 걸리니 공고가 나오기 전에 시작하는 것이 좋습니다.',
    sections: [
      {
        title: '등록과 계정',
        table: {
          head: ['갖출 것', '설명'],
          rows: [
            { name: 'NCAGE 코드', en: 'NATO Commercial and Government Entity Code', desc: '미국 밖 회사가 받는 업체 코드입니다. SAM.gov 등록 전에 받습니다.' },
            { name: 'Login.gov 계정', desc: 'SAM.gov 에 들어갈 때 쓰는 본인 인증 계정입니다.' },
            { name: 'SAM.gov 등록', en: 'System for Award Management', desc: '미 연방 계약을 하려면 등록해야 합니다. 등록하면 UEI 를 받고, 해마다 갱신합니다.' },
            { name: 'PIEE 계정', en: 'Procurement Integrated Enterprise Environment', desc: '미 국방부 조달 시스템입니다. 견적·제안 제출과 낙찰 뒤 대금 청구에 씁니다.' },
          ],
        },
      },
      {
        title: '회사 서류',
        body: '사업자등록증, 법인 등기부등본 같은 회사 서류와 영문 회사 이름·주소를 맞춰 둡니다. NCAGE 와 SAM.gov 에 넣는 이름과 주소가 서로 같아야 합니다.',
      },
    ],
    note: { icon: 'flag', text: '등록이 처음이면 순서대로 맡길 수 있습니다.', link: { label: 'SAM 등록 대행 보기', to: serviceHref('sam-registration') } },
  },
  {
    name: '제출',
    goLabel: '4. 제출로',
    title: '4. 견적서와 제안서 내기',
    lead: '공고문의 제출 안내(Instructions to Offerors)에 낼 것, 내는 방법, 마감이 모두 적혀 있습니다. 이 안내를 그대로 따르는 것이 가장 중요합니다.',
    sections: [
      {
        title: '표준 서식',
        body: '상용 품목·용역 공고는 SF 1449 를 표지로 씁니다. 칸을 채우고 서명해 냅니다.',
      },
      {
        title: '가격표',
        body: '공고의 가격 항목(CLIN)마다 단가와 금액을 적습니다. 옵션 기간이 있으면 기간마다 적습니다.',
      },
      {
        title: '공고 변경',
        body: '공고가 바뀌면 SF 30 으로 변경이 나옵니다. 받은 변경마다 받았다는 서명을 함께 냅니다. 빠뜨리면 견적이 받아들여지지 않을 수 있습니다.',
      },
      {
        title: '마감',
        body: '마감 시각이 지난 견적은 받지 않는 것이 원칙입니다. 내는 방법(이메일, PIEE 등)과 시간대를 미리 확인합니다.',
      },
    ],
    note: { icon: 'file', text: '서식 원본과 함께 내는 것을 서식 모음에 모았습니다.', link: { label: '서식 모음 보기', to: academyHref('forms') } },
  },
  {
    name: '낙찰 뒤',
    goLabel: '5. 낙찰 뒤로',
    title: '5. 낙찰 뒤',
    lead: '낙찰되면 계약 서류를 받고, 계약에 적힌 대로 일을 하고, 대금을 청구합니다.',
    sections: [
      {
        title: '계약 서류',
        body: '계약서를 받으면 기간, 금액, 옵션, 청구 방법을 먼저 확인합니다.',
      },
      {
        title: '대금 청구',
        body: '미 국방부 계약 대금은 PIEE 의 전자 청구(WAWF)로 냅니다. 계약서의 DFARS 252.232-7006 조항에 청구 방법이 적혀 있습니다.',
      },
      {
        title: '옵션과 다음 공고',
        body: '옵션 기간이 있는 계약은 미군이 옵션을 행사하면 이어집니다. 계약이 끝날 때쯤 같은 일이 다시 공고되기도 합니다.',
      },
      {
        title: 'SAM.gov 등록 유지',
        body: '계약을 수행하는 동안에도 SAM.gov 등록이 활성이어야 합니다. 해마다 갱신합니다.',
      },
    ],
    note: { icon: 'file', text: '대금 청구가 처음이면 도움을 받을 수 있습니다.', link: { label: '대금 청구 보기', to: serviceHref('invoicing') } },
  },
]

/* 용어 사전 */

export interface Term {
  abbr: string
  en: string
  ko: string
  desc: string
  related?: LinkTo
}

export const GLOSSARY_TEXT = {
  title: '용어 사전',
  desc: '미군 공고와 SAM.gov 에 자주 나오는 말을 한글로 풉니다',
  placeholder: '용어를 영문이나 한글로 찾아보세요 (예: UEI, 견적 요청)',
  search: '찾기',
  all: '전체',
  countLabel: '용어',
  count: (n: number) => `${n}개`,
  basis: '공식 정의를 근거로 풀었습니다',
  none: '찾는 용어가 없습니다',
  noneAction: '1:1 문의',
}

export const INDEX_LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

export const TERMS: Term[] = [
  { abbr: 'CAGE / NCAGE', en: 'Commercial and Government Entity Code', ko: '업체 코드', desc: '업체마다 주는 다섯 자리 코드입니다. 미국 회사는 CAGE, 미국 밖 회사는 NCAGE 를 받습니다. 미국 밖 회사는 SAM.gov 등록 전에 NCAGE 를 받아야 합니다.', related: { label: 'SAM 등록 대행', to: serviceHref('sam-registration') } },
  { abbr: 'CLIN', en: 'Contract Line Item Number', ko: '계약 품목 번호', desc: '계약에서 값을 매기는 항목마다 붙는 번호입니다. 가격표는 CLIN 마다 단가와 금액을 적습니다.', related: { label: '서식 모음', to: academyHref('forms') } },
  { abbr: 'CO', en: 'Contracting Officer', ko: '계약관', desc: '미 정부를 대신해 계약을 맺고, 바꾸고, 끝낼 권한을 가진 사람입니다.' },
  { abbr: 'DFARS', en: 'Defense Federal Acquisition Regulation Supplement', ko: '미 국방부 조달 규정 보충', desc: 'FAR 에 더해 미 국방부 계약에 적용하는 규정입니다. 계약서에 252 로 시작하는 조항 번호로 나옵니다.' },
  { abbr: 'FAR', en: 'Federal Acquisition Regulation', ko: '미 연방 조달 규정', desc: '미 연방 정부가 물품과 용역을 사는 방법을 정한 규정입니다. 계약서에 52 로 시작하는 조항 번호로 나옵니다.' },
  { abbr: 'LPTA', en: 'Lowest Price Technically Acceptable', ko: '최저가 적격 선정', desc: '기술 기준을 넘은 회사 가운데 가장 낮은 가격을 낸 회사를 고르는 평가 방식입니다.' },
  { abbr: 'NAICS', en: 'North American Industry Classification System', ko: '북미 산업 분류 코드', desc: '업종을 나타내는 여섯 자리 숫자입니다. 공고마다 하나가 붙고, 그 업종의 소기업 기준을 정하는 데 씁니다.' },
  { abbr: 'PIEE', en: 'Procurement Integrated Enterprise Environment', ko: '미 국방부 조달 통합 시스템', desc: '미 국방부 조달 업무를 모은 시스템입니다. 견적·제안 제출과 낙찰 뒤 대금 청구(WAWF)에 씁니다.', related: { label: 'PIEE 등록 대행', to: serviceHref('piee-registration') } },
  { abbr: 'PWS', en: 'Performance Work Statement', ko: '수행 업무 기술서', desc: '무엇을 어떤 수준으로 해야 하는지 적은 문서입니다. 공고 첨부에 들어 있고, 견적과 제안의 바탕이 됩니다.' },
  { abbr: 'RFP', en: 'Request for Proposal', ko: '제안 요청', desc: '기술 제안서와 가격을 함께 내 달라는 요청입니다. 가격과 함께 기술과 실적을 평가할 때 씁니다.' },
  { abbr: 'RFQ', en: 'Request for Quotation', ko: '견적 요청', desc: '가격 견적을 내 달라는 요청입니다. 제안 요청(RFP)보다 간단한 구매에 많이 씁니다.', related: { label: '서식 모음', to: academyHref('forms') } },
  { abbr: 'SAM', en: 'System for Award Management', ko: '미 연방 조달 업체 등록 시스템', desc: '미 연방 정부와 계약하려는 회사가 등록하는 공식 사이트(SAM.gov)입니다. 해마다 갱신합니다.', related: { label: 'SAM 등록 대행', to: serviceHref('sam-registration') } },
  { abbr: 'SOW', en: 'Statement of Work', ko: '작업 기술서', desc: '해야 할 일과 방법을 적은 문서입니다. 수행 업무 기술서(PWS)처럼 공고 첨부에 들어 있습니다.' },
  { abbr: 'UEI', en: 'Unique Entity ID', ko: '고유 업체 번호', desc: 'SAM.gov 가 등록한 회사에 주는 열두 자리 번호입니다. 미 연방 계약에서 회사를 가리킬 때 씁니다.', related: { label: 'SAM 등록 대행', to: serviceHref('sam-registration') } },
  { abbr: 'WAWF', en: 'Wide Area WorkFlow', ko: '전자 청구', desc: 'PIEE 안에서 미 국방부 계약 대금을 청구하고 납품을 확인하는 기능입니다.', related: { label: '대금 청구', to: serviceHref('invoicing') } },
]

/* 서식 모음 */

export const FORMS_TEXT = {
  title: '서식 모음',
  desc: '미군 공고에 답할 때 자주 쓰는 미 연방 표준 서식과 준비물입니다',
  formsTitle: '표준 서식',
  formsNote: '원본은 미 연방 조달청(GSA) 서식 자료실에서 받습니다',
  head: ['서식', '이름', '쓰는 때', '받는 곳'],
  sourceLabel: 'GSA',
  sourceUrl: 'https://www.gsa.gov/forms',
  bringTitle: '견적을 낼 때 흔히 함께 내는 것',
  bringNote: '공고마다 다릅니다. 공고문의 제출 안내(Instructions to Offerors)를 먼저 확인하세요.',
  bringLink: { label: '교육 자료 4. 제출', to: `${academyHref('materials')}?step=4` },
}

export const FORMS = [
  { no: 'SF 1449', ko: '상용 품목·용역 입찰 요청, 계약, 주문', en: 'Solicitation/Contract/Order for Commercial Products and Commercial Services', when: '상용 품목과 용역 공고의 표지 서식입니다. 견적을 낼 때 칸을 채우고 서명합니다.' },
  { no: 'SF 30', ko: '입찰 요청 변경, 계약 수정', en: 'Amendment of Solicitation/Modification of Contract', when: '공고가 바뀌면 이 서식으로 나옵니다. 받았다는 서명을 견적에 함께 냅니다.' },
  { no: 'SF 18', ko: '견적 요청', en: 'Request for Quotations', when: '견적 요청 공고에서 가격을 적어 내는 서식입니다.' },
  { no: 'SF 33', ko: '입찰 요청, 제안, 낙찰', en: 'Solicitation, Offer and Award', when: '상용이 아닌 품목·용역 공고의 표지 서식입니다.' },
]

export const BRING = [
  { name: '서명한 표지 서식', desc: '공고가 SF 1449 나 SF 18 을 쓰면 그 서식' },
  { name: '가격표', desc: '공고의 가격 항목(CLIN)마다 적습니다' },
  { name: '받은 공고 변경(SF 30)마다 받았다는 서명' },
  { name: '기술 자료나 수행 계획', desc: '공고가 요구할 때' },
  { name: 'SAM.gov 등록 확인' },
]
