import { LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import authService from '@/services/authService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'

function SignupPage() {
  const navigate = useNavigate()
  const alert = useAlert()
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateForm = (field) => (event) => {
    setForm((current) => ({
      ...current,
      [field]: event.target.value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    try {
      setIsSubmitting(true)
      await authService().signup(form)
      alert.alertSuccess('알림', '회원가입이 완료되었습니다.')
      navigate('/login')
    } catch (error) {
      alert.alertWarning('알림', error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="w-full max-w-xl rounded-lg border border-slate-200 bg-white p-7 shadow-sm sm:p-10">
      <div className="mb-8">
        <p className="text-sm font-medium text-cyan-700">Foryour</p>
        <h1 className="mt-2 text-2xl font-semibold text-slate-950">회원가입</h1>
      </div>

      <form className="space-y-5" onSubmit={handleSubmit}>
        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">이름</span>
          <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 bg-white px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
            <UserRound size={18} className="text-slate-400" aria-hidden="true" />
            <input
              type="text"
              className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm text-slate-950 outline-none placeholder:text-slate-400"
              placeholder="홍길동"
              value={form.name}
              onChange={updateForm('name')}
              required
            />
          </span>
        </label>

        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">이메일</span>
          <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 bg-white px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
            <Mail size={18} className="text-slate-400" aria-hidden="true" />
            <input
              type="email"
              className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm text-slate-950 outline-none placeholder:text-slate-400"
              placeholder="email@example.com"
              value={form.email}
              onChange={updateForm('email')}
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
              placeholder="8자 이상 입력"
              value={form.password}
              onChange={updateForm('password')}
              minLength={8}
              required
            />
          </span>
        </label>

        <button
          type="submit"
          disabled={isSubmitting}
          className="h-12 w-full rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800 focus:outline-none focus:ring-2 focus:ring-cyan-200 disabled:cursor-not-allowed disabled:bg-slate-400"
        >
          {isSubmitting ? '가입 중...' : '가입하기'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="font-semibold text-cyan-700 hover:text-cyan-800">
          로그인
        </Link>
      </p>
    </section>
  )
}

export default SignupPage
