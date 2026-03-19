import { createContext, useContext, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

interface AuthContextType {
  isAuthenticated: boolean
  userEmail: string | null
  signIn: (email: string, accessToken: string, refreshToken: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem('accessToken'))
  const navigate = useNavigate()

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
