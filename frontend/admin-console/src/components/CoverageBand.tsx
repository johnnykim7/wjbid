import { useState } from 'react'
import { Badge, Button } from '@therecommerce-develop/ds-ui'

export interface Coverage {
  matrixReady: boolean
  targetCount: number
  fulfilledCount: number
  missing: { reqId: string; text: string | null; unmapped: boolean }[]
  unknownIds: { reqId: string; sectionId: string; sectionTitle: string | null }[]
  sections: { sectionId: string; refs: { reqId: string; fulfilled: boolean }[] }[]
}

const TEXT_PREVIEW = 80
// ds-ui outline 배지는 관리자 색 변수에서 글자가 흰색이 되어 회색 알약을 직접 칠한다(InquiryPills 와 같은 방식)
const NEUTRAL_PILL = 'bg-gray-100 text-gray-700'

function preview(text: string | null): string {
  if (!text) return ''
  return text.length > TEXT_PREVIEW ? `${text.slice(0, TEXT_PREVIEW)}...` : text
}

/** CR-159 제안서 Section 관리 트리 위 커버리지 검증 띠. 경고만 하고 아무것도 막지 않는다(BIZ-078) */
export default function CoverageBand({ coverage, failed, onRetry, onSelectSection, sectionTitle }: {
  coverage: Coverage | null
  failed: boolean
  onRetry: () => void
  onSelectSection: (sectionId: string) => void
  sectionTitle: (sectionId: string) => string
}) {
  const [open, setOpen] = useState(false)

  if (failed) {
    return (
      <div className="mb-4 p-3 border rounded-lg flex items-center gap-3 bg-white">
        <span className="text-sm text-red-600">커버리지 검증을 불러오지 못했습니다.</span>
        <Button variant="outline" size="sm" onClick={onRetry}>다시 시도</Button>
      </div>
    )
  }
  if (!coverage) return null
  if (!coverage.matrixReady) {
    return (
      <div className="mb-4 p-3 border rounded-lg bg-white text-sm text-gray-500">
        매트릭스가 없는 공고라 커버리지 검증을 하지 않습니다.
      </div>
    )
  }

  const unmappedCount = coverage.missing.filter((m) => m.unmapped).length
  const warn = coverage.missing.length > 0 || coverage.unknownIds.length > 0
  const ratio = coverage.targetCount === 0 ? 100 : Math.round((coverage.fulfilledCount / coverage.targetCount) * 100)
  const sectionsOf = (reqId: string) =>
    coverage.sections.filter((s) => s.refs.some((r) => r.reqId === reqId)).map((s) => s.sectionId)

  const sectionLink = (sectionId: string) => (
    <Button key={sectionId} variant="dim" size="sm" className="px-1 h-auto underline" onClick={() => onSelectSection(sectionId)}>
      {sectionTitle(sectionId)}
    </Button>
  )

  return (
    <div className={`mb-4 p-3 border rounded-lg ${warn ? 'bg-amber-50 border-amber-300' : 'bg-white'}`}>
      <div className="flex flex-wrap items-center gap-3 text-sm">
        <span className="font-semibold text-gray-800">커버리지 검증</span>
        <span>
          커버리지 {coverage.fulfilledCount} / {coverage.targetCount} ({ratio}%)
        </span>
        <Badge variant="warning" size="sm" className={coverage.missing.length > 0 ? '' : NEUTRAL_PILL}>누락 REQ {coverage.missing.length}</Badge>
        <Badge variant="warning" size="sm" className={unmappedCount > 0 ? '' : NEUTRAL_PILL}>매핑 안 된 REQ {unmappedCount}</Badge>
        <Badge variant="destructive" size="sm" className={coverage.unknownIds.length > 0 ? '' : NEUTRAL_PILL}>
          매트릭스에 없는 ID {coverage.unknownIds.length}
        </Badge>
        {warn && (
          <Button variant="outline" size="sm" className="ml-auto" onClick={() => setOpen(!open)}>
            {open ? '접기' : '펼치기'}
          </Button>
        )}
      </div>

      {open && warn && (
        <div className="mt-3 grid gap-3 md:grid-cols-2 text-sm">
          {coverage.missing.length > 0 && (
            <div>
              <div className="text-xs font-medium text-amber-800 mb-1">누락 REQ</div>
              <ul className="space-y-1">
                {coverage.missing.map((m) => (
                  <li key={m.reqId} className="bg-white rounded p-1.5">
                    <div className="flex items-center gap-2">
                      <span className="font-mono">{m.reqId}</span>
                      {m.unmapped
                        ? <Badge variant="warning" size="xs">매핑 안 된 REQ</Badge>
                        : sectionsOf(m.reqId).map(sectionLink)}
                    </div>
                    <div className="text-xs text-gray-600">{preview(m.text)}</div>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {coverage.unknownIds.length > 0 && (
            <div>
              <div className="text-xs font-medium text-red-700 mb-1">매트릭스에 없는 ID</div>
              <ul className="space-y-1">
                {coverage.unknownIds.map((u) => (
                  <li key={`${u.sectionId}-${u.reqId}`} className="bg-white rounded p-1.5 flex items-center gap-2">
                    <span className="font-mono">{u.reqId}</span>
                    {sectionLink(u.sectionId)}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
