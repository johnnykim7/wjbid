import { useState } from 'react'
import { Button } from '@therecommerce-develop/ds-ui'
import { runAwardBackfill, triggerCollection, type AwardBackfillResult } from '../api/client'
import DateField from '../components/DateField'

const BACKFILL_TEXT = {
  title: '지난 낙찰 공지 다시 받기',
  desc: '기간 안 낙찰 공지를 SAM.gov 에서 다시 받아 업체 이름, UEI, 금액을 채웁니다. 없는 공지는 새로 넣습니다.',
  from: '공지일 시작',
  to: '공지일 끝',
  pick: '날짜 고르기',
  limit: '기간은 1년 안. 수집 키워드마다 SAM 검색 1회(1000건이 넘으면 더)를 씁니다.',
  confirm: (from: string, to: string) => `${from} ~ ${to} 낙찰 공지를 다시 받습니다. SAM 검색 1회를 씁니다(1000건이 넘으면 더). 실행할까요?`,
  run: '실행',
  running: '받는 중',
  needDates: '시작과 끝 날짜를 고르세요.',
  fail: '다시 받지 못했습니다.',
  result: ['SAM 호출', '받은 낙찰 공지', '업체 채운 행', '새로 넣은 행', '업체 이름 없는 행'],
}

export default function CollectionPage() {
  const [daysBack, setDaysBack] = useState(30)
  const [triggering, setTriggering] = useState(false)
  const [result, setResult] = useState<{ status: string; message: string; note?: string } | null>(null)
  const [error, setError] = useState('')

  const [backfillFrom, setBackfillFrom] = useState<string | null>(null)
  const [backfillTo, setBackfillTo] = useState<string | null>(null)
  const [backfilling, setBackfilling] = useState(false)
  const [backfillResult, setBackfillResult] = useState<AwardBackfillResult | null>(null)
  const [backfillError, setBackfillError] = useState('')

  const handleBackfill = async () => {
    if (!backfillFrom || !backfillTo) {
      setBackfillError(BACKFILL_TEXT.needDates)
      return
    }
    if (!confirm(BACKFILL_TEXT.confirm(backfillFrom, backfillTo))) return
    setBackfilling(true)
    setBackfillResult(null)
    setBackfillError('')
    try {
      const { data } = await runAwardBackfill(backfillFrom, backfillTo)
      setBackfillResult(data)
    } catch (e) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setBackfillError(msg ?? BACKFILL_TEXT.fail)
    } finally {
      setBackfilling(false)
    }
  }

  const handleTrigger = async () => {
    if (!confirm(`최근 ${daysBack}일치 SAM.gov 공고를 수집하시겠습니까?`)) return
    setTriggering(true)
    setResult(null)
    setError('')
    try {
      const { data } = await triggerCollection(daysBack)
      setResult(data)
    } catch {
      setError('수집 트리거에 실패했습니다. 서버 상태를 확인하세요.')
    } finally {
      setTriggering(false)
    }
  }

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-xl font-bold text-gray-900">SAM.gov 수집</h1>
        <p className="text-sm text-gray-500 mt-0.5">SAM.gov 입찰 공고를 수동으로 수집합니다.</p>
      </div>

      {/* 수집 트리거 카드 */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 max-w-lg">
        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
            <i className="fa-solid fa-cloud-arrow-down text-blue-600" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-gray-900">수동 수집 트리거</h2>
            <p className="text-xs text-gray-500">백그라운드에서 비동기로 실행됩니다.</p>
          </div>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              수집 기간 (최근 N일)
            </label>
            <div className="flex items-center gap-3">
              <input
                type="number"
                min={1}
                max={365}
                value={daysBack}
                onChange={e => setDaysBack(Number(e.target.value))}
                className="w-28 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-secondary"
              />
              <span className="text-sm text-gray-500">일 전까지의 공고 수집</span>
            </div>
            <p className="mt-1 text-xs text-gray-400">SAM.gov API 일일 쿼터에 주의하세요.</p>
          </div>

          <button
            onClick={handleTrigger}
            disabled={triggering}
            className="flex items-center gap-2 px-5 py-2 bg-secondary text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition disabled:opacity-50"
          >
            {triggering ? (
              <><i className="fa-solid fa-circle-notch fa-spin" /> 처리 중...</>
            ) : (
              <><i className="fa-solid fa-play" /> 수집 시작</>
            )}
          </button>
        </div>
      </div>

      {/* 결과 */}
      {result && (
        <div className="max-w-lg bg-emerald-50 border border-emerald-200 rounded-xl p-5 space-y-2">
          <div className="flex items-center gap-2 text-emerald-700 font-medium text-sm">
            <i className="fa-solid fa-circle-check" />
            수집 요청이 수락되었습니다.
          </div>
          <p className="text-sm text-emerald-700">{result.message}</p>
          {result.note && (
            <p className="text-xs text-emerald-600 opacity-80">{result.note}</p>
          )}
        </div>
      )}

      {error && (
        <div className="max-w-lg bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-600">
          <i className="fa-solid fa-triangle-exclamation mr-2" />
          {error}
        </div>
      )}

      {/* CR-136: 지난 낙찰 공지 다시 받기 */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 max-w-lg space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
            <i className="fa-solid fa-trophy text-blue-600" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-gray-900">{BACKFILL_TEXT.title}</h2>
            <p className="text-xs text-gray-500">{BACKFILL_TEXT.desc}</p>
          </div>
        </div>
        <div className="flex items-end gap-3 flex-wrap">
          <div className="space-y-1">
            <div className="text-sm font-medium text-gray-700">{BACKFILL_TEXT.from}</div>
            <DateField value={backfillFrom} onChange={setBackfillFrom} placeholder={BACKFILL_TEXT.pick} label={BACKFILL_TEXT.from} disabled={backfilling} />
          </div>
          <div className="space-y-1">
            <div className="text-sm font-medium text-gray-700">{BACKFILL_TEXT.to}</div>
            <DateField value={backfillTo} onChange={setBackfillTo} placeholder={BACKFILL_TEXT.pick} label={BACKFILL_TEXT.to} disabled={backfilling} />
          </div>
          <Button variant="primary" onClick={handleBackfill} disabled={backfilling}>
            {backfilling
              ? <><i className="fa-solid fa-circle-notch fa-spin" /> {BACKFILL_TEXT.running}</>
              : <><i className="fa-solid fa-play" /> {BACKFILL_TEXT.run}</>}
          </Button>
        </div>
        <p className="text-xs text-gray-400">{BACKFILL_TEXT.limit}</p>
        {backfillResult && (
          <dl className="grid grid-cols-5 gap-2 bg-emerald-50 border border-emerald-200 rounded-lg p-3 text-center">
            {[backfillResult.samCalls, backfillResult.fetched, backfillResult.filled, backfillResult.created, backfillResult.withoutAwardee]
              .map((v, i) => (
                <div key={BACKFILL_TEXT.result[i]}>
                  <dt className="text-[11px] text-emerald-700">{BACKFILL_TEXT.result[i]}</dt>
                  <dd className="text-lg font-bold text-emerald-800">{v}</dd>
                </div>
              ))}
          </dl>
        )}
        {backfillError && (
          <p className="text-sm text-red-600"><i className="fa-solid fa-triangle-exclamation mr-2" />{backfillError}</p>
        )}
      </div>

      {/* 안내 */}
      <div className="max-w-lg bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-1.5">
        <div className="text-sm font-medium text-amber-800">
          <i className="fa-solid fa-circle-info mr-2" />
          수집 안내
        </div>
        <ul className="text-xs text-amber-700 space-y-1 list-disc list-inside">
          <li>수집은 백그라운드에서 비동기로 실행되며, 완료까지 수 분이 소요될 수 있습니다.</li>
          <li>SAM.gov API는 일일 쿼터가 있으며, 초과 시 당일 UTC 자정까지 차단됩니다.</li>
          <li>수집 진행 상황은 서버 로그에서 확인할 수 있습니다.</li>
          <li>이미 수집된 공고는 중복 저장되지 않습니다.</li>
        </ul>
      </div>
    </div>
  )
}
