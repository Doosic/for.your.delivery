import { productService } from '@/services/productService.js'
import { productMocks } from '@/services/mockData.js'

export const homeService = {
  async getFeed() {
    const response = await productService.search({ query: '고양이 사료', size: 8 })
    const items = response.items?.length ? response.items : productMocks
    return {
      live: response.live,
      warnings: response.warnings ?? [],
      hotProducts: items.slice(0, 4),
      bestPriceDeals: [...items].sort((a, b) => a.price - b.price).slice(0, 3),
    }
  },
}
