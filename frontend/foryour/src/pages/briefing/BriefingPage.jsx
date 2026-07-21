import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { briefingService } from '@/services/briefingService.js'
import { briefingMock } from '@/services/mockData.js'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const BADGE_CLASS = {
  BUY_NOW: 'bg-emerald-50 text-emerald-600',
  WAIT: 'bg-amber-50 text-amber-600',
  HOLD: 'bg-slate-100 text-slate-400',
  PLAN: 'bg-cyan-50 text-cyan-700',
}

function BriefingPage() {
  const { user, isLoggedIn } = useAuth()
  const [briefing, setBriefing] = useState({ ...briefingMock, live: false })

  useEffect(() => {
    briefingService.getToday().then(setBriefing)
  }, [])

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-xl font-semibold">좋은 아침이에요, {isLoggedIn ? `${user?.name}님` : '게스트님'}</h1>
        <p className="mt-1 text-sm text-slate-500">오늘의 브리핑</p>
        <div className="mt-2">
          <ApiNameChip>{API_ENDPOINTS.briefingToday}</ApiNameChip>
          <span className={`ml-2 rounded-md px-2.5 py-1 text-xs font-semibold ${briefing.live ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
            {briefing.live ? '실제 데이터' : '목업 데이터'}
          </span>
        </div>
      </header>

      {!isLoggedIn && (
        <p className="rounded-lg border border-cyan-200 bg-cyan-50 px-4 py-3 text-sm text-slate-600">
          지금은 예시 브리핑이에요. 로그인하면 내 재고·일정 기준으로 바뀌어요.
        </p>
      )}

      <div className="rounded-lg bg-slate-950 p-6 text-white">
        <p className="text-sm font-semibold text-cyan-300">AI 브리핑</p>
        <p className="mt-2 text-base leading-7 sm:text-lg">{briefing.summary}</p>
      </div>

      {/* 모바일 1열 → 데스크톱 3열 */}
      <div className="grid gap-4 md:grid-cols-3">
        {briefing.sections.map((section) => (
          <section key={section.title} className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold">{section.title}</h2>
            <ul className="mt-3 space-y-3">
              {section.items.map((item) => (
                <li key={item.name} className="flex items-center gap-3">
                  <div className="h-11 w-11 shrink-0 rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{item.name}</p>
                    <p className="truncate text-xs text-slate-500">{item.note}</p>
                  </div>
                  <span
                    className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${BADGE_CLASS[item.decision] ?? BADGE_CLASS.PLAN}`}
                  >
                    {item.label}
                  </span>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>

      <div className="rounded-lg bg-cyan-50 p-4 text-sm text-slate-600">
        구매 데이터가 쌓일수록 브리핑이 정확해져요.{' '}
        <Link to="/app/imports" className="font-semibold text-cyan-800 hover:underline">
          구매목록 가져오기 →
        </Link>
      </div>
    </div>
  )
}

export default BriefingPage
