/** 서류함 문구 (CR-147, T3-3 「서류함」, BID-MKT-035) */
export const VAULT_TEXT = {
  desc: '사업자등록증·지명원 등 공통 서류를 한 번 등록해 두면 제안서 작성 시 자동으로 채워집니다.',
  excluded: 'CAGE 코드와 SAM 등록증은 등록 대행으로 받는 서류라 여기에 올리지 않습니다.',
  /** 고정 칸 안내. 서버가 주는 분류 코드로 찾고, 없으면 안내를 두지 않는다 */
  slotHint: {
    BUSINESS_REGISTRATION_CERTIFICATE: '3개월 안에 뗀 것',
    COMPANY_PROFILE: '원본 파일',
    FINANCIAL_STATEMENT: '최근 3개년',
    LICENSE: '사업 관련 인허가증',
  } as Record<string, string>,
  language: { KO: '국문', EN: '영문' } as Record<string, string>,
  partial: '일부 등록',
}
