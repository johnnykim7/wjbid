// CR-124 — 로컬 Card 어댑터: 내부를 DS Card로 위임. 기존 API(Card/CardHeader/CardContent/CardFooter) 보존.
import {
  Card as DsCard,
  CardHeader as DsCardHeader,
  CardContent as DsCardContent,
  CardFooter as DsCardFooter,
} from '@therecommerce-develop/ds-ui'
import { cn } from '../../lib/utils'

export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <DsCard data-slot="card" className={className} {...props} />
}

export function CardHeader({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <DsCardHeader className={cn('px-6 py-4', className)} {...props} />
}

export function CardContent({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <DsCardContent className={cn('p-6', className)} {...props} />
}

export function CardFooter({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <DsCardFooter className={cn('px-6 py-4', className)} {...props} />
}
