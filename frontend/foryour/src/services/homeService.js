import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const homeService = {
  async getFeed() {
    const response = await api.GET('/delivery/wp/home/feed')
    return assertSuccessBody(response, response.msg || 'fail')
  },
}
