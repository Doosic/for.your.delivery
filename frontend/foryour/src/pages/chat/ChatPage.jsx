import { SendHorizontal } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ApiNameChip from '@/components/ApiNameChip.jsx';
import { API_ENDPOINTS } from '@/services/apiBlueprint.js';
import { agentService } from '@/services/agentService.js';
import { briefingService } from '@/services/briefingService.js';
import { briefingMock } from '@/services/mockData.js';
import { productService } from '@/services/productService.js';
import { useAuth } from '@/shared/hooks/useAuth.jsx';

const BADGE_CLASS = {
  BUY_NOW: 'bg-emerald-50 text-emerald-600',
  WAIT: 'bg-amber-50 text-amber-600',
  HOLD: 'bg-slate-100 text-slate-400',
  PLAN: 'bg-cyan-50 text-cyan-700',
};

const toProduct = (item, productSq) => ({
  productSq: String(productSq),
  name: item.name,
  price: item.price ?? 0,
  mallName: item.mallName || '추천 상품',
  source: item.source || 'DEMO',
  imageUrl: item.imageUrl || '',
  productUrl: '',
  providerCode: item.providerCode || 'LOCAL_FALLBACK',
});

const resolveProductSq = (item, index) =>
  item.productSq || `BRIEF-${index + 1}-${encodeURIComponent(item.name).slice(0, 40)}`;

function ChatPage() {
  const { user, isLoggedIn } = useAuth();
  const [briefing, setBriefing] = useState({ ...briefingMock, live: false });
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

  const openProductDetail = (item, productSq) => {
    productService.setSelectedProduct(toProduct(item, productSq));
  };

  return (
    <div className='mx-auto flex h-[calc(100vh-11rem)] max-w-3xl flex-col md:h-[calc(100vh-9rem)]'>
      <header className='pb-4'>
        <h1 className='text-xl font-semibold'>
          좋은 아침이에요, {isLoggedIn ? `${user?.name}님` : '게스트님'}
        </h1>
        <p className='mt-1 text-sm text-slate-500'>오늘의 브리핑</p>
        <div className='mt-2 flex flex-wrap items-center gap-2'>
          <ApiNameChip>{API_ENDPOINTS.briefingToday}</ApiNameChip>
          <ApiNameChip>{API_ENDPOINTS.chatStream}</ApiNameChip>
          <span
            className={`rounded-md px-2.5 py-1 text-xs font-semibold ${
              briefing.live
                ? 'bg-emerald-50 text-emerald-700'
                : 'bg-amber-50 text-amber-700'
            }`}
          >
            {briefing.live ? '실제 데이터' : '목업 데이터'}
          </span>
        </div>
      </header>

      <div className='flex-1 space-y-3 overflow-y-auto pb-4'>
        <div className='flex justify-start'>
          <div className='max-w-full space-y-3'>
            <p className='rounded-2xl rounded-bl-md border border-slate-200 bg-white px-4 py-2.5 text-sm leading-6 text-slate-900 shadow-sm'>
              {briefing.summary}
            </p>

            <div className='grid gap-3 md:grid-cols-3'>
              {briefing.sections.map((section) => (
                <section
                  key={section.title}
                  className='rounded-lg border border-slate-200 bg-white p-4 shadow-sm'
                >
                  <h2 className='text-sm font-semibold'>{section.title}</h2>
                  <ul className='mt-3 space-y-3'>
                    {section.items.map((item, index) => {
                      const productSq = resolveProductSq(item, index);
                      return (
                        <li key={productSq}>
                          <Link
                            to={`/app/products/${encodeURIComponent(productSq)}`}
                            onClick={() => openProductDetail(item, productSq)}
                            className='flex cursor-pointer items-center gap-3 rounded-md transition hover:bg-slate-50'
                          >
                            <div className='h-10 w-10 shrink-0 rounded-md bg-gradient-to-br from-slate-200 to-slate-50' />
                            <div className='min-w-0 flex-1'>
                              <p className='truncate text-sm font-medium'>
                                {item.name}
                              </p>
                              <p className='truncate text-xs text-slate-500'>
                                {item.note}
                              </p>
                            </div>
                            <span
                              className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${
                                BADGE_CLASS[item.decision] ?? BADGE_CLASS.PLAN
                              }`}
                            >
                              {item.label}
                            </span>
                          </Link>
                        </li>
                      );
                    })}
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
                    {message.card.items.map((item, index) => {
                      const productSq = resolveProductSq(item, index);
                      return (
                        <li key={productSq}>
                          <Link
                            to={`/app/products/${encodeURIComponent(productSq)}`}
                            onClick={() => openProductDetail(item, productSq)}
                            className='flex items-center justify-between gap-3 rounded-md px-1 py-1 text-sm transition hover:bg-slate-50'
                          >
                            <span>{item.name}</span>
                            <span
                              className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                                BADGE_CLASS[item.decision] ?? BADGE_CLASS.PLAN
                              }`}
                            >
                              {item.label}
                            </span>
                          </Link>
                        </li>
                      );
                    })}
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
