import api from '@/shared/libs/api.js';
import { productService } from '@/services/productService.js';
import { assertSuccessBody } from '@/shared/libs/api-result.js';

const EMPTY_BRIEFING = {
  summary: '오늘의 구매 정보를 불러오지 못했어요. 잠시 후 다시 확인해 주세요.',
  sections: [],
  live: false,
};

const recommendation = (product, decision, label, note) => ({
  ...product,
  decision,
  label,
  note,
});

export const briefingService = {
  async getToday() {
    try {
      const response = await api.GET('/delivery/wb/home/feed', undefined, {
        skipUnauthorizedRedirect: true,
      });
      const body = assertSuccessBody(response, response.msg || 'fail');
      const hotProducts = (body.hotProducts ?? []).filter(productService.isLiveProduct);
      const bestPriceDeals = (body.bestPriceDeals ?? []).filter(productService.isLiveProduct);
      const sections = [
        {
          title: '오늘 살펴볼 것',
          items: hotProducts.slice(0, 3).map((product) =>
            recommendation(product, 'PLAN', '추천', '현재 인기와 관심 정보를 반영한 상품'),
          ),
        },
        {
          title: '가격 기회',
          items: bestPriceDeals.slice(0, 3).map((product) =>
            recommendation(product, 'BUY_NOW', '가격 확인', '최근 수집 가격을 기준으로 확인할 상품'),
          ),
        },
      ];
      return {
        summary: sections.some((section) => section.items.length > 0)
          ? '관심 정보와 실제 판매 가격을 기준으로 오늘 확인할 상품을 정리했어요.'
          : '현재 추천할 수 있는 실제 상품 데이터가 없어요.',
        sections,
        live: sections.some((section) => section.items.length > 0),
      };
    } catch {
      return EMPTY_BRIEFING;
    }
  },
};
