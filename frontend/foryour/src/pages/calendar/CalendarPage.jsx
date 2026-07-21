import { CalendarDays, CheckCircle2, Link as LinkIcon, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const UPCOMING_EVENTS = [
  {
    eventSq: 1,
    title: '주말 캠핑',
    startsAt: '2026-07-25 09:00',
    location: '가평',
    suggestion: '모기퇴치제, 아이스박스, 숯은 7/23까지 구매 권장',
  },
  {
    eventSq: 2,
    title: '반려묘 병원 방문',
    startsAt: '2026-07-28 15:30',
    location: '동네 동물병원',
    suggestion: '이동장 패드와 간식 재고 확인',
  },
  {
    eventSq: 3,
    title: '친구 생일',
    startsAt: '2026-08-02 19:00',
    location: '성수',
    suggestion: '선물 후보를 7/30 브리핑에 노출',
  },
]

function CalendarPage() {
  const alert = useAlert()
  const { isLoggedIn } = useAuth()
  const { openLogin } = useOutletContext()
  const [connected, setConnected] = useState(false)
  const [isSyncing, setIsSyncing] = useState(false)

  const handleConnect = async () => {
    if (!isLoggedIn) {
      openLogin()
      return
    }
    setConnected(true)
    await alert.alertSuccess('알림', 'Google Calendar 연결이 완료되었습니다. (데모)')
  }

  const handleSync = async () => {
    setIsSyncing(true)
    setTimeout(async () => {
      setIsSyncing(false)
      await alert.alertSuccess('알림', '다가오는 일정 3건을 분석했습니다. (데모)')
    }, 900)
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
        </div>
        <button
          type="button"
          onClick={connected ? handleSync : handleConnect}
          disabled={isSyncing}
          className="flex h-10 items-center justify-center gap-2 rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800 disabled:opacity-50"
        >
          {connected ? <RefreshCw size={15} className={isSyncing ? 'animate-spin' : ''} aria-hidden="true" /> : <LinkIcon size={15} aria-hidden="true" />}
          {connected ? (isSyncing ? '동기화 중...' : '일정 다시 분석') : 'Google로 연결'}
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
          로그인을 완료하면 Google OAuth 동의 화면으로 이동하고, 읽기 권한으로 일정 제목·시간·위치만 가져옵니다.
        </section>
      )}

      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-4">
          <h2 className="text-base font-semibold">다가오는 일정 분석</h2>
        </div>
        <ul className="divide-y divide-slate-100">
          {UPCOMING_EVENTS.map((event) => (
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
