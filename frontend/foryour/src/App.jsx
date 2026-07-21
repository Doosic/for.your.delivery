import { Navigate, Route, Routes } from 'react-router-dom'
import AuthLayout from '@/layouts/AuthLayout.jsx'
import MainLayout from '@/layouts/MainLayout.jsx'
import LoginPage from '@/pages/auth/LoginPage.jsx'
import SignupPage from '@/pages/auth/SignupPage.jsx'
import OAuthCompletePage from '@/pages/auth/OAuthCompletePage.jsx'
import HomePage from '@/pages/home/HomePage.jsx'
import SearchPage from '@/pages/search/SearchPage.jsx'
import ProductDetailPage from '@/pages/product/ProductDetailPage.jsx'
import ChatPage from '@/pages/chat/ChatPage.jsx'
import ImportListPage from '@/pages/imports/ImportListPage.jsx'
import ConnectionsPage from '@/pages/imports/ConnectionsPage.jsx'
import CalendarPage from '@/pages/calendar/CalendarPage.jsx'

function App() {
  return (
    <Routes>
      {/* 쇼핑몰 메인 — 로그인 강제 없음, 전 화면 게스트 접근 가능 */}
      <Route path="/" element={<MainLayout />}>
        <Route index element={<Navigate to="/app/main" replace />} />
        <Route path="/app/main" element={<HomePage />} />
        <Route path="/app/search" element={<SearchPage />} />
        <Route path="/app/products/:productSq" element={<ProductDetailPage />} />
        <Route path="/app/briefing" element={<ChatPage />} />
        <Route path="/app/agent" element={<Navigate to="/app/briefing" replace />} />
        <Route path="/app/chat" element={<Navigate to="/app/briefing" replace />} />
        <Route path="/app/imports" element={<ImportListPage />} />
        <Route path="/app/imports/connections" element={<ConnectionsPage />} />
        <Route path="/app/calendar" element={<CalendarPage />} />
      </Route>

      {/* 기존 인증 플로우 유지 (회원가입은 전체 화면 페이지) */}
      <Route element={<AuthLayout />}>
        <Route path="/app/login" element={<LoginPage />} />
        <Route path="/app/signup" element={<SignupPage />} />
        <Route path="/app/complete" element={<OAuthCompletePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/app/main" replace />} />
    </Routes>
  )
}

export default App
