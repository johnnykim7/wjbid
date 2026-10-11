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
import ProposalDetailPage from './pages/ProposalDetailPage'
import GuidePage from './pages/GuidePage'
import NoticesPage from './pages/NoticesPage'
import FaqPage from './pages/FaqPage'
import SupportPage from './pages/SupportPage'
import NotificationsPage from './pages/NotificationsPage'
import TermsPage from './pages/TermsPage'
import PrivacyPage from './pages/PrivacyPage'
import RefundPage from './pages/RefundPage'
import DisclaimerPage from './pages/DisclaimerPage'
import EligibilityPage from './pages/EligibilityPage'
import EligibilityDetailPage from './pages/EligibilityDetailPage'
import VerdictPage from './pages/VerdictPage'
import CalendarPage from './pages/CalendarPage'
import SavedSearchesPage from './pages/SavedSearchesPage'
import BillingPage from './pages/BillingPage'
import DocumentsPage from './pages/DocumentsPage'
import ComingSoonPage from './pages/ComingSoonPage'
import DocumentExpiryPage from './pages/DocumentExpiryPage'
import PreCheckPage from './pages/PreCheckPage'
import PreCheckDetailPage from './pages/PreCheckDetailPage'
import AwardeesPage from './pages/AwardeesPage'
import BaseMapPage from './pages/BaseMapPage'
import ServicePage from './pages/ServicePage'
import AcademyClassPage from './pages/AcademyClassPage'
import AcademyMaterialsPage from './pages/AcademyMaterialsPage'
import AcademyGlossaryPage from './pages/AcademyGlossaryPage'
import AcademyFormsPage from './pages/AcademyFormsPage'
import PreSolicitationPage from './pages/PreSolicitationPage'
import AmendmentsPage from './pages/AmendmentsPage'
import NoticeSamplePage from './pages/NoticeSamplePage'
import RecentAwardsPage from './pages/RecentAwardsPage'
import MonthlyTrendPage from './pages/MonthlyTrendPage'
import NaicsTrendPage from './pages/NaicsTrendPage'

function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <AppLayout />
}

function AppRoutes() {
  return (
    <Routes>
      {/* 홈: 띠가 화면 끝까지 차도록 가운데 상자 없이 (CR-127) */}
      <Route element={<AppLayout contained={false} />}>
        <Route index element={<HomePage />} />
      </Route>

      {/* 공개 페이지 (GNB 포함) */}
      <Route element={<AppLayout />}>
        <Route path="/coming-soon" element={<ComingSoonPage />} />
        {/* 메뉴 속 화면 (CR-133) */}
        <Route path="/coming-soon/:key" element={<ComingSoonPage />} />
        <Route path="/services/:key" element={<ServicePage />} />
        <Route path="/academy/class" element={<AcademyClassPage />} />
        <Route path="/academy/materials" element={<AcademyMaterialsPage />} />
        <Route path="/academy/glossary" element={<AcademyGlossaryPage />} />
        <Route path="/academy/forms" element={<AcademyFormsPage />} />
        {/* 있는 데이터로 여는 메뉴 속 화면은 비로그인에게도 보인다(CR-135) */}
        <Route path="/bids/pre-solicitation" element={<PreSolicitationPage />} />
        <Route path="/bids/amendments" element={<AmendmentsPage />} />
        <Route path="/bids/sample" element={<NoticeSamplePage />} />
        <Route path="/awards/recent" element={<RecentAwardsPage />} />
        <Route path="/awards/awardees" element={<AwardeesPage />} />
        <Route path="/analysis/monthly" element={<MonthlyTrendPage />} />
        <Route path="/analysis/naics" element={<NaicsTrendPage />} />
        <Route path="/analysis/base-map" element={<BaseMapPage />} />
        <Route path="/pricing" element={<PricingPage />} />
        <Route path="/guide" element={<GuidePage />} />
        <Route path="/notices" element={<NoticesPage />} />
        <Route path="/faq" element={<FaqPage />} />
        <Route path="/support" element={<SupportPage />} />
        {/* 법적·정책 (CR-124) */}
        <Route path="/terms" element={<TermsPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/refund" element={<RefundPage />} />
        <Route path="/disclaimer" element={<DisclaimerPage />} />

        {/* 적격성 진단은 비로그인에게도 안내를 보이고, 공고별 진단은 화면이 로그인으로 보낸다(CR-137) */}
        <Route path="/fit/eligibility" element={<EligibilityPage />} />
        <Route path="/fit/eligibility/:noticeId" element={<EligibilityDetailPage />} />
        {/* 공고별 판정도 비로그인에게 뜻과 로그인 안내를 보인다(CR-140) */}
        <Route path="/fit/verdicts" element={<VerdictPage />} />

        {/* 로그인 / 회원가입도 공통 틀 안에 (CR-131) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* 인증 필요 페이지 (GNB 포함) */}
      <Route element={<ProtectedRoute />}>
        <Route path="/search" element={<SearchPage />} />
        <Route path="/search/:id" element={<BidDetailPage />} />
        <Route path="/proposals" element={<ProposalsPage />} />
        <Route path="/proposals/:id" element={<ProposalDetailPage />} />
        <Route path="/bookmarks" element={<BookmarksPage />} />
        <Route path="/calendar" element={<CalendarPage />} />
        <Route path="/saved-searches" element={<SavedSearchesPage />} />
        <Route path="/documents" element={<DocumentsPage />} />
        <Route path="/documents/expiry" element={<DocumentExpiryPage />} />
        <Route path="/documents/pre-check" element={<PreCheckPage />} />
        <Route path="/documents/pre-check/:bidRequestId" element={<PreCheckDetailPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/billing" element={<BillingPage />} />
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
