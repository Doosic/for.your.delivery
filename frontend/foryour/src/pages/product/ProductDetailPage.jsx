import { ChevronLeft } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

// 화면 데모용 정적 데이터 — 백엔드 연동 시 productService.getDetail(productSq)로 교체
const PRODUCT = {
  name: '오리진 캣 6가지 생선 1.8kg',
  price: 38900,
  allTimeLowDiff: '-2.5%',
  priceHistory: [0.7, 0.64, 0.8, 0.58, 0.52, 0.44],
  priceComparison: [
    { seller: '쿠팡', note: '38,900원 · 무료배송' },
    { seller: '네이버쇼핑', note: '39,400원 · +3,000원' },
  ],
  aiScore: {
    score: 87,
    reasons: [
      { label: '영양 균형', detail: '우수 — 조단백 40%', good: true },
      { label: '성분 안전성', detail: '인공보존료 없음', good: true },
      { label: '가격 적정성', detail: '역대가 근접', good: false },
    ],
  },
  nutrition: [
    ['조단백', '40% 이상'],
    ['조지방', '20% 이상'],
    ['조섬유', '3% 이하'],
    ['수분', '10% 이하'],
    ['칼슘 / 인', '1.4% / 1.1%'],
  ],
  ingredients: ['신선 연어', '청어', '가자미', '달걀', '그레인프리'],
}

function ProductDetailPage() {
  const navigate = useNavigate()
  const alert = useAlert()
  const { isLoggedIn } = useAuth()
  const product = PRODUCT
  const score = product.aiScore.score

  const handleBuy = () => {
    alert.alertSuccess('알림', '최저가 판매처(쿠팡)로 이동합니다. (데모)')
  }

  return (
    <div className="space-y-5">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="flex items-center gap-1 text-sm font-medium text-slate-500 transition hover:text-slate-900"
      >
        <ChevronLeft size={16} aria-hidden="true" /> 뒤로
      </button>

      {/* 데스크톱 2열: 좌 이미지+가격 / 우 AI·영양 */}
      <div className="grid gap-5 lg:grid-cols-[1.1fr_1fr]">
        <div className="space-y-5">
          <div className="flex items-center justify-center rounded-lg border border-slate-200 bg-white p-10 shadow-sm">
            <div className="aspect-[4/3] w-full max-w-sm rounded-lg bg-gradient-to-br from-slate-200 to-slate-50" />
          </div>

          <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <h1 className="text-lg font-semibold leading-snug">{product.name}</h1>
              <span className="shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-600">
                최저가
              </span>
            </div>
            <p className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold">{product.price.toLocaleString()}원</span>
              <span className="text-xs font-semibold text-rose-500">
                역대 최저 대비 {product.allTimeLowDiff}
              </span>
            </p>

            <p className="mt-4 text-xs text-slate-400">최근 6개월 가격 추이</p>
            <div className="mt-2 flex h-16 items-end gap-1.5">
              {product.priceHistory.map((height, index) => (
                <div
                  key={index}
                  style={{ height: `${height * 100}%` }}
                  className={`flex-1 rounded-t ${
                    index === product.priceHistory.length - 1 ? 'bg-cyan-700' : 'bg-cyan-100'
                  }`}
                />
              ))}
            </div>

            <ul className="mt-4 space-y-2">
              {product.priceComparison.map((quote) => (
                <li key={quote.seller} className="flex items-center justify-between text-sm">
                  <span className="text-slate-500">{quote.seller}</span>
                  <span className="font-semibold">{quote.note}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="space-y-5">
          {/* AI 추천도 — 영양정보 있는 상품만 노출 */}
          <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold">AI 추천도</h2>
              <span className="rounded-full bg-cyan-50 px-2.5 py-1 text-xs font-semibold text-cyan-700">
                {isLoggedIn ? '맞춤 기준' : '일반 기준 · 로그인 시 맞춤'}
              </span>
            </div>

            <div className="mt-4 flex items-center gap-5">
              <div
                className="grid h-20 w-20 shrink-0 place-items-center rounded-full"
                style={{ background: `conic-gradient(#0e7490 ${score * 3.6}deg, #e2e8f0 0deg)` }}
              >
                <div className="grid h-[60px] w-[60px] place-items-center rounded-full bg-white">
                  <span className="text-lg font-bold text-cyan-700">{score}</span>
                </div>
              </div>
              <ul className="min-w-0 flex-1 space-y-1.5">
                {product.aiScore.reasons.map((reason) => (
                  <li key={reason.label} className="flex items-center justify-between gap-2 text-sm">
                    <span className="text-slate-500">{reason.label}</span>
                    <span className={`text-right font-semibold ${reason.good ? 'text-emerald-600' : 'text-slate-900'}`}>
                      {reason.detail}
                    </span>
                  </li>
                ))}
              </ul>
            </div>

            <p className="mt-4 text-sm leading-6 text-slate-500">
              {isLoggedIn
                ? '반려묘 정보와 구매 이력을 반영한 맞춤 추천도예요.'
                : '영양성분표와 원료 구성을 분석한 일반 추천도예요. 로그인하면 반려묘 정보와 구매 이력을 반영한 맞춤 추천도로 바뀌어요.'}
            </p>
          </div>

          {/* 영양정보 */}
          <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold">
              영양정보 <span className="text-xs font-normal text-slate-400">100g당</span>
            </h2>
            <ul className="mt-3 divide-y divide-slate-100">
              {product.nutrition.map(([label, value]) => (
                <li key={label} className="flex items-center justify-between py-2 text-sm">
                  <span className="text-slate-500">{label}</span>
                  <span className="font-semibold">{value}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* 주요 성분 */}
          <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold">주요 성분</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              {product.ingredients.map((ingredient) => (
                <span
                  key={ingredient}
                  className={`rounded-full border px-3 py-1 text-xs font-semibold ${
                    ingredient === '그레인프리'
                      ? 'border-emerald-300 text-emerald-600'
                      : 'border-slate-300 text-slate-600'
                  }`}
                >
                  {ingredient}
                </span>
              ))}
            </div>
          </div>

          {/* CTA */}
          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => navigate('/agent')}
              className="flex h-12 flex-1 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-semibold text-slate-900 transition hover:bg-slate-50"
            >
              AI 타이밍 판단
            </button>
            <button
              type="button"
              onClick={handleBuy}
              className="h-12 flex-[1.4] rounded-md bg-cyan-700 text-sm font-semibold text-white transition hover:bg-cyan-800"
            >
              최저가로 구매
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
