import api from '@/shared/libs/api.js'
import { productMocks } from '@/services/mockData.js'

const SELECTED_PRODUCT_KEY = 'delivery-selected-product'
const DEFAULT_KEYWORDS = ['고양이 사료', '고양이 간식', '고양이 모래', '세탁세제', '캠핑 준비물', '생일 선물']

const normalizeProduct = (product) => ({
  productSq: String(product.productSq ?? product.id),
  name: product.name,
  price: Number(product.price ?? product.lprice ?? 0),
  mallName: product.mallName ?? product.source ?? '',
  source: product.source ?? 'LOCAL',
  imageUrl: product.imageUrl ?? product.image ?? '',
  productUrl: product.productUrl ?? product.link ?? '',
  providerCode: product.providerCode ?? 'API',
})

const fallbackSearch = (query) => {
  const normalizedQuery = query.trim()
  const matchedItems = productMocks.filter((product) => (
    !normalizedQuery ||
    product.name.includes(normalizedQuery) ||
    product.searchTerms?.some((term) => term.includes(normalizedQuery) || normalizedQuery.includes(term))
  ))
  const items = matchedItems.length || !normalizedQuery
    ? matchedItems
    : productMocks.map((product, index) => ({
        ...product,
        productSq: `LOCAL-KEYWORD-${index + 1}`,
        name: `${normalizedQuery} ${['추천 상품', '인기 상품', '실속 상품', '무료배송 상품'][index]}`,
        mallName: '통합 상품 검색',
        source: 'AGGREGATED',
        productUrl: `https://search.shopping.naver.com/search/all?query=${encodeURIComponent(normalizedQuery)}`,
      }))

  return {
    query,
    source: 'ALL',
    live: false,
    warnings: ['상품 검색 API 미연결 또는 응답 실패로 목업 데이터를 표시합니다.'],
    keywordSuggestions: DEFAULT_KEYWORDS,
    items,
  }
}

export const productService = {
  async search({ query = '고양이 사료', size = 20 }) {
    try {
      const response = await api.GET('/delivery/wp/products', { query, size })
      const body = response.body ?? response.data
      if (!Array.isArray(body?.items)) {
        throw new Error('Invalid product search response')
      }
      return {
        ...body,
        live: body.live ?? true,
        keywordSuggestions: body.keywordSuggestions?.length ? body.keywordSuggestions : DEFAULT_KEYWORDS,
        items: body.items.map(normalizeProduct),
      }
    } catch {
      return fallbackSearch(query)
    }
  },

  async detail(productSq) {
    const selected = productService.getSelectedProduct(productSq)
    if (selected) {
      return selected
    }

    try {
      const response = await api.GET(`/delivery/wp/products/${productSq}`)
      const body = response.body ?? response.data
      const product = body?.product ?? body
      if (!product?.name) {
        throw new Error('Invalid product detail response')
      }
      return normalizeProduct(product)
    } catch {
      return productMocks.find((product) => product.productSq === productSq) ?? productMocks[0]
    }
  },

  setSelectedProduct(product) {
    sessionStorage.setItem(SELECTED_PRODUCT_KEY, JSON.stringify(normalizeProduct(product)))
  },

  getSelectedProduct(productSq) {
    try {
      const product = JSON.parse(sessionStorage.getItem(SELECTED_PRODUCT_KEY))
      return product?.productSq === String(productSq) ? product : null
    } catch {
      return null
    }
  },
}
