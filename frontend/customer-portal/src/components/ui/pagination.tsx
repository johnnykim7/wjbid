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
    <div className={cn('bg-white px-4 py-3 border-t border-gray-200 flex items-center justify-between', className)}>
      {totalElements != null && (
        <p className="text-sm text-gray-700 hidden sm:block">
          Showing <span className="font-medium">{from}</span> to{' '}
          <span className="font-medium">{to}</span> of{' '}
          <span className="font-medium">{totalElements.toLocaleString()}</span> results
        </p>
      )}
      <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px ml-auto">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <i className="fa-solid fa-chevron-left text-xs" />
        </button>
        {pages.map((p) => (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            className={cn(
              'relative inline-flex items-center px-4 py-2 border text-sm font-medium',
              p === page
                ? 'z-10 bg-secondary border-secondary text-white'
                : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
            )}
          >
            {p + 1}
          </button>
        ))}
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <i className="fa-solid fa-chevron-right text-xs" />
        </button>
      </nav>
    </div>
  )
}
