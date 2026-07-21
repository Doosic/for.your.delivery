import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

const SELECTED_PRODUCT_KEY = 'delivery-selected-product'

const isLiveProduct = (product) =>
  product &&
  product.source !== 'DEMO' &&
  product.providerCode !== 'SERVER_DEMO' &&
  product.providerCode !== 'LOCAL_FALLBACK'

export const productService = {
  async search({ query = '', size = 20, sort = 'LOW_PRICE', personalized = false }) {
    const normalizedQuery = query.trim()
    if (!normalizedQuery && !personalized) {
      return {
        query: '',
        live: false,
        items: [],
        keywordSuggestions: [],
        warnings: [],
      }
    }

    const access = personalized ? 'wb' : 'wp'
    const response = await api.GET(`/delivery/${access}/products`, {
      query: normalizedQuery,
      size,
      sort,
    })
    const body = assertSuccessBody(response, response.msg || 'fail')
    const items = (body.items ?? []).filter(isLiveProduct)
    return {
      ...body,
      live: Boolean(body.live && items.length),
      items,
      warnings: (body.warnings ?? []).filter((warning) => !warning.includes('데모')),
    }
  },

  async detail(productSq) {
    const selected = productService.getSelectedProduct(productSq)
    if (selected) {
      return selected
    }

    const response = await api.GET(`/delivery/wp/products/${productSq}`)
    const product = assertSuccessBody(response, response.msg || 'fail').product
    return isLiveProduct(product) ? product : null
  },

  openSeller(product, { isLoggedIn = false, sourceContext = 'OTHER' } = {}) {
    if (!product?.productUrl) return false

    const provider = encodeURIComponent(product.source)
    const providerCode = encodeURIComponent(product.providerCode)
    const access = isLoggedIn ? 'wb' : 'wp'
    void api.POST(
      `/delivery/${access}/products/${provider}/${providerCode}/purchase-click`,
      { sourceContext },
      { skipUnauthorizedRedirect: true },
    ).catch(() => undefined)
    window.open(product.productUrl, '_blank', 'noopener,noreferrer')
    return true
  },

  setSelectedProduct(product) {
    sessionStorage.setItem(SELECTED_PRODUCT_KEY, JSON.stringify(product))
  },

  getSelectedProduct(productSq) {
    try {
      const product = JSON.parse(sessionStorage.getItem(SELECTED_PRODUCT_KEY))
      return product?.productSq === String(productSq) ? product : null
    } catch {
      return null
    }
  },

  isLiveProduct,
}
