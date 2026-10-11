import { useEffect, useLayoutEffect, useRef, useState } from 'react'

/**
 * 메뉴 속 화면 2묶음의 불러오기 (CR-135). key 가 바뀔 때마다 다시 부르고, 다시 부르는 동안 앞 결과를 그대로 둬 표가 깜박이지 않게 한다.
 * 늦게 온 앞 요청의 결과는 버린다.
 */
export function useMarketLoad<T>(load: () => Promise<T>, key: string) {
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<{ forKey: string; data: T | null; error: boolean } | null>(null)
  const loadRef = useRef(load)
  useLayoutEffect(() => {
    loadRef.current = load
  })
  const requestKey = `${key}#${attempt}`

  useEffect(() => {
    let alive = true
    loadRef.current()
      .then((data) => {
        if (alive) setResult({ forKey: requestKey, data, error: false })
      })
      .catch(() => {
        if (alive) setResult((prev) => ({ forKey: requestKey, data: prev?.data ?? null, error: true }))
      })
    return () => {
      alive = false
    }
  }, [requestKey])

  const status: 'loading' | 'done' | 'error' = result?.forKey !== requestKey ? 'loading' : result.error ? 'error' : 'done'
  return { data: result?.data ?? null, status, reload: () => setAttempt((n) => n + 1) }
}
