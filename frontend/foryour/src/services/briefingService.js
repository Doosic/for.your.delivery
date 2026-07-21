import api from '@/shared/libs/api.js'
import { briefingMock } from '@/services/mockData.js'

export const briefingService = {
  async getToday() {
    try {
      const response = await api.GET('/delivery/wp/briefings/today')
      return { ...(response.body ?? response.data), live: true }
    } catch {
      return { ...briefingMock, live: false }
    }
  },
}
