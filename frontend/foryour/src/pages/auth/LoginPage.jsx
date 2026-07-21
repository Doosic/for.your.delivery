import { LockKeyhole, Mail } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import BrandLogo from '@/components/BrandLogo.jsx'
import authService from '@/services/authService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const alert = useAlert()
  const { saveUser } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    const oauthError = searchParams.get('error')
    if (!oauthError) return

    const message = oauthError === 'google_email_missing'
      ? 'Google 계정에서 이메일 정보를 받지 못했습니다.'
      : oauthError === 'google_account_link_failed'
        ? 'Google 계정을 FUB 회원 정보와 연결하지 못했습니다.'
        : 'Google 로그인을 완료하지 못했습니다.'
    alert.alertWarning('Google 로그인', message)
  }, [alert, searchParams])

  const handleSubmit = async (event) => {
    event.preventDefault()

    try {
      setIsSubmitting(true)
      const user = await authService().login(email, password)
      saveUser(user)
      // await alert.alertSuccess('알림', '로그인되었습니다.')
      navigate('/app/main')
    } catch (error) {
      alert.alertWarning('알림', error.message)
      setPassword('')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="grid w-full max-w-5xl overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm md:grid-cols-[1fr_1.05fr]">
      <div className="hidden bg-slate-950 p-10 text-white md:flex md:flex-col md:justify-between">
        <div>
          <BrandLogo inverse />
          <h1 className="mt-8 text-4xl font-semibold leading-tight">필요해지기 전에,<br />먼저 준비하는 쇼핑.</h1>
        </div>
        <p className="text-sm leading-6 text-slate-300">구매 이력과 일정을 연결해 다음 구매 시점을 준비합니다.</p>
      </div>

      <div className="p-7 sm:p-10">
        <div className="mb-8">
          <BrandLogo compact />
          <h2 className="mt-2 text-2xl font-semibold text-slate-950">로그인</h2>
        </div>

        <form className="space-y-5" onSubmit={handleSubmit}>
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">이메일</span>
            <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 bg-white px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
              <Mail size={18} className="text-slate-400" aria-hidden="true" />
              <input
                type="email"
                className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm text-slate-950 outline-none placeholder:text-slate-400"
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
                className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm text-slate-950 outline-none placeholder:text-slate-400"
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
            className="h-12 w-full rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800 focus:outline-none focus:ring-2 focus:ring-cyan-200 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isSubmitting ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div className="my-5 flex items-center gap-3 text-xs text-slate-400">
          <span className="h-px flex-1 bg-slate-200" />
          또는
          <span className="h-px flex-1 bg-slate-200" />
        </div>

        <button
          type="button"
          onClick={() => authService().loginWithGoogle()}
          className="flex h-12 w-full items-center justify-center gap-3 rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-cyan-100"
        >
          <span className="grid size-6 place-items-center rounded-full border border-slate-200 text-sm font-bold text-blue-600" aria-hidden="true">G</span>
          Google로 계속하기
        </button>

        <p className="mt-6 text-center text-sm text-slate-600">
          계정이 없나요?{' '}
          <Link to="/app/signup" className="font-semibold text-cyan-700 hover:text-cyan-800">
            회원가입
          </Link>
        </p>
      </div>
    </section>
  )
}

export default LoginPage
