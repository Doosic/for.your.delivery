import { LoaderCircle } from 'lucide-react'
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import BrandLogo from '@/components/BrandLogo.jsx'
import authService from '@/services/authService.js'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

function OAuthCompletePage() {
  const navigate = useNavigate()
  const { saveUser } = useAuth()

  useEffect(() => {
    let active = true

    const completeLogin = async () => {
      try {
        const user = await authService().me()
        if (!active) return
        saveUser(user)
        navigate('/app/main', { replace: true })
      } catch {
        if (!active) return
        navigate('/app/login?error=google_session_failed', { replace: true })
      }
    }

    completeLogin()
    return () => {
      active = false
    }
  }, [navigate, saveUser])

  return (
    <section className="flex min-h-64 w-full max-w-md flex-col items-center justify-center gap-5 rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
      <BrandLogo compact />
      <LoaderCircle size={28} className="animate-spin text-cyan-700" aria-hidden="true" />
      <p className="text-sm font-medium text-slate-600">Google 로그인을 완료하는 중입니다.</p>
    </section>
  )
}

export default OAuthCompletePage
