import { Navigate, Route, Routes } from 'react-router-dom'
import AuthLayout from '@/layouts/AuthLayout.jsx'
import MainLayout from '@/layouts/MainLayout.jsx'
import CompletePage from '@/pages/CompletePage.jsx'
import LoginPage from '@/pages/auth/LoginPage.jsx'
import SignupPage from '@/pages/auth/SignupPage.jsx'
import HomePage from '@/pages/home/HomePage.jsx'
import SearchPage from '@/pages/search/SearchPage.jsx'
import ProductDetailPage from '@/pages/product/ProductDetailPage.jsx'
import BriefingPage from '@/pages/briefing/BriefingPage.jsx'
import AgentShoppingPage from '@/pages/agent/AgentShoppingPage.jsx'
import ChatPage from '@/pages/chat/ChatPage.jsx'
import ImportListPage from '@/pages/imports/ImportListPage.jsx'
import ConnectionsPage from '@/pages/imports/ConnectionsPage.jsx'
import CalendarPage from '@/pages/calendar/CalendarPage.jsx'

function App() {
  return (
    <Routes>
      {/* 쇼핑몰 메인 — 로그인 강제 없음, 전 화면 게스트 접근 가능 */}
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="products/:productSq" element={<ProductDetailPage />} />
        <Route path="briefing" element={<BriefingPage />} />
        <Route path="agent" element={<AgentShoppingPage />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="imports" element={<ImportListPage />} />
        <Route path="imports/connections" element={<ConnectionsPage />} />
        <Route path="calendar" element={<CalendarPage />} />
      </Route>

      {/* 기존 인증 플로우 유지 (회원가입은 전체 화면 페이지) */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>
      <Route path="/complete" element={<CompletePage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
