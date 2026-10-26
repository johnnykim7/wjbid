/**
 * CR-132: 1:1 문의 목록.
 * 고른 상태·유형·쪽은 주소에 두고, 보던 스크롤 위치는 상세에 들어갈 때 적어 두었다가 돌아오면 되살린다.
 */
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Button,
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTable,
  CardToolbar,
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
} from '@therecommerce-develop/ds-ui'
import {
  getAdminInquiries,
  getAdminInquiryCounts,
  getInquiryTypes,
  type InquiryCounts,
  type InquiryListItem,
  type InquiryStatus,
} from '../api/client'
import { InquiryMemberPill, InquiryStatusPill, InquiryTypePill } from '../components/InquiryPills'
import ListPagination from '../components/ListPagination'
import { TableMessageRow } from '../components/LoadState'

const PAGE_SIZE = 20
const ALL = 'ALL'
const SCROLL_KEY = 'inquiry-list-scroll'

const STATUS_TABS: { key: InquiryStatus | typeof ALL; label: string; count: (c: InquiryCounts) => number }[] = [
  { key: ALL, label: '전체', count: (c) => c.all },
  { key: 'RECEIVED', label: '미답변', count: (c) => c.received },
  { key: 'ANSWERED', label: '답변완료', count: (c) => c.answered },
]

export default function InquiryAdminPage() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const status = (params.get('status') ?? 'RECEIVED') as InquiryStatus | typeof ALL
  const typeCode = params.get('type') ?? ALL
  const page = Number(params.get('page') ?? '0')

  const rootRef = useRef<HTMLDivElement>(null)
  const [types, setTypes] = useState<{ code: string; label: string }[]>([])
  const [counts, setCounts] = useState<InquiryCounts | null>(null)
  const [rows, setRows] = useState<InquiryListItem[] | null>(null)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [failed, setFailed] = useState(false)

  const load = useCallback(() => {
    getAdminInquiries({
      status: status === ALL ? undefined : status,
      typeCode: typeCode === ALL ? undefined : typeCode,
      page,
      size: PAGE_SIZE,
    })
      .then((res) => {
        setFailed(false)
        setRows(res.data.content)
        setTotal(res.data.totalElements)
        setTotalPages(res.data.totalPages)
      })
      .catch(() => {
        setRows(null)
        setFailed(true)
      })
  }, [status, typeCode, page])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    getInquiryTypes().then((res) => setTypes(res.data)).catch(() => setTypes([]))
    getAdminInquiryCounts().then((res) => setCounts(res.data)).catch(() => setCounts(null))
  }, [])

  // 상세에서 돌아왔을 때 표가 그려진 뒤 한 번만 되살린다
  useLayoutEffect(() => {
    if (!rows) return
    const saved = sessionStorage.getItem(SCROLL_KEY)
    if (saved === null) return
    sessionStorage.removeItem(SCROLL_KEY)
    const main = rootRef.current?.closest('main')
    if (main) main.scrollTop = Number(saved)
  }, [rows])

  const setFilter = (next: { status?: string; type?: string; page?: number }) => {
    const p = new URLSearchParams(params)
    if (next.status !== undefined) p.set('status', next.status)
    if (next.type !== undefined) {
      if (next.type === ALL) p.delete('type')
      else p.set('type', next.type)
    }
    p.set('page', String(next.page ?? 0))
    setParams(p)
  }

  const openDetail = (id: string) => {
    const main = rootRef.current?.closest('main')
    sessionStorage.setItem(SCROLL_KEY, String(main?.scrollTop ?? 0))
    navigate(`/inquiries/${id}`)
  }

  const from = total === 0 ? 0 : page * PAGE_SIZE + 1
  const to = Math.min(total, (page + 1) * PAGE_SIZE)

  return (
    <div ref={rootRef} className="space-y-4">
      <Card>
        <CardHeader className="py-2.5">
          <div className="inline-flex">
            {STATUS_TABS.map((tab) => (
              <Button
                key={tab.key}
                type="button"
                size="sm"
                variant={status === tab.key ? 'primary' : 'outline'}
                onClick={() => setFilter({ status: tab.key })}
                className="rounded-none first:rounded-l-lg last:rounded-r-lg -ml-px first:ml-0"
              >
                {tab.label}
                {counts && <span className="ml-1 text-xs opacity-80">{tab.count(counts)}</span>}
              </Button>
            ))}
          </div>

          <CardToolbar>
            <span className="text-xs text-muted-foreground">최근 접수 순</span>
            <Select value={typeCode} onValueChange={(v) => setFilter({ type: v })}>
              <SelectTrigger className="w-48">
                <SelectValue placeholder="유형 전체" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>유형 전체</SelectItem>
                {types.map((t) => (
                  <SelectItem key={t.code} value={t.code}>
                    {t.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </CardToolbar>
        </CardHeader>

        {failed ? (
          <CardContent className="space-y-3 py-12 text-center text-sm text-muted-foreground">
            <p>문의를 불러오지 못했습니다.</p>
            <Button variant="outline" size="sm" onClick={load}>
              다시 시도
            </Button>
          </CardContent>
        ) : (
          <CardTable>
            <Table>
              <TableHeader className="bg-muted">
                <TableRow>
                  {['접수 일시', '유형', '제목', '이름', '이메일', '구분', '상태'].map((h) => (
                    <TableHead key={h}>{h}</TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {!rows && <TableMessageRow colSpan={7} loading />}
                {rows && rows.length === 0 && <TableMessageRow colSpan={7}>해당하는 문의가 없습니다.</TableMessageRow>}
                {rows?.map((r) => (
                  <TableRow key={r.id} onClick={() => openDetail(r.id)} className="cursor-pointer">
                    <TableCell className="whitespace-nowrap text-xs text-muted-foreground">{r.createdAt}</TableCell>
                    <TableCell>
                      <InquiryTypePill label={r.typeLabel} />
                    </TableCell>
                    <TableCell
                      className={`max-w-[360px] truncate text-foreground ${
                        r.status === 'RECEIVED' ? 'font-bold' : 'font-medium'
                      }`}
                    >
                      {r.title}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{r.name}</TableCell>
                    <TableCell className="whitespace-nowrap text-xs text-muted-foreground">{r.email}</TableCell>
                    <TableCell>
                      <InquiryMemberPill member={r.member} />
                    </TableCell>
                    <TableCell>
                      <InquiryStatusPill status={r.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardTable>
        )}

        {!failed && (
          <CardFooter className="justify-between py-2 text-xs text-muted-foreground">
            <span>{total === 0 ? '0건' : `${total}건 중 ${from}-${to}`}</span>
            <ListPagination page={page} totalPages={totalPages} onChange={(p) => setFilter({ page: p })} />
          </CardFooter>
        )}
      </Card>
    </div>
  )
}
