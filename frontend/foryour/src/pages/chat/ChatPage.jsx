import { CalendarClock, ExternalLink, SendHorizontal, TrendingDown } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { agentService } from '@/services/agentService.js';
import { briefingService } from '@/services/briefingService.js';
import { productService } from '@/services/productService.js';
import { useAuth } from '@/shared/hooks/useAuth.jsx';

const BADGE_CLASS = {
  BUY_NOW: 'bg-emerald-50 text-emerald-600',
  WAIT: 'bg-amber-50 text-amber-600',
  HOLD: 'bg-slate-100 text-slate-400',
  PLAN: 'bg-cyan-50 text-cyan-700',
  BUY_BY: 'bg-blue-50 text-blue-700',
};

const EMPTY_BRIEFING = {
  summary: '오늘의 실제 상품과 가격 정보를 확인하고 있어요.',
  sections: [],
  live: false,
};

function ProductRecommendation({ item, isLoggedIn, showDecision = false }) {
  const imageUrl = item.imageSources?.card1x || item.imageUrl;
  const image2x = item.imageSources?.card2x;
  const insight = item.priceInsight;

  return (
    <li className='flex items-center gap-2 rounded-md transition hover:bg-slate-50'>
      <Link
        to={`/app/products/${encodeURIComponent(item.productSq)}`}
        onClick={() => productService.setSelectedProduct(item)}
        className='flex min-w-0 flex-1 items-center gap-3 p-1'
      >
        {imageUrl ? (
          <img
            src={imageUrl}
            srcSet={image2x && image2x !== imageUrl ? `${imageUrl} 1x, ${image2x} 2x` : undefined}
            alt=''
            className='h-12 w-12 shrink-0 rounded-md object-cover'
          />
        ) : (
          <div className='h-12 w-12 shrink-0 rounded-md bg-slate-100' />
        )}
        <div className='min-w-0 flex-1'>
          <p className='truncate text-sm font-medium'>{item.name}</p>
          <p className='truncate text-xs text-slate-500'>
            {Number(item.price).toLocaleString()}원 · {item.mallName}
          </p>
          {showDecision && (
            <span
              className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${
                BADGE_CLASS[item.decision] ?? BADGE_CLASS.PLAN
              }`}
            >
              {item.label}
            </span>
          )}
          {item.note && (
            <p className='mt-1 line-clamp-2 text-xs leading-5 text-slate-500'>{item.note}</p>
          )}
          <div className='mt-1.5 flex flex-wrap gap-x-3 gap-y-1 text-[11px] text-slate-500'>
            {item.recommendedBuyBy && (
              <span className='inline-flex items-center gap-1'>
                <CalendarClock size={12} aria-hidden='true' />
                {item.recommendedBuyBy}까지 주문
              </span>
            )}
            {insight?.historicalLow && (
              <span className='inline-flex items-center gap-1 font-semibold text-emerald-700'>
                <TrendingDown size={12} aria-hidden='true' />
                최근 {insight.windowDays}일 최저가
              </span>
            )}
            {insight?.nearHistoricalLow && !insight.historicalLow && (
              <span className='inline-flex items-center gap-1 font-semibold text-emerald-700'>
                <TrendingDown size={12} aria-hidden='true' />
                {insight.windowDays}일 최저가 근접
              </span>
            )}
            {insight?.expectedOptimalDate && !insight.historicalLow && (
              <span className='inline-flex items-center gap-1 text-amber-700'>
                <TrendingDown size={12} aria-hidden='true' />
                {insight.expectedOptimalDate} 가격 재확인
              </span>
            )}
          </div>
        </div>
      </Link>
      {item.productUrl && (
        <button
          type='button'
          onClick={() => productService.openSeller(item, { isLoggedIn, sourceContext: 'AGENT_CHAT' })}
          className='grid h-9 w-9 shrink-0 place-items-center rounded-md border border-slate-200 text-slate-500 transition hover:border-cyan-300 hover:text-cyan-700'
          aria-label={`${item.mallName} 판매처 열기`}
          title='판매처 열기'
        >
          <ExternalLink size={15} aria-hidden='true' />
        </button>
      )}
    </li>
  );
}

function ChatPage() {
  const { user, isLoggedIn } = useAuth();
  const [briefing, setBriefing] = useState(EMPTY_BRIEFING);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');

  useEffect(() => {
    briefingService.getToday().then(setBriefing);
  }, []);

  const handleSend = async (event) => {
    event.preventDefault();
    const text = input.trim();
    if (!text) return;
    const userMessage = { id: Date.now(), role: 'user', text };
    setMessages((previous) => [...previous, userMessage]);
    setInput('');
    const assistantMessage = await agentService.sendMessage(text);
    setMessages((previous) => [
      ...previous,
      { id: Date.now() + 1, ...assistantMessage },
    ]);
  };

  return (
    <div className='mx-auto flex h-[calc(100vh-11rem)] max-w-3xl flex-col md:h-[calc(100vh-9rem)]'>
      <header className='pb-4'>
        <h1 className='text-xl font-semibold'>
          좋은 아침이에요, {isLoggedIn ? `${user?.name}님` : '게스트님'}
        </h1>
        <p className='mt-1 text-sm text-slate-500'>오늘의 브리핑</p>
      </header>

      <div className='flex-1 space-y-3 overflow-y-auto pb-4'>
        <div className='flex justify-start'>
          <div className='max-w-full space-y-3'>
            <p className='rounded-2xl rounded-bl-md border border-slate-200 bg-white px-4 py-2.5 text-sm leading-6 text-slate-900 shadow-sm'>
              {briefing.summary}
            </p>

            <div className='grid gap-3 md:grid-cols-3'>
              {briefing.sections.filter((section) => section.items.length > 0).map((section) => (
                <section
                  key={section.title}
                  className='rounded-lg border border-slate-200 bg-white p-4 shadow-sm'
                >
                  <h2 className='text-sm font-semibold'>{section.title}</h2>
                  {section.subtitle && (
                    <p className='mt-1 text-xs leading-5 text-slate-500'>{section.subtitle}</p>
                  )}
                  <ul className='mt-3 space-y-3'>
                    {section.items.map((item) => (
                      <ProductRecommendation
                        key={item.productSq}
                        item={item}
                        isLoggedIn={isLoggedIn}
                        showDecision
                      />
                    ))}
                  </ul>
                </section>
              ))}
            </div>
          </div>
        </div>

        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div className='max-w-[85%] space-y-2'>
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
                <div className='rounded-lg border border-slate-200 bg-white p-4 shadow-sm'>
                  <p className='text-sm font-semibold'>{message.card.title}</p>
                  <ul className='mt-2 space-y-2'>
                    {message.card.items.map((item) => (
                      <ProductRecommendation
                        key={item.productSq}
                        item={item}
                        isLoggedIn={isLoggedIn}
                        showDecision
                      />
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      <form
        onSubmit={handleSend}
        className='flex items-center gap-2 border-t border-slate-200 bg-[#f6f7fb] pt-3'
      >
        <input
          className='h-11 min-w-0 flex-1 rounded-full border border-slate-300 bg-white px-4 text-sm outline-none placeholder:text-slate-400 focus:border-cyan-600 focus:ring-2 focus:ring-cyan-100'
          placeholder='메시지를 입력하세요'
          value={input}
          onChange={(event) => setInput(event.target.value)}
        />
        <button
          type='submit'
          className='grid h-11 w-11 shrink-0 place-items-center rounded-full bg-cyan-700 text-white transition hover:bg-cyan-800'
          aria-label='보내기'
        >
          <SendHorizontal size={18} />
        </button>
      </form>
    </div>
  );
}

export default ChatPage;
