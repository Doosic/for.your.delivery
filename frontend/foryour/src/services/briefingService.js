import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const briefingService = {
  async getToday() {
    const response = await api.GET('/delivery/wp/briefings/today')
    return assertSuccessBody(response, response.msg || 'fail')
  },
}
