// CR-124 — 로컬 Badge 어댑터: 내부를 DS Badge로 위임. 기존 variant(active/closing/...) → DS variant 매핑.
import { Badge as DsBadge } from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

type LocalVariant =
  | 'active' | 'closing' | 'closed' | 'pending'
  | 'secondary' | 'info' | 'draft' | 'review' | 'final'
type LocalSize = 'sm' | 'md'

// 로컬 variant → DS Badge variant (appearance=light 톤 유지)
const VARIANT_MAP: Record<LocalVariant, string> = {
  active: 'success',
  closing: 'warning',
  closed: 'secondary',
  pending: 'info',
  secondary: 'secondary',
  info: 'info',
  draft: 'success',
  review: 'warning',
  final: 'primary',
}
const SIZE_MAP: Record<LocalSize, string> = { sm: 'sm', md: 'md' }

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: LocalVariant
  size?: LocalSize
}

export function Badge({ className, variant = 'active', size = 'md', ...props }: BadgeProps) {
  return (
    <DsBadge
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      variant={VARIANT_MAP[variant] as any}
      appearance="light"
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      size={SIZE_MAP[size] as any}
      data-slot="badge"
      className={cn(className)}
      {...props}
    />
  )
}
