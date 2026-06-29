import { useNavigate, useLocation } from 'react-router-dom'
import { Header, CategoryNav, type CategoryItem } from '@therecommerce-develop/ds-ui'
import { useAuth } from '../../context/AuthContext'

/**
 * CR-124 — DS 기반 공통 헤더 (상단 GNB형)
 * - Header: 로고 + (비로그인) navItems / (로그인) user 드롭다운
 * - CategoryNav(horizontal): 로그인 후 대분류 GNB + 하위메뉴
 * 컨셉: docs/ui-mockup/customer-portal-military-poc.html
 */

// 비로그인 상단 메뉴
const PUBLIC_NAV = [
  { label: '서비스 소개', href: '/#about' },
  { label: '요금 안내', href: '/pricing' },
  { label: '공지사항', href: '/notices' },
  { label: 'FAQ', href: '/faq' },
  { label: '고객센터', href: '/support' },
]

// 로그인 후 GNB 대분류 + 하위 (IA 정본)
const APP_CATEGORIES: CategoryItem[] = [
  {
    id: 'search', label: '공고 검색', href: '/search',
    children: [
      { id: 'search-all', label: '전체 공고 검색', href: '/search' },
      { id: 'deadline', label: '마감 임박', href: '/search?deadline' },
      { id: 'bookmarks', label: '관심 공고', href: '/bookmarks' },
      { id: 'calendar', label: '입찰 캘린더', href: '/calendar' },
      { id: 'saved', label: '저장한 검색', href: '/saved-searches' },
    ],
  },
  {
    id: 'proposals', label: '내 제안서', href: '/proposals',
    children: [
      { id: 'proposals-list', label: '제안서 목록', href: '/proposals' },
      { id: 'progress', label: '진행 현황', href: '/proposals?progress' },
    ],
  },
  { id: 'documents', label: '서류함', href: '/documents' },
  { id: 'notifications', label: '알림', href: '/notifications' },
]

export function AppHeader() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, userEmail, logout } = useAuth()

  // 현재 경로 기반 GNB active 판정
  const activeId = (() => {
    const p = location.pathname
    if (p.startsWith('/search') || p === '/bookmarks' || p === '/calendar' || p === '/saved-searches') return 'search'
    if (p.startsWith('/proposals')) return 'proposals'
    if (p.startsWith('/documents')) return 'documents'
    if (p.startsWith('/notifications')) return 'notifications'
    return undefined
  })()

  // CategoryItem에 onClick 주입 (DS는 href만으론 SPA 라우팅 안 함 → onClick으로 navigate)
  const categories: CategoryItem[] = APP_CATEGORIES.map((c) => ({
    ...c,
    onClick: () => c.href && navigate(c.href),
    children: c.children?.map((s) => ({ ...s, onClick: () => s.href && navigate(s.href) })),
  }))

  return (
    <header>
      <Header
        logo={<span className="text-lg font-bold text-[#13294f]">WJbid</span>}
        onLogoClick={() => navigate('/')}
        navItems={
          isAuthenticated
            ? undefined
            : PUBLIC_NAV.map((n) => ({
                label: n.label,
                onClick: () => navigate(n.href),
              }))
        }
        user={isAuthenticated ? { name: userEmail ?? '내 계정' } : undefined}
        userMenuItems={
          isAuthenticated
            ? [
                { label: '기업 프로필', onClick: () => navigate('/profile') },
                { label: '결제 내역', onClick: () => navigate('/billing') },
                { label: '로그아웃', onClick: () => { logout(); navigate('/') } },
              ]
            : undefined
        }
        onLoginClick={isAuthenticated ? undefined : () => navigate('/login')}
      />
      {isAuthenticated && (
        <CategoryNav variant="horizontal" categories={categories} activeCategoryId={activeId} />
      )}
    </header>
  )
}
