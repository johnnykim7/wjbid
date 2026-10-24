import { useCallback, useEffect, useState } from 'react'
import {
  Badge,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@therecommerce-develop/ds-ui'
import { getDocumentReading } from '../api/client'
import { FACT_CATEGORY_LABEL } from './documentReading'
import type { ReadingDetail } from './documentReading'

/** CR-169 고객 서류 한 개에서 읽은 사실 목록. 입찰 상세의 서류 줄과 절 상세의 근거 서류 꼬리표가 같이 쓴다 */
export default function DocumentReadingFacts({ readingId }: { readingId: string }) {
  const [detail, setDetail] = useState<ReadingDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setFailed(false)
    try {
      const { data } = await getDocumentReading(readingId)
      setDetail(data)
    } catch {
      setFailed(true)
    } finally {
      setLoading(false)
    }
  }, [readingId])

  useEffect(() => { load() }, [load])

  if (loading) return <p className="text-xs text-gray-400">불러오는 중</p>
  if (failed || !detail) {
    return (
      <div className="flex items-center gap-3">
        <p className="text-xs text-red-600">불러오지 못했습니다.</p>
        <Button variant="outline" size="sm" onClick={load}>다시 시도</Button>
      </div>
    )
  }

  const facts = detail.facts ?? []
  return (
    <div className="space-y-2">
      {detail.status === 'FAILED' && detail.failureReason && (
        <p className="text-xs text-red-700">읽지 못함: {detail.failureReason}</p>
      )}
      {facts.length === 0 ? (
        <p className="text-xs text-gray-500">읽은 사실이 없습니다.</p>
      ) : (
        <Table className="text-xs">
          <TableHeader>
            <TableRow>
              <TableHead className="w-28">분류</TableHead>
              <TableHead className="w-48">항목</TableHead>
              <TableHead>값</TableHead>
              <TableHead className="w-14">쪽</TableHead>
              <TableHead className="w-24">읽은 방법</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {facts.map((f, i) => (
              <TableRow key={i} className="align-top">
                <TableCell>{(f.category && FACT_CATEGORY_LABEL[f.category]) || f.category || '-'}</TableCell>
                <TableCell className="whitespace-pre-wrap">{f.key || '-'}</TableCell>
                <TableCell className="whitespace-pre-wrap">{f.value || '-'}</TableCell>
                <TableCell>{f.page != null && f.page !== '' ? f.page : '-'}</TableCell>
                <TableCell>
                  {f.readMode === 'VISION' ? <Badge variant="warning" size="sm">글자 인식</Badge> : '-'}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}
