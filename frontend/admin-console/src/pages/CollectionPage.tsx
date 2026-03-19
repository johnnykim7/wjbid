import { useState } from 'react'
import { triggerCollection } from '../api/client'

export default function CollectionPage() {
  const [daysBack, setDaysBack] = useState(30)
  const [triggering, setTriggering] = useState(false)
  const [result, setResult] = useState<{ status: string; message: string; note?: string } | null>(null)
  const [error, setError] = useState('')

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
