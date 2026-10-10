import { useNavigate, useLocation } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'

/**
 * CR-124 — 샘플 mock(customer-portal-military-poc.html) GNB 그대로 재현.
 * 짙은 네이비 바 + 흰 텍스트 GNB + 우측 알림/프로필. DS Header는 톤이 안 맞아 미사용.
 * 컨셉 정본: docs/ui-mockup/customer-portal-military-poc.html
 */

// 비로그인 상단 메뉴
const PUBLIC_NAV = [
  { label: '서비스 소개', href: '/#about' },
  { label: '이용 안내', href: '/guide' },
  { label: '요금 안내', href: '/pricing' },
  { label: '공지사항', href: '/notices' },
  { label: '고객센터', href: '/support' },
]

// 로그인 후 GNB 대분류
const APP_NAV = [
  { id: 'search', label: '공고 검색', href: '/search' },
  { id: 'proposals', label: '내 제안서', href: '/proposals' },
  { id: 'documents', label: '서류함', href: '/documents' },
  { id: 'guide', label: '이용 안내', href: '/guide' },
  { id: 'pricing', label: '요금 안내', href: '/pricing' },
]

export function AppHeader() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, userEmail, logout } = useAuth()
  const [userMenuOpen, setUserMenuOpen] = useState(false)

  // 해시(/#about)면 홈 이동 후 섹션 스크롤, 아니면 일반 navigate
  const handleNav = (href: string) => {
    const [path, hash] = href.split('#')
    if (hash) {
      const scrollToHash = () =>
        document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth' })
      if (location.pathname === (path || '/')) {
        scrollToHash()
      } else {
        navigate(path || '/')
        setTimeout(scrollToHash, 100) // 페이지 마운트 후 스크롤
      }
    } else {
      navigate(href)
    }
  }

  // 현재 경로 기반 GNB active
  const activeId = (() => {
    const p = location.pathname
    if (p.startsWith('/search') || p === '/bookmarks' || p === '/calendar' || p === '/saved-searches') return 'search'
    if (p.startsWith('/proposals')) return 'proposals'
    if (p.startsWith('/documents')) return 'documents'
    if (p.startsWith('/guide')) return 'guide'
    if (p.startsWith('/pricing')) return 'pricing'
    return ''
  })()

  const navItems = isAuthenticated ? APP_NAV : PUBLIC_NAV.map((n, i) => ({ id: String(i), ...n }))
  const initials = (userEmail ?? 'WJ').slice(0, 2).toUpperCase()

  return (
    <header className="bg-[#1b3a72] text-white shrink-0 sticky top-0 z-50">
      <div className="max-w-[1600px] mx-auto px-6 lg:px-10 h-[60px] flex items-center">
        {/* 로고 */}
        <div className="flex items-center gap-2.5 mr-10 cursor-pointer"
             onClick={() => navigate('/')}>
          <div className="w-8 h-8 rounded bg-white/15 grid place-items-center font-bold">W</div>
          <span className="font-semibold tracking-tight text-[17px]">WJbid</span>
        </div>

        {/* GNB */}
        <nav className="flex items-center h-full text-[15px] font-medium">
          {navItems.map((item) => {
            const isActive = isAuthenticated && 'id' in item && item.id === activeId
            return (
              <a
                key={item.label}
                onClick={() => handleNav(item.href)}
                className={
                  'relative flex items-center h-full px-6 cursor-pointer hover:bg-white/5 ' +
                  (isActive
                    ? "text-white after:content-[''] after:absolute after:left-0 after:right-0 after:bottom-0 after:h-[3px] after:bg-white"
                    : 'text-white/90 hover:text-white')
                }
              >
                {item.label}
              </a>
            )
          })}
        </nav>

        {/* 우측 */}
        <div className="ml-auto flex items-center gap-4 text-sm">
          {isAuthenticated ? (
            <>
              <button onClick={() => navigate('/notifications')}
                className="relative text-white/80 hover:text-white">
                <i className="fa-solid fa-bell" />
                <span className="absolute -top-1 -right-1 w-2 h-2 bg-red-400 rounded-full" />
              </button>
              <div className="relative">
                <button onClick={() => setUserMenuOpen((v) => !v)}
                  className="flex items-center gap-2 text-white/90 hover:text-white">
                  <span className="w-7 h-7 rounded-full bg-white/15 grid place-items-center text-xs font-medium">{initials}</span>
                  <span className="hidden md:inline">{userEmail ?? '내 계정'}</span>
                  <i className="fa-solid fa-chevron-down text-[10px]" />
                </button>
                {userMenuOpen && (
                  <>
                    <div className="fixed inset-0 z-10" onClick={() => setUserMenuOpen(false)} />
                    <div className="absolute right-0 mt-2 w-44 bg-white rounded-lg shadow-lg border border-slate-200 py-1 z-20 text-slate-700">
                      <a onClick={() => { navigate('/profile'); setUserMenuOpen(false) }}
                        className="block px-4 py-2 text-sm hover:bg-slate-50 cursor-pointer">기업 프로필</a>
                      <a onClick={() => { navigate('/billing'); setUserMenuOpen(false) }}
                        className="block px-4 py-2 text-sm hover:bg-slate-50 cursor-pointer">결제 내역</a>
                      <div className="border-t border-slate-100 my-1" />
                      <a onClick={() => { logout(); navigate('/'); setUserMenuOpen(false) }}
                        className="block px-4 py-2 text-sm text-red-600 hover:bg-slate-50 cursor-pointer">로그아웃</a>
                    </div>
                  </>
                )}
              </div>
            </>
          ) : (
            <>
              <button onClick={() => navigate('/login')}
                className="text-white/90 hover:text-white">로그인</button>
              <button onClick={() => navigate('/register')}
                className="px-4 py-2 rounded bg-white text-[#1b3a72] font-medium hover:bg-slate-100">무료로 시작</button>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
