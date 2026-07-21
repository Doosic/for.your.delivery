import { Navigate, Route, Routes } from 'react-router-dom'
import AuthLayout from '@/layouts/AuthLayout.jsx'
import CompletePage from '@/pages/CompletePage.jsx'
import LoginPage from '@/pages/auth/LoginPage.jsx'
import SignupPage from '@/pages/auth/SignupPage.jsx'

function App() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route index element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/app/signup" element={<SignupPage />} />
      </Route>
      <Route path="/app/complete" element={<CompletePage />} />
    </Routes>
  )
}

export default App
