import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import {
  Button,
  Card,
  CardFooter,
  CardHeader,
  CardTable,
  CardToolbar,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@therecommerce-develop/ds-ui'
import { getAdminNotices } from '../api/client'
import { DdayBadge } from '../utils/dday'
import type { StateTone } from '../utils/bidState'
import { ToneBadge } from '../components/StateBadge'
import ListPagination from '../components/ListPagination'
import { TableMessageRow } from '../components/LoadState'

interface NoticeAdmin {
  id: string
  opportunityId: string
  originTitle: string
  solicitationNumber?: string
  organizationName?: string
  noticeTypeKo?: string       // 공고유형(한글)
  postedDate?: string         // 공고일
  responseDeadline?: string   // 마감일
  koreanTitle?: string
  generationStatus: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  visibility: 'HIDDEN' | 'VISIBLE'
  analyzedAt?: string
}

const GEN_TONE: Record<string, StateTone> = {
  PENDING: 'warning',
  ANALYZING: 'info',
  COMPLETED: 'success',
  FAILED: 'destructive',
}
const GEN_LABELS: Record<string, string> = {
  PENDING: '대기', ANALYZING: '분석 중', COMPLETED: '완료', FAILED: '실패',
}

export default function NoticeAdminPage() {
  const navigate = useNavigate()
  const [notices, setNotices] = useState<NoticeAdmin[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)

  const fetchData = async () => {
    setLoading(true)
    try {
      const { data } = await getAdminNotices(page)
      setNotices(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('공고문 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [page])

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardToolbar className="ms-auto">
            <Button variant="outline" onClick={() => navigate('/opportunities')}>
              <ArrowLeft />
              원본 공고
            </Button>
          </CardToolbar>
        </CardHeader>
        <CardTable>
          <Table className="table-fixed">
            <TableHeader className="bg-muted">
              <TableRow>
                <TableHead className="w-[32%]">공고문</TableHead>
                <TableHead>기관</TableHead>
                <TableHead className="w-28 whitespace-nowrap">공고유형</TableHead>
                <TableHead className="w-32 whitespace-nowrap">공고일</TableHead>
                <TableHead className="w-44 whitespace-nowrap">마감일</TableHead>
                <TableHead className="w-28 whitespace-nowrap text-center">진행상태</TableHead>
                <TableHead className="w-24 whitespace-nowrap text-center">노출</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableMessageRow colSpan={7} loading />
              ) : notices.length === 0 ? (
                <TableMessageRow colSpan={7}>공고문이 없습니다. 원본 공고에서 "공고문 만들기"로 생성하세요.</TableMessageRow>
              ) : (
                notices.map((n) => (
                  <TableRow key={n.id} className="cursor-pointer" onClick={() => navigate(`/notices/${n.id}`)}>
                    <TableCell>
                      <div className="truncate font-medium text-foreground">{n.koreanTitle || n.originTitle}</div>
                      <div className="truncate text-xs text-muted-foreground">{n.originTitle}</div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      <div className="truncate" title={n.organizationName || ''}>{n.organizationName || '-'}</div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      <div className="truncate" title={n.noticeTypeKo || ''}>{n.noticeTypeKo || '-'}</div>
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {n.postedDate ? new Date(n.postedDate).toLocaleDateString('ko') : '-'}
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {n.responseDeadline ? (
                        <div className="flex items-center gap-1.5">
                          <span>{new Date(n.responseDeadline).toLocaleDateString('ko')}</span>
                          <DdayBadge deadline={n.responseDeadline} />
                        </div>
                      ) : '-'}
                    </TableCell>
                    <TableCell className="text-center">
                      <ToneBadge tone={GEN_TONE[n.generationStatus] ?? 'gray'}>
                        {GEN_LABELS[n.generationStatus] || n.generationStatus}
                      </ToneBadge>
                    </TableCell>
                    <TableCell className="text-center">
                      <ToneBadge tone={n.visibility === 'VISIBLE' ? 'success' : 'gray'}>
                        {n.visibility === 'VISIBLE' ? '노출' : '비노출'}
                      </ToneBadge>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardTable>
        {totalPages > 1 && (
          <CardFooter className="justify-center py-2">
            <ListPagination page={page} totalPages={totalPages} onChange={setPage} />
          </CardFooter>
        )}
      </Card>
    </div>
  )
}
