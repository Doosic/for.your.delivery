import { CalendarDays, CheckCircle2, Link as LinkIcon, RefreshCw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { calendarService } from '@/services/calendarService.js'
import { importService } from '@/services/importService.js'
import { calendarMock } from '@/services/mockData.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

function CalendarPage() {
  const alert = useAlert()
  const { isLoggedIn } = useAuth()
  const { openLogin } = useOutletContext()
  const [connected, setConnected] = useState(false)
  const [isSyncing, setIsSyncing] = useState(false)
  const [suggestions, setSuggestions] = useState(calendarMock.suggestions)
  const [isLive, setIsLive] = useState(false)

  useEffect(() => {
    calendarService.getSuggestions().then((response) => {
      setSuggestions(response.suggestions)
      setConnected(Boolean(response.connected))
      setIsLive(Boolean(response.live))
    })
  }, [])

  const handleConnect = async () => {
    if (!isLoggedIn) {
      openLogin()
      return
    }
    const result = await importService.connect('GOOGLE_CALENDAR')
    if (result.redirecting) return
    setConnected(true)
    await alert.alertSuccess('알림', 'Google Calendar 연결이 완료되었습니다.')
  }

  const handleSync = async () => {
    setIsSyncing(true)
    const response = await calendarService.sync()
    const nextSuggestions = await calendarService.getSuggestions()
    setSuggestions(nextSuggestions.suggestions)
    setIsLive(Boolean(nextSuggestions.live))
    setIsSyncing(false)
    await alert.alertSuccess('알림', `다가오는 일정 ${response.eventCount ?? suggestions.length}건을 분석했습니다.`)
  }

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="flex items-center gap-2 text-sm font-semibold text-emerald-700">
            <CalendarDays size={16} aria-hidden="true" /> Google Calendar
          </p>
          <h1 className="mt-1 text-xl font-semibold">일정 기반 선행구매</h1>
          <p className="mt-1 text-sm leading-6 text-slate-500">
            일정에서 필요한 물품을 먼저 찾고, 배송 기간을 고려해 구매 마감일을 제안해요.
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            <ApiNameChip>{API_ENDPOINTS.calendarSync}</ApiNameChip>
            <ApiNameChip>{API_ENDPOINTS.calendarSuggestions}</ApiNameChip>
            <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${isLive ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
              {isLive ? '실제 데이터' : '목업 데이터'}
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={connected ? handleSync : handleConnect}
          disabled={isSyncing}
          className="flex h-10 items-center justify-center gap-2 rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800 disabled:opacity-50"
        >
          {connected ? <RefreshCw size={15} className={isSyncing ? 'animate-spin' : ''} aria-hidden="true" /> : <LinkIcon size={15} aria-hidden="true" />}
          {connected ? (isSyncing ? '동기화 중...' : '일정 다시 분석') : 'Calendar 연결'}
        </button>
      </header>

      <section className="grid gap-3 md:grid-cols-3">
        {[
          ['연결 상태', connected ? '연결됨' : isLoggedIn ? '연결 대기' : '로그인 필요'],
          ['분석 범위', '오늘부터 30일'],
          ['브리핑 반영', connected ? '활성' : '대기'],
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

      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-4">
          <h2 className="text-base font-semibold">다가오는 일정 분석</h2>
        </div>
        <ul className="divide-y divide-slate-100">
          {suggestions.map((event) => (
            <li key={event.eventSq} className="p-4">
              <div className="flex items-start gap-3">
                <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-600" aria-hidden="true" />
                <div className="min-w-0">
                  <p className="font-medium">{event.title}</p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {event.startsAt} · {event.location}
                  </p>
                  <p className="mt-2 text-sm leading-6 text-slate-600">{event.suggestion}</p>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}

export default CalendarPage
