// CR-124 — 로컬 EmptyState 어댑터: 내부를 DS Empty 조립으로 위임. 기존 props(icon/title/description/action) 보존.
import {
  Empty as DsEmpty,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
  EmptyDescription,
  EmptyContent,
} from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

interface EmptyStateProps {
  icon?: string
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <DsEmpty className={cn('py-16', className)}>
      <EmptyHeader>
        {icon && (
          <EmptyMedia variant="icon">
            <i className={cn(icon, 'text-2xl text-ink-mute')} />
          </EmptyMedia>
        )}
        <EmptyTitle>{title}</EmptyTitle>
        {description && <EmptyDescription>{description}</EmptyDescription>}
      </EmptyHeader>
      {action && <EmptyContent>{action}</EmptyContent>}
    </DsEmpty>
  )
}
