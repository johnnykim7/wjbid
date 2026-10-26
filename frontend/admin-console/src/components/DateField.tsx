import { useState } from 'react'
import { CalendarIcon } from 'lucide-react'
import { Button, Calendar, Popover, PopoverContent, PopoverTrigger } from '@therecommerce-develop/ds-ui'
import { toDate, toYmd } from '../utils/ymd'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

/** 날짜 고르기 칸 (CR-136). 고르면 바로 onChange 로 YYYY-MM-DD 를 넘긴다 */
export default function DateField({ value, onChange, placeholder, label, disabled }: {
  value: string | null
  onChange: (ymd: string) => void
  placeholder: string
  label: string
  disabled?: boolean
}) {
  const [open, setOpen] = useState(false)
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="outline" aria-label={label} disabled={disabled} className="min-w-36 justify-start">
          <CalendarIcon className="size-4 text-muted-foreground" />
          <span className={value ? '' : 'text-muted-foreground'}>{value ?? placeholder}</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-2" align="start">
        <Calendar
          mode="single"
          selected={value ? toDate(value) : undefined}
          defaultMonth={value ? toDate(value) : undefined}
          onSelect={(d) => {
            if (!d) return
            onChange(toYmd(d))
            setOpen(false)
          }}
          formatters={{
            formatCaption: (m: Date) => `${m.getFullYear()}년 ${m.getMonth() + 1}월`,
            formatWeekdayName: (d: Date) => WEEKDAYS[d.getDay()],
          }}
        />
      </PopoverContent>
    </Popover>
  )
}
