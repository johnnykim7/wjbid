// CR-124 — 로컬 Table 어댑터: 내부를 DS Table로 위임. 기존 API(Table/TableHeader/TableBody/TableRow/TableHead/TableCell) 보존.
import {
  Table as DsTable,
  TableHeader as DsTableHeader,
  TableBody as DsTableBody,
  TableRow as DsTableRow,
  TableHead as DsTableHead,
  TableCell as DsTableCell,
} from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

export function Table({ className, ...props }: React.HTMLAttributes<HTMLTableElement>) {
  return <DsTable data-slot="table" className={cn('min-w-full', className)} {...props} />
}

export function TableHeader({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return <DsTableHeader className={className} {...props} />
}

export function TableBody({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return <DsTableBody className={className} {...props} />
}

export function TableRow({ className, ...props }: React.HTMLAttributes<HTMLTableRowElement>) {
  // DS 기본 구분선(border-border)이 진해 연한 톤으로 덮음
  return <DsTableRow className={cn('!border-line-soft', className)} {...props} />
}

export function TableHead({ className, ...props }: React.ThHTMLAttributes<HTMLTableCellElement>) {
  return <DsTableHead className={cn('!border-line-soft', className)} {...props} />
}

export function TableCell({ className, ...props }: React.TdHTMLAttributes<HTMLTableCellElement>) {
  return <DsTableCell className={className} {...props} />
}
