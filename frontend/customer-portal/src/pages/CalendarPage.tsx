import { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Calendar } from '@therecommerce-develop/ds-ui'
import { Card, CardHeader, CardContent } from '../components/ui/card'
import { Badge } from '../components/ui/badge'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { EmptyState } from '../components/ui/empty-state'
import { getMyBookmarks } from '../api/client'
import type { Opportunity } from '../types'

function daysLeft(deadline: string): number {
  return Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000)
}

function deadlineBadge(deadline: string): { variant: 'closing' | 'active' | 'closed'; label: string } {
  const d = daysLeft(deadline)
  if (d < 0) return { variant: 'closed', label: '마감' }
  if (d <= 14) return { variant: 'closing', label: `D-${d}` }
  return { variant: 'active', label: `D-${d}` }
}

export default function CalendarPage() {
  const [bookmarks, setBookmarks] = useState<Opportunity[]>([])
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState<Date | undefined>(new Date())

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getMyBookmarks()
        setBookmarks(res.data.content ?? [])
      } catch {
        // BE 미연동/오류 시 더미 폴백
        setBookmarks([])
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  // 관심 공고 마감일을 달력 modifier 로 표시
  const deadlineDates = useMemo(
    () =>
      bookmarks
        .filter((b) => b.responseDeadline)
        .map((b) => {
          const dt = new Date(b.responseDeadline)
          return new Date(dt.getFullYear(), dt.getMonth(), dt.getDate())
        }),
    [bookmarks]
  )

  // 마감 임박순 정렬 (지난 공고 제외, 가까운 순)
  const upcoming = useMemo(
    () =>
      [...bookmarks]
        .filter((b) => b.responseDeadline && daysLeft(b.responseDeadline) >= 0)
        .sort(
          (a, b) =>
            new Date(a.responseDeadline).getTime() - new Date(b.responseDeadline).getTime()
        ),
    [bookmarks]
  )

  // 선택한 날짜에 마감인 공고
  const sameDay = (a: string, d?: Date) => {
    if (!d) return false
    const x = new Date(a)
    return (
      x.getFullYear() === d.getFullYear() &&
      x.getMonth() === d.getMonth() &&
      x.getDate() === d.getDate()
    )
  }
  const onSelectedDay = bookmarks.filter((b) => sameDay(b.responseDeadline, selected))

  if (loading) return <LoadingSpinner fullPage />

  return (
    <div className="space-y-6">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-ink">입찰 캘린더</h2>
        <p className="text-sm text-ink-mute mt-1">관심 공고의 마감일을 달력에서 한눈에 확인하세요.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 월 달력 */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <h3 className="text-base font-bold text-ink">월 달력</h3>
          </CardHeader>
          <CardContent className="flex justify-center">
            <Calendar
              mode="single"
              selected={selected}
              onSelect={setSelected}
              modifiers={{ deadline: deadlineDates }}
              modifiersClassNames={{
                deadline: 'bg-brand-soft text-ink font-bold rounded-md',
              }}
            />
          </CardContent>
        </Card>

        {/* 마감 임박 사이드 리스트 */}
        <Card>
          <CardHeader>
            <h3 className="text-base font-bold text-ink">
              마감 임박 공고
              {upcoming.length > 0 && (
                <span className="ml-2 text-xs font-normal text-ink-mute">{upcoming.length}건</span>
              )}
            </h3>
          </CardHeader>
          <CardContent className="p-0">
            {upcoming.length === 0 ? (
              <EmptyState
                icon="fa-regular fa-calendar"
                title="예정된 마감 공고가 없습니다"
                description="입찰 검색에서 관심 공고를 저장하세요."
              />
            ) : (
              <ul className="divide-y divide-line-soft max-h-[480px] overflow-y-auto">
                {upcoming.map((b) => {
                  const badge = deadlineBadge(b.responseDeadline)
                  return (
                    <li key={b.id}>
                      <Link
                        to={`/search/${b.id}`}
                        className="block px-5 py-3 hover:bg-brand-softer transition"
                      >
                        <div className="flex items-start justify-between gap-2 mb-1">
                          <span className="text-xs font-bold text-ink font-mono truncate">
                            {b.solicitationNumber}
                          </span>
                          <Badge variant={badge.variant} size="sm">
                            {badge.label}
                          </Badge>
                        </div>
                        <p className="text-sm font-medium text-ink line-clamp-2 leading-snug">
                          {b.title}
                        </p>
                        <p className="text-xs text-ink-mute mt-1">
                          <i className="fa-regular fa-clock mr-1" />
                          {b.responseDeadline?.slice(0, 10)}
                        </p>
                      </Link>
                    </li>
                  )
                })}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 선택한 날짜의 마감 공고 */}
      {selected && (
        <Card className="mt-6">
          <CardHeader>
            <h3 className="text-base font-bold text-ink">
              {selected.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })}{' '}
              마감 공고
            </h3>
          </CardHeader>
          <CardContent>
            {onSelectedDay.length === 0 ? (
              <p className="text-sm text-ink-mute">이 날짜에 마감되는 관심 공고가 없습니다.</p>
            ) : (
              <ul className="space-y-2">
                {onSelectedDay.map((b) => (
                  <li key={b.id} className="flex items-center justify-between text-sm">
                    <Link to={`/search/${b.id}`} className="text-brand hover:underline truncate">
                      <span className="font-mono text-xs text-ink mr-2">
                        {b.solicitationNumber}
                      </span>
                      {b.title}
                    </Link>
                    <Badge variant={deadlineBadge(b.responseDeadline).variant} size="sm">
                      {deadlineBadge(b.responseDeadline).label}
                    </Badge>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
