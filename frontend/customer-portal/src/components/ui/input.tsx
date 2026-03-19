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
    <input
      data-slot="input"
      className={cn(
        'w-full bg-gray-50 border border-gray-200 rounded-lg py-2 text-sm text-gray-800',
        'focus:outline-none focus:ring-2 focus:ring-secondary focus:bg-white transition',
        hasIcon ? 'pl-10 pr-4' : 'px-4',
        className
      )}
      {...props}
    />
  )
}
