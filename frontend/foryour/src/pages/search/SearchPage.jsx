import { Search as SearchIcon } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const CATEGORIES = ['전체', '사료', '간식', '모래', '용품']

// 화면 데모용 정적 데이터 — 백엔드 연동 시 productService.search()로 교체
const PRODUCTS = [
  { productSq: 1, name: '오리진 캣 6가지 생선 1.8kg', price: 38900, category: '사료', tag: '최저가', tagClass: 'bg-emerald-50 text-emerald-600' },
  { productSq: 4, name: '로얄캐닌 인도어 2kg', price: 29800, category: '사료', tag: '인기', tagClass: 'bg-cyan-50 text-cyan-700' },
  { productSq: 5, name: '고양이 모래 벤토나이트 6L', price: 7900, category: '모래', tag: '역대가', tagClass: 'bg-rose-50 text-rose-600' },
  { productSq: 8, name: '츄르 버라이어티 20개입', price: 12500, category: '간식', tag: '신상품', tagClass: 'bg-amber-50 text-amber-600' },
  { productSq: 9, name: '캣타워 스크래처 2단', price: 45000, category: '용품', tag: '인기', tagClass: 'bg-cyan-50 text-cyan-700' },
  { productSq: 10, name: '습식캔 24개 세트', price: 28900, category: '간식', tag: '자주 구매', tagClass: 'bg-cyan-50 text-cyan-700' },
]

function SearchPage() {
  const { isLoggedIn } = useAuth()
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('전체')

  const results = PRODUCTS.filter(
    (product) =>
      (category === '전체' || product.category === category) &&
      (keyword.trim() === '' || product.name.includes(keyword.trim())),
  )

  return (
    <div className="space-y-5">
      {/* 검색바 */}
      <form
        className="flex h-12 items-center gap-3 rounded-lg border border-slate-300 bg-white px-4 shadow-sm focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100"
        onSubmit={(event) => event.preventDefault()}
      >
        <SearchIcon size={18} className="text-slate-400" aria-hidden="true" />
        <input
          className="h-full min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-slate-400"
          placeholder="상품명을 입력하세요"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <button type="submit" className="text-sm font-semibold text-cyan-700">
          검색
        </button>
      </form>

      {/* 카테고리 칩 */}
      <div className="flex flex-wrap gap-2">
        {CATEGORIES.map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setCategory(item)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition ${
              category === item
                ? 'bg-slate-900 text-white'
                : 'border border-slate-300 bg-white text-slate-600 hover:border-slate-400'
            }`}
          >
            {item}
          </button>
        ))}
      </div>

      {/* 결과 그리드 — 모바일 2열 → 데스크톱 4열 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {results.map((product) => (
          <Link
            key={product.productSq}
            to={`/products/${product.productSq}`}
            className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition hover:border-cyan-300 hover:shadow"
          >
            <div className="aspect-[4/3] rounded-md bg-gradient-to-br from-slate-200 to-slate-50" />
            <p className="mt-2 line-clamp-2 text-sm font-medium leading-5">{product.name}</p>
            <p className="mt-1 text-base font-bold">{product.price.toLocaleString()}원</p>
            <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${product.tagClass}`}>
              {product.tag}
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
