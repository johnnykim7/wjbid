import { useMemo } from 'react'
import { Button } from '@therecommerce-develop/ds-ui'
import { Card } from '../components/ui/card'
import { Badge } from '../components/ui/badge'
import { StatCard } from '../components/ui/stat-card'

// ── 더미 데이터 (BE 미연동) ─────────────────────────────────────
// 제출 서류 슬롯. 실제 연동 시 getRequiredDocumentSlots / getClientDocuments
// (둘 다 bidRequestId 필요) 로 채운다.
interface DocSlot {
  id: string
  label: string
  description: string
  required: boolean
  fileName?: string
  uploadedAt?: string
}

const DUMMY_SLOTS: DocSlot[] = [
  {
    id: 'd1',
    label: '사업자등록증',
    description: 'Business Registration Certificate',
    required: true,
    fileName: 'biz_registration.pdf',
    uploadedAt: '2026-06-20',
  },
  {
    id: 'd2',
    label: '회사소개서',
    description: 'Company Profile',
    required: true,
    fileName: 'company_profile_2026.pdf',
    uploadedAt: '2026-06-21',
  },
  {
    id: 'd3',
    label: '과거 수행실적',
    description: 'Past Performance Records',
    required: true,
  },
  {
    id: 'd4',
    label: '재무제표',
    description: 'Financial Statements (최근 3년)',
    required: true,
  },
  {
    id: 'd5',
    label: 'CAGE 코드 증빙',
    description: 'CAGE Code Documentation',
    required: false,
    fileName: 'cage_code.png',
    uploadedAt: '2026-06-22',
  },
  {
    id: 'd6',
    label: 'SAM 등록 확인서',
    description: 'SAM.gov Registration',
    required: false,
  },
]

export default function DocumentsPage() {
  const slots = DUMMY_SLOTS

  const { uploaded, missing, requiredMissing } = useMemo(() => {
    const up = slots.filter((s) => s.fileName).length
    return {
      uploaded: up,
      missing: slots.length - up,
      requiredMissing: slots.filter((s) => s.required && !s.fileName).length,
    }
  }, [slots])

  return (
    <div className="space-y-6">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-gray-800">서류함</h2>
        <p className="text-sm text-gray-500 mt-1">
          입찰에 필요한 제출 서류를 미리 등록해 두면 제안서 작성이 빨라집니다.
        </p>
      </div>

      {/* 요약 */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <StatCard
          label="제출 완료"
          value={uploaded}
          unit={`/ ${slots.length}`}
          icon="fa-solid fa-circle-check"
          iconBg="bg-emerald-100"
          iconColor="text-emerald-600"
        />
        <StatCard
          label="미업로드"
          value={missing}
          unit="건"
          icon="fa-solid fa-circle-exclamation"
          iconBg="bg-amber-100"
          iconColor="text-amber-600"
        />
        <StatCard
          label="필수 미제출"
          value={requiredMissing}
          unit="건"
          icon="fa-solid fa-triangle-exclamation"
          iconBg="bg-red-100"
          iconColor="text-red-600"
          dark={requiredMissing > 0}
        />
      </div>

      {/* 슬롯 카드 그리드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {slots.map((slot) => {
          const done = !!slot.fileName
          return (
            <Card key={slot.id} className="p-5 flex flex-col">
              <div className="flex items-start justify-between gap-2 mb-2">
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <h3 className="text-sm font-bold text-gray-800 truncate">{slot.label}</h3>
                    {slot.required && (
                      <span className="text-red-500 text-xs font-bold" title="필수">
                        *
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-400 mt-0.5 truncate">{slot.description}</p>
                </div>
                {done ? (
                  <Badge variant="active" size="sm">
                    <i className="fa-solid fa-check mr-1" /> 완료
                  </Badge>
                ) : (
                  <Badge variant="closing" size="sm">
                    미업로드
                  </Badge>
                )}
              </div>

              <div className="flex-1 my-3">
                {done ? (
                  <div className="flex items-center gap-2 rounded-lg bg-gray-50 border border-gray-100 px-3 py-2">
                    <i className="fa-solid fa-file-lines text-secondary" />
                    <div className="min-w-0">
                      <p className="text-xs font-medium text-gray-700 truncate">{slot.fileName}</p>
                      <p className="text-[11px] text-gray-400">{slot.uploadedAt} 업로드</p>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center justify-center rounded-lg border border-dashed border-gray-200 px-3 py-4 text-xs text-gray-400">
                    <i className="fa-solid fa-cloud-arrow-up mr-1.5" /> 파일을 업로드하세요
                  </div>
                )}
              </div>

              {/* BE 미연동: 업로드 흐름은 추후 uploadToSlot/uploadClientDocument 연결 */}
              <div className="flex gap-2">
                <Button variant={done ? 'outline' : 'primary'} size="sm" className="flex-1">
                  <i className={`fa-solid ${done ? 'fa-rotate' : 'fa-arrow-up-from-bracket'} mr-1.5`} />
                  {done ? '교체' : '업로드'}
                </Button>
                {done && (
                  <Button variant="ghost" size="icon" className="text-gray-400 hover:text-red-500" title="삭제">
                    <i className="fa-regular fa-trash-can" />
                  </Button>
                )}
              </div>
            </Card>
          )
        })}
      </div>

      <p className="mt-6 text-xs text-gray-400">
        <i className="fa-solid fa-circle-info mr-1" />
        서류함은 현재 화면 시연용 더미 데이터입니다. 실제 제출 슬롯은 제안서 상세 화면에서 관리됩니다. (BE 미연동)
      </p>
    </div>
  )
}
