// CR-124 — 로컬 Input 어댑터: 내부를 DS Input으로 위임. InputWrapper/hasIcon API 보존.
import { Input as DsInput } from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

interface InputWrapperProps extends React.HTMLAttributes<HTMLDivElement> {
  icon?: React.ReactNode
}

export function InputWrapper({ className, icon, children, ...props }: InputWrapperProps) {
  return (
    <div
      data-slot="input-wrapper"
      className={cn('relative flex items-center', className)}
      {...props}
    >
      {icon && (
        <span className="absolute left-3 text-gray-400 pointer-events-none">
          {icon}
        </span>
      )}
      {children}
    </div>
  )
}

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  hasIcon?: boolean
}

export function Input({ className, hasIcon, ...props }: InputProps) {
  return (
    <DsInput
      className={cn('w-full', hasIcon && 'pl-10', className)}
      {...props}
    />
  )
}
