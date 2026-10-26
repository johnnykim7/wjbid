import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CircleCheck, FilePenLine, Loader } from 'lucide-react'
import {
  Card,
  CardContent,
  CardHeader,
  CardTable,
  CardTitle,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@therecommerce-develop/ds-ui'
import { getBidRequestStats, getAdminBidRequests } from '../api/client'
import StateBadge from '../components/StateBadge'
import { Loading, TableMessageRow } from '../components/LoadState'

interface BidRequest {
  id: string
  state: string
  opportunityTitle?: string
  agencyName?: string
  memberEmail?: string
  createdAt?: string
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const [stats, setStats] = useState<Record<string, number>>({})
  const [recent, setRecent] = useState<BidRequest[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getBidRequestStats().then(r => setStats(r.data)),
      getAdminBidRequests(undefined, 0).then(r => setRecent(r.data.content ?? [])),
    ]).finally(() => setLoading(false))
  }, [])

  const total = stats.total ?? Object.values(stats).reduce((a, b) => a + b, 0)
  const active = (stats.ANALYZING ?? 0) + (stats.GENERATING ?? 0) +
    (stats.REVIEW ?? 0) + (stats.CONFIRMED ?? 0)
  const submitted = stats.SUBMITTED ?? 0

  const STAT_CARDS = [
    { label: '전체 입찰 요청', value: total, Icon: FilePenLine, color: 'text-blue-600', bg: 'bg-blue-50' },
    { label: '진행 중', value: active, Icon: Loader, color: 'text-purple-600', bg: 'bg-purple-50' },
    { label: '제출 완료', value: submitted, Icon: CircleCheck, color: 'text-emerald-600', bg: 'bg-emerald-50' },
  ]

  if (loading) return <Loading className="h-64" />

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-4">
        {STAT_CARDS.map(({ label, value, Icon, color, bg }) => (
          <Card key={label}>
            <CardContent className="p-5">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm text-muted-foreground">{label}</span>
                <div className={`size-9 rounded-lg ${bg} flex items-center justify-center`}>
                  <Icon className={`size-4 ${color}`} />
                </div>
              </div>
              <div className="text-3xl font-bold text-foreground">{value}</div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>최근 입찰 요청</CardTitle>
        </CardHeader>
        <CardTable>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>공고 제목</TableHead>
                <TableHead>기관</TableHead>
                <TableHead>회원</TableHead>
                <TableHead>상태</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recent.length === 0 && <TableMessageRow colSpan={4}>입찰 요청이 없습니다.</TableMessageRow>}
              {recent.slice(0, 8).map((br) => (
                <TableRow key={br.id} className="cursor-pointer" onClick={() => navigate(`/bid-requests/${br.id}`)}>
                  <TableCell className="max-w-xs truncate font-medium text-foreground">
                    {br.opportunityTitle ?? '-'}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{br.agencyName ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{br.memberEmail ?? '-'}</TableCell>
                  <TableCell>
                    <StateBadge state={br.state} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardTable>
      </Card>
    </div>
  )
}
