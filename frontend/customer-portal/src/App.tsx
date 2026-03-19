import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { AppLayout } from './components/layout/AppLayout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import HomePage from './pages/HomePage'
import SearchPage from './pages/SearchPage'
import BidDetailPage from './pages/BidDetailPage'
import ProposalsPage from './pages/ProposalsPage'
import BookmarksPage from './pages/BookmarksPage'
import PricingPage from './pages/PricingPage'
import ProfilePage from './pages/ProfilePage'

function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <AppLayout />
}

function AppRoutes() {
  return (
    <Routes>
      {/* 공개 페이지 (GNB 포함) */}
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="/pricing" element={<PricingPage />} />
      </Route>

      {/* 로그인 / 회원가입 (GNB 없음) */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* 인증 필요 페이지 (GNB 포함) */}
      <Route element={<ProtectedRoute />}>
        <Route path="/search" element={<SearchPage />} />
        <Route path="/search/:id" element={<BidDetailPage />} />
        <Route path="/proposals" element={<ProposalsPage />} />
        <Route path="/bookmarks" element={<BookmarksPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
