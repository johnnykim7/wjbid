import type { OpportunityFile } from '../types'

/** 공고 상세, 원본 공고 상세 첨부 칸 문구 (CR-168, T3-3 「공고 상세」, T3-4 「원본 공고 상세」 5-1, 5-2) */
export const ATTACHMENT_TEXT = {
  download: '내려받기',
  downloading: '받는 중',
  failed: '파일을 받지 못했습니다. 잠시 뒤 다시 시도해 주세요.',
  empty: '첨부 파일 정보가 없습니다.',
  samCheck: 'SAM.gov에서 첨부 파일 확인',
  // 관리자가 올린 파일은 따로 부르는 이름을 두지 않아 꼬리표가 없다
  source: { SAM: 'SAM.gov', PIEE: 'PIEE', UPLOAD: null } as Record<OpportunityFile['source'], string | null>,
}
