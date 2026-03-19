import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../../lib/utils'

const buttonVariants = cva(
  'inline-flex items-center justify-center font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        primary: 'bg-secondary text-white hover:bg-blue-600 shadow-sm',
        brandBlue: 'bg-brandBlue text-white hover:bg-blue-800 shadow-sm',
        accent: 'bg-accent text-white hover:bg-emerald-600 shadow-sm',
        outline: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50',
        ghost: 'text-gray-500 hover:text-gray-700 hover:bg-gray-100',
        dark: 'bg-primary text-white hover:bg-slate-800',
        destructive: 'bg-red-500 text-white hover:bg-red-600',
      },
      size: {
        lg: 'h-11 px-6 rounded-lg text-sm',
        md: 'h-9 px-4 rounded-lg text-sm',
        sm: 'h-7 px-3 rounded-md text-xs',
        icon: 'h-9 w-9 rounded-lg',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
)

interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

export function Button({ className, variant, size, ...props }: ButtonProps) {
  return (
    <button
      data-slot="button"
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  )
}
