import type { MouseEvent } from 'react'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@therecommerce-develop/ds-ui'

// 지금 쪽 앞뒤 둘씩과 처음, 끝 쪽만 번호로 보이고 사이는 줄임표로 둔다
function pageWindow(page: number, totalPages: number): (number | 'gap')[] {
  const keep = new Set([0, totalPages - 1])
  for (let i = page - 2; i <= page + 2; i++) if (i >= 0 && i < totalPages) keep.add(i)
  const sorted = [...keep].sort((a, b) => a - b)
  const out: (number | 'gap')[] = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) out.push('gap')
    out.push(p)
  })
  return out
}

/** 목록 카드 바닥의 쪽 넘김. page 는 0 부터 센다. 한 쪽뿐이면 그리지 않는다 */
export default function ListPagination({
  page,
  totalPages,
  onChange,
}: {
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null

  const go = (p: number) => (e: MouseEvent) => {
    e.preventDefault()
    if (p >= 0 && p < totalPages && p !== page) onChange(p)
  }

  return (
    <Pagination>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            href="#"
            onClick={go(page - 1)}
            aria-disabled={page === 0}
            className={page === 0 ? 'pointer-events-none opacity-40' : undefined}
          />
        </PaginationItem>
        {pageWindow(page, totalPages).map((p, i) =>
          p === 'gap' ? (
            <PaginationItem key={`gap-${i}`}>
              <PaginationEllipsis />
            </PaginationItem>
          ) : (
            <PaginationItem key={p}>
              <PaginationLink href="#" isActive={p === page} onClick={go(p)}>
                {p + 1}
              </PaginationLink>
            </PaginationItem>
          ),
        )}
        <PaginationItem>
          <PaginationNext
            href="#"
            onClick={go(page + 1)}
            aria-disabled={page >= totalPages - 1}
            className={page >= totalPages - 1 ? 'pointer-events-none opacity-40' : undefined}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  )
}
