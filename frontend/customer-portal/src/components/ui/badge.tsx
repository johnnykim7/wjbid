import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../../lib/utils'

const badgeVariants = cva(
  'inline-flex items-center font-semibold rounded-full',
  {
    variants: {
      variant: {
        active: 'bg-green-100 text-green-800',
        closing: 'bg-yellow-100 text-yellow-800',
        closed: 'bg-gray-100 text-gray-600',
        pending: 'bg-blue-100 text-blue-800',
        secondary: 'bg-gray-100 text-gray-700',
        info: 'bg-sky-100 text-sky-800',
        draft: 'bg-green-100 text-green-800',
        review: 'bg-yellow-100 text-yellow-800',
        final: 'bg-blue-100 text-blue-800',
      },
      size: {
        sm: 'px-2 py-0.5 text-xs leading-5',
        md: 'px-2.5 py-1 text-xs leading-5',
      },
    },
    defaultVariants: {
      variant: 'active',
      size: 'md',
    },
  }
)

interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, size, ...props }: BadgeProps) {
  return (
    <span
      data-slot="badge"
      className={cn(badgeVariants({ variant, size }), className)}
      {...props}
    />
  )
}
