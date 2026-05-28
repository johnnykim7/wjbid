import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useNotifications } from '../../hooks/useNotifications'
import { NotificationPanel } from '../NotificationPanel'

const PUBLIC_NAV = [
  { label: '서비스 소개', anchor: 'about' },
  { label: '입찰 프로세스', anchor: 'process' },
  { label: '가이드', path: '/guide' },
  { label: '요금제', path: '/pricing' },
  { label: '고객센터', anchor: 'contact' },
]

const APP_NAV = [
  { label: '입찰 검색', to: '/search' },
  { label: '내 제안서', to: '/proposals' },
  { label: '관심 공고', to: '/bookmarks' },
]

export function GlobalNav() {
  const { isAuthenticated, userEmail, logout } = useAuth()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [notifOpen, setNotifOpen] = useState(false)
  const navigate = useNavigate()
  const { items, unreadCount, loading, loadList, markRead } =
    useNotifications(isAuthenticated)

  const handlePublicLink = (item: (typeof PUBLIC_NAV)[number]) => {
    if (item.anchor) {
      const el = document.getElementById(item.anchor)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' })
      } else {
        navigate('/')
        setTimeout(() => document.getElementById(item.anchor!)?.scrollIntoView({ behavior: 'smooth' }), 120)
      }
    } else if (item.path) {
      navigate(item.path)
    }
    setMobileOpen(false)
  }

  return (
    <header className="sticky top-0 z-50 bg-white nav-shadow">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-6">

        {/* Logo */}
        <Link to="/" className="flex items-center gap-2.5 flex-shrink-0">
          <div
            className="w-8 h-8 rounded-md flex items-center justify-center flex-shrink-0"
            style={{ background: '#0c1e3d' }}
          >
            <i className="fa-solid fa-file-signature text-white text-sm" />
          </div>
          <span className="font-bold text-[17px] leading-none" style={{ color: '#0c1e3d' }}>
            WJbid<span className="font-normal text-sm" style={{ color: '#1a56db' }}>.com</span>
          </span>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center gap-0.5 flex-1">
          {isAuthenticated
            ? APP_NAV.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                      isActive
                        ? 'bg-blue-50 text-[#1a56db]'
                        : 'text-gray-600 hover:text-[#0c1e3d] hover:bg-gray-50'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))
            : PUBLIC_NAV.map((item) => (
                <button
                  key={item.label}
                  onClick={() => handlePublicLink(item)}
                  className="px-4 py-2 text-sm font-medium text-gray-600 hover:text-[#0c1e3d] hover:bg-gray-50 rounded-md transition-colors"
                >
                  {item.label}
                </button>
              ))}
        </nav>

        {/* Right actions */}
        <div className="flex items-center gap-2 flex-shrink-0">
          {isAuthenticated && (
            <div className="relative">
              <button
                onClick={() => setNotifOpen(!notifOpen)}
                className="relative p-2 text-gray-400 hover:text-gray-600 transition-colors"
                aria-label="알림"
              >
                <i className="fa-regular fa-bell text-xl" />
                {unreadCount > 0 && (
                  <span className="absolute top-0.5 right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                )}
              </button>
              {notifOpen && (
                <NotificationPanel
                  items={items}
                  loading={loading}
                  onClose={() => setNotifOpen(false)}
                  onLoad={loadList}
                  onMarkRead={markRead}
                />
              )}
            </div>
          )}

          {isAuthenticated ? (
            <div className="relative">
              <button
                onClick={() => setUserMenuOpen(!userMenuOpen)}
                className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-50 transition-colors text-sm"
              >
                <div
                  className="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
                  style={{ background: '#1a56db' }}
                >
                  {userEmail?.charAt(0).toUpperCase() ?? 'U'}
                </div>
                <span className="font-medium text-gray-700 hidden sm:block max-w-[140px] truncate">
                  {userEmail}
                </span>
                <i className="fa-solid fa-chevron-down text-[10px] text-gray-400" />
              </button>

              {userMenuOpen && (
                <>
                  <div className="fixed inset-0 z-10" onClick={() => setUserMenuOpen(false)} />
                  <div className="absolute right-0 mt-1 w-52 bg-white rounded-xl shadow-lg border border-gray-200 py-1.5 z-20">
                    <NavLink
                      to="/profile"
                      className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50"
                      onClick={() => setUserMenuOpen(false)}
                    >
                      <i className="fa-solid fa-gear w-4 text-gray-400 text-center" />
                      기업 프로필 설정
                    </NavLink>
                    <NavLink
                      to="/pricing"
                      className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50"
                      onClick={() => setUserMenuOpen(false)}
                    >
                      <i className="fa-solid fa-credit-card w-4 text-gray-400 text-center" />
                      요금제 관리
                    </NavLink>
                    <div className="my-1 h-px bg-gray-100" />
                    <button
                      onClick={() => { logout(); setUserMenuOpen(false) }}
                      className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 w-full text-left"
                    >
                      <i className="fa-solid fa-right-from-bracket w-4 text-center" />
                      로그아웃
                    </button>
                  </div>
                </>
              )}
            </div>
          ) : (
            <>
              <button
                onClick={() => navigate('/login')}
                className="hidden sm:block px-4 py-2 text-sm font-medium text-gray-600 hover:text-[#0c1e3d] rounded-md transition-colors"
              >
                로그인
              </button>
              <button
                onClick={() => navigate('/login')}
                className="px-4 py-2 text-sm font-semibold text-white rounded-lg transition-colors"
                style={{ background: '#0c1e3d' }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#162d57')}
                onMouseLeave={(e) => (e.currentTarget.style.background = '#0c1e3d')}
              >
                무료 시작
              </button>
            </>
          )}

          {/* Mobile toggle */}
          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="md:hidden p-2 rounded-md text-gray-500 hover:bg-gray-100 transition-colors"
          >
            <i className={mobileOpen ? 'fa-solid fa-xmark text-base' : 'fa-solid fa-bars text-base'} />
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileOpen && (
        <div className="md:hidden border-t border-gray-100 bg-white px-4 py-3 space-y-0.5">
          {isAuthenticated
            ? APP_NAV.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setMobileOpen(false)}
                  className={({ isActive }) =>
                    `block px-3 py-2.5 text-sm font-medium rounded-md ${
                      isActive ? 'bg-blue-50 text-[#1a56db]' : 'text-gray-700 hover:bg-gray-50'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))
            : PUBLIC_NAV.map((item) => (
                <button
                  key={item.label}
                  onClick={() => handlePublicLink(item)}
                  className="block w-full text-left px-3 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 rounded-md"
                >
                  {item.label}
                </button>
              ))}

          {!isAuthenticated && (
            <div className="pt-3 pb-1 border-t border-gray-100 mt-2 flex gap-2">
              <button
                onClick={() => { navigate('/login'); setMobileOpen(false) }}
                className="flex-1 py-2.5 text-sm font-medium border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
              >
                로그인
              </button>
              <button
                onClick={() => { navigate('/login'); setMobileOpen(false) }}
                className="flex-1 py-2.5 text-sm font-semibold text-white rounded-lg"
                style={{ background: '#0c1e3d' }}
              >
                무료 시작
              </button>
            </div>
          )}
        </div>
      )}
    </header>
  )
}
