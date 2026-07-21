import api from '@/shared/libs/api.js'
import { briefingMock } from '@/services/mockData.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const briefingService = {
  async getToday() {
    try {
      const response = await api.GET('/delivery/wp/briefings/today')
      return { ...assertSuccessBody(response, response.msg || 'fail'), live: true }
    } catch {
      return { ...briefingMock, live: false }
    }
  },
}
