import { Building2, ChevronLeft, ExternalLink, FileText, Tags } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
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

  const detailImageUrl = product.imageSources?.detail || product.imageUrl
  const categoryLabel = product.categories?.join(' · ') || '상품'
  const description = product.description?.trim()
    || `${categoryLabel} 상품입니다. 판매처에서 제공한 상품명과 분류 정보를 기준으로 정리했으며, 옵션과 배송 조건은 구매 전에 확인해 주세요.`

  const handleBuy = () => {
    const opened = productService.openSeller(product, { isLoggedIn, sourceContext: 'OTHER' })
    if (!opened) alert.alertWarning('알림', '판매처 링크가 아직 없어요.')
  }

  const reasons = [
    { label: '가격 정보', detail: product.price > 0 ? '판매가 확인됨' : '가격 확인 필요', good: product.price > 0 },
    { label: '판매처', detail: product.mallName || product.source, good: true },
  ]

  return (
    <div className="space-y-5">
      <div className="flex items-center">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="flex items-center gap-1 text-sm font-medium text-slate-500 transition hover:text-slate-900"
        >
          <ChevronLeft size={16} aria-hidden="true" /> 뒤로
        </button>
      </div>

      <div className="grid gap-5 lg:grid-cols-[1.1fr_1fr]">
        <div className="space-y-5">
          <div className="flex items-center justify-center rounded-lg border border-slate-200 bg-white p-10 shadow-sm">
            {detailImageUrl ? (
              <img src={detailImageUrl} alt="" className="aspect-[4/3] w-full max-w-sm rounded-lg object-contain" />
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
                ['판매처', product.mallName],
                ['카테고리', categoryLabel],
              ].map(([label, value]) => (
                <li key={label} className="flex items-center justify-between gap-4 text-sm">
                  <span className="text-slate-500">{label}</span>
                  <span className="truncate font-semibold">{value}</span>
                </li>
              ))}
            </ul>

            <div className="mt-5 border-t border-slate-100 pt-5">
              <div className="flex items-center justify-between gap-3">
                <h2 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <FileText size={16} className="text-cyan-700" aria-hidden="true" /> 상품 정보 요약
                </h2>
                <span className="text-xs font-medium text-slate-400">판매처 제공 정보 기준</span>
              </div>
              <p className="mt-3 text-sm leading-6 text-slate-600">{description}</p>

              {(product.brand || product.maker) && (
                <dl className="mt-4 grid gap-2 bg-slate-50 p-3 text-sm sm:grid-cols-2">
                  {product.brand && (
                    <div className="flex min-w-0 items-center gap-2">
                      <Tags size={15} className="shrink-0 text-slate-400" aria-hidden="true" />
                      <dt className="shrink-0 text-slate-500">브랜드</dt>
                      <dd className="truncate font-semibold text-slate-800">{product.brand}</dd>
                    </div>
                  )}
                  {product.maker && (
                    <div className="flex min-w-0 items-center gap-2">
                      <Building2 size={15} className="shrink-0 text-slate-400" aria-hidden="true" />
                      <dt className="shrink-0 text-slate-500">제조사</dt>
                      <dd className="truncate font-semibold text-slate-800">{product.maker}</dd>
                    </div>
                  )}
                </dl>
              )}
            </div>
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
              onClick={() => navigate('/app/briefing')}
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
