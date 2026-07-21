import { CalendarDays, Flame, Import, Sparkles, TrendingDown } from 'lucide-react'
import { Link, useNavigate, useOutletContext } from 'react-router-dom'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

// 화면 데모용 정적 데이터 — 백엔드 연동 시 homeService.getHomeFeed()로 교체
const HOT_PRODUCTS = [
  { productSq: 1, name: '오리진 캣 6가지 생선 1.8kg', price: 38900, tag: '검색 1위' },
  { productSq: 2, name: '스탠리 텀블러 887ml', price: 27500, tag: '급상승' },
  { productSq: 3, name: '제로 콜라 190ml x30', price: 13900, tag: '1만개 구매' },
  { productSq: 4, name: '로얄캐닌 인도어 2kg', price: 29800, tag: '인기' },
]

const BEST_PRICE_DEALS = [
  { productSq: 5, name: '고양이 모래 벤토나이트 6L', price: 7900, note: '역대 최저 대비 -2%', tag: '역대가' },
  { productSq: 6, name: '세탁세제 리필 2.6L', price: 12900, note: '최근 6개월 최저', tag: '역대가' },
  { productSq: 7, name: '물티슈 캡형 10팩', price: 8100, note: '평균 대비 -18%', tag: '특가' },
]

function HomePage() {
  const navigate = useNavigate()
  const { user, isLoggedIn } = useAuth()
  const { openLogin } = useOutletContext()

  const formatPrice = (value) => `${value.toLocaleString()}원`

  return (
    <div className="space-y-6">
      {/* 검색바 */}
      <button
        type="button"
        onClick={() => navigate('/search')}
        className="flex h-12 w-full items-center rounded-lg border border-slate-300 bg-white px-4 text-left text-sm text-slate-400 shadow-sm transition hover:border-cyan-500"
      >
        검색어를 입력하세요 — 예: 고양이 사료
      </button>

      {/* 히어로: 게스트=서비스 소개 / 로그인=AI 브리핑 요약 */}
      <section className="grid gap-4 md:grid-cols-[1.4fr_1fr]">
        <div className="rounded-lg bg-slate-950 p-6 text-white sm:p-8">
          {isLoggedIn ? (
            <>
              <p className="flex items-center gap-1.5 text-sm font-semibold text-cyan-300">
                <Sparkles size={15} aria-hidden="true" /> AI 브리핑
              </p>
              <h1 className="mt-3 text-xl font-semibold leading-snug sm:text-2xl">
                {user?.name}님, 오늘은 2건만 사면 돼요
              </h1>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                세탁세제는 오늘이 적기, 사료는 7/28까지 기다리면 약 2,800원 아껴요.
              </p>
              <Link
                to="/briefing"
                className="mt-5 inline-flex h-10 items-center rounded-md bg-white px-4 text-sm font-semibold text-cyan-800 transition hover:bg-cyan-50"
              >
                브리핑 전체 보기
              </Link>
            </>
          ) : (
            <>
              <h1 className="text-xl font-semibold leading-snug sm:text-2xl">
                검색 전에, AI가 먼저 준비하는 쇼핑
              </h1>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                로그인하면 재고·일정 기반 AI 브리핑을 내 데이터 기준으로 볼 수 있어요.
              </p>
              <div className="mt-5 flex gap-2">
                <button
                  type="button"
                  onClick={() => navigate('/signup')}
                  className="inline-flex h-10 items-center rounded-md bg-white px-4 text-sm font-semibold text-cyan-800 transition hover:bg-cyan-50"
                >
                  무료로 시작하기
                </button>
                <button
                  type="button"
                  onClick={openLogin}
                  className="inline-flex h-10 items-center rounded-md border border-slate-600 px-4 text-sm font-semibold text-white transition hover:bg-white/10"
                >
                  로그인
                </button>
              </div>
            </>
          )}
        </div>

        {/* Agent 쇼핑 티저 */}
        <div className="flex flex-col justify-between rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div>
            <h2 className="text-base font-semibold">Agent 쇼핑</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              재구매 시점 추적, 가격 하락 알림, 일정 준비까지 AI가 대신 쇼핑해요.
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/agent')}
            className="mt-4 h-10 rounded-md border border-cyan-200 text-sm font-semibold text-cyan-700 transition hover:bg-cyan-50"
          >
            Agent 쇼핑 열기
          </button>
        </div>
      </section>

      {/* 지금 핫한 상품 */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="flex items-center gap-1.5 text-base font-semibold">
            <Flame size={17} className="text-rose-500" aria-hidden="true" /> 지금 핫한 상품
          </h2>
          <span className="text-xs text-slate-400">실시간 인기</span>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {HOT_PRODUCTS.map((product) => (
            <Link
              key={product.productSq}
              to={`/products/${product.productSq}`}
              className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition hover:border-cyan-300 hover:shadow"
            >
              <div className="aspect-[4/3] rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
              <p className="mt-2 line-clamp-2 text-sm font-medium leading-5">{product.name}</p>
              <p className="mt-1 text-base font-bold">{formatPrice(product.price)}</p>
              <p className="mt-0.5 text-xs font-semibold text-rose-500">{product.tag}</p>
            </Link>
          ))}
        </div>
      </section>

      {/* 역대가 도달 */}
      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="flex items-center gap-1.5 text-base font-semibold">
            <TrendingDown size={17} className="text-emerald-600" aria-hidden="true" /> 역대가 도달
          </h2>
          <span className="rounded-full bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-600">오늘만</span>
        </div>
        <ul className="divide-y divide-slate-100">
          {BEST_PRICE_DEALS.map((deal) => (
            <li key={deal.productSq}>
              <Link to={`/products/${deal.productSq}`} className="flex items-center gap-4 py-3 transition hover:bg-slate-50">
                <div className="h-12 w-12 shrink-0 rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{deal.name}</p>
                  <p className="text-xs text-slate-500">
                    {deal.note} · {formatPrice(deal.price)}
                  </p>
                </div>
                <span
                  className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                    deal.tag === '역대가' ? 'bg-rose-50 text-rose-600' : 'bg-emerald-50 text-emerald-600'
                  }`}
                >
                  {deal.tag}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="grid gap-3 md:grid-cols-2">
        <Link
          to="/imports"
          className="flex items-center gap-4 rounded-lg border border-slate-200 bg-white p-5 shadow-sm transition hover:border-cyan-300 hover:shadow"
        >
          <span className="grid h-11 w-11 shrink-0 place-items-center rounded-md bg-cyan-50 text-cyan-700">
            <Import size={20} aria-hidden="true" />
          </span>
          <span>
            <span className="block text-sm font-semibold">구매 목록 가져오기</span>
            <span className="mt-1 block text-xs leading-5 text-slate-500">
              로그인 후 네이버, 쿠팡, Gmail 구매내역을 동기화해요.
            </span>
          </span>
        </Link>
        <Link
          to="/calendar"
          className="flex items-center gap-4 rounded-lg border border-slate-200 bg-white p-5 shadow-sm transition hover:border-cyan-300 hover:shadow"
        >
          <span className="grid h-11 w-11 shrink-0 place-items-center rounded-md bg-emerald-50 text-emerald-700">
            <CalendarDays size={20} aria-hidden="true" />
          </span>
          <span>
            <span className="block text-sm font-semibold">구글 캘린더 연결</span>
            <span className="mt-1 block text-xs leading-5 text-slate-500">
              일정 기반으로 필요한 물품과 구매 마감일을 추천해요.
            </span>
          </span>
        </Link>
      </section>
    </div>
  )
}

export default HomePage
