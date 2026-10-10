import type { ReactNode } from 'react'
import { Icon, type IconName } from '../layout/Icon'

/** 속페이지 화면 정본에만 있는 선 아이콘 (CR-133). 공통 틀 아이콘은 그대로 Icon 을 쓴다. */
const EXTRA: Record<string, ReactNode> = {
  home: <path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1Z" />,
  flag: <path d="M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 8 2a6 6 0 0 0 3.6-1.2.5.5 0 0 1 .4.4V14a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.5" />,
  globe: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18" /></>,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
  hour: <path d="M6 2h12M6 22h12M7 2v4l5 6-5 6v4M17 2v4l-5 6 5 6v4" />,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
}

export type SubIconName = IconName | 'home' | 'flag' | 'globe' | 'mail' | 'hour' | 'clock'

export function SubIcon({ name, className }: { name: SubIconName; className?: string }) {
  if (!(name in EXTRA)) return <Icon name={name as IconName} />
  return (
    <svg className={className ? `i ${className}` : 'i'} viewBox="0 0 24 24" aria-hidden="true">
      {EXTRA[name]}
    </svg>
  )
}
