import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { getNotifications, markNotificationAsRead } from '../api/client'
import type { Notification } from '../types'

function formatDateTime(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate(),
  ).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(
    d.getMinutes(),
  ).padStart(2, '0')}`
}

export default function NotificationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getNotifications(0, 50)
        setItems(res.data.content ?? [])
      } catch {
        setItems([])
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const handleClick = async (n: Notification) => {
    if (!n.read) {
      try {
        await markNotificationAsRead(n.id)
        setItems((prev) =>
          prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)),
        )
      } catch {
        /* 읽음 처리 실패해도 이동은 진행 */
      }
    }
    if (n.referenceType === 'Opportunity' && n.referenceId) {
      navigate(`/search/${n.referenceId}`)
    }
  }

  if (loading) return <LoadingSpinner fullPage />

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold text-ink mb-6">
        알림
        {items.length > 0 && (
          <span className="ml-2 text-sm font-normal text-ink-mute">{items.length}건</span>
        )}
      </h2>

      {items.length === 0 ? (
        <div className="bg-white rounded-xl border border-line p-16 text-center text-ink-mute shadow-sm">
          <i className="fa-regular fa-bell text-4xl mb-3 block" />
          <p className="font-medium">알림이 없습니다.</p>
          <p className="text-sm mt-1">새 입찰 공고가 등록되면 알림을 받아보실 수 있습니다.</p>
        </div>
      ) : (
        <div className="bg-white border border-line rounded-xl shadow-sm overflow-hidden divide-y divide-line-soft">
          {items.map((n) => {
            const clickable = n.referenceType === 'Opportunity' && !!n.referenceId
            return (
              <Button
                key={n.id}
                variant="ghost"
                onClick={() => handleClick(n)}
                className={`block w-full h-auto text-left px-5 py-4 rounded-none ${
                  clickable ? 'cursor-pointer' : 'cursor-default'
                } ${n.read ? 'opacity-60' : ''}`}
              >
                <div className="flex items-start gap-3">
                  {!n.read && (
                    <span className="mt-1.5 h-2 w-2 rounded-full bg-brand flex-shrink-0" />
                  )}
                  <div className={n.read ? 'pl-5' : ''}>
                    <p className="text-sm text-ink font-medium">{n.subject}</p>
                    <p className="text-xs text-ink-mute mt-1">{formatDateTime(n.sentAt)}</p>
                  </div>
                </div>
              </Button>
            )
          })}
        </div>
      )}
    </div>
  )
}
