import { ArrowDownUp, Flame, Search as SearchIcon, ShoppingBag } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { productService } from '@/services/productService.js'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const DEFAULT_QUERY = '생활용품'
const SORT_OPTIONS = [
  { value: 'LOW_PRICE', label: '최저가순', icon: ArrowDownUp },
  { value: 'POPULAR', label: '인기순', icon: Flame },
  { value: 'PURCHASE', label: '구매순', icon: ShoppingBag },
]

function SearchPage() {
  const { isLoggedIn } = useAuth()
  const [keyword, setKeyword] = useState('')
  const [currentQuery, setCurrentQuery] = useState(DEFAULT_QUERY)
  const [sort, setSort] = useState('LOW_PRICE')
  const [results, setResults] = useState([])
  const [keywordSuggestions, setKeywordSuggestions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [warnings, setWarnings] = useState([])

  const loadProducts = async (query, nextSort) => {
    setIsLoading(true)
    setCurrentQuery(query)
    try {
      const response = await productService.search({ query, size: 24, sort: nextSort })
      setResults(response.items ?? [])
      setKeywordSuggestions(response.keywordSuggestions ?? [])
      setWarnings(response.warnings ?? [])
    } catch {
      setResults([])
      setWarnings(['상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'])
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    productService.search({ query: DEFAULT_QUERY, size: 24, sort: 'LOW_PRICE' })
      .then((response) => {
        if (!active) return
        setResults(response.items ?? [])
        setKeywordSuggestions(response.keywordSuggestions ?? [])
        setWarnings(response.warnings ?? [])
      })
      .catch(() => {
        if (!active) return
        setResults([])
        setWarnings(['상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'])
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const search = (nextKeyword = keyword) => {
    const query = nextKeyword.trim()
    if (!query) return
    setKeyword(query)
    loadProducts(query, sort)
  }

  const changeSort = (nextSort) => {
    if (nextSort === sort) return
    setSort(nextSort)
    loadProducts(currentQuery, nextSort)
  }

  return (
    <div className="space-y-5">
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

      <section>
        <p className="mb-2 text-xs font-semibold text-slate-400">카테고리</p>
        <div className="flex gap-2 overflow-x-auto pb-1 sm:flex-wrap">
          {keywordSuggestions.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => search(item)}
              className={`shrink-0 rounded-full border px-4 py-1.5 text-sm font-medium transition ${currentQuery === item ? 'border-cyan-600 bg-cyan-50 text-cyan-800' : 'border-slate-300 bg-white text-slate-600 hover:border-cyan-400 hover:text-cyan-800'}`}
            >
              {item}
            </button>
          ))}
        </div>
      </section>

      {warnings.length > 0 && (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {warnings.join(' · ')}
        </p>
      )}

      <div className="flex flex-col gap-3 border-b border-slate-200 pb-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold text-cyan-700">네이버 쇼핑 랭킹</p>
          <h1 className="mt-1 text-xl font-semibold text-slate-950">{currentQuery}</h1>
        </div>
        <div className="inline-flex h-10 self-start rounded-md border border-slate-300 bg-white p-1 sm:self-auto" aria-label="상품 정렬">
          {SORT_OPTIONS.map(({ value, label, icon: Icon }) => (
            <button
              key={value}
              type="button"
              onClick={() => changeSort(value)}
              className={`flex items-center gap-1.5 rounded px-3 text-xs font-semibold transition ${sort === value ? 'bg-slate-950 text-white' : 'text-slate-500 hover:bg-slate-100'}`}
            >
              <Icon size={13} aria-hidden="true" /> {label}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {Array.from({ length: 8 }, (_, index) => (
            <div key={index} className="animate-pulse rounded-lg border border-slate-200 bg-white p-3">
              <div className="aspect-[4/3] rounded-md bg-slate-100" />
              <div className="mt-3 h-4 rounded bg-slate-100" />
              <div className="mt-2 h-5 w-2/3 rounded bg-slate-100" />
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {results.map((product, index) => {
            const imageUrl = product.imageSources?.card1x || product.imageUrl
            const image2x = product.imageSources?.card2x
            return (
              <Link
                key={product.productSq}
                to={`/app/products/${product.productSq}`}
                onClick={() => productService.setSelectedProduct(product)}
                className="relative rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition hover:border-cyan-300 hover:shadow"
              >
                {sort === 'POPULAR' && (
                  <span className="absolute left-5 top-5 z-10 grid h-7 w-7 place-items-center rounded-md bg-slate-950 text-xs font-bold text-white">{index + 1}</span>
                )}
                <div className="relative aspect-[4/3] overflow-hidden rounded-md bg-slate-100">
                  {imageUrl && (
                    <img
                      src={imageUrl}
                      srcSet={image2x ? `${imageUrl} 1x, ${image2x} 2x` : undefined}
                      alt=""
                      onError={(event) => event.currentTarget.classList.add('hidden')}
                      className="absolute inset-0 h-full w-full object-cover"
                    />
                  )}
                </div>
                <p className="mt-2 line-clamp-2 text-sm font-medium leading-5">{product.name}</p>
                <p className="mt-1 text-base font-bold">{product.price.toLocaleString()}원</p>
                <p className="mt-1 truncate text-xs font-semibold text-slate-500">{product.mallName || product.source}</p>
              </Link>
            )
          })}
        </div>
      )}

      {!isLoading && results.length === 0 && (
        <p className="py-10 text-center text-sm text-slate-400">검색 결과가 없어요</p>
      )}

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
