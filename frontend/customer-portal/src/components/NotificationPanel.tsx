import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Notification } from '../types'

interface Props {
  items: Notification[]
  loading: boolean
  onClose: () => void
  onLoad: () => void
  onMarkRead: (id: string) => void
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const min = Math.floor(diff / 60_000)
  if (min < 1) return '방금 전'
  if (min < 60) return `${min}분 전`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr}시간 전`
  return `${Math.floor(hr / 24)}일 전`
}

/**
 * 알림 벨 드롭다운 패널 (CR-006).
 * 공고 관련 알림 클릭 시 해당 공고 상세로 이동하며 읽음 처리.
 */
export function NotificationPanel({ items, loading, onClose, onLoad, onMarkRead }: Props) {
  const navigate = useNavigate()

  useEffect(() => {
    onLoad()
  }, [onLoad])

  const handleClick = (n: Notification) => {
    if (!n.read) onMarkRead(n.id)
    if (n.referenceType === 'Opportunity' && n.referenceId) {
      navigate(`/search/${n.referenceId}`)
      onClose()
    }
  }

  return (
    <>
      <div className="fixed inset-0 z-10" onClick={onClose} />
      <div className="absolute right-0 mt-1 w-80 bg-white rounded-xl shadow-lg border border-gray-200 z-20 overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
          <span className="font-semibold text-sm text-gray-800">알림</span>
          <button
            onClick={() => { navigate('/notifications'); onClose() }}
            className="text-xs text-[#1a56db] hover:underline"
          >
            전체 보기
          </button>
        </div>

        <div className="max-h-96 overflow-y-auto">
          {loading ? (
            <div className="px-4 py-8 text-center text-sm text-gray-400">불러오는 중…</div>
          ) : items.length === 0 ? (
            <div className="px-4 py-8 text-center text-sm text-gray-400">알림이 없습니다.</div>
          ) : (
            items.map((n) => (
              <button
                key={n.id}
                onClick={() => handleClick(n)}
                className={`block w-full text-left px-4 py-3 border-b border-gray-50 hover:bg-gray-50 transition-colors ${
                  n.read ? 'opacity-60' : ''
                }`}
              >
                <div className="flex items-start gap-2">
                  {!n.read && (
                    <span className="mt-1.5 h-2 w-2 rounded-full bg-[#1a56db] flex-shrink-0" />
                  )}
                  <div className={n.read ? 'pl-4' : ''}>
                    <p className="text-sm text-gray-800 font-medium leading-snug">{n.subject}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{timeAgo(n.sentAt)}</p>
                  </div>
                </div>
              </button>
            ))
          )}
        </div>
      </div>
    </>
  )
}
