import { LockKeyhole, Mail, X } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import BrandLogo from '@/components/BrandLogo.jsx'
import authService from '@/services/authService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

/**
 * 로그인 모달 — 헤더의 '로그인' 버튼을 눌렀을 때만 뜬다 (로그인 강제 없음).
 * POST /wp/user/login 응답으로 세션 상태를 갱신한다.
 */
function LoginModal({ onClose }) {
  const alert = useAlert()
  const { saveUser } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()

    try {
      setIsSubmitting(true)
      const user = await authService().login(email, password)
      saveUser(user)
      onClose()
      await alert.alertSuccess('알림', '로그인되었습니다.')
    } catch (error) {
      alert.alertWarning('알림', error.message)
      setPassword('')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <section
        className="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-7 shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="mb-6 flex items-start justify-between">
          <div>
            <BrandLogo compact />
            <h2 className="mt-1 text-2xl font-semibold text-slate-950">로그인</h2>
            <p className="mt-1 text-xs text-slate-500">로그인하면 AI 브리핑이 내 데이터 기준으로 바뀌어요</p>
          </div>
          <button type="button" onClick={onClose} className="text-slate-400 transition hover:text-slate-700" aria-label="닫기">
            <X size={20} />
          </button>
        </div>

        <button
          type="button"
          onClick={() => authService().loginWithGoogle()}
          className="mb-4 flex h-12 w-full items-center justify-center gap-3 rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
        >
          <span className="grid size-6 place-items-center rounded-full border border-slate-200 text-sm font-bold text-blue-600" aria-hidden="true">G</span>
          Google로 계속하기
        </button>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">이메일</span>
            <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 bg-white px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
              <Mail size={18} className="text-slate-400" aria-hidden="true" />
              <input
                type="email"
                className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-slate-400"
                placeholder="email@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </span>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">비밀번호</span>
            <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 bg-white px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
              <LockKeyhole size={18} className="text-slate-400" aria-hidden="true" />
              <input
                type="password"
                className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-slate-400"
                placeholder="비밀번호"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </span>
          </label>

          <button
            type="submit"
            disabled={isSubmitting}
            className="h-12 w-full rounded-md bg-cyan-700 text-sm font-semibold text-white transition hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isSubmitting ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-slate-600">
          계정이 없나요?{' '}
          <Link to="/app/signup" onClick={onClose} className="font-semibold text-cyan-700 hover:text-cyan-800">
            회원가입
          </Link>
        </p>
      </section>
    </div>
  )
}

export default LoginModal
