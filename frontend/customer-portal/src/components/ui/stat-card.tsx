// CR-124 — 로컬 StatCard 어댑터: 컨테이너를 DS Card로 위임(DS에 1:1 StatCard 없음). 기존 props 전부 보존.
import { Card as DsCard } from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

interface StatCardProps {
  label: string
  value: string | number
  unit?: string
  icon?: string
  iconBg?: string
  iconColor?: string
  badge?: React.ReactNode
  action?: React.ReactNode
  dark?: boolean
  className?: string
}

export function StatCard({
  label,
  value,
  unit,
  icon,
  iconBg,
  iconColor,
  badge,
  action,
  dark,
  className,
}: StatCardProps) {
  return (
    <DsCard
      data-slot="stat-card"
      className={cn(
        'p-4',
        dark ? 'bg-brand-deep border-dark-line text-white' : '',
        className
      )}
    >
      {icon && (
        <div className={cn('w-9 h-9 rounded-lg flex items-center justify-center mb-3', iconBg ?? 'bg-surface')}>
          <i className={cn(icon, 'text-sm', iconColor ?? 'text-ink-sub')} />
        </div>
      )}
      <div className={cn('text-sm mb-1', dark ? 'text-ink-faint' : 'text-ink-mute')}>{label}</div>
      <div className="flex items-end justify-between">
        <div className={cn('text-2xl font-bold', dark ? 'text-white' : 'text-ink')}>
          {value}
          {unit && (
            <span className={cn('text-sm font-normal ml-1', dark ? 'text-ink-mute' : 'text-ink-mute')}>
              {unit}
            </span>
          )}
        </div>
        {badge && <div>{badge}</div>}
        {action && <div>{action}</div>}
      </div>
    </DsCard>
  )
}
