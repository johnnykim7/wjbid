/**
 * CR-124 — 로컬 Button 어댑터. 내부 구현을 DS(@therecommerce-develop/ds-ui) Button으로 위임.
 * 화면들은 기존 variant(primary/accent/dark/...)를 그대로 쓰고, 여기서 DS variant로 매핑한다.
 * → 화면 코드 변경 없이 전 버튼이 DS로 전환됨.
 */
import { Button as DsButton } from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

type LocalVariant =
  | 'primary' | 'brandBlue' | 'accent' | 'outline' | 'ghost' | 'dark' | 'destructive'
type LocalSize = 'lg' | 'md' | 'sm' | 'icon'

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: LocalVariant
  size?: LocalSize
}

// 로컬 variant → DS variant (+ 보존용 className)
const VARIANT_MAP: Record<LocalVariant, { ds: string; cls?: string }> = {
  primary:     { ds: 'primary' },
  brandBlue:   { ds: 'primary' },
  accent:      { ds: 'primary' },
  outline:     { ds: 'outline' },
  ghost:       { ds: 'ghost' },
  dark:        { ds: 'mono' },
  destructive: { ds: 'destructive' },
}
const SIZE_MAP: Record<LocalSize, string> = { lg: 'lg', md: 'md', sm: 'sm', icon: 'icon' }

export function Button({ className, variant = 'primary', size = 'md', ...props }: ButtonProps) {
  const m = VARIANT_MAP[variant]
  return (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <DsButton
      variant={m.ds as any}
      size={SIZE_MAP[size] as any}
      className={cn(m.cls, className)}
      {...props}
    />
  )
}
