import { useState } from 'react'
import { Button, Calendar, Popover, PopoverContent, PopoverTrigger } from '@therecommerce-develop/ds-ui'
import { Icon } from '../layout/Icon'
import { DATE_PICK_TEXT as T } from '../../data/expiryContent'
import { toDate, toYmd } from '../../utils/kstDate'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

/**
 * 날짜 고르기 칸 (CR-136, T3-4 시나리오 3). 날짜를 고르고 저장을 눌러야 onSave 를 부른다.
 * value 가 없으면 "만료일 넣기" 로 보인다. 공통 틀(.wj) 밖의 창 안에서는 plain 으로 ds-ui 단추 모양을 쓴다.
 */
export function DatePick({ value, onSave, busy, label, plain, placeholder }: {
  value: string | null
  onSave: (ymd: string) => void
  busy?: boolean
  label?: string
  plain?: boolean
  /** 날짜가 없을 때 단추 글. 없으면 "만료일 넣기" */
  placeholder?: string
}) {
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState<Date | undefined>(value ? toDate(value) : undefined)

  const change = (o: boolean) => {
    if (o) setDraft(value ? toDate(value) : undefined)
    setOpen(o)
  }
  const save = () => {
    if (!draft) return
    onSave(toYmd(draft))
    setOpen(false)
  }

  return (
    <Popover open={open} onOpenChange={change}>
      <PopoverTrigger asChild>
        {plain ? (
          <Button variant="outline" disabled={busy} aria-label={label}>
            <Icon name="cal" />{value ?? placeholder ?? T.add}
          </Button>
        ) : (
          <Button className={`wj-reset dbtn${value ? '' : ' add'}${open ? ' act' : ''}`} disabled={busy} aria-label={label}>
            <Icon name="cal" />{value ?? placeholder ?? T.add}
          </Button>
        )}
      </PopoverTrigger>
      {/* 떠 있는 창은 공통 틀 밖(문서 끝)에 그려져 .wj 규칙이 닿지 않는다. 그래서 단추는 ds-ui 모양 그대로 쓴다 */}
      <PopoverContent className="wj-dpop" align="start">
        <Calendar
          mode="single"
          selected={draft}
          onSelect={setDraft}
          defaultMonth={draft}
          formatters={{
            formatCaption: (m: Date) => `${m.getFullYear()}년 ${m.getMonth() + 1}월`,
            formatWeekdayName: (d: Date) => WEEKDAYS[d.getDay()],
          }}
        />
        <div className="ft">
          <Button variant="outline" size="sm" onClick={() => setOpen(false)}>{T.close}</Button>
          <Button variant="primary" size="sm" disabled={!draft} onClick={save}>
            {draft ? T.saveWith(toYmd(draft)) : T.save}
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}
