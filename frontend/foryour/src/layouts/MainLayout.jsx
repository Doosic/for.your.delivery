import { Bot, CalendarDays, Home, Import, LogOut, Search, Sparkles } from 'lucide-react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import BrandLogo from '@/components/BrandLogo.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const NAV_ITEMS = [
  { to: '/', label: '홈', icon: Home },
  { to: '/search', label: '검색', icon: Search },
  { to: '/briefing', label: 'AI 브리핑', icon: Sparkles },
  { to: '/agent', label: 'Agent 쇼핑', icon: Bot },
  { to: '/imports', label: '구매목록', icon: Import },
  { to: '/calendar', label: '캘린더', icon: CalendarDays },
]

/**
 * 반응형 셸: md 미만 = 하단 탭, md 이상 = 상단 헤더 내비.
 * 로그인은 강제하지 않는다 — 모든 화면 게스트 접근 가능, 로그인 버튼을 눌렀을 때만 모달 표시.
 */
function MainLayout() {
  const { user, isLoggedIn, clearUser } = useAuth()
  const navigate = useNavigate()

  const navLinkClass = ({ isActive }) =>
    `flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition ${
      isActive ? 'bg-cyan-50 text-cyan-800' : 'text-slate-600 hover:bg-slate-100'
    }`

  return (
    <div className="min-h-screen bg-[#f6f7fb] text-slate-950">
      {/* 상단 헤더 (모바일에서는 로고+로그인만) */}
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/" aria-label="FUB 홈">
            <BrandLogo />
          </Link>

          <nav className="hidden items-center gap-1 md:flex">
            {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} end={to === '/'} className={navLinkClass}>
                <Icon size={16} aria-hidden="true" />
                {label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            {isLoggedIn ? (
              <>
                <span className="text-sm font-medium text-slate-700">{user?.name}님</span>
                <button
                  type="button"
                  onClick={clearUser}
                  className="flex h-9 items-center gap-1.5 rounded-md border border-slate-300 px-3 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
                >
                  <LogOut size={14} aria-hidden="true" />
                  로그아웃
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => navigate('/login')}
                className="flex h-9 items-center rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800"
              >
                로그인
              </button>
            )}
          </div>
        </div>
      </header>

      {/* 본문 — 모바일은 하단 탭 높이만큼 하단 패딩 */}
      <main className="mx-auto w-full max-w-6xl px-4 pb-24 pt-6 sm:px-6 md:pb-10">
        <Outlet context={{ openLogin: () => navigate('/login') }} />
      </main>

      {/* 모바일 하단 탭 */}
      <nav className="fixed inset-x-0 bottom-0 z-20 flex border-t border-slate-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium transition ${
                isActive ? 'text-cyan-700' : 'text-slate-400'
              }`
            }
          >
            <Icon size={20} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>

    </div>
  )
}

export default MainLayout
