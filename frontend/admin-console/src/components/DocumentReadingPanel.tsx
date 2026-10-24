import { Fragment, useCallback, useEffect, useRef, useState } from 'react'
import {
  Badge,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  toast,
} from '@therecommerce-develop/ds-ui'
import { getDocumentReadings, rerunDocumentReadings } from '../api/client'
import DocumentReadingFacts from './DocumentReadingFacts'
import { ORIGIN_LABEL, isReadingInFlight, readingStatusLabel } from './documentReading'
import type { ReadingItem, ReadingOverview } from './documentReading'

const POLL_INTERVAL_MS = 10_000

function fulfillsText(item: ReadingItem): string {
  const parts = (item.fulfills ?? [])
    .map((f) => [f.reqId, f.requirementTitle].filter(Boolean).join(' '))
    .filter(Boolean)
  return parts.length > 0 ? parts.join(', ') : '-'
}

/** 다시 읽기를 걸 수 있는 줄. 읽는 중, 읽음, 읽을 수 없는 형식은 서버도 건너뛴다 */
function rerunnable(item: ReadingItem): boolean {
  return item.status === null || item.status === 'FAILED'
}

/**
 * CR-169 입찰 상세 관리의 고객 서류 읽기 영역.
 * 읽는 중이면 10초마다 다시 부르고, 읽는 중인지를 상태 패널에 알려 문서 작성 중 전환을 막게 한다.
 */
export default function DocumentReadingPanel({ bidRequestId, bidState, reloadKey, onInProgressChange }: {
  bidRequestId: string
  bidState: string
  reloadKey?: number
  onInProgressChange?: (inProgress: boolean) => void
}) {
  const [overview, setOverview] = useState<ReadingOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [openReadingId, setOpenReadingId] = useState<string | null>(null)
  const [running, setRunning] = useState(false)
  const notify = useRef(onInProgressChange)
  notify.current = onInProgressChange

  const apply = (data: ReadingOverview) => {
    setOverview(data)
    notify.current?.(data.inProgress)
  }

  const load = useCallback(async (quiet = false) => {
    if (!quiet) setLoading(true)
    setFailed(false)
    try {
      const { data } = await getDocumentReadings(bidRequestId)
      apply(data)
    } catch {
      setFailed(true)
    } finally {
      if (!quiet) setLoading(false)
    }
  }, [bidRequestId])

  useEffect(() => { load() }, [load, reloadKey, bidState])

  const inProgress = overview?.inProgress ?? false
  useEffect(() => {
    if (!inProgress) return
    const timer = window.setInterval(() => load(true), POLL_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [inProgress, load])

  const rerun = async (readingIds?: string[]) => {
    setRunning(true)
    try {
      const { data } = await rerunDocumentReadings(bidRequestId, readingIds)
      apply(data)
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(message || '다시 읽기를 걸지 못했습니다.')
      load(true)
    } finally {
      setRunning(false)
    }
  }

  const items = overview?.items ?? []
  const started = overview?.readingStarted ?? false
  const canRerun = started && bidState === 'ANALYZING'
  const doneCount = items.filter((i) => i.status === 'DONE').length

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <h2 className="text-sm font-semibold text-gray-900 flex items-center gap-2 mr-2">
          <i className="fa-solid fa-magnifying-glass text-secondary" /> 고객 서류 읽기
        </h2>
        {started && items.length > 0 && (
          <span className="text-xs text-gray-500">{items.length}개 중 읽음 {doneCount}개</span>
        )}
        {inProgress && <Badge variant="info" size="sm">읽는 중</Badge>}
        {canRerun && items.some(rerunnable) && (
          <Button variant="outline" size="sm" className="ml-auto" disabled={running} onClick={() => rerun()}>
            다시 읽기
          </Button>
        )}
      </div>

      {loading ? (
        <p className="text-sm text-gray-400">불러오는 중</p>
      ) : failed ? (
        <div className="flex items-center gap-3">
          <p className="text-sm text-red-600">불러오지 못했습니다.</p>
          <Button variant="outline" size="sm" onClick={() => load()}>다시 시도</Button>
        </div>
      ) : items.length === 0 ? (
        <p className="text-sm text-gray-500">읽을 고객 서류가 없습니다.</p>
      ) : (
        <>
          {!started && <p className="text-sm text-gray-500">분석 단계로 전환하면 읽습니다.</p>}
          <Table className="text-sm">
            <TableHeader>
              <TableRow>
                <TableHead>파일 이름</TableHead>
                <TableHead className="w-28">출처</TableHead>
                <TableHead>충족하는 요구 항목</TableHead>
                <TableHead className="w-44">상태</TableHead>
                <TableHead className="w-20 whitespace-nowrap">사실 수</TableHead>
                {canRerun && <TableHead className="w-24" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => {
                const key = item.readingId ?? `${item.origin}-${item.documentId}`
                const open = item.readingId !== null && openReadingId === item.readingId
                return (
                  <Fragment key={key}>
                    <TableRow
                      className={`align-top ${item.readingId ? 'cursor-pointer' : ''}`}
                      onClick={() => item.readingId && setOpenReadingId(open ? null : item.readingId)}
                    >
                      <TableCell className="break-all">{item.fileName}</TableCell>
                      <TableCell className="whitespace-nowrap text-gray-600">{ORIGIN_LABEL[item.origin]}</TableCell>
                      <TableCell className="text-gray-600">{fulfillsText(item)}</TableCell>
                      <TableCell>
                        {started && (
                          <>
                            <span className={item.status === 'FAILED' ? 'text-red-700' : isReadingInFlight(item.status) ? 'text-blue-700' : ''}>
                              {readingStatusLabel(item.status, item.visionUsed)}
                            </span>
                            {item.status === 'FAILED' && item.failureReason && (
                              <p className="text-xs text-red-600">{item.failureReason}</p>
                            )}
                          </>
                        )}
                      </TableCell>
                      <TableCell>{item.status === 'DONE' ? item.factCount : '-'}</TableCell>
                      {canRerun && (
                        <TableCell onClick={(e) => e.stopPropagation()}>
                          {item.status === 'FAILED' && item.readingId && (
                            <Button variant="outline" size="sm" disabled={running}
                              onClick={() => rerun([item.readingId as string])}>
                              다시 읽기
                            </Button>
                          )}
                        </TableCell>
                      )}
                    </TableRow>
                    {open && item.readingId && (
                      <TableRow className="bg-gray-50">
                        <TableCell colSpan={canRerun ? 6 : 5}>
                          <DocumentReadingFacts readingId={item.readingId} />
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                )
              })}
            </TableBody>
          </Table>
        </>
      )}
    </div>
  )
}
