import { Outlet, useLocation } from 'react-router-dom'
import { AppHeader } from './AppHeader'
import { AppFooter } from './AppFooter'

/**
 * CR-124 — 공통 레이아웃 (상단 GNB형)
 * 본문에 공통 컨테이너(max-w-[1280px] + 좌우 여백)를 줘 헤더/푸터와 정렬.
 * 메인 랜딩(/)은 풀폭 히어로라 컨테이너 예외(풀블리드).
 */
export function AppLayout() {
  const { pathname } = useLocation()
  const fullBleed = pathname === '/'   // 홈 랜딩만 풀폭

  return (
    <div className="min-h-screen flex flex-col bg-[#f4f6f9]">
      <AppHeader />
      <main className="flex-1">
        {fullBleed ? (
          <Outlet />
        ) : (
          <div className="max-w-[1280px] mx-auto px-6 py-7">
            <Outlet />
          </div>
        )}
      </main>
      <AppFooter />
    </div>
  )
}
