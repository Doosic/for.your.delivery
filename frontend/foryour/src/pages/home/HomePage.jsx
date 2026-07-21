import { ArrowRight, Flame, Search, Sparkles, TrendingDown } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useOutletContext } from 'react-router-dom'
import { homeService } from '@/services/homeService.js'
import { productService } from '@/services/productService.js'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const EMPTY_FEED = {
  live: false,
  warnings: [],
  hotProducts: [],
  bestPriceDeals: [],
}

function HomePage() {
  const navigate = useNavigate()
  const { user, isLoggedIn } = useAuth()
  const { openLogin } = useOutletContext()
  const [homeFeed, setHomeFeed] = useState(EMPTY_FEED)

  useEffect(() => {
    homeService.getFeed({ personalized: isLoggedIn })
      .then((response) => {
        const live = Boolean(response.live)
        setHomeFeed({
          ...response,
          live,
          hotProducts: live ? (response.hotProducts ?? []).filter(productService.isLiveProduct) : [],
          bestPriceDeals: live ? (response.bestPriceDeals ?? []).filter(productService.isLiveProduct) : [],
        })
      })
      .catch(() => setHomeFeed(EMPTY_FEED))
  }, [isLoggedIn])

  const formatPrice = (value) => `${value.toLocaleString()}원`

  return (
    <div className="space-y-6">
      {/* 검색바 */}
      <Link
        to="/app/search"
        className="flex h-12 w-full items-center gap-2 rounded-lg border border-slate-300 bg-white px-4 text-sm text-slate-400 shadow-sm transition hover:border-cyan-500 hover:text-cyan-700 focus:outline-none focus:ring-2 focus:ring-cyan-100"
      >
        <Search size={17} aria-hidden="true" />
        검색어를 입력하세요
      </Link>

      <section>
        {isLoggedIn ? (
          <Link
            to="/app/briefing"
            className="group flex min-h-40 flex-col justify-between gap-6 rounded-lg bg-slate-950 p-6 text-white shadow-sm transition hover:bg-slate-900 sm:flex-row sm:items-end sm:p-8"
          >
            <div>
              <p className="flex items-center gap-1.5 text-sm font-semibold text-cyan-300">
                <Sparkles size={15} aria-hidden="true" /> AI 브리핑
              </p>
              <h1 className="mt-3 text-xl font-semibold leading-snug sm:text-2xl">
                {user?.name}님, 오늘의 구매 타이밍을 확인해 보세요
              </h1>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                실제 가격과 판매처 정보를 기준으로 살 상품과 기다릴 상품을 정리해 드려요.
              </p>
            </div>
            <span className="flex h-10 shrink-0 items-center gap-2 self-start rounded-md bg-white px-4 text-sm font-semibold text-cyan-800 transition group-hover:bg-cyan-50 sm:self-auto">
              브리핑 전체 보기 <ArrowRight size={16} aria-hidden="true" />
            </span>
          </Link>
        ) : (
          <div className="rounded-lg bg-slate-950 p-6 text-white sm:p-8">
              <h1 className="text-xl font-semibold leading-snug sm:text-2xl">
                검색 전에, AI가 먼저 준비하는 쇼핑
              </h1>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                로그인하면 재고·일정 기반 AI 브리핑을 내 데이터 기준으로 볼 수 있어요.
              </p>
              <div className="mt-5 flex gap-2">
                <button
                  type="button"
                  onClick={() => navigate('/app/signup')}
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
          </div>
        )}
      </section>

      {/* 지금 핫한 상품 */}
      <section>
        <div className="mb-3 flex items-center">
          <h2 className="flex items-center gap-1.5 text-base font-semibold">
            <Flame size={17} className="text-rose-500" aria-hidden="true" /> 지금 핫한 상품
          </h2>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {homeFeed.hotProducts.map((product) => (
            <Link
              key={product.productSq}
              to={`/app/products/${product.productSq}`}
              onClick={() => productService.setSelectedProduct(product)}
              className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition hover:border-cyan-300 hover:shadow"
            >
              {product.imageSources?.card1x || product.imageUrl ? (
                <img
                  src={product.imageSources?.card1x || product.imageUrl}
                  srcSet={product.imageSources?.card2x ? `${product.imageSources.card1x} 1x, ${product.imageSources.card2x} 2x` : undefined}
                  alt=""
                  className="aspect-[4/3] w-full rounded-md object-cover"
                />
              ) : (
                <div className="aspect-[4/3] rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
              )}
              <p className="mt-2 line-clamp-2 text-sm font-medium leading-5">{product.name}</p>
              <p className="mt-1 text-base font-bold">{formatPrice(product.price)}</p>
              <p className="mt-0.5 text-xs font-semibold text-rose-500">{product.mallName}</p>
            </Link>
          ))}
        </div>
        {homeFeed.hotProducts.length === 0 && (
          <p className="rounded-lg border border-slate-200 bg-white p-8 text-center text-sm text-slate-400">
            상품 정보를 불러오는 중이거나 현재 표시할 상품이 없어요.
          </p>
        )}
      </section>

      {/* 역대가 도달 */}
      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-3 flex items-center">
          <h2 className="flex items-center gap-1.5 text-base font-semibold">
            <TrendingDown size={17} className="text-emerald-600" aria-hidden="true" /> 역대가 도달
          </h2>
        </div>
        <ul className="divide-y divide-slate-100">
          {homeFeed.bestPriceDeals.map((deal) => (
            <li key={deal.productSq}>
              <Link
                to={`/app/products/${deal.productSq}`}
                onClick={() => productService.setSelectedProduct(deal)}
                className="flex items-center gap-4 py-3 transition hover:bg-slate-50"
              >
                {deal.imageSources?.card1x || deal.imageUrl ? (
                  <img
                    src={deal.imageSources?.card1x || deal.imageUrl}
                    alt=""
                    className="h-12 w-12 shrink-0 rounded-md object-cover"
                  />
                ) : (
                  <div className="h-12 w-12 shrink-0 rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
                )}
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{deal.name}</p>
                  <p className="text-xs text-slate-500">
                    {deal.mallName} · {formatPrice(deal.price)}
                  </p>
                </div>
                <span className="rounded-full bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-600">
                  최저가순
                </span>
              </Link>
            </li>
          ))}
        </ul>
        {homeFeed.bestPriceDeals.length === 0 && (
          <p className="py-8 text-center text-sm text-slate-400">아직 역대가로 판단할 실제 가격 데이터가 없어요.</p>
        )}
      </section>
    </div>
  )
}

export default HomePage
