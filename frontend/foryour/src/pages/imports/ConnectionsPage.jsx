import { CalendarDays, ChevronLeft, ShieldCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { importService } from '@/services/importService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'

const SOURCE_ICON_BG = {
  NAVER: 'bg-emerald-100',
  COUPANG: 'bg-rose-100',
  GMAIL: 'bg-orange-100',
  GOOGLE_CALENDAR: 'bg-emerald-100',
}

const HIDDEN_SOURCES = new Set(['CODEF', 'FILE'])

function ConnectionsPage() {
  const alert = useAlert()
  const [connections, setConnections] = useState([])

  useEffect(() => {
    importService.getConnections().then((response) => {
      setConnections(
        (response.connections ?? []).filter((connection) => !HIDDEN_SOURCES.has(connection.source)),
      )
    })
  }, [])

  const toggleConnection = async (target) => {
    if (!target.connected) {
      const result = await importService.connect(target.source)
      if (result.redirecting || result.requiresConfiguration) return
    }
    setConnections((previous) =>
      previous.map((connection) =>
        connection.importConnectionSq === target.importConnectionSq
          ? {
              ...connection,
              connected: !connection.connected,
              description: !connection.connected ? '연결됨 · 방금 연결' : connection.description,
              autoSync: '매일 06:00',
            }
          : connection,
      ),
    )
    if (!target.connected) {
      alert.alertSuccess('알림', `${target.label} 연결이 완료되었습니다.`)
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-5">
      <header className="flex items-center gap-2">
        <Link to="/app/imports" className="text-slate-500 transition hover:text-slate-900" aria-label="뒤로">
          <ChevronLeft size={20} aria-hidden="true" />
        </Link>
        <div>
          <h1 className="text-xl font-semibold">소스 연결 관리</h1>
        </div>
      </header>

      <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <p className="flex items-center gap-2 text-sm font-semibold text-emerald-700">
          <CalendarDays size={16} aria-hidden="true" /> 일정 기반 선행구매
        </p>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          구글 캘린더를 연결하면 일정명, 날짜, 위치를 기준으로 필요한 물품 후보와 구매 시점을 계산해 브리핑에 반영해요.
        </p>
      </div>

      {connections.map((connection) => (
        <section
          key={connection.importConnectionSq}
          className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div className="flex items-center gap-3">
            <div className={`h-10 w-10 shrink-0 rounded-lg ${SOURCE_ICON_BG[connection.source]}`} />
            <div className="min-w-0 flex-1">
              <h2 className="text-sm font-semibold">{connection.label}</h2>
              <p className="truncate text-xs text-slate-500">{connection.description}</p>
            </div>
            {/* 토글 */}
            <button
              type="button"
              role="switch"
              aria-checked={connection.connected}
              onClick={() => toggleConnection(connection)}
              className={`relative h-6 w-11 shrink-0 rounded-full transition ${
                connection.connected ? 'bg-cyan-700' : 'bg-slate-200'
              }`}
            >
              <span
                className={`absolute top-[3px] h-[18px] w-[18px] rounded-full bg-white transition-all ${
                  connection.connected ? 'left-[23px]' : 'left-[3px]'
                }`}
              />
            </button>
          </div>

          {connection.connected && (
            <p className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-sm">
              <span className="text-slate-500">자동 동기화</span>
              <span className="font-semibold">{connection.autoSync}</span>
            </p>
          )}
          {!connection.connected && connection.action && (
            <button
              type="button"
              onClick={() => toggleConnection(connection)}
              className="mt-3 h-10 w-full rounded-md border border-slate-300 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            >
              {connection.action}
            </button>
          )}
        </section>
      ))}

      <div className="flex gap-3 rounded-lg bg-cyan-50 p-4">
        <ShieldCheck size={18} className="mt-0.5 shrink-0 text-cyan-700" aria-hidden="true" />
        <p className="text-sm leading-6 text-slate-600">
          <span className="font-semibold text-cyan-800">내 데이터는 이렇게 쓰여요 · </span>
          계정 비밀번호는 저장하지 않아요. 구매내역은 재고 예측과 추천에만 사용되고, 언제든 연결
          해제·삭제할 수 있어요.
        </p>
      </div>
    </div>
  )
}

export default ConnectionsPage
