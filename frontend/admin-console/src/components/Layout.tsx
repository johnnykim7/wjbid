import { NavLink, Outlet, useNavigate } from 'react-router-dom'

const NAV = [
  { to: '/', icon: 'fa-solid fa-chart-line', label: '대시보드', exact: true },
  { to: '/members', icon: 'fa-solid fa-users', label: '회원 관리' },
  { to: '/bid-requests', icon: 'fa-solid fa-file-contract', label: '입찰 요청 관리' },
  { to: '/opportunities', icon: 'fa-solid fa-inbox', label: '원본 공고' },
  { to: '/notices', icon: 'fa-solid fa-bullhorn', label: '공고문 관리' },
  { to: '/collection', icon: 'fa-solid fa-satellite-dish', label: 'SAM.gov 수집' },
  { to: '/templates', icon: 'fa-solid fa-file-lines', label: '문서 템플릿' },
  { to: '/rfp-samples', icon: 'fa-solid fa-trophy', label: '성공 제안서 패턴' },
]

export default function Layout() {
  const navigate = useNavigate()

  const handleLogout = () => {
    localStorage.removeItem('adminToken')
    navigate('/login')
  }

  return (
    <div className="flex h-screen bg-gray-100 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-60 bg-sidebar flex-shrink-0 flex flex-col">
        <div className="px-6 py-5 border-b border-white/10">
          <div className="text-white font-bold text-lg">Bidding Agency</div>
          <div className="text-white/50 text-xs mt-0.5">Admin Console</div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
          {NAV.map(({ to, icon, label, exact }) => (
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
              <i className={`${icon} w-4 text-center`} />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="px-3 py-4 border-t border-white/10">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-white/70 hover:bg-white/10 hover:text-white transition-colors w-full"
          >
            <i className="fa-solid fa-right-from-bracket w-4 text-center" />
            로그아웃
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="h-14 bg-white border-b border-gray-200 flex items-center px-6 flex-shrink-0">
          <span className="text-sm text-gray-500">
            <i className="fa-solid fa-shield-halved mr-2 text-secondary" />
            Admin Console
          </span>
        </header>
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
