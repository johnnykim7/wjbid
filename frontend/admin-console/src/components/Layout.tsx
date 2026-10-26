import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  ChartLine,
  FilePenLine,
  FileText,
  Headset,
  Images,
  Inbox,
  LogOut,
  MapPin,
  Megaphone,
  SatelliteDish,
  Trophy,
  Users,
  type LucideIcon,
} from 'lucide-react'
import { Button } from '@therecommerce-develop/ds-ui'
import { getAdminInquiryCounts } from '../api/client'
import { INQUIRY_COUNTS_CHANGED } from './inquiryCounts'

const NAV: { to: string; icon: LucideIcon; label: string; exact?: boolean }[] = [
  { to: '/', icon: ChartLine, label: '대시보드', exact: true },
  { to: '/members', icon: Users, label: '회원 관리' },
  { to: '/bid-requests', icon: FilePenLine, label: '입찰 요청 관리' },
  { to: '/opportunities', icon: Inbox, label: '원본 공고' },
  { to: '/notices', icon: Megaphone, label: '공고문 관리' },
  { to: '/collection', icon: SatelliteDish, label: 'SAM.gov 수집' },
  { to: '/bases', icon: MapPin, label: '기지 관리' },
  { to: '/site-photos', icon: Images, label: '고객 화면 사진' },
  { to: '/templates', icon: FileText, label: '문서 템플릿' },
  { to: '/rfp-samples', icon: Trophy, label: '성공 제안서 패턴' },
  { to: '/inquiries', icon: Headset, label: '1:1 문의' },
]

// 문서 편집과 제안서 Section 화면은 입찰 상세에서 들어가므로 입찰 요청 관리에 속한다
function currentMenuLabel(pathname: string) {
  if (pathname.startsWith('/documents/')) return '입찰 요청 관리'
  const menu = NAV.find(({ to, exact }) => (exact ? pathname === to : pathname === to || pathname.startsWith(`${to}/`)))
  return menu?.label ?? ''
}

export default function Layout() {
  const navigate = useNavigate()
  const location = useLocation()
  // CR-132: 1:1 문의 미답변 건수. 읽지 못하면 숫자를 감춘다
  const [unanswered, setUnanswered] = useState<number | null>(null)

  useEffect(() => {
    const refresh = () =>
      getAdminInquiryCounts()
        .then((res) => setUnanswered(res.data.received))
        .catch(() => setUnanswered(null))
    refresh()
    window.addEventListener(INQUIRY_COUNTS_CHANGED, refresh)
    return () => window.removeEventListener(INQUIRY_COUNTS_CHANGED, refresh)
  }, [location.pathname])

  const handleLogout = () => {
    localStorage.removeItem('adminToken')
    navigate('/login')
  }

  return (
    <div className="flex h-screen bg-muted overflow-hidden">
      <aside className="w-60 bg-sidebar flex-shrink-0 flex flex-col">
        <div className="px-6 py-5 border-b border-white/10">
          <div className="text-white font-bold text-lg">Bidding Agency</div>
          <div className="text-white/50 text-xs mt-0.5">Admin Console</div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
          {NAV.map(({ to, icon: Icon, label, exact }) => (
            <NavLink
              key={to}
              to={to}
              end={exact}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                  isActive
                    ? 'bg-secondary text-white font-medium'
                    : 'text-white/70 hover:bg-white/10 hover:text-white'
                }`
              }
            >
              <Icon className="size-4 shrink-0" />
              {label}
              {to === '/inquiries' && !!unanswered && (
                <span className="ml-auto bg-danger text-white text-[11px] font-bold rounded-full px-[7px] py-px">
                  {unanswered}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="px-3 py-4 border-t border-white/10">
          <Button
            variant="ghost"
            onClick={handleLogout}
            className="w-full justify-start gap-3 px-3 py-2.5 h-auto rounded-lg text-sm font-normal text-white/70 hover:bg-white/10 hover:text-white"
          >
            <LogOut className="size-4 shrink-0" />
            로그아웃
          </Button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="h-14 bg-white border-b border-border flex items-center px-6 flex-shrink-0">
          <h1 className="text-base font-semibold text-foreground">{currentMenuLabel(location.pathname)}</h1>
        </header>
        <main className="flex-1 overflow-y-auto">
          <div className="space-y-4 px-6 pt-3 pb-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
