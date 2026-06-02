import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { TopHeader } from './TopHeader'

const PAGE_TITLES: Record<string, string> = {
  '/search': '실시간 입찰 검색',
  '/proposals': '내 제안서 관리',
  '/bookmarks': '관심 공고',
  '/pricing': '요금제 및 결제',
  '/profile': '기업 프로필 설정',
}

function getTitle(pathname: string): string {
  if (pathname.startsWith('/search/')) return '입찰 공고 상세'
  return PAGE_TITLES[pathname] ?? '대시보드'
}

export function DashboardLayout() {
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const location = useLocation()
  const title = getTitle(location.pathname)

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Desktop sidebar */}
      <aside className="hidden lg:flex flex-col w-[var(--sidebar-width)] bg-sidebar text-white fixed inset-y-0 left-0 z-20 shadow-xl">
        <SidebarInner />
      </aside>

      {/* Mobile sidebar overlay */}
      {mobileSidebarOpen && (
        <>
          <div
            className="sidebar-overlay lg:hidden"
            onClick={() => setMobileSidebarOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 z-40 w-64 bg-sidebar text-white flex flex-col lg:hidden shadow-xl">
            <SidebarInner onItemClick={() => setMobileSidebarOpen(false)} />
          </aside>
        </>
      )}

      {/* Main content */}
      <div className="flex flex-col flex-1 min-w-0 lg:pl-[var(--sidebar-width)]">
        <TopHeader
          title={title}
          onMobileMenuClick={() => setMobileSidebarOpen(true)}
        />
        <main className="flex-1 overflow-y-auto overflow-x-hidden bg-surface">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

// Inline sidebar content to avoid circular imports
import { NavLink } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { cn } from '../../lib/utils'

const NAV_ITEMS = [
  { to: '/search', icon: 'fa-solid fa-magnifying-glass', label: 'SAM.gov 입찰 검색', badge: null },
  { to: '/proposals', icon: 'fa-solid fa-folder-open', label: '내 제안서 관리', badge: 3 },
  { to: '/bookmarks', icon: 'fa-regular fa-bookmark', label: '관심 공고', badge: 2 },
]

const SETTING_ITEMS = [
  { to: '/pricing', icon: 'fa-solid fa-credit-card', label: '요금제 및 결제' },
  { to: '/profile', icon: 'fa-solid fa-gear', label: '기업 프로필 설정' },
]

function SidebarInner({ onItemClick }: { onItemClick?: () => void }) {
  const { userEmail, logout } = useAuth()

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      'flex items-center px-3 py-2.5 rounded-lg font-medium transition-colors text-sm',
      isActive
        ? 'bg-secondary/20 text-secondary'
        : 'text-white/70 hover:bg-white/10 hover:text-white'
    )

  return (
    <div className="flex flex-col h-full">
      {/* Logo */}
      <div className="h-16 flex items-center px-6 border-b border-slate-700 flex-shrink-0" style={{ background: '#0f172a' }}>
        <i className="fa-solid fa-file-signature text-2xl mr-2" style={{ color: '#3b82f6' }} />
        <span className="font-extrabold text-xl tracking-wide text-white">
          WJbid<span className="text-slate-400 text-sm font-medium">.com</span>
        </span>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3 mt-2">
          Main Menu
        </p>
        <nav className="space-y-1">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={navLinkClass}
              onClick={onItemClick}
            >
              <i className={cn(item.icon, 'w-6 text-center')} />
              <span className="ml-2">{item.label}</span>
              {item.badge && (
                <span className="ml-auto text-white text-xs font-bold py-0.5 px-2 rounded-full" style={{ background: '#3b82f6' }}>
                  {item.badge}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3 mt-8">
          Settings
        </p>
        <nav className="space-y-1">
          {SETTING_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={navLinkClass}
              onClick={onItemClick}
            >
              <i className={cn(item.icon, 'w-6 text-center')} />
              <span className="ml-2">{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      {/* User Profile */}
      <div className="p-4 border-t border-slate-700 flex-shrink-0" style={{ background: 'rgba(15,23,42,0.5)' }}>
        <div className="flex items-center">
          <img
            src={`https://ui-avatars.com/api/?name=${encodeURIComponent(userEmail ?? 'U')}&background=3b82f6&color=fff`}
            alt="User"
            className="h-9 w-9 rounded-full border-2 border-gray-600 flex-shrink-0"
          />
          <div className="ml-3 min-w-0 flex-1">
            <p className="text-sm font-medium text-white truncate">{userEmail ?? '사용자'}</p>
            <p className="text-xs text-green-400 flex items-center">
              <span className="w-2 h-2 rounded-full bg-green-400 mr-1 pulse-dot inline-block" />
              Pro Plan (5회 남음)
            </p>
          </div>
          <button
            onClick={logout}
            className="ml-2 text-gray-400 hover:text-white transition flex-shrink-0"
            title="로그아웃"
          >
            <i className="fa-solid fa-right-from-bracket text-sm" />
          </button>
        </div>
      </div>
    </div>
  )
}
