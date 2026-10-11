import { useEffect, useState } from 'react'
import { federalApi } from '../../api/federal'
import type { FederalSource } from '../../types/federal'

/** 미 연방 계약 화면 공통 판단과 불러오기 (CR-142) */

/** 한 번이라도 받았으면 참. 한 번도 없으면 화면은 "자료를 받고 있습니다" */
export const isSynced = (s: FederalSource | null | undefined) => !!s?.lastSyncedAt

/** 업종 고르기 단추에 보일 이름. 고른 업종의 이름을 한 번 찾아 둔다 */
export function useNaicsName(code: string | null) {
  const [found, setFound] = useState<{ code: string; name: string | null } | null>(null)
  useEffect(() => {
    if (!code) return
    let alive = true
    federalApi.naics(code)
      .then((r) => {
        if (alive) setFound({ code, name: r.items.find((o) => o.code === code)?.name ?? null })
      })
      .catch(() => { if (alive) setFound({ code, name: null }) })
    return () => {
      alive = false
    }
  }, [code])
  return found?.code === code ? found.name : null
}
