import { Bot, MessageCircle } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

const BADGE_CLASS = {
  BUY_NOW: 'bg-emerald-50 text-emerald-600',
  WAIT: 'bg-amber-50 text-amber-600',
  HOLD: 'bg-slate-100 text-slate-400',
}

// 화면 데모용 정적 데이터 — 백엔드 연동 시 briefingService.getAgentStatus()로 교체
const MONITORING = [
  { label: '재구매 모니터링', value: '5개 품목 추적 중' },
  { label: '가격 추적', value: '2건 · 하락 시 알림' },
  { label: '일정 준비', value: '캠핑 7/25 · 주문 마감 7/23' },
]

const DECISIONS = [
  { name: '세탁세제 리필 2.6L', note: '재고 D-3 · 12,900원', decision: 'BUY_NOW', label: 'BUY NOW' },
  { name: '고양이 사료 1.5kg', note: '7/28 최저가 예상', decision: 'WAIT', label: 'WAIT' },
  { name: '물티슈 캡형 10팩', note: '재고 충분 · 보류', decision: 'HOLD', label: 'HOLD' },
]

function AgentShoppingPage() {
  const navigate = useNavigate()

  return (
    <div className="space-y-5">
      <div className="rounded-lg bg-slate-950 p-6 text-white">
        <p className="flex items-center gap-1.5 text-sm font-semibold text-cyan-300">
          <Bot size={15} aria-hidden="true" /> Agent 쇼핑
        </p>
        <h1 className="mt-2 text-xl font-semibold">Agent가 대신 쇼핑하고 있어요</h1>
        <dl className="mt-4 grid gap-2 sm:grid-cols-3">
          {MONITORING.map((item) => (
            <div key={item.label} className="rounded-md bg-white/10 p-3">
              <dt className="text-xs text-cyan-300">{item.label}</dt>
              <dd className="mt-0.5 text-sm font-semibold">{item.value}</dd>
            </div>
          ))}
        </dl>
      </div>

      <div className="grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-base font-semibold">오늘의 판단</h2>
          <ul className="mt-3 divide-y divide-slate-100">
            {DECISIONS.map((item) => (
              <li key={item.name} className="flex items-center gap-4 py-3">
                <div className="h-12 w-12 shrink-0 rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{item.name}</p>
                  <p className="truncate text-xs text-slate-500">{item.note}</p>
                </div>
                <span className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${BADGE_CLASS[item.decision]}`}>
                  {item.label}
                </span>
              </li>
            ))}
          </ul>
        </section>

        <section className="flex flex-col rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-base font-semibold">대화로 요청하기</h2>
          <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">
            "다음 주 캠핑 준비물 골라줘" 처럼 말하면 후보 탐색부터 타이밍 판단까지 해드려요.
          </p>
          <button
            type="button"
            onClick={() => navigate('/chat')}
            className="mt-4 flex h-11 items-center justify-center gap-2 rounded-md bg-cyan-700 text-sm font-semibold text-white transition hover:bg-cyan-800"
          >
            <MessageCircle size={16} aria-hidden="true" />
            AI 어시스턴트 열기
          </button>
        </section>
      </div>
    </div>
  )
}

export default AgentShoppingPage
