import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { getMyBidRequests, getMyBookmarks, getMyProfile } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import { useNotifications } from '../../hooks/useNotifications'

export interface MemberSummary {
  companyName: string | null
  bookmarkCount: number | null
  bidRequestCount: number | null
  unreadCount: number
}

/**
 * 머리말 아이콘 숫자와 홈 "오늘 확인할 것" 이 같은 값을 쓰도록 공통 틀에서 한 번 읽는다.
 * 관심 공고·입찰 신청은 다른 화면에서 바뀌므로 화면을 옮길 때마다 다시 읽는다.
 */
export function useMemberSummary(): MemberSummary {
  const { isAuthenticated, userEmail } = useAuth()
  const { pathname } = useLocation()
  const { unreadCount } = useNotifications(isAuthenticated)
  const [companyName, setCompanyName] = useState<string | null>(null)
  const [bookmarkCount, setBookmarkCount] = useState<number | null>(null)
  const [bidRequestCount, setBidRequestCount] = useState<number | null>(null)

  useEffect(() => {
    if (!isAuthenticated) {
      setCompanyName(null)
      return
    }
    let alive = true
    getMyProfile()
      .then(({ data }) => {
        if (alive) setCompanyName(data.companyName || userEmail)
      })
      .catch(() => {
        if (alive) setCompanyName(userEmail)
      })
    return () => {
      alive = false
    }
  }, [isAuthenticated, userEmail])

  useEffect(() => {
    if (!isAuthenticated) {
      setBookmarkCount(null)
      setBidRequestCount(null)
      return
    }
    let alive = true
    Promise.allSettled([getMyBookmarks(0), getMyBidRequests(0)]).then(([bookmarks, bids]) => {
      if (!alive) return
      setBookmarkCount(bookmarks.status === 'fulfilled' ? (bookmarks.value.data.totalElements ?? null) : null)
      setBidRequestCount(bids.status === 'fulfilled' ? (bids.value.data.totalElements ?? null) : null)
    })
    return () => {
      alive = false
    }
  }, [isAuthenticated, pathname])

  return { companyName, bookmarkCount, bidRequestCount, unreadCount }
}
