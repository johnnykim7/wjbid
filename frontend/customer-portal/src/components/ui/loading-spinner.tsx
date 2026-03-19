import { cn } from '../../lib/utils'

interface LoadingSpinnerProps {
  className?: string
  fullPage?: boolean
}

export function LoadingSpinner({ className, fullPage }: LoadingSpinnerProps) {
  if (fullPage) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-64">
        <div className={cn('loader', className)} />
      </div>
    )
  }
  return <div className={cn('loader', className)} />
}
