import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { AppHeader } from './AppHeader'
import { AppFooter } from './AppFooter'

/**
 * CR-124 — 공통 레이아웃 (상단 GNB형)
 * 본문에 공통 컨테이너(max-w-[1280px] + 좌우 여백)를 줘 헤더/푸터와 정렬.
 * CR-127 — 메인이 업무 화면(플랫)으로 바뀌어 풀블리드 예외 폐기. 전 페이지 동일 컨테이너.
 */
export function AppLayout() {
  const { pathname, hash } = useLocation()

  // 라우트 전환 시 최상단으로 (해시 스크롤 이동 중이면 제외)
  useEffect(() => {
    if (!hash) window.scrollTo(0, 0)
  }, [pathname, hash])

  return (
    <div className="min-h-screen flex flex-col bg-[#f4f6f9]">
      <AppHeader />
      <main className="flex-1">
        <div className="max-w-[1280px] mx-auto px-6 py-7">
          <Outlet />
        </div>
      </main>
      <AppFooter />
    </div>
  )
}
