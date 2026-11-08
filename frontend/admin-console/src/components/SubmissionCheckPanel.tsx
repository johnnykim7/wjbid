import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Alert,
  AlertDescription,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  FieldError,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Textarea,
  toast,
} from '@therecommerce-develop/ds-ui'
import {
  clearSubmissionCondition,
  clearSubmissionLineNote,
  exportAdminPriceTable,
  getAdminPriceTable,
  getSubmissionCheck,
  resolveSubmissionCondition,
  saveSubmissionLineNote,
} from '../api/client'
import { ToneBadge } from './StateBadge'
import { Loading } from './LoadState'
import {
  LINE_DISPOSITIONS,
  LINE_STATUS,
  PRICE_STATUS,
  VERDICT,
  blockerLabel,
  formatNumber,
  lineReason,
  type ConditionRow,
  type LineDisposition,
  type LineRow,
  type LineStatus,
  type PriceTable,
  type SubmissionCheck,
} from './submissionCheck'

const ALL = 'ALL'

const PARTY_LABEL: Record<string, string> = {
  CLIENT_UPLOAD: '고객 업로드',
  PLATFORM_GENERATED: '플랫폼 생성',
  SYSTEM_FORM: '정부 양식',
}

function errorMessage(error: unknown, fallback: string): string {
  const response = (error as { response?: { data?: { message?: string } } }).response
  return response?.data?.message ?? fallback
}

function LineDialog({
  bidRequestId,
  line,
  onClose,
  onSaved,
}: {
  bidRequestId: string
  line: LineRow
  onClose: () => void
  onSaved: (check: SubmissionCheck) => void
}) {
  const initial = LINE_DISPOSITIONS.find((d) => d === line.note?.disposition) ?? LINE_DISPOSITIONS[0]
  const [disposition, setDisposition] = useState<LineDisposition>(initial)
  const [reason, setReason] = useState(line.note?.reason ?? '')
  const [sourceRef, setSourceRef] = useState(line.note?.sourceRef ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const reasonRequired = disposition === 'NOT_ANSWERED' || disposition === 'EQUIVALENT'
  const sourceRequired = disposition === 'NOT_ANSWERED'
  const reasonMissing = reasonRequired && !reason.trim()
  const sourceMissing = sourceRequired && !sourceRef.trim()

  const run = async (action: () => Promise<{ data: SubmissionCheck }>, fallback: string) => {
    setSaving(true)
    setError(null)
    try {
      const res = await action()
      onSaved(res.data)
    } catch (e) {
      setError(errorMessage(e, fallback))
      setSaving(false)
    }
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{line.reqId} 줄 처리</DialogTitle>
        </DialogHeader>
        <DialogBody className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <div className="space-y-1 text-sm">
            <p className="whitespace-pre-wrap">{line.text ?? '-'}</p>
            {line.translation && <p className="whitespace-pre-wrap text-muted-foreground">{line.translation}</p>}
          </div>
          <div className="flex items-center gap-2 text-sm">
            <span className="text-muted-foreground">지금 상태</span>
            <ToneBadge tone={LINE_STATUS[line.status].tone}>{LINE_STATUS[line.status].label}</ToneBadge>
          </div>
          <div className="space-y-1">
            <Label htmlFor="line-disposition">처리 종류</Label>
            <Select value={disposition} onValueChange={(v) => setDisposition(v as LineDisposition)}>
              <SelectTrigger id="line-disposition">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {LINE_DISPOSITIONS.map((d) => (
                  <SelectItem key={d} value={d}>
                    {LINE_STATUS[d].label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1">
            <Label htmlFor="line-reason">사유</Label>
            <Textarea id="line-reason" value={reason} rows={3} onChange={(e) => setReason(e.target.value)} />
            {reasonMissing && <FieldError>사유를 적어야 저장할 수 있습니다</FieldError>}
          </div>
          <div className="space-y-1">
            <Label htmlFor="line-source">공고 원문 근거</Label>
            <Input id="line-source" value={sourceRef} onChange={(e) => setSourceRef(e.target.value)} />
            {sourceMissing && <FieldError>공고 원문 근거를 적어야 저장할 수 있습니다</FieldError>}
          </div>
        </DialogBody>
        <DialogFooter>
          {line.note && (
            <Button
              variant="outline"
              disabled={saving}
              onClick={() =>
                run(() => clearSubmissionLineNote(bidRequestId, line.reqId), '처리를 지우지 못했습니다')
              }
            >
              처리 지우기
            </Button>
          )}
          <Button variant="outline" disabled={saving} onClick={onClose}>
            취소
          </Button>
          <Button
            disabled={saving || reasonMissing || sourceMissing}
            onClick={() =>
              run(
                () =>
                  saveSubmissionLineNote(bidRequestId, line.reqId, {
                    disposition,
                    reason: reason.trim() || null,
                    sourceRef: sourceRef.trim() || null,
                  }),
                '처리를 저장하지 못했습니다',
              )
            }
          >
            저장
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function ConditionDialog({
  bidRequestId,
  condition,
  onClose,
  onSaved,
}: {
  bidRequestId: string
  condition: ConditionRow
  onClose: () => void
  onSaved: (check: SubmissionCheck) => void
}) {
  const [reason, setReason] = useState(condition.note?.reason ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const save = async () => {
    setSaving(true)
    setError(null)
    try {
      const res = await resolveSubmissionCondition(bidRequestId, condition.key, reason.trim())
      onSaved(res.data)
    } catch (e) {
      setError(errorMessage(e, '해소 근거를 저장하지 못했습니다'))
      setSaving(false)
    }
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>결정 조건 해소 근거</DialogTitle>
        </DialogHeader>
        <DialogBody className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <p className="whitespace-pre-wrap text-sm">{condition.text}</p>
          <div className="space-y-1">
            <Label htmlFor="condition-reason">해소 근거</Label>
            <Textarea id="condition-reason" value={reason} rows={4} onChange={(e) => setReason(e.target.value)} />
            {!reason.trim() && <FieldError>해소 근거를 적어야 저장할 수 있습니다</FieldError>}
          </div>
        </DialogBody>
        <DialogFooter>
          <Button variant="outline" disabled={saving} onClick={onClose}>
            취소
          </Button>
          <Button disabled={saving || !reason.trim()} onClick={save}>
            저장
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function PriceDialog({ table, onClose }: { table: PriceTable; onClose: () => void }) {
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle>가격 표</DialogTitle>
        </DialogHeader>
        <DialogBody className="max-h-[70vh] space-y-6 overflow-y-auto">
          {table.periods.map((period) => (
            <div key={period.period} className="space-y-2">
              <p className="text-sm font-medium">{period.period}</p>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>CLIN</TableHead>
                    <TableHead>품목</TableHead>
                    <TableHead className="text-right">수량</TableHead>
                    <TableHead>단위</TableHead>
                    <TableHead className="text-right">단가</TableHead>
                    <TableHead className="text-right">금액</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {period.lines.map((line) => (
                    <TableRow key={line.clin}>
                      <TableCell>{line.clin}</TableCell>
                      <TableCell>{line.description ?? '-'}</TableCell>
                      <TableCell className="text-right">{formatNumber(line.quantity)}</TableCell>
                      <TableCell>{line.unit ?? '-'}</TableCell>
                      <TableCell className="text-right">{formatNumber(line.unitPrice)}</TableCell>
                      <TableCell className="text-right">{formatNumber(line.amount)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <p className="text-right text-sm">소계 {formatNumber(period.subtotal)}</p>
            </div>
          ))}
          <p className="text-right text-sm font-medium">합계 {formatNumber(table.total)}</p>
        </DialogBody>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            닫기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export default function SubmissionCheckPanel({ bidRequestId }: { bidRequestId: string }) {
  const [check, setCheck] = useState<SubmissionCheck | null>(null)
  const [loadError, setLoadError] = useState(false)
  const [filter, setFilter] = useState<LineStatus | typeof ALL>(ALL)
  const [line, setLine] = useState<LineRow | null>(null)
  const [condition, setCondition] = useState<ConditionRow | null>(null)
  const [priceTable, setPriceTable] = useState<PriceTable | null>(null)

  const load = useCallback(() => {
    setLoadError(false)
    getSubmissionCheck(bidRequestId)
      .then((res) => setCheck(res.data))
      .catch(() => setLoadError(true))
  }, [bidRequestId])

  useEffect(load, [load])

  const statusesPresent = useMemo(() => {
    const present = new Set(check?.lines.map((l) => l.status) ?? [])
    return (Object.keys(LINE_STATUS) as LineStatus[]).filter((s) => present.has(s))
  }, [check])

  const lines = useMemo(
    () => (check?.lines ?? []).filter((l) => filter === ALL || l.status === filter),
    [check, filter],
  )

  const applySaved = (saved: SubmissionCheck) => {
    setCheck(saved)
    setLine(null)
    setCondition(null)
    toast.success('저장했습니다')
  }

  const clearCondition = async (key: string) => {
    try {
      const res = await clearSubmissionCondition(bidRequestId, key)
      setCheck(res.data)
    } catch (e) {
      toast.error(errorMessage(e, '해소를 지우지 못했습니다'))
    }
  }

  const openPrice = async () => {
    try {
      const res = await getAdminPriceTable(bidRequestId)
      setPriceTable(res.data)
    } catch (e) {
      toast.error(errorMessage(e, '가격 표를 불러오지 못했습니다'))
    }
  }

  const downloadPrice = async () => {
    try {
      const res = await exportAdminPriceTable(bidRequestId)
      const url = URL.createObjectURL(res.data)
      const link = document.createElement('a')
      link.href = url
      link.download = 'price-table.xlsx'
      link.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      toast.error(errorMessage(e, '엑셀을 내려받지 못했습니다'))
    }
  }

  const body = () => {
    if (loadError) {
      return (
        <Alert variant="destructive">
          <AlertDescription>제출 검사를 불러오지 못했습니다</AlertDescription>
        </Alert>
      )
    }
    if (!check) return <Loading className="h-24" />
    if (check.lines.length === 0 && check.blockers.some((b) => b.code === 'NOT_READY')) {
      return <p className="text-sm text-muted-foreground">공고 분석이 끝나야 볼 수 있습니다</p>
    }
    const priceStatus = PRICE_STATUS[check.price.status]
    const hasPrice = check.price.status === 'INCOMPLETE' || check.price.status === 'COMPLETE'
    return (
      <div className="space-y-6">
        <section className="space-y-2">
          <ToneBadge tone={check.submittable ? 'success' : 'destructive'}>
            {check.submittable ? '제출 가능' : '제출 불가'}
          </ToneBadge>
          {check.blockers.length > 0 && (
            <ul className="flex flex-wrap gap-x-4 gap-y-1 text-sm">
              {check.blockers.map((b) => (
                <li key={b.code}>
                  {blockerLabel(b.code)} <span className="font-medium">{b.count}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="space-y-2">
          <h3 className="text-sm font-medium">절 사실 검증</h3>
          <ul className="divide-y divide-border rounded border border-border text-sm">
            {check.sections.map((section) => (
              <li key={section.sectionId} className="flex items-center justify-between gap-3 px-3 py-2">
                <Link to={`/documents/${section.documentId}/sections`} className="truncate hover:underline">
                  {section.title}
                </Link>
                <span className="flex shrink-0 items-center gap-1">
                  {section.inputChanged && <ToneBadge tone="warning">입력 바뀜</ToneBadge>}
                  <ToneBadge tone={VERDICT[section.verdict].tone}>{VERDICT[section.verdict].label}</ToneBadge>
                </span>
              </li>
            ))}
          </ul>
        </section>

        <section className="space-y-2">
          <div className="flex items-center justify-between gap-3">
            <h3 className="text-sm font-medium">요구 줄 {check.lines.length}</h3>
            <div className="w-44">
              <Select value={filter} onValueChange={(v) => setFilter(v as LineStatus | typeof ALL)}>
                <SelectTrigger aria-label="상태 거르기">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>전체 상태</SelectItem>
                  {statusesPresent.map((s) => (
                    <SelectItem key={s} value={s}>
                      {LINE_STATUS[s].label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>REQ ID</TableHead>
                <TableHead>원문</TableHead>
                <TableHead>채우는 쪽</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>답한 문장이나 사유</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {lines.map((row) => (
                <TableRow key={row.reqId}>
                  <TableCell className="whitespace-nowrap">{row.reqId}</TableCell>
                  <TableCell className="max-w-md whitespace-normal">
                    <p>{row.text ?? '-'}</p>
                    {row.translation && <p className="text-xs text-muted-foreground">{row.translation}</p>}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {(row.fulfillmentParty && PARTY_LABEL[row.fulfillmentParty]) ?? '-'}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    <ToneBadge tone={LINE_STATUS[row.status].tone}>{LINE_STATUS[row.status].label}</ToneBadge>
                  </TableCell>
                  <TableCell className="max-w-md whitespace-normal text-xs">
                    {row.evidenceQuote ?? lineReason(row.reason) ?? row.note?.reason ?? '-'}
                  </TableCell>
                  <TableCell>
                    <Button variant="outline" size="sm" onClick={() => setLine(row)}>
                      처리
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </section>

        <section className="space-y-2">
          <h3 className="text-sm font-medium">결정 조건 {check.conditions.length}</h3>
          {check.conditions.length === 0 ? (
            <p className="text-sm text-muted-foreground">결정 조건이 없습니다</p>
          ) : (
            <ul className="divide-y divide-border rounded border border-border text-sm">
              {check.conditions.map((c) => (
                <li key={c.key} className="space-y-1 px-3 py-2">
                  <div className="flex items-start justify-between gap-3">
                    <p className="whitespace-pre-wrap">{c.text}</p>
                    <ToneBadge tone={c.resolved ? 'success' : 'warning'}>{c.resolved ? '해소' : '미해결'}</ToneBadge>
                  </div>
                  {c.note?.reason && <p className="text-xs text-muted-foreground">해소 근거: {c.note.reason}</p>}
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => setCondition(c)}>
                      해소 근거 적기
                    </Button>
                    {c.resolved && (
                      <Button variant="outline" size="sm" onClick={() => clearCondition(c.key)}>
                        지우기
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="space-y-2">
          <h3 className="text-sm font-medium">가격 표 요약</h3>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
            <ToneBadge tone={priceStatus.tone}>{priceStatus.label}</ToneBadge>
            {hasPrice && (
              <>
                <span>
                  채운 줄 {check.price.filledCount} / {check.price.lineCount}
                </span>
                <span>합계 {formatNumber(check.price.total)}</span>
                <Button variant="outline" size="sm" onClick={openPrice}>
                  가격 표 보기
                </Button>
                <Button variant="outline" size="sm" onClick={downloadPrice}>
                  엑셀 내려받기
                </Button>
              </>
            )}
          </div>
        </section>
      </div>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>제출 검사</CardTitle>
      </CardHeader>
      <CardContent>
        {body()}
        {line && (
          <LineDialog bidRequestId={bidRequestId} line={line} onClose={() => setLine(null)} onSaved={applySaved} />
        )}
        {condition && (
          <ConditionDialog
            bidRequestId={bidRequestId}
            condition={condition}
            onClose={() => setCondition(null)}
            onSaved={applySaved}
          />
        )}
        {priceTable && <PriceDialog table={priceTable} onClose={() => setPriceTable(null)} />}
      </CardContent>
    </Card>
  )
}
