import type { ReactNode } from 'react'
import { Spinner, TableCell, TableRow } from '@therecommerce-develop/ds-ui'

/** 화면이나 카드 하나를 불러오는 동안 */
export function Loading({ className = 'h-48' }: { className?: string }) {
  return (
    <div className={`flex items-center justify-center gap-2 text-sm text-muted-foreground ${className}`}>
      <Spinner />
      불러오는 중
    </div>
  )
}

/** 표 안에서 불러오는 중, 빈 목록 안내 한 줄 */
export function TableMessageRow({
  colSpan,
  loading,
  children,
}: {
  colSpan: number
  loading?: boolean
  children?: ReactNode
}) {
  return (
    <TableRow className="hover:bg-transparent">
      <TableCell colSpan={colSpan} className="h-32 text-center text-sm text-muted-foreground">
        {loading ? (
          <span className="inline-flex items-center gap-2">
            <Spinner />
            불러오는 중
          </span>
        ) : (
          children
        )}
      </TableCell>
    </TableRow>
  )
}
