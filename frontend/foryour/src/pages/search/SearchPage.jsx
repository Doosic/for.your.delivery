import { Search as SearchIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { productService } from '@/services/productService.js'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

function SearchPage() {
  const { isLoggedIn } = useAuth()
  const [keyword, setKeyword] = useState('고양이 사료')
  const [results, setResults] = useState([])
  const [keywordSuggestions, setKeywordSuggestions] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [isLive, setIsLive] = useState(false)
  const [warnings, setWarnings] = useState([])

  const search = async (nextKeyword = keyword) => {
    setIsLoading(true)
    const response = await productService.search({ query: nextKeyword, size: 20 })
    setResults(response.items ?? [])
    setKeywordSuggestions(response.keywordSuggestions ?? [])
    setIsLive(Boolean(response.live))
    setWarnings(response.warnings ?? [])
    setIsLoading(false)
  }

  useEffect(() => {
    let active = true
    productService.search({ query: '고양이 사료', size: 20 }).then((response) => {
      if (!active) return
      setResults(response.items ?? [])
      setKeywordSuggestions(response.keywordSuggestions ?? [])
      setIsLive(Boolean(response.live))
      setWarnings(response.warnings ?? [])
    })
    return () => {
      active = false
    }
  }, [])

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-2">
        <ApiNameChip>{API_ENDPOINTS.productSearch}</ApiNameChip>
        <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${isLive ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
          {isLive ? '실제 데이터' : '목업 데이터'}
        </span>
      </div>

      {/* 검색바 */}
      <form
        className="flex h-12 items-center gap-3 rounded-lg border border-slate-300 bg-white px-4 shadow-sm focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100"
        onSubmit={(event) => {
          event.preventDefault()
          search()
        }}
      >
        <SearchIcon size={18} className="text-slate-400" aria-hidden="true" />
        <input
          className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-slate-400"
          placeholder="상품명을 입력하세요"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <button type="submit" className="text-sm font-semibold text-cyan-700">
          {isLoading ? '검색 중' : '검색'}
        </button>
      </form>

      {/* 키워드 칩 */}
      <div className="flex flex-wrap gap-2">
        {keywordSuggestions.map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => {
              setKeyword(item)
              search(item)
            }}
            className="rounded-full border border-slate-300 bg-white px-4 py-1.5 text-sm font-medium text-slate-600 transition hover:border-cyan-400 hover:text-cyan-800"
          >
            {item}
          </button>
        ))}
      </div>

      {warnings.length > 0 && (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {warnings.join(' · ')}
        </p>
      )}

      {/* 결과 그리드 — 모바일 2열 → 데스크톱 4열 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {results.map((product) => (
          <Link
            key={product.productSq}
            to={`/products/${product.productSq}`}
            onClick={() => productService.setSelectedProduct(product)}
            className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition hover:border-cyan-300 hover:shadow"
          >
            {product.imageUrl ? (
              <img src={product.imageUrl} alt="" className="aspect-[4/3] w-full rounded-md object-cover" />
            ) : (
              <div className="aspect-[4/3] rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
            )}
            <p className="mt-2 line-clamp-2 text-sm font-medium leading-5">{product.name}</p>
            <p className="mt-1 text-base font-bold">{product.price.toLocaleString()}원</p>
            <span className="mt-1 inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
              {product.mallName || product.source}
            </span>
          </Link>
        ))}
      </div>

      {results.length === 0 && (
        <p className="py-10 text-center text-sm text-slate-400">검색 결과가 없어요</p>
      )}

      {/* AI 맞춤 안내 */}
      <div className="rounded-lg bg-cyan-50 p-4 text-sm leading-6 text-slate-600">
        <span className="font-semibold text-cyan-800">AI 맞춤 정렬 · </span>
        {isLoggedIn
          ? '구매 패턴 기반 맞춤 정렬이 적용됐어요. 자주 사는 상품과 최적 구매시점이 함께 표시돼요.'
          : '로그인하면 구매 패턴 기반으로 자주 사는 상품과 최적 구매시점이 함께 표시돼요.'}
      </div>
    </div>
  )
}

export default SearchPage
