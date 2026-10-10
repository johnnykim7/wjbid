import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Card } from '../components/ui/card'
import { Badge } from '../components/ui/badge'
import { StatCard } from '../components/ui/stat-card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../components/ui/table'
import { EmptyState } from '../components/ui/empty-state'
import { formatWon } from '../hooks/usePricing'

/**
 * 결제 내역 (CR-129, T3-3)
 * 회원 월 요금과 대행·컨설팅 결제를 함께 보여 준다. 결제 연동 전이라 내역은 비어 있다.
 */

type BillingKind = 'MEMBERSHIP' | 'SERVICE'

const KIND_LABEL: Record<BillingKind, string> = {
  MEMBERSHIP: '회원 월 요금',
  SERVICE: '대행·컨설팅',
}

interface BillingRow {
  id: string
  date: string
  kind: BillingKind
  description: string
  amount: number
}

const rows: BillingRow[] = []

export default function BillingPage() {
  const { total, membershipCount, serviceCount } = useMemo(() => {
    return {
      total: rows.reduce((sum, r) => sum + r.amount, 0),
      membershipCount: rows.filter((r) => r.kind === 'MEMBERSHIP').length,
      serviceCount: rows.filter((r) => r.kind === 'SERVICE').length,
    }
  }, [])

  return (
    <div className="space-y-6">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-ink">결제 내역</h2>
        <p className="text-sm text-ink-mute mt-1">회원 월 요금과 대행·컨설팅 결제 내역입니다.</p>
      </div>

      {/* 안내 띠 */}
      <div className="mb-6 flex items-center gap-3 rounded-xl border border-brand-line bg-brand-soft px-5 py-4">
        <div className="w-10 h-10 rounded-lg bg-white flex items-center justify-center flex-shrink-0">
          <i className="fa-solid fa-receipt text-brand" />
        </div>
        <div className="text-sm flex-1">
          <p className="font-semibold text-ink">회원은 월 단위, 대행·컨설팅은 건별 견적</p>
          <p className="text-ink-mute mt-0.5">
            회원 월 요금은 매달, 대행·컨설팅은 상담 후 견적에 따라 결제됩니다.
          </p>
        </div>
        <Link to="/pricing" className="text-sm font-semibold text-brand hover:underline whitespace-nowrap">
          요금 안내
        </Link>
      </div>

      {/* 요약 숫자 */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <StatCard
          label="누적 결제 금액"
          value={formatWon(total)}
          icon="fa-solid fa-won-sign"
          iconBg="bg-brand-soft"
          iconColor="text-ink"
          dark
        />
        <StatCard
          label="회원 결제"
          value={membershipCount}
          unit="회"
          icon="fa-solid fa-id-card"
          iconBg="bg-brand-soft"
          iconColor="text-brand"
        />
        <StatCard
          label="대행·컨설팅"
          value={serviceCount}
          unit="건"
          icon="fa-solid fa-handshake"
          iconBg="bg-success-soft"
          iconColor="text-success"
        />
      </div>

      {/* 내역 표 */}
      <Card className="overflow-hidden">
        {rows.length === 0 ? (
          <EmptyState
            icon="fa-solid fa-receipt"
            title="결제 내역이 없습니다"
            description="회원 결제나 대행·컨설팅 결제를 하면 내역이 표시됩니다."
          />
        ) : (
          <Table>
            <TableHeader>
              <tr>
                <TableHead className="w-36">일자</TableHead>
                <TableHead className="w-36">항목</TableHead>
                <TableHead>내용</TableHead>
                <TableHead className="w-36 text-right">금액</TableHead>
              </tr>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="whitespace-nowrap text-ink-mute">{r.date}</TableCell>
                  <TableCell>
                    <Badge variant={r.kind === 'MEMBERSHIP' ? 'info' : 'final'} size="sm">
                      {KIND_LABEL[r.kind]}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-ink-sub">{r.description}</TableCell>
                  <TableCell className="text-right font-semibold text-ink">{formatWon(r.amount)}</TableCell>
                </TableRow>
              ))}
              <TableRow className="hover:bg-transparent bg-surface">
                <TableCell colSpan={3} className="text-right font-bold text-ink-sub">
                  합계
                </TableCell>
                <TableCell className="text-right font-bold text-ink">{formatWon(total)}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        )}
      </Card>

      <p className="mt-6 text-xs text-ink-mute">
        <i className="fa-solid fa-circle-info mr-1" />
        결제 기능은 준비 중입니다.
      </p>
    </div>
  )
}
