import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const calendarService = {
  async getSuggestions() {
    const response = await api.GET('/delivery/wp/calendar/purchase-suggestions')
    return assertSuccessBody(response, response.msg || 'fail')
  },

  async sync() {
    const response = await api.POST('/delivery/wp/calendar/sync', {
      calendarIds: ['primary'],
    })
    return assertSuccessBody(response, response.msg || 'fail')
  },
}
