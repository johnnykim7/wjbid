import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import MemberAdminPage from './pages/MemberAdminPage'
import BidRequestAdminPage from './pages/BidRequestAdminPage'
import CollectionPage from './pages/CollectionPage'
import DocumentTemplatePage from './pages/DocumentTemplatePage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('adminToken')
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="members" element={<MemberAdminPage />} />
          <Route path="bid-requests" element={<BidRequestAdminPage />} />
          <Route path="collection" element={<CollectionPage />} />
          <Route path="templates" element={<DocumentTemplatePage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
