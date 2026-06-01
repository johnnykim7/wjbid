import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { cn } from '../../lib/utils'

interface NavItem {
  to: string
  icon: string
  label: string
  badge?: string | number
}

const NAV_ITEMS: NavItem[] = [
  { to: '/search', icon: 'fa-solid fa-magnifying-glass', label: 'SAM.gov 입찰 검색', badge: undefined },
  { to: '/proposals', icon: 'fa-solid fa-folder-open', label: '내 제안서 관리', badge: 3 },
  { to: '/bookmarks', icon: 'fa-regular fa-bookmark', label: '관심 공고', badge: 2 },
]

const SETTING_ITEMS: NavItem[] = [
  { to: '/guide', icon: 'fa-solid fa-book-open', label: '이용 가이드' },
  { to: '/pricing', icon: 'fa-solid fa-credit-card', label: '요금제 및 결제' },
  { to: '/profile', icon: 'fa-solid fa-gear', label: '기업 프로필 설정' },
]

function SidebarContent({ onItemClick }: { onItemClick?: () => void }) {
  const { userEmail, logout } = useAuth()

  const handleLogout = () => {
    logout()
    if (onItemClick) onItemClick()
  }

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      'flex items-center px-3 py-2.5 rounded-lg font-medium transition-colors text-sm',
      isActive
        ? 'bg-secondary/10 text-secondary'
        : 'text-gray-300 hover:bg-gray-800 hover:text-white'
    )

  return (
    <div className="flex flex-col h-full">
      {/* Logo */}
      <div className="h-16 flex items-center px-6 border-b border-gray-700 bg-primary flex-shrink-0">
        <i className="fa-solid fa-file-signature text-secondary text-2xl mr-2" />
        <span className="font-extrabold text-xl tracking-wide text-white">
          WJbid<span className="text-gray-400 text-sm font-medium">.com</span>
        </span>
      </div>

      {/* Nav */}
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
                <span className="ml-auto bg-secondary text-white text-xs font-bold py-0.5 px-2 rounded-full">
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
      <div className="p-4 border-t border-gray-700 bg-primary/50 flex-shrink-0">
        <div className="flex items-center">
          <img
            src={`https://ui-avatars.com/api/?name=${encodeURIComponent(userEmail ?? 'U')}&background=3b82f6&color=fff`}
            alt="User"
            className="h-9 w-9 rounded-full border-2 border-gray-600"
          />
          <div className="ml-3 min-w-0 flex-1">
            <p className="text-sm font-medium text-white truncate">
              {userEmail ?? '사용자'}
            </p>
            <p className="text-xs text-green-400 flex items-center">
              <span className="w-2 h-2 rounded-full bg-green-400 mr-1 pulse-dot inline-block" />
              Pro Plan (제안서 5회 남음)
            </p>
          </div>
          <button
            onClick={handleLogout}
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

export function Sidebar() {
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <>
      {/* Mobile hamburger button - shown in TopHeader, but expose toggle */}
      <button
        id="mobile-sidebar-toggle"
        className="lg:hidden"
        onClick={() => setMobileOpen(true)}
        aria-label="사이드바 열기"
      />

      {/* Desktop Sidebar */}
      <aside className="hidden lg:flex flex-col w-[var(--sidebar-width)] bg-sidebar text-white fixed inset-y-0 left-0 z-20 shadow-xl">
        <SidebarContent />
      </aside>

      {/* Mobile Sidebar */}
      {mobileOpen && (
        <>
          <div
            className="sidebar-overlay lg:hidden"
            onClick={() => setMobileOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 z-40 w-64 bg-sidebar text-white flex flex-col lg:hidden shadow-xl">
            <SidebarContent onItemClick={() => setMobileOpen(false)} />
          </aside>
        </>
      )}
    </>
  )
}

export function MobileSidebarToggle({ onClick }: { onClick: () => void }) {
  return (
    <button
      className="lg:hidden text-gray-500 hover:text-gray-700 mr-4"
      onClick={onClick}
    >
      <i className="fa-solid fa-bars text-xl" />
    </button>
  )
}
