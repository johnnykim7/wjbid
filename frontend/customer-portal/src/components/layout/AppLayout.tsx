import { Outlet } from 'react-router-dom'
import { AppHeader } from './AppHeader'
import { AppFooter } from './AppFooter'

/**
 * CR-124 — DS 기반 공통 레이아웃 (상단 GNB형)
 * AppHeader(Header+CategoryNav) + 본문 + AppFooter
 * 기존 GlobalNav는 보존(롤백/참고용), 본 레이아웃이 정본 셸.
 */
export function AppLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-[#f4f6f9]">
      <AppHeader />
      <main className="flex-1">
        <Outlet />
      </main>
      <AppFooter />
    </div>
  )
}
