import { ChevronLeft, ExternalLink } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { productService } from '@/services/productService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

function ProductDetailPage() {
  const navigate = useNavigate()
  const { productSq } = useParams()
  const alert = useAlert()
  const { isLoggedIn } = useAuth()
  const [product, setProduct] = useState(() => productService.getSelectedProduct(productSq))
  const score = isLoggedIn ? 87 : 76

  useEffect(() => {
    productService.detail(productSq).then(setProduct)
  }, [productSq])

  if (!product) {
    return (
      <div className="rounded-lg border border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
        상품 정보를 불러오는 중입니다.
      </div>
    )
  }

  const handleBuy = () => {
    if (product.productUrl) {
      window.open(product.productUrl, '_blank', 'noopener,noreferrer')
      return
    }
    alert.alertWarning('알림', '판매처 링크가 아직 없어요.')
  }

  const reasons = [
    { label: '가격 정보', detail: product.price > 0 ? '판매가 확인됨' : '가격 확인 필요', good: product.price > 0 },
    { label: '판매처', detail: product.mallName || product.source, good: true },
    { label: '데이터', detail: product.providerCode === 'LOCAL_FALLBACK' ? '목업 fallback' : '외부 API', good: product.providerCode !== 'LOCAL_FALLBACK' },
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="flex items-center gap-1 text-sm font-medium text-slate-500 transition hover:text-slate-900"
        >
          <ChevronLeft size={16} aria-hidden="true" /> 뒤로
        </button>
        <ApiNameChip>{API_ENDPOINTS.productDetail}</ApiNameChip>
      </div>

      <div className="grid gap-5 lg:grid-cols-[1.1fr_1fr]">
        <div className="space-y-5">
          <div className="flex items-center justify-center rounded-lg border border-slate-200 bg-white p-10 shadow-sm">
            {product.imageUrl ? (
              <img src={product.imageUrl} alt="" className="aspect-[4/3] w-full max-w-sm rounded-lg object-contain" />
            ) : (
              <div className="aspect-[4/3] w-full max-w-sm rounded-lg bg-gradient-to-br from-slate-200 to-slate-50" />
            )}
          </div>

          <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <h1 className="text-lg font-semibold leading-snug">{product.name}</h1>
              <span className="shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-600">
                {product.source}
              </span>
            </div>
            <p className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold">{product.price.toLocaleString()}원</span>
              <span className="text-xs font-semibold text-slate-500">{product.mallName}</span>
            </p>

            <ul className="mt-4 space-y-2">
              {[
                ['데이터 출처', product.providerCode],
                ['판매처', product.mallName],
                ['상품번호', product.productSq],
              ].map(([label, value]) => (
                <li key={label} className="flex items-center justify-between gap-4 text-sm">
                  <span className="text-slate-500">{label}</span>
                  <span className="truncate font-semibold">{value}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="space-y-5">
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
                {reasons.map((reason) => (
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
                ? '구매 이력과 일정 데이터를 함께 반영해 구매 타이밍을 판단합니다.'
                : '로그인하면 구매 이력과 일정 데이터를 반영한 맞춤 추천도로 바뀌어요.'}
            </p>
          </div>

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
              className="flex h-12 flex-[1.4] items-center justify-center gap-2 rounded-md bg-cyan-700 text-sm font-semibold text-white transition hover:bg-cyan-800"
            >
              <ExternalLink size={16} aria-hidden="true" />
              판매처로 이동
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
