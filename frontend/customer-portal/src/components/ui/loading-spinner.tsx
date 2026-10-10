// CR-124 — 로컬 LoadingSpinner 어댑터: 내부를 DS Spinner로 위임. 기존 props(className/fullPage) 보존.
import { Spinner as DsSpinner } from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

interface LoadingSpinnerProps {
  className?: string
  fullPage?: boolean
}

export function LoadingSpinner({ className, fullPage }: LoadingSpinnerProps) {
  const spinner = <DsSpinner className={cn('size-6 text-primary', className)} />
  if (fullPage) {
    return <div className="flex-1 flex items-center justify-center min-h-64">{spinner}</div>
  }
  return spinner
}
