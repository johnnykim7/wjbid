/** 이메일 가입과 SNS 가입이 같이 쓰는 검사 규칙과 문구 (CR-131, CR-146) */

export const REQUIRED = '필수 입력 항목입니다'
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const COUNT_WORD = ['', '한', '두', '세', '네', '다섯', '여섯', '일곱', '여덟']

export type CompanyKey = 'companyName' | 'businessRegistrationNumber' | 'contactPerson' | 'phone'
export const COMPANY_KEYS: CompanyKey[] = ['companyName', 'businessRegistrationNumber', 'contactPerson', 'phone']

export type Agree = { terms: boolean; privacy: boolean; marketing: boolean }
export const AGREE_REQUIRED = '필수 약관 두 가지에 동의해 주세요'

/** 서버 SNS 구분값(SnsProvider)마다 화면에 쓰는 이름. 목록에 없는 구분값은 단추를 만들지 않는다 */
export const SNS_NAME: Record<string, string> = {
  NAVER: '네이버',
  KAKAO: '카카오',
}

/** SNS 에 다녀온 뒤 로그인 전에 보던 화면으로 돌려보내려고 잠시 적어 두는 곳 (CR-146 시나리오 7) */
export const SNS_RETURN_KEY = 'snsReturnTo'
