import { ChevronLeft, SendHorizontal } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { agentService } from '@/services/agentService.js'

const BADGE_CLASS = {
  BUY_NOW: 'bg-emerald-50 text-emerald-600',
  WAIT: 'bg-amber-50 text-amber-600',
}

function ChatPage() {
  const navigate = useNavigate()
  const [messages, setMessages] = useState(agentService.getInitialMessages())
  const [input, setInput] = useState('')

  const handleSend = async (event) => {
    event.preventDefault()
    const text = input.trim()
    if (!text) return
    const userMessage = { id: Date.now(), role: 'user', text }
    setMessages((previous) => [...previous, userMessage])
    setInput('')
    const assistantMessage = await agentService.sendMessage(text)
    setMessages((previous) => [...previous, { id: Date.now() + 1, ...assistantMessage }])
  }

  return (
    <div className="mx-auto flex h-[calc(100vh-11rem)] max-w-2xl flex-col md:h-[calc(100vh-9rem)]">
      <header className="flex items-center gap-2 pb-4">
        <button type="button" onClick={() => navigate(-1)} className="text-slate-500 transition hover:text-slate-900" aria-label="뒤로">
          <ChevronLeft size={20} />
        </button>
        <h1 className="text-xl font-semibold">AI 어시스턴트</h1>
        <ApiNameChip>{API_ENDPOINTS.chatStream}</ApiNameChip>
      </header>

      {/* 메시지 영역 */}
      <div className="flex-1 space-y-3 overflow-y-auto pb-4">
        {messages.map((message) => (
          <div key={message.id} className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className="max-w-[80%] space-y-2">
              <p
                className={`rounded-2xl px-4 py-2.5 text-sm leading-6 ${
                  message.role === 'user'
                    ? 'rounded-br-md bg-cyan-700 text-white'
                    : 'rounded-bl-md border border-slate-200 bg-white text-slate-900 shadow-sm'
                }`}
              >
                {message.text}
              </p>
              {message.card && (
                <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
                  <p className="text-sm font-semibold">{message.card.title}</p>
                  <ul className="mt-2 space-y-2">
                    {message.card.items.map((item) => (
                      <li key={item.name} className="flex items-center justify-between text-sm">
                        <span>{item.name}</span>
                        <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${BADGE_CLASS[item.decision]}`}>
                          {item.label}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* 입력바 */}
      <form
        onSubmit={handleSend}
        className="flex items-center gap-2 border-t border-slate-200 bg-[#f6f7fb] pt-3"
      >
        <input
          className="h-11 min-w-0 flex-1 rounded-full border border-slate-300 bg-white px-4 text-sm outline-none placeholder:text-slate-400 focus:border-cyan-600 focus:ring-2 focus:ring-cyan-100"
          placeholder="메시지를 입력하세요"
          value={input}
          onChange={(event) => setInput(event.target.value)}
        />
        <button
          type="submit"
          className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-cyan-700 text-white transition hover:bg-cyan-800"
          aria-label="보내기"
        >
          <SendHorizontal size={18} />
        </button>
      </form>
    </div>
  )
}

export default ChatPage
