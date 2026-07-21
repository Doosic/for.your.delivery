import { CalendarDays, CheckCircle2 } from 'lucide-react'
import { startTransition, useEffect, useMemo, useState } from 'react'
import { calendarService } from '@/services/calendarService.js'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const RANGE_OPTIONS = [
  { days: 7, label: '7일' },
  { days: 14, label: '14일' },
  { days: 30, label: '30일' },
  { days: 60, label: '60일' },
  { days: 90, label: '90일' },
]

function CalendarPage() {
  const { isLoggedIn } = useAuth()
  const [rangeDays, setRangeDays] = useState(30)
  const range = useMemo(() => calendarService.getDefaultRange(rangeDays), [rangeDays])
  const [isLoading, setIsLoading] = useState(true)
  const [events, setEvents] = useState([])
  const [warning, setWarning] = useState(null)

  useEffect(() => {
    let cancelled = false

    calendarService
      .getSuggestions({ from: range.from, to: range.to })
      .then((response) => {
        if (cancelled) return
        startTransition(() => {
          setEvents(response.suggestions ?? [])
          setWarning(response.warning || null)
          setIsLoading(false)
        })
      })

    return () => {
      cancelled = true
    }
  }, [range.from, range.to])

  const handleRangeChange = (days) => {
    if (days === rangeDays) return
    setIsLoading(true)
    setRangeDays(days)
  }

  return (
    <div className="space-y-5">
      <header>
        <div>
          <p className="flex items-center gap-2 text-sm font-semibold text-emerald-700">
            <CalendarDays size={16} aria-hidden="true" /> 내 일정
          </p>
          <h1 className="mt-1 text-xl font-semibold">다가오는 일정</h1>
          <p className="mt-1 text-sm leading-6 text-slate-500">
            계정에 저장된 일정과 미리 준비할 상품을 함께 표시합니다.
          </p>
        </div>
      </header>

      <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500">조회 범위</p>
            <p className="mt-1 text-sm font-semibold text-slate-900">
              오늘({range.from})부터 {rangeDays}일 · {range.to}까지
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {RANGE_OPTIONS.map((option) => (
              <button
                key={option.days}
                type="button"
                onClick={() => handleRangeChange(option.days)}
                className={`h-9 rounded-md px-3 text-sm font-semibold transition ${
                  rangeDays === option.days
                    ? 'bg-cyan-700 text-white'
                    : 'border border-slate-300 bg-white text-slate-600 hover:bg-slate-50'
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="grid gap-3 md:grid-cols-3">
        {[
          ['데이터 기준', isLoggedIn ? '내 계정' : '로그인 필요'],
          ['조회 기간', `${rangeDays}일`],
          ['일정 수', isLoading ? '불러오는 중' : `${events.length}건`],
        ].map(([label, value]) => (
          <div key={label} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-xs font-medium text-slate-500">{label}</p>
            <p className="mt-1 text-base font-semibold">{value}</p>
          </div>
        ))}
      </section>

      {!isLoggedIn && (
        <section className="rounded-lg border border-cyan-200 bg-cyan-50 p-4 text-sm leading-6 text-slate-600">
          로그인하면 계정별로 저장된 일정과 일정 기반 준비물 추천을 확인할 수 있어요.
        </section>
      )}

      {warning && (
        <section className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {warning}
        </section>
      )}

      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-4">
          <h2 className="text-base font-semibold">다가오는 일정 목록</h2>
          <p className="mt-1 text-xs text-slate-500">
            {range.from}부터 {range.to}까지 · {rangeDays}일
          </p>
        </div>

        {isLoading ? (
          <p className="p-10 text-center text-sm text-slate-400">일정을 불러오는 중입니다...</p>
        ) : events.length === 0 ? (
          <p className="p-10 text-center text-sm text-slate-400">
            {isLoggedIn
              ? '해당 기간에 저장된 일정이 없습니다.'
              : '로그인하면 내 일정을 확인할 수 있어요.'}
          </p>
        ) : (
          <ul className="max-h-[28rem] divide-y divide-slate-100 overflow-y-auto">
            {events.map((event) => (
              <li key={event.eventSq ?? event.calendarEventSq ?? event.title} className="p-4">
                <div className="flex items-start gap-3">
                  <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-600" aria-hidden="true" />
                  <div className="min-w-0">
                    <p className="font-medium">{event.title}</p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {event.startsAt}
                      {event.location ? ` · ${event.location}` : ''}
                    </p>
                    {event.suggestion && (
                      <p className="mt-2 text-sm leading-6 text-slate-600">{event.suggestion}</p>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

export default CalendarPage
