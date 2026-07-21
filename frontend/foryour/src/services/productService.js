import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

const SELECTED_PRODUCT_KEY = 'delivery-selected-product'

export const productService = {
  async search({ query = '고양이 사료', size = 20 }) {
    const response = await api.GET('/delivery/wp/products', { query, size })
    return assertSuccessBody(response, response.msg || 'fail')
  },

  async detail(productSq) {
    const selected = productService.getSelectedProduct(productSq)
    if (selected) {
      return selected
    }

    const response = await api.GET(`/delivery/wp/products/${productSq}`)
    return assertSuccessBody(response, response.msg || 'fail').product
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
}
