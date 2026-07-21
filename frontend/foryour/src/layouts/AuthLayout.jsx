import { Outlet } from 'react-router-dom'

function AuthLayout() {
  return (
    <main className="min-h-screen bg-[#f6f7fb] px-5 py-8 text-slate-950">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-6xl items-center justify-center">
        <Outlet />
      </div>
    </main>
  )
}

export default AuthLayout
