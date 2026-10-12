import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyProfile } from '../api/client'

/** 회원 구분 (CR-157). 내 정보를 받기 전에는 loading */
export type MembershipState = 'loading' | 'member' | 'free'

interface AuthContextType {
  isAuthenticated: boolean
  userEmail: string | null
  membership: MembershipState
  /** 지금 회원 기간의 끝일(YYYY-MM-DD). 관리자와 무료회원은 null */
  membershipEndsOn: string | null
  signIn: (email: string, accessToken: string, refreshToken: string) => void
  logout: () => void
}

type Loaded = { token: string; member: boolean; endsOn: string | null }

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem('accessToken'))
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (!accessToken) return
    let alive = true
    getMyProfile()
      .then((res) => {
        if (alive) setLoaded({ token: accessToken, member: !!res.data?.membership?.member, endsOn: res.data?.membership?.endsOn ?? null })
      })
      // 회원 구분을 모르면 무료회원처럼 그린다. 서버가 다시 막으므로 공고문 내용은 나가지 않는다
      .catch(() => {
        if (alive) setLoaded({ token: accessToken, member: false, endsOn: null })
      })
    return () => { alive = false }
  }, [accessToken])

  // 다른 토큰으로 받은 값은 쓰지 않는다. 로그인하면 새로 받을 때까지 loading 이다
  const current = accessToken && loaded?.token === accessToken ? loaded : null
  const membership: MembershipState = !accessToken ? 'free' : !current ? 'loading' : current.member ? 'member' : 'free'

  const signIn = useCallback((email: string, access: string, refresh: string) => {
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
    localStorage.setItem('userEmail', email)
    setAccessToken(access)
  }, [])

  const logout = useCallback(() => {
    localStorage.clear()
    setAccessToken(null)
    navigate('/login', { replace: true })
  }, [navigate])

  return (
    <AuthContext.Provider value={{
      isAuthenticated: !!accessToken,
      userEmail: localStorage.getItem('userEmail'),
      membership,
      membershipEndsOn: current?.endsOn ?? null,
      signIn,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
