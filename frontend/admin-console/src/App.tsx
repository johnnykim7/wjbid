import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import MemberAdminPage from './pages/MemberAdminPage'
import BidRequestAdminPage from './pages/BidRequestAdminPage'
import CollectionPage from './pages/CollectionPage'
import DocumentTemplatePage from './pages/DocumentTemplatePage'
import DocumentEditPage from './pages/DocumentEditPage'
import BidRequestDetailPage from './pages/BidRequestDetailPage'
import OpportunityAdminPage from './pages/OpportunityAdminPage'
import OpportunityAdminDetailPage from './pages/OpportunityAdminDetailPage'
import RfpSamplePage from './pages/RfpSamplePage'
import RfpSampleDetailPage from './pages/RfpSampleDetailPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('adminToken')
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter basename={import.meta.env.BASE_URL}>
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
          <Route path="bid-requests/:id" element={<BidRequestDetailPage />} />
          <Route path="opportunities" element={<OpportunityAdminPage />} />
          <Route path="opportunities/:id" element={<OpportunityAdminDetailPage />} />
          <Route path="collection" element={<CollectionPage />} />
          <Route path="templates" element={<DocumentTemplatePage />} />
          <Route path="rfp-samples" element={<RfpSamplePage />} />
          <Route path="rfp-samples/:id" element={<RfpSampleDetailPage />} />
          <Route path="documents/:id/edit" element={<DocumentEditPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
