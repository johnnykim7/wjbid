import { useMemo } from 'react'
import { Card } from '../components/ui/card'
import { Badge } from '../components/ui/badge'
import { StatCard } from '../components/ui/stat-card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../components/ui/table'
import { EmptyState } from '../components/ui/empty-state'

// ── 더미 데이터 (BE 미연동) ─────────────────────────────────────
// 건당 과금 결제 내역. 실제 연동 시 결제/과금 API 필요.
type BillingItem = '공고분석' | '제안서'

interface BillingRow {
  id: string
  date: string
  item: BillingItem
  noticeId: string
  amount: number
}

const DUMMY_BILLING: BillingRow[] = [
  { id: 'b1', date: '2026-06-27', item: '제안서', noticeId: 'SPMYM2-26-T-1050', amount: 150000 },
  { id: 'b2', date: '2026-06-25', item: '공고분석', noticeId: 'W912UM-26-R-0033', amount: 30000 },
  { id: 'b3', date: '2026-06-22', item: '제안서', noticeId: 'W912UM-26-R-0033', amount: 150000 },
  { id: 'b4', date: '2026-06-18', item: '공고분석', noticeId: 'SPE300-26-Q-0210', amount: 30000 },
  { id: 'b5', date: '2026-06-15', item: '공고분석', noticeId: 'FA5240-26-B-0007', amount: 30000 },
]

const KRW = (n: number) => n.toLocaleString('ko-KR') + '원'

export default function BillingPage() {
  const rows = DUMMY_BILLING

  const { total, analysisCount, proposalCount } = useMemo(() => {
    return {
      total: rows.reduce((sum, r) => sum + r.amount, 0),
      analysisCount: rows.filter((r) => r.item === '공고분석').length,
      proposalCount: rows.filter((r) => r.item === '제안서').length,
    }
  }, [rows])

  return (
    <div className="space-y-6">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-ink">결제 내역</h2>
        <p className="text-sm text-ink-mute mt-1">건당 과금 · 구독 없음 — 사용한 만큼만 결제됩니다.</p>
      </div>

      {/* 안내 배너 */}
      <div className="mb-6 flex items-center gap-3 rounded-xl border border-brand-line bg-brand-softer px-5 py-4">
        <div className="w-10 h-10 rounded-lg bg-brand-soft flex items-center justify-center flex-shrink-0">
          <i className="fa-solid fa-receipt text-brand" />
        </div>
        <div className="text-sm">
          <p className="font-semibold text-ink">건당 과금 · 구독 없음</p>
          <p className="text-ink-mute mt-0.5">
            공고분석 및 제안서 작성은 사용 건수만큼 청구됩니다. 월 구독료는 없습니다.
          </p>
        </div>
      </div>

      {/* 요약 통계 */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <StatCard
          label="누적 결제 금액"
          value={KRW(total)}
          icon="fa-solid fa-won-sign"
          iconBg="bg-brand-soft"
          iconColor="text-brand"
          dark
        />
        <StatCard
          label="공고분석"
          value={analysisCount}
          unit="건"
          icon="fa-solid fa-magnifying-glass-chart"
          iconBg="bg-brand-soft"
          iconColor="text-brand"
        />
        <StatCard
          label="제안서 작성"
          value={proposalCount}
          unit="건"
          icon="fa-solid fa-file-pen"
          iconBg="bg-brand-soft"
          iconColor="text-brand"
        />
      </div>

      {/* 내역 테이블 */}
      <Card className="overflow-hidden">
        {rows.length === 0 ? (
          <EmptyState
            icon="fa-solid fa-receipt"
            title="결제 내역이 없습니다"
            description="공고분석 또는 제안서 작성을 이용하면 내역이 표시됩니다."
          />
        ) : (
          <Table>
            <TableHeader>
              <tr>
                <TableHead className="w-36">일자</TableHead>
                <TableHead className="w-32">항목</TableHead>
                <TableHead>공고번호</TableHead>
                <TableHead className="w-36 text-right">금액</TableHead>
              </tr>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="whitespace-nowrap text-ink-mute">{r.date}</TableCell>
                  <TableCell>
                    <Badge variant={r.item === '제안서' ? 'final' : 'info'} size="sm">
                      {r.item}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs text-ink font-semibold">
                    {r.noticeId}
                  </TableCell>
                  <TableCell className="text-right font-semibold text-ink">
                    {KRW(r.amount)}
                  </TableCell>
                </TableRow>
              ))}
              <TableRow className="hover:bg-transparent bg-surface">
                <TableCell colSpan={3} className="text-right font-bold text-ink-sub">
                  합계
                </TableCell>
                <TableCell className="text-right font-bold text-ink">{KRW(total)}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        )}
      </Card>

      <p className="mt-6 text-xs text-ink-mute">
        <i className="fa-solid fa-circle-info mr-1" />
        결제 내역은 현재 화면 시연용 더미 데이터입니다. (BE 미연동)
      </p>
    </div>
  )
}
