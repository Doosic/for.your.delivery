import { CalendarDays, Home, Import, LogOut, Search, Sparkles, UserRound } from 'lucide-react'
import { useEffect } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import BrandLogo from '@/components/BrandLogo.jsx'
import authService from '@/services/authService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const NAV_ITEMS = [
  { to: '/app/main', label: '홈', icon: Home },
  { to: '/app/search', label: '검색', icon: Search },
  { to: '/app/briefing', label: 'AI 브리핑', icon: Sparkles, requiresAuth: true },
  { to: '/app/imports', label: '구매목록', icon: Import, requiresAuth: true },
  { to: '/app/calendar', label: '캘린더', icon: CalendarDays, requiresAuth: true },
]

const LOGIN_REQUIRED_MESSAGE = '로그인 후 사용할 수 있는 메뉴입니다.'
const PROTECTED_PATHS = NAV_ITEMS
  .filter((item) => item.requiresAuth)
  .map((item) => item.to)
  .concat('/app/my')

const isProtectedPath = (pathname) => (
  PROTECTED_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`))
)

function MainLayout() {
  const { user, status, isLoggedIn, clearUser } = useAuth()
  const alert = useAlert()
  const location = useLocation()
  const navigate = useNavigate()

  const navLinkClass = ({ isActive }) =>
    `flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition ${
      isActive ? 'bg-cyan-50 text-cyan-800' : 'text-slate-600 hover:bg-slate-100'
    }`

  const handleLogout = async () => {
    const response = await authService().logout()
    if (response?.success === false) return

    clearUser()
    navigate('/app/main')
  }

  const handleProtectedNavigation = (event, item) => {
    if (!item.requiresAuth || isLoggedIn) return

    event.preventDefault()
    alert.alertWarning('알림', LOGIN_REQUIRED_MESSAGE)
    // navigate('/app/login')
  }

  useEffect(() => {
    // 새로고침 직후 refreshAuth 완료 전에는 보호 경로로 강제 이동하지 않는다.
    if (status === 'loading' || status === 'idle') return
    if (!isLoggedIn && isProtectedPath(location.pathname)) {
      navigate('/app/main', { replace: true })
    }
  }, [isLoggedIn, location.pathname, navigate, status])

  return (
    <div className="min-h-screen bg-[#f6f7fb] text-slate-950">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/app/main" aria-label="FUB 홈">
            <BrandLogo />
          </Link>

          <nav className="hidden items-center gap-1 md:flex">
            {NAV_ITEMS.map((item) => {
              const { to, label, icon: Icon } = item
              return (
                <NavLink
                  key={to}
                  to={to}
                  end={to === '/app/main'}
                  onClick={(event) => handleProtectedNavigation(event, item)}
                  className={navLinkClass}
                >
                  <Icon size={16} aria-hidden="true" />
                  {label}
                </NavLink>
              )
            })}
          </nav>

          <div className="flex items-center gap-2">
            {isLoggedIn ? (
              <>
                <Link
                  to="/app/my"
                  className="flex h-9 items-center gap-1.5 rounded-md px-2 text-sm font-medium text-slate-700 transition hover:bg-cyan-50 hover:text-cyan-800"
                  aria-label="마이페이지"
                  title="마이페이지"
                >
                  <UserRound size={16} aria-hidden="true" />
                  <span className="hidden sm:inline">{user?.name}님</span>
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex h-9 items-center gap-1.5 rounded-md border border-slate-300 px-3 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
                >
                  <LogOut size={14} aria-hidden="true" />
                  로그아웃
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => navigate('/app/login')}
                className="flex h-9 items-center rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800"
              >
                로그인
              </button>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl px-4 pb-24 pt-6 sm:px-6 md:pb-10">
        <Outlet context={{ openLogin: () => navigate('/app/login') }} />
      </main>

      <nav className="fixed inset-x-0 bottom-0 z-20 flex border-t border-slate-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden">
        {NAV_ITEMS.map((item) => {
          const { to, label, icon: Icon } = item
          return (
            <NavLink
              key={to}
              to={to}
              end={to === '/app/main'}
              onClick={(event) => handleProtectedNavigation(event, item)}
              className={({ isActive }) =>
                `flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium transition ${
                  isActive ? 'text-cyan-700' : 'text-slate-400'
                }`
              }
            >
              <Icon size={20} aria-hidden="true" />
              {label}
            </NavLink>
          )
        })}
      </nav>
    </div>
  )
}

export default MainLayout
