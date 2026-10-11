// 제출 전 검사 응답 (CR-141, T3-2 G-2). 시각은 서버가 존을 붙여 준다(+09:00)

export type PreCheckVerdict = 'MATCH' | 'MISMATCH' | 'OVER' | 'NEEDS_HUMAN' | 'NO_CONDITION' | 'NOT_UPLOADED'
export type FileFormatName = 'PDF' | 'WORD' | 'EXCEL' | 'HWP' | 'DWG'

export interface PreCheckSummary {
  /** 필수 고객 올림 항목 수 */
  required: number
  uploaded: number
  missing: number
  /** 이어진 항목 가운데 형식이 어긋나거나 쪽수가 넘은 수 */
  mismatch: number
  /** 이어진 항목 가운데 사람이 볼 수 */
  needsHuman: number
}

export interface PreCheckRow {
  bidRequestId: string
  state: string
  solicitationNumber: string | null
  title: string | null
  titleKo: string | null
  responseDeadline: string | null
  analysisReady: boolean
  summary: PreCheckSummary | null
}

export interface PreCheckDocument {
  clientDocumentId: string
  fileName: string
  extension: string | null
  pageCount: number | null
  fileSize: number | null
  uploadedAt: string | null
}

export interface PreCheckClientItem {
  requirementItemId: string
  title: string
  description: string | null
  mandatory: boolean
  isBlocker: boolean
  sourceRef: string | null
  notes: string | null
  formatText: string | null
  pageLimitText: string | null
  document: PreCheckDocument | null
  presence: 'UPLOADED' | 'MISSING'
  format: { verdict: PreCheckVerdict; basis: string | null; allowed: FileFormatName[] }
  pages: { verdict: PreCheckVerdict; basis: string | null; limit: number | null }
}

export interface PreCheckPlatformItem {
  requirementItemId: string
  title: string
  fulfillmentParty: string
  sourceRef: string | null
}

export interface PreCheckDetail {
  bidRequestId: string
  state: string
  solicitationNumber: string | null
  title: string | null
  titleKo: string | null
  responseDeadline: string | null
  /** 공개된 공고문이 있을 때만 */
  noticeId: string | null
  analysisReady: boolean
  clientItems: PreCheckClientItem[]
  platformItems: PreCheckPlatformItem[]
  summary: PreCheckSummary | null
  canTransition: boolean
  /** 접수 전(CREATED)이나 서류 대기(DOCS_PENDING)면 true. false 면 올리기, 고르기, 바꾸기, 빼기 단추를 두지 않는다 */
  editable: boolean
  /** 올릴 수 있는 파일 한도(MB). 서버 설정값 */
  maxUploadMb: number
}

/** GET /member-documents 에서 고르기 창이 쓰는 칸만 */
export interface PreCheckVaultDocument {
  id: string
  categoryLabel: string
  fileName: string
  displayName?: string | null
  /** YYYY-MM-DD */
  expiresOn?: string | null
  createdAt: string
}

export interface PreCheckVaultResponse {
  fixedSlots: { files: { document: PreCheckVaultDocument | null }[] }[]
  others: PreCheckVaultDocument[]
}
