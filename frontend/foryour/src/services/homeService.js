import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const homeService = {
  async getFeed({ personalized = false } = {}) {
    const response = await api.GET(personalized ? '/delivery/wb/home/feed' : '/delivery/wp/home/feed')
    return assertSuccessBody(response, response.msg || 'fail')
  },
}
