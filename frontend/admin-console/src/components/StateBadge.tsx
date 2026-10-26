import type { ReactNode } from 'react'
import { Badge } from '@therecommerce-develop/ds-ui'
import { STATE_LABELS, STATE_TONE, type StateTone } from '../utils/bidState'

// 관리자 테마의 secondary 가 파랑이라 회색은 클래스로 덮는다
const GRAY = 'bg-gray-100 text-gray-700'

export function ToneBadge({ tone, size, title, className = '', children }: {
  tone: StateTone
  size?: 'xs' | 'sm' | 'md' | 'lg'
  title?: string
  className?: string
  children: ReactNode
}) {
  if (tone === 'gray') {
    return (
      <Badge variant="secondary" appearance="light" size={size} title={title} className={`whitespace-nowrap ${GRAY} ${className}`}>
        {children}
      </Badge>
    )
  }
  return (
    <Badge variant={tone} appearance="light" size={size} title={title} className={`whitespace-nowrap ${className}`}>
      {children}
    </Badge>
  )
}

export default function StateBadge({ state, label }: { state: string; label?: string }) {
  return <ToneBadge tone={STATE_TONE[state] ?? 'gray'}>{label ?? STATE_LABELS[state] ?? state}</ToneBadge>
}
