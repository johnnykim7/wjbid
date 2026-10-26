import { useState } from 'react'
import {
  Alert,
  AlertContent,
  AlertDescription,
  AlertToolbar,
  Badge,
  Button,
  Card,
  CardContent,
} from '@therecommerce-develop/ds-ui'
import { ToneBadge } from './StateBadge'

export interface Coverage {
  matrixReady: boolean
  evaluatedDocument: boolean
  targetCount: number
  fulfilledCount: number
  missing: { reqId: string; text: string | null; unmapped: boolean }[]
  unknownIds: { reqId: string; sectionId: string; sectionTitle: string | null }[]
  sections: { sectionId: string; refs: { reqId: string; fulfilled: boolean }[] }[]
}

const TEXT_PREVIEW = 80

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
      <Alert variant="destructive" appearance="light" className="items-center">
        <AlertDescription className="grow">커버리지 검증을 불러오지 못했습니다.</AlertDescription>
        <AlertToolbar>
          <Button variant="outline" size="sm" onClick={onRetry}>다시 시도</Button>
        </AlertToolbar>
      </Alert>
    )
  }
  if (!coverage) return null
  // CR-178 커버레터 같은 평가받지 않는 문서는 요구사항을 걸지 않는다(BIZ-095)
  if (!coverage.evaluatedDocument) {
    return (
      <Card>
        <CardContent className="text-sm text-muted-foreground">
          평가받는 문서가 아니라 커버리지 검증을 하지 않습니다.
        </CardContent>
      </Card>
    )
  }
  if (!coverage.matrixReady) {
    return (
      <Card>
        <CardContent className="text-sm text-muted-foreground">
          매트릭스가 없는 공고라 커버리지 검증을 하지 않습니다.
        </CardContent>
      </Card>
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

  const summary = (
    <div className="flex flex-wrap items-center gap-3 text-sm">
      <span className="font-semibold text-foreground">커버리지 검증</span>
      <span>
        커버리지 {coverage.fulfilledCount} / {coverage.targetCount} ({ratio}%)
      </span>
      <ToneBadge tone={coverage.missing.length > 0 ? 'warning' : 'gray'}>누락 REQ {coverage.missing.length}</ToneBadge>
      <ToneBadge tone={unmappedCount > 0 ? 'warning' : 'gray'}>매핑 안 된 REQ {unmappedCount}</ToneBadge>
      <ToneBadge tone={coverage.unknownIds.length > 0 ? 'destructive' : 'gray'}>
        매트릭스에 없는 ID {coverage.unknownIds.length}
      </ToneBadge>
      {warn && (
        <Button variant="outline" size="sm" className="ml-auto" onClick={() => setOpen(!open)}>
          {open ? '접기' : '펼치기'}
        </Button>
      )}
    </div>
  )

  if (!warn) {
    return (
      <Card>
        <CardContent>{summary}</CardContent>
      </Card>
    )
  }

  return (
    <Alert variant="warning" appearance="light">
      <AlertContent className="grow">
        {summary}
        {open && (
          <div className="mt-3 grid gap-3 md:grid-cols-2 text-sm">
            {coverage.missing.length > 0 && (
              <div>
                <div className="text-xs font-medium mb-1">누락 REQ</div>
                <ul className="space-y-1">
                  {coverage.missing.map((m) => (
                    <li key={m.reqId} className="bg-background rounded p-1.5">
                      <div className="flex items-center gap-2">
                        <span className="font-mono">{m.reqId}</span>
                        {m.unmapped
                          ? <Badge variant="warning" appearance="light" size="xs" className="whitespace-nowrap">매핑 안 된 REQ</Badge>
                          : sectionsOf(m.reqId).map(sectionLink)}
                      </div>
                      <div className="text-xs text-muted-foreground">{preview(m.text)}</div>
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {coverage.unknownIds.length > 0 && (
              <div>
                <div className="text-xs font-medium text-destructive mb-1">매트릭스에 없는 ID</div>
                <ul className="space-y-1">
                  {coverage.unknownIds.map((u) => (
                    <li key={`${u.sectionId}-${u.reqId}`} className="bg-background rounded p-1.5 flex items-center gap-2">
                      <span className="font-mono">{u.reqId}</span>
                      {sectionLink(u.sectionId)}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </AlertContent>
    </Alert>
  )
}
