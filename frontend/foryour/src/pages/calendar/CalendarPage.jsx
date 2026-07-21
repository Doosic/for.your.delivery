import { CalendarDays, CheckCircle2, Link as LinkIcon, RefreshCw } from 'lucide-react'
import { startTransition, useEffect, useMemo, useState } from 'react'
import { useOutletContext, useSearchParams } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { calendarService } from '@/services/calendarService.js'
import { importService } from '@/services/importService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const RANGE_OPTIONS = [
  { days: 7, label: '7일' },
  { days: 14, label: '14일' },
  { days: 30, label: '30일' },
  { days: 60, label: '60일' },
  { days: 90, label: '90일' },
]

function CalendarPage() {
  const alert = useAlert()
  const { isLoggedIn } = useAuth()
  const { openLogin } = useOutletContext()
  const [searchParams, setSearchParams] = useSearchParams()
  const [rangeDays, setRangeDays] = useState(30)
  const range = useMemo(() => calendarService.getDefaultRange(rangeDays), [rangeDays])
  const [connected, setConnected] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSyncing, setIsSyncing] = useState(false)
  const [events, setEvents] = useState([])
  const [isLive, setIsLive] = useState(false)
  const [warning, setWarning] = useState(null)

  useEffect(() => {
    let cancelled = false

    calendarService
      .getSuggestions({ from: range.from, to: range.to })
      .then((response) => {
        if (cancelled) return
        startTransition(() => {
          setEvents(response.suggestions ?? [])
          setConnected(Boolean(response.connected))
          setIsLive(Boolean(response.live))
          setWarning(response.warning || null)
          setIsLoading(false)
        })
      })

    return () => {
      cancelled = true
    }
  }, [range.from, range.to])

  useEffect(() => {
    const calendarStatus = searchParams.get('calendar')
    if (!calendarStatus) return

    if (calendarStatus === 'connected') {
      alert.alertSuccess('알림', 'Google Calendar 연결이 완료되었습니다.')
    } else if (calendarStatus === 'denied' || calendarStatus === 'error') {
      alert.alertWarning('알림', 'Google Calendar 연결을 완료하지 못했습니다.')
    }

    startTransition(() => {
      const nextParams = new URLSearchParams(searchParams)
      nextParams.delete('calendar')
      setSearchParams(nextParams, { replace: true })
    })
  }, [alert, searchParams, setSearchParams])

  const applyResponse = (response) => {
    setEvents(response.suggestions ?? [])
    setConnected(Boolean(response.connected))
    setIsLive(Boolean(response.live))
    setWarning(response.warning || null)
    setIsLoading(false)
    return response
  }

  const reloadEvents = async () => {
    setIsLoading(true)
    const response = await calendarService.getSuggestions({
      from: range.from,
      to: range.to,
    })
    return applyResponse(response)
  }

  const handleRangeChange = (days) => {
    if (days === rangeDays) return
    setIsLoading(true)
    setRangeDays(days)
  }

  const handleConnect = async () => {
    if (!isLoggedIn) {
      openLogin()
      return
    }
    const result = await importService.connect('GOOGLE_CALENDAR')
    if (result.redirecting) return
    setConnected(true)
    await alert.alertSuccess('알림', 'Google Calendar 연결이 완료되었습니다.')
    await reloadEvents()
  }

  const handleSync = async () => {
    setIsSyncing(true)
    const response = await calendarService.sync({
      from: range.from,
      to: range.to,
    })
    const next = await reloadEvents()
    setIsSyncing(false)
    await alert.alertSuccess(
      '알림',
      `다가오는 일정 ${response.eventCount ?? next.suggestions?.length ?? 0}건을 불러왔습니다.`,
    )
  }

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="flex items-center gap-2 text-sm font-semibold text-emerald-700">
            <CalendarDays size={16} aria-hidden="true" /> Google Calendar
          </p>
          <h1 className="mt-1 text-xl font-semibold">다가오는 일정</h1>
          <p className="mt-1 text-sm leading-6 text-slate-500">
            오늘부터 선택한 기간의 Google Calendar 일정을 불러와 표시합니다.
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            <ApiNameChip>{API_ENDPOINTS.calendarSuggestions}</ApiNameChip>
            <ApiNameChip>{API_ENDPOINTS.calendarSync}</ApiNameChip>
            <span
              className={`rounded-md px-2.5 py-1 text-xs font-semibold ${
                isLive ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'
              }`}
            >
              {isLive ? '실제 데이터' : '목업 데이터'}
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={connected ? handleSync : handleConnect}
          disabled={isSyncing || isLoading}
          className="flex h-10 items-center justify-center gap-2 rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800 disabled:opacity-50"
        >
          {connected ? (
            <RefreshCw size={15} className={isSyncing ? 'animate-spin' : ''} aria-hidden="true" />
          ) : (
            <LinkIcon size={15} aria-hidden="true" />
          )}
          {connected
            ? isSyncing
              ? '불러오는 중...'
              : '일정 다시 불러오기'
            : 'Calendar 연결'}
        </button>
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
          ['연결 상태', connected ? '연결됨' : isLoggedIn ? '연결 대기' : '로그인 필요'],
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
          FUB 로그인 후 Google Calendar 읽기 권한만 연결합니다. Google 계정 정보로 FUB에 로그인하거나 새 회원을 만들지 않습니다.
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
            {connected
              ? '해당 기간에 표시할 일정이 없습니다.'
              : 'Google Calendar를 연결하면 다가오는 일정이 여기에 표시됩니다.'}
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
