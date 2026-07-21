import { CalendarDays, ChevronDown, Home, Import, LogOut, Search, Sparkles, UserRound } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
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
  const userMenuRef = useRef(null)
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const navLinkClass = ({ isActive }) =>
    `flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition ${
      isActive ? 'bg-cyan-50 text-cyan-800' : 'text-slate-600 hover:bg-slate-100'
    }`

  const handleLogout = async () => {
    if (isLoggingOut) return
    setIsLoggingOut(true)
    try {
      await authService().logout()
    } catch {
      // 사용자가 로그아웃을 요청한 경우 서버 세션 상태와 무관하게 화면 세션을 종료한다.
    } finally {
      setIsUserMenuOpen(false)
      clearUser()
      navigate('/app/main')
      setIsLoggingOut(false)
    }
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

  useEffect(() => {
    if (!isUserMenuOpen) return undefined

    const closeOnOutsideClick = (event) => {
      if (!userMenuRef.current?.contains(event.target)) setIsUserMenuOpen(false)
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setIsUserMenuOpen(false)
    }

    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [isUserMenuOpen])

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
              <div ref={userMenuRef} className="relative">
                <button
                  type="button"
                  onClick={() => setIsUserMenuOpen((current) => !current)}
                  className="flex h-10 items-center gap-2 rounded-md border border-slate-200 bg-white px-2.5 text-sm font-medium text-slate-700 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-800"
                  aria-haspopup="menu"
                  aria-expanded={isUserMenuOpen}
                  aria-controls="user-account-menu"
                >
                  <span className="grid size-6 place-items-center rounded bg-slate-950 text-xs font-semibold text-white" aria-hidden="true">
                    {user?.name?.trim()?.slice(0, 1) || 'F'}
                  </span>
                  <span className="hidden max-w-28 truncate sm:inline">{user?.name}님</span>
                  <ChevronDown size={15} className={`transition ${isUserMenuOpen ? 'rotate-180' : ''}`} aria-hidden="true" />
                </button>

                {isUserMenuOpen && (
                  <div id="user-account-menu" role="menu" className="absolute right-0 top-12 z-30 w-52 rounded-md border border-slate-200 bg-white p-1.5 shadow-lg">
                    <div className="border-b border-slate-100 px-3 py-2.5">
                      <p className="truncate text-sm font-semibold text-slate-800">{user?.name}님</p>
                      <p className="mt-0.5 truncate text-xs text-slate-500">{user?.email}</p>
                    </div>
                    <Link
                      to="/app/my"
                      role="menuitem"
                      onClick={() => setIsUserMenuOpen(false)}
                      className="mt-1 flex h-10 items-center gap-2 rounded px-3 text-sm font-medium text-slate-700 transition hover:bg-cyan-50 hover:text-cyan-800 focus:bg-cyan-50 focus:outline-none"
                    >
                      <UserRound size={16} aria-hidden="true" />
                      마이페이지
                    </Link>
                    <button
                      type="button"
                      role="menuitem"
                      onClick={handleLogout}
                      disabled={isLoggingOut}
                      className="flex h-10 w-full items-center gap-2 rounded px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100 focus:bg-slate-100 focus:outline-none disabled:cursor-wait disabled:text-slate-400"
                    >
                      <LogOut size={16} aria-hidden="true" />
                      {isLoggingOut ? '로그아웃 중...' : '로그아웃'}
                    </button>
                  </div>
                )}
              </div>
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
