import { useEffect, useState } from 'react'
import { getPricing } from '../api/client'

/** 회원 월 금액이 미정이거나 조회에 실패했을 때 금액 자리에 쓰는 말 (T3-3 요금 안내) */
export const MEMBER_PRICE_PENDING_LABEL = '요금 안내 준비 중'

export const formatWon = (n: number) => `${n.toLocaleString('ko-KR')}원`

/**
 * 요금 조회 훅 (CR-129).
 * 요금 안내 화면과 홈 요금 구역이 같은 금액을 쓰도록 한 곳에서 꺼낸다.
 * memberMonthlyPrice 가 null 이면 금액 미정이다. 조회 실패도 미정과 같이 보인다.
 */
export function usePricing() {
  const [memberMonthlyPrice, setMemberMonthlyPrice] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    getPricing()
      .then((res) => {
        if (alive) setMemberMonthlyPrice(res.data.memberMonthlyPrice ?? null)
      })
      .catch(() => {
        /* 실패는 미정과 같이 보인다 */
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  return { memberMonthlyPrice, loading }
}
