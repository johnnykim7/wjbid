import { useCallback, useEffect, useState } from 'react'
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
} from '../api/client'
import type { Notification } from '../types'

const POLL_INTERVAL_MS = 60_000

/**
 * 고객 인앱 알림 훅 (CR-006).
 * 미읽음 카운트는 60초 주기 폴링. 패널을 열 때 목록을 조회한다.
 */
export function useNotifications(isAuthenticated: boolean) {
  const [items, setItems] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)

  const refreshCount = useCallback(async () => {
    if (!isAuthenticated) return
    try {
      const res = await getUnreadNotificationCount()
      setUnreadCount(res.data.unreadCount ?? 0)
    } catch {
      /* 카운트 실패는 무시 (벨 배지만 영향) */
    }
  }, [isAuthenticated])

  const loadList = useCallback(async () => {
    if (!isAuthenticated) return
    setLoading(true)
    try {
      const res = await getNotifications(0, 20)
      setItems(res.data.content ?? [])
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated])

  const markRead = useCallback(async (id: string) => {
    await markNotificationAsRead(id)
    setItems((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n)),
    )
    setUnreadCount((c) => Math.max(0, c - 1))
  }, [])

  useEffect(() => {
    if (!isAuthenticated) {
      setItems([])
      setUnreadCount(0)
      return
    }
    refreshCount()
    const timer = setInterval(refreshCount, POLL_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [isAuthenticated, refreshCount])

  return { items, unreadCount, loading, loadList, markRead, refreshCount }
}
