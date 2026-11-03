import { useEffect, useState } from 'react'
import { awardeeBaseApi } from '../../api/awardeeBase'
import { RECOMMENDED_BASE_COUNT } from './siteContent'

// 기지 목록은 화면을 옮겨도 바뀌지 않으므로 앱을 열 때 한 번만 부른다
let pending: Promise<string[]> | null = null

function load(): Promise<string[]> {
  pending ??= awardeeBaseApi
    .baseStats()
    .then((s) =>
      s.bases
        .filter((b) => b.opportunityCount > 0)
        .sort((a, b) => b.opportunityCount - a.opportunityCount)
        .slice(0, RECOMMENDED_BASE_COUNT)
        .map((b) => b.nameKo),
    )
    .catch(() => [])
  return pending
}

/** 머리말 추천 검색어: 공고가 많은 기지의 한글 이름 (CR-187). 못 불러오면 빈 목록 */
export function useRecommendedKeywords(): string[] {
  const [keywords, setKeywords] = useState<string[]>([])

  useEffect(() => {
    let alive = true
    load().then((k) => {
      if (alive) setKeywords(k)
    })
    return () => {
      alive = false
    }
  }, [])

  return keywords
}
