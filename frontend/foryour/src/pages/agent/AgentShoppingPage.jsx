import { Bot, MessageCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { agentService } from '@/services/agentService.js'
import { agentMock } from '@/services/mockData.js'

const BADGE_CLASS = {
  BUY_NOW: 'bg-emerald-50 text-emerald-600',
  WAIT: 'bg-amber-50 text-amber-600',
  HOLD: 'bg-slate-100 text-slate-400',
}

function AgentShoppingPage() {
  const navigate = useNavigate()
  const [agentStatus, setAgentStatus] = useState({ ...agentMock, live: false })

  useEffect(() => {
    agentService.getStatus().then(setAgentStatus)
  }, [])

  return (
    <div className="space-y-5">
      <div className="rounded-lg bg-slate-950 p-6 text-white">
        <p className="flex items-center gap-1.5 text-sm font-semibold text-cyan-300">
          <Bot size={15} aria-hidden="true" /> Agent 쇼핑
        </p>
        <div className="mt-2 flex flex-wrap gap-2">
          <ApiNameChip>{API_ENDPOINTS.agentStatus}</ApiNameChip>
          <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${agentStatus.live ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
            {agentStatus.live ? '실제 데이터' : '목업 데이터'}
          </span>
        </div>
        <h1 className="mt-2 text-xl font-semibold">Agent가 대신 쇼핑하고 있어요</h1>
        <dl className="mt-4 grid gap-2 sm:grid-cols-3">
          {agentStatus.monitoring.map((item) => (
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
            {agentStatus.decisions.map((item) => (
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
