import { CheckCircle2 } from 'lucide-react'

function CompletePage() {
  return (
    <main className="min-h-screen bg-[#f6f7fb] px-5 py-8 text-slate-950">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-4xl items-center justify-center">
        <div className="w-full max-w-xl rounded-lg border border-slate-200 bg-white p-8 text-center shadow-sm sm:p-10">
          <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-cyan-50 text-cyan-700">
            <CheckCircle2 size={30} aria-hidden="true" />
          </div>
          <h1 className="mt-6 text-2xl font-semibold text-slate-950">로그인이 완료되었습니다.</h1>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            임시 완료 화면입니다. 이후 메인 화면이 준비되면 이 경로를 교체하면 됩니다.
          </p>
        </div>
      </section>
    </main>
  )
}

export default CompletePage
