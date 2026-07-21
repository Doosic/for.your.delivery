import { ChevronLeft, SendHorizontal } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const BADGE_CLASS = {
  BUY_NOW: 'bg-emerald-50 text-emerald-600',
  WAIT: 'bg-amber-50 text-amber-600',
}

// 화면 데모용 대화 — 백엔드 연동 시 SSE 스트리밍(chatService)으로 교체
const INITIAL_MESSAGES = [
  { id: 1, role: 'user', text: '다음 주 캠핑 가는데 뭘 준비해야 해?' },
  {
    id: 2,
    role: 'assistant',
    text: '7/25 캠핑 일정을 확인했어요. 재고와 이전 기록 기준으로 6가지가 필요하고, 그중 2개는 오늘이 가장 저렴해요.',
    card: {
      title: '캠핑 준비물 추천',
      items: [
        { name: '부탄가스 4개입', decision: 'BUY_NOW', label: 'BUY NOW' },
        { name: '아이스팩 대형', decision: 'BUY_NOW', label: 'BUY NOW' },
        { name: '화로용 숯 3kg', decision: 'WAIT', label: 'WAIT · 7/23' },
      ],
    },
  },
  { id: 3, role: 'user', text: '부탄가스 장바구니에 담아줘' },
  { id: 4, role: 'assistant', text: '담았어요. 결제 전 최저가 판매처(쿠팡, 무료배송) 링크로 연결해 드릴게요.' },
]

function ChatPage() {
  const navigate = useNavigate()
  const [messages, setMessages] = useState(INITIAL_MESSAGES)
  const [input, setInput] = useState('')

  const handleSend = (event) => {
    event.preventDefault()
    const text = input.trim()
    if (!text) return
    setMessages((previous) => [
      ...previous,
      { id: Date.now(), role: 'user', text },
      { id: Date.now() + 1, role: 'assistant', text: '요청을 확인했어요. 후보를 찾아 타이밍을 판단해 드릴게요. (데모 응답)' },
    ])
    setInput('')
  }

  return (
    <div className="mx-auto flex h-[calc(100vh-11rem)] max-w-2xl flex-col md:h-[calc(100vh-9rem)]">
      <header className="flex items-center gap-2 pb-4">
        <button type="button" onClick={() => navigate(-1)} className="text-slate-500 transition hover:text-slate-900" aria-label="뒤로">
          <ChevronLeft size={20} />
        </button>
        <h1 className="text-xl font-semibold">AI 어시스턴트</h1>
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
