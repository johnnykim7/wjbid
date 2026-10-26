import { useState } from 'react'
import { CircleCheck, CloudDownload, Info, Play, TriangleAlert, Trophy } from 'lucide-react'
import {
  Alert,
  AlertContent,
  AlertDescription,
  AlertIcon,
  AlertTitle,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  ConfirmDialog,
  FieldError,
  Input,
  Label,
  Spinner,
} from '@therecommerce-develop/ds-ui'
import { runAwardBackfill, triggerCollection, type AwardBackfillResult } from '../api/client'
import DateField from '../components/DateField'

const BACKFILL_TEXT = {
  title: '지난 낙찰 공지 다시 받기',
  desc: '기간 안 낙찰 공지를 SAM.gov 에서 다시 받아 업체 이름, UEI, 금액을 채웁니다. 없는 공지는 새로 넣습니다.',
  from: '공지일 시작',
  to: '공지일 끝',
  pick: '날짜 고르기',
  limit: '기간은 1년 안. 수집 키워드마다 SAM 검색 1회(1000건이 넘으면 더)를 씁니다.',
  confirm: (from: string, to: string) => `${from} ~ ${to} 낙찰 공지를 다시 받습니다. SAM 검색 1회를 씁니다(1000건이 넘으면 더).`,
  run: '실행',
  running: '받는 중',
  needDates: '시작과 끝 날짜를 고르세요.',
  fail: '다시 받지 못했습니다.',
  result: ['SAM 호출', '받은 낙찰 공지', '업체 채운 행', '새로 넣은 행', '업체 이름 없는 행'],
}

export default function CollectionPage() {
  const [daysBack, setDaysBack] = useState(30)
  const [triggerDays, setTriggerDays] = useState<number | null>(null)
  const [triggering, setTriggering] = useState(false)
  const [result, setResult] = useState<{ status: string; message: string; note?: string } | null>(null)
  const [error, setError] = useState('')

  const [backfillFrom, setBackfillFrom] = useState<string | null>(null)
  const [backfillTo, setBackfillTo] = useState<string | null>(null)
  const [backfillRange, setBackfillRange] = useState<{ from: string; to: string } | null>(null)
  const [datesError, setDatesError] = useState('')
  const [backfilling, setBackfilling] = useState(false)
  const [backfillResult, setBackfillResult] = useState<AwardBackfillResult | null>(null)
  const [backfillError, setBackfillError] = useState('')

  const askBackfill = () => {
    if (!backfillFrom || !backfillTo) {
      setDatesError(BACKFILL_TEXT.needDates)
      return
    }
    setDatesError('')
    setBackfillRange({ from: backfillFrom, to: backfillTo })
  }

  const handleBackfill = async (from: string, to: string) => {
    setBackfilling(true)
    setBackfillResult(null)
    setBackfillError('')
    try {
      const { data } = await runAwardBackfill(from, to)
      setBackfillResult(data)
    } catch (e) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setBackfillError(msg ?? BACKFILL_TEXT.fail)
    } finally {
      setBackfilling(false)
    }
  }

  const handleTrigger = async (days: number) => {
    setTriggering(true)
    setResult(null)
    setError('')
    try {
      const { data } = await triggerCollection(days)
      setResult(data)
    } catch {
      setError('수집 트리거에 실패했습니다. 서버 상태를 확인하세요.')
    } finally {
      setTriggering(false)
    }
  }

  const confirmRange = backfillRange ?? { from: backfillFrom ?? '', to: backfillTo ?? '' }

  return (
    <div className="space-y-4">
      {/* 수집 트리거 카드 */}
      <Card className="max-w-lg">
        <CardHeader className="py-3">
          <div className="flex items-center gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50">
              <CloudDownload className="size-4 text-blue-600" />
            </div>
            <div className="space-y-1">
              <CardTitle>수동 수집 트리거</CardTitle>
              <CardDescription>백그라운드에서 비동기로 실행됩니다.</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4 py-4">
          <div className="space-y-1.5">
            <Label htmlFor="collection-days-back">수집 기간 (최근 N일)</Label>
            <div className="flex items-center gap-3">
              <Input
                id="collection-days-back"
                type="number"
                min={1}
                max={365}
                value={daysBack}
                onChange={e => setDaysBack(Number(e.target.value))}
                className="w-28"
              />
              <span className="text-sm text-muted-foreground">일 전까지의 공고 수집</span>
            </div>
            <p className="text-xs text-muted-foreground">SAM.gov API 일일 쿼터에 주의하세요.</p>
          </div>

          <Button variant="primary" onClick={() => setTriggerDays(daysBack)} disabled={triggering}>
            {triggering ? <Spinner /> : <Play />}
            {triggering ? '처리 중...' : '수집 시작'}
          </Button>
        </CardContent>
      </Card>

      {/* 결과 */}
      {result && (
        <Alert variant="success" appearance="light" className="max-w-lg">
          <AlertIcon>
            <CircleCheck />
          </AlertIcon>
          <AlertContent>
            <AlertTitle>수집 요청이 수락되었습니다.</AlertTitle>
            <AlertDescription className="space-y-1">
              <div>{result.message}</div>
              {result.note && <div className="text-xs opacity-80">{result.note}</div>}
            </AlertDescription>
          </AlertContent>
        </Alert>
      )}

      {error && (
        <Alert variant="destructive" appearance="light" className="max-w-lg">
          <AlertIcon>
            <TriangleAlert />
          </AlertIcon>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* CR-136: 지난 낙찰 공지 다시 받기 */}
      <Card className="max-w-lg">
        <CardHeader className="py-3">
          <div className="flex items-center gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50">
              <Trophy className="size-4 text-blue-600" />
            </div>
            <div className="space-y-1">
              <CardTitle>{BACKFILL_TEXT.title}</CardTitle>
              <CardDescription>{BACKFILL_TEXT.desc}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4 py-4">
          <div className="space-y-1.5">
            <div className="flex flex-wrap items-end gap-3">
              <div className="flex flex-col gap-1.5">
                <Label>{BACKFILL_TEXT.from}</Label>
                <DateField
                  value={backfillFrom}
                  onChange={(v) => { setBackfillFrom(v); setDatesError('') }}
                  placeholder={BACKFILL_TEXT.pick}
                  label={BACKFILL_TEXT.from}
                  disabled={backfilling}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label>{BACKFILL_TEXT.to}</Label>
                <DateField
                  value={backfillTo}
                  onChange={(v) => { setBackfillTo(v); setDatesError('') }}
                  placeholder={BACKFILL_TEXT.pick}
                  label={BACKFILL_TEXT.to}
                  disabled={backfilling}
                />
              </div>
              <Button variant="primary" onClick={askBackfill} disabled={backfilling}>
                {backfilling ? <Spinner /> : <Play />}
                {backfilling ? BACKFILL_TEXT.running : BACKFILL_TEXT.run}
              </Button>
            </div>
            <FieldError>{datesError}</FieldError>
          </div>
          <p className="text-xs text-muted-foreground">{BACKFILL_TEXT.limit}</p>
          {backfillResult && (
            <Alert variant="success" appearance="light">
              <dl className="grid w-full grid-cols-5 gap-2 text-center">
                {[backfillResult.samCalls, backfillResult.fetched, backfillResult.filled, backfillResult.created, backfillResult.withoutAwardee]
                  .map((v, i) => (
                    <div key={BACKFILL_TEXT.result[i]}>
                      <dt className="text-[11px] text-muted-foreground">{BACKFILL_TEXT.result[i]}</dt>
                      <dd className="text-lg font-bold text-foreground">{v}</dd>
                    </div>
                  ))}
              </dl>
            </Alert>
          )}
          {backfillError && (
            <Alert variant="destructive" appearance="light">
              <AlertIcon>
                <TriangleAlert />
              </AlertIcon>
              <AlertDescription>{backfillError}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      {/* 안내 */}
      <Alert variant="warning" appearance="light" className="max-w-lg">
        <AlertIcon>
          <Info />
        </AlertIcon>
        <AlertContent>
          <AlertTitle>수집 안내</AlertTitle>
          <AlertDescription>
            <ul className="list-inside list-disc space-y-1 text-xs">
              <li>수집은 백그라운드에서 비동기로 실행되며, 완료까지 수 분이 소요될 수 있습니다.</li>
              <li>SAM.gov API는 일일 쿼터가 있으며, 초과 시 당일 UTC 자정까지 차단됩니다.</li>
              <li>수집 진행 상황은 서버 로그에서 확인할 수 있습니다.</li>
              <li>이미 수집된 공고는 중복 저장되지 않습니다.</li>
            </ul>
          </AlertDescription>
        </AlertContent>
      </Alert>

      {/* 수집과 다시 받기는 오래 걸려 창을 닫고 화면에서 진행과 결과를 보인다(T3-4 확인 창 1, 2) */}
      <ConfirmDialog
        open={triggerDays !== null}
        onOpenChange={(open) => { if (!open) setTriggerDays(null) }}
        title="공고 수집"
        description={`최근 ${triggerDays ?? daysBack}일치 SAM.gov 공고를 수집합니다.`}
        confirmLabel="수집 시작"
        onConfirm={() => {
          const days = triggerDays
          setTriggerDays(null)
          if (days !== null) handleTrigger(days)
        }}
      />

      <ConfirmDialog
        open={backfillRange !== null}
        onOpenChange={(open) => { if (!open) setBackfillRange(null) }}
        title={BACKFILL_TEXT.title}
        description={BACKFILL_TEXT.confirm(confirmRange.from, confirmRange.to)}
        confirmLabel={BACKFILL_TEXT.run}
        onConfirm={() => {
          const range = backfillRange
          setBackfillRange(null)
          if (range) handleBackfill(range.from, range.to)
        }}
      />
    </div>
  )
}
