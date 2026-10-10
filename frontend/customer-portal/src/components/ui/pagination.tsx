// CR-124 — 로컬 Pagination 어댑터: 내부를 DS Pagination 조립으로 위임. 기존 고수준 API(page/totalPages/onPageChange) 보존.
import {
  Pagination as DsPagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationPrevious,
  PaginationNext,
} from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

interface PaginationProps {
  page: number
  totalPages: number
  totalElements?: number
  pageSize?: number
  onPageChange: (page: number) => void
  className?: string
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  pageSize = 20,
  onPageChange,
  className,
}: PaginationProps) {
  const from = page * pageSize + 1
  const to = Math.min((page + 1) * pageSize, totalElements ?? 0)
  const pages = Array.from({ length: Math.min(totalPages, 5) }, (_, i) => i)

  return (
    <div className={cn('bg-white px-4 py-3 border-t border-line flex items-center justify-between', className)}>
      {totalElements != null && (
        <p className="text-sm text-ink-sub hidden sm:block">
          전체 <span className="font-medium">{totalElements.toLocaleString()}</span>건 중{' '}
          <span className="font-medium">{from}</span>–<span className="font-medium">{to}</span>건 표시
        </p>
      )}
      <DsPagination className="ml-auto w-auto mx-0">
        <PaginationContent>
          <PaginationItem>
            <PaginationPrevious
              href="#"
              aria-disabled={page === 0}
              className={page === 0 ? 'pointer-events-none opacity-40' : ''}
              onClick={(e) => {
                e.preventDefault()
                if (page > 0) onPageChange(page - 1)
              }}
            />
          </PaginationItem>
          {pages.map((p) => (
            <PaginationItem key={p}>
              <PaginationLink
                href="#"
                isActive={p === page}
                onClick={(e) => {
                  e.preventDefault()
                  onPageChange(p)
                }}
              >
                {p + 1}
              </PaginationLink>
            </PaginationItem>
          ))}
          <PaginationItem>
            <PaginationNext
              href="#"
              aria-disabled={page >= totalPages - 1}
              className={page >= totalPages - 1 ? 'pointer-events-none opacity-40' : ''}
              onClick={(e) => {
                e.preventDefault()
                if (page < totalPages - 1) onPageChange(page + 1)
              }}
            />
          </PaginationItem>
        </PaginationContent>
      </DsPagination>
    </div>
  )
}
