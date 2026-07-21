import { CheckCircle2, LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import authService from '@/services/authService.js'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

function CompletePage() {
  const { saveUser } = useAuth()
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    authService().me()
      .then((user) => {
        saveUser(user)
        setStatus('complete')
      })
      .catch(() => setStatus('error'))
  }, [saveUser])

  return (
    <main className="min-h-screen bg-[#f6f7fb] px-5 py-8 text-slate-950">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-4xl items-center justify-center">
        <div className="w-full max-w-xl rounded-lg border border-slate-200 bg-white p-8 text-center shadow-sm sm:p-10">
          <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-cyan-50 text-cyan-700">
            {status === 'loading' ? <LoaderCircle size={30} className="animate-spin" aria-hidden="true" /> : <CheckCircle2 size={30} aria-hidden="true" />}
          </div>
          <h1 className="mt-6 text-2xl font-semibold text-slate-950">
            {status === 'loading' ? '로그인 정보를 확인하고 있어요.' : status === 'complete' ? '로그인이 완료되었습니다.' : '로그인 정보를 확인하지 못했습니다.'}
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            {status === 'complete' ? 'Google 계정과 캘린더 읽기 권한이 FUB에 연결됐습니다.' : '다시 로그인하면 연결을 이어갈 수 있습니다.'}
          </p>
          <Link
            to={status === 'error' ? '/app/login' : '/app/main'}
            className="mt-6 inline-flex h-11 items-center rounded-md bg-cyan-700 px-5 text-sm font-semibold text-white transition hover:bg-cyan-800"
          >
            {status === 'error' ? '로그인으로 돌아가기' : '메인으로 이동'}
          </Link>
        </div>
      </section>
    </main>
  )
}

export default CompletePage
