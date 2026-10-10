import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { AppHeader } from './AppHeader'
import { AppFooter } from './AppFooter'
import { useMemberSummary } from './useMemberSummary'
import '../../wjbid-aida.css'

/**
 * 공통 틀 (CR-127). 로그인·회원가입을 뺀 모든 고객 화면에 머리말과 꼬리말을 씌운다.
 * 보통 화면은 1200px 가운데 정렬 상자 안에 그리고, 홈은 띠가 화면 끝까지 차야 해서 상자 없이(contained=false) 그린다.
 * 회원 숫자는 머리말과 홈이 같이 쓰므로 여기서 한 번 읽어 Outlet context 로 넘긴다.
 */
export function AppLayout({ contained = true }: { contained?: boolean }) {
  const { pathname, hash } = useLocation()
  const member = useMemberSummary()

  // 라우트 전환 시 최상단으로 (해시 스크롤 이동 중이면 제외)
  useEffect(() => {
    if (!hash) window.scrollTo(0, 0)
  }, [pathname, hash])

  return (
    <div className={`min-h-screen flex flex-col ${contained ? 'bg-surface' : 'bg-white'}`}>
      <AppHeader member={member} />
      <main className="flex-1">
        {contained ? (
          <div className="max-w-[1200px] mx-auto py-7">
            <Outlet context={member} />
          </div>
        ) : (
          <Outlet context={member} />
        )}
      </main>
      <AppFooter />
    </div>
  )
}
