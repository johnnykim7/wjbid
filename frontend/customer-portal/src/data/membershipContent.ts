/** 회원 전용 화면 문구 (CR-157, T3-3 「회원 전용 화면」, T3-4 「공고문 회원 전용」) */
export const MEMBERSHIP_TEXT = {
  pricing: '요금 안내',
  consult: '1:1 상담',
  toSearch: '마감 전 공고로',
  // 원본 공고 상세에서 같은 공고의 공고문이 있을 때 무료회원에게 보이는 띠
  originBanner: '한글 공고문은 회원 전용입니다. 회원은 이 공고의 요약, 제출 서류, 우리 회사 판정을 볼 수 있습니다.',
  notice: {
    title: '공고문은 회원 전용입니다',
    desc: '회원은 공고 요약, 제출 서류, 우리 회사 판정을 보고 제안서 자문·검토를 신청할 수 있습니다. 공고 원문은 무료로 볼 수 있습니다.',
  },
  eligibility: {
    title: '참가 자격 확인은 회원 전용입니다',
    desc: '회원은 공고마다 참가 자격에 답하고 우리 회사가 낼 수 있는지 판정을 봅니다.',
  },
  verdict: {
    title: '공고별 판정은 회원 전용입니다',
    desc: '회원은 참가 자격 확인의 답으로 공고마다 우리 회사 판정을 봅니다.',
  },
  // 마감 전 공고 우리 회사 칸에서 공고문이 있는 줄. "회원" 만 두면 무료회원이 자기 등급으로 읽는다 (CR-166)
  rowTag: '회원 전용',
  rowTagTitle: '한글 공고문이 있는 공고입니다. 회원 전용',
  // 모든 화면 유틸 띠의 회사명 옆 꼬리표 (CR-162)
  headerTag: { member: '회원', free: '무료회원' },
  profile: {
    label: '회원 구분',
    member: (endsOn: string) => `회원, ${endsOn.replaceAll('-', '.')} 까지`,
    admin: '관리자',
    free: '무료회원',
  },
}

export const PRICING_PATH = '/pricing'
export const CONSULT_PATH = '/support'
