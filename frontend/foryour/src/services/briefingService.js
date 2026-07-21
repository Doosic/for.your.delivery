import api from '@/shared/libs/api.js';
import { assertSuccessBody } from '@/shared/libs/api-result.js';

const EMPTY_BRIEFING = {
  summary: '오늘의 구매 정보를 불러오지 못했어요. 잠시 후 다시 확인해 주세요.',
  sections: [],
  live: false,
};

export const briefingService = {
  async getToday() {
    try {
      const response = await api.GET('/delivery/wb/agent/briefing/today', undefined, {
        skipUnauthorizedRedirect: true,
      });
      const body = assertSuccessBody(response, response.msg || 'fail');
      return {
        ...body,
        sections: body.sections ?? [],
        live: Boolean(body.live),
        recommendationBasis: body.recommendationBasis,
      };
    } catch {
      return EMPTY_BRIEFING;
    }
  },
};
