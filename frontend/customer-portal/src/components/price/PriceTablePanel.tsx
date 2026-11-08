import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button } from '../ui/button'
import { Input } from '../ui/input'
import { LoadingSpinner } from '../ui/loading-spinner'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../ui/table'
import { exportPriceTable, getPriceTable, savePriceTable } from '../../api/client'
import type { PriceTable } from '../../types/priceTable'

const UNIT_PRICE_PATTERN = /^\d+(\.\d+)?$/

function cellKey(period: string, clin: string): string {
  return `${period}\u0000${clin}`
}

function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-'
  const [whole, fraction] = String(value).split('.')
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return fraction ? `${grouped}.${fraction}` : grouped
}

function inputsOf(table: PriceTable): Record<string, string> {
  const inputs: Record<string, string> = {}
  table.periods.forEach((period) =>
    period.lines.forEach((line) => {
      inputs[cellKey(period.period, line.clin)] = line.unitPrice === null ? '' : String(line.unitPrice)
    }),
  )
  return inputs
}

function errorMessage(error: unknown, fallback: string): string {
  const response = (error as { response?: { data?: { message?: string } } }).response
  return response?.data?.message ?? fallback
}

export function PriceTablePanel({ bidRequestId }: { bidRequestId: string }) {
  const [table, setTable] = useState<PriceTable | null>(null)
  const [inputs, setInputs] = useState<Record<string, string>>({})
  const [loadError, setLoadError] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const applyTable = useCallback((next: PriceTable) => {
    setTable(next)
    setInputs(inputsOf(next))
  }, [])

  useEffect(() => {
    let active = true
    setLoadError(false)
    getPriceTable(bidRequestId)
      .then((res) => {
        if (active) applyTable(res.data)
      })
      .catch(() => {
        if (active) setLoadError(true)
      })
    return () => {
      active = false
    }
  }, [bidRequestId, applyTable])

  const invalid = useMemo(
    () => Object.values(inputs).some((value) => value.trim() !== '' && !UNIT_PRICE_PATTERN.test(value.trim())),
    [inputs],
  )

  const save = async () => {
    if (!table) return
    setSaving(true)
    setSaveError(null)
    try {
      const unitPrices = table.periods.flatMap((period) =>
        period.lines.map((line) => ({
          clin: line.clin,
          period: period.period,
          unitPrice: inputs[cellKey(period.period, line.clin)]?.trim() || null,
        })),
      )
      const res = await savePriceTable(bidRequestId, unitPrices)
      applyTable(res.data)
    } catch (e) {
      setSaveError(errorMessage(e, '단가를 저장하지 못했습니다'))
    } finally {
      setSaving(false)
    }
  }

  const download = async () => {
    try {
      const res = await exportPriceTable(bidRequestId)
      const url = URL.createObjectURL(res.data)
      const link = document.createElement('a')
      link.href = url
      link.download = 'price-table.xlsx'
      link.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setSaveError(errorMessage(e, '엑셀을 내려받지 못했습니다'))
    }
  }

  if (loadError) {
    return (
      <div className="p-6 max-w-5xl mx-auto">
        <div className="rounded-xl border border-danger/30 bg-danger-soft p-4 text-sm text-danger">
          가격 표를 불러오지 못했습니다.
        </div>
      </div>
    )
  }
  if (!table) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner />
      </div>
    )
  }
  if (table.status === 'NOT_READY' || table.status === 'NOT_APPLICABLE') {
    return (
      <p className="text-sm text-ink-mute text-center py-12">
        {table.status === 'NOT_READY' ? '공고 분석이 끝나면 표가 나옵니다' : '이 공고에는 가격 표가 없습니다'}
      </p>
    )
  }

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      <div className="rounded-xl border border-line bg-white p-4">
        <p className="text-sm text-ink">공고의 가격 항목으로 만든 표입니다. 줄마다 단가를 넣고 저장하면 금액과 합계가 계산됩니다.</p>
        <p className="mt-1 text-sm font-semibold text-ink">
          {table.status === 'COMPLETE' ? '모든 줄의 단가를 넣었습니다' : '단가를 넣지 않은 줄이 있습니다'}
        </p>
      </div>

      {saveError && (
        <div className="rounded-xl border border-danger/30 bg-danger-soft p-4 text-sm text-danger">{saveError}</div>
      )}

      {table.periods.map((period) => (
        <section key={period.period} className="rounded-xl border border-line bg-white">
          <h3 className="border-b border-line-soft px-4 py-3 text-sm font-semibold text-ink">{period.period}</h3>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>CLIN</TableHead>
                <TableHead>품목</TableHead>
                <TableHead className="text-right">수량</TableHead>
                <TableHead>단위</TableHead>
                <TableHead className="w-44 text-right">단가</TableHead>
                <TableHead className="text-right">금액</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {period.lines.map((line) => {
                const key = cellKey(period.period, line.clin)
                const value = inputs[key] ?? ''
                const wrong = value.trim() !== '' && !UNIT_PRICE_PATTERN.test(value.trim())
                return (
                  <TableRow key={key}>
                    <TableCell className="whitespace-nowrap">{line.clin}</TableCell>
                    <TableCell className="whitespace-normal">{line.description ?? '-'}</TableCell>
                    <TableCell className="text-right">{formatNumber(line.quantity)}</TableCell>
                    <TableCell>{line.unit ?? '-'}</TableCell>
                    <TableCell className="text-right">
                      <Input
                        aria-label={`${line.clin} 단가`}
                        className="text-right"
                        inputMode="decimal"
                        disabled={!table.editable}
                        value={value}
                        onChange={(e) => setInputs((prev) => ({ ...prev, [key]: e.target.value }))}
                      />
                      {wrong && <p className="mt-1 text-xs text-danger">0 이상의 숫자만 넣을 수 있습니다</p>}
                    </TableCell>
                    <TableCell className="text-right whitespace-nowrap">{formatNumber(line.amount)}</TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
          <p className="border-t border-line-soft px-4 py-3 text-right text-sm text-ink">
            소계 {formatNumber(period.subtotal)}
          </p>
        </section>
      ))}

      <div className="flex items-center justify-between gap-3">
        <p className="text-base font-bold text-ink">합계 {formatNumber(table.total)}</p>
        <div className="flex gap-2">
          <Button variant="outline" onClick={download}>
            엑셀 내려받기
          </Button>
          {table.editable && (
            <Button disabled={saving || invalid} onClick={save}>
              저장
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
