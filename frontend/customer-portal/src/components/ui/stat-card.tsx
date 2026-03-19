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
    <div
      data-slot="stat-card"
      className={cn(
        'rounded-xl border p-4 shadow-sm',
        dark
          ? 'bg-gradient-to-r from-primary to-slate-800 border-gray-700 text-white'
          : 'bg-white border-gray-200',
        className
      )}
    >
      {icon && (
        <div className={cn('w-9 h-9 rounded-lg flex items-center justify-center mb-3', iconBg ?? 'bg-gray-100')}>
          <i className={cn(icon, 'text-sm', iconColor ?? 'text-gray-600')} />
        </div>
      )}
      <div className={cn('text-sm mb-1', dark ? 'text-gray-300' : 'text-gray-500')}>{label}</div>
      <div className="flex items-end justify-between">
        <div className={cn('text-2xl font-bold', dark ? 'text-white' : 'text-gray-800')}>
          {value}
          {unit && (
            <span className={cn('text-sm font-normal ml-1', dark ? 'text-gray-400' : 'text-gray-500')}>
              {unit}
            </span>
          )}
        </div>
        {badge && <div>{badge}</div>}
        {action && <div>{action}</div>}
      </div>
    </div>
  )
}
