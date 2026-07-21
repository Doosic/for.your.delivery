import api from '@/shared/libs/api.js'
import { calendarMock } from '@/services/mockData.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const calendarService = {
  async getSuggestions() {
    try {
      const response = await api.GET('/delivery/wp/calendar/purchase-suggestions')
      return assertSuccessBody(response, response.msg || 'fail')
    } catch {
      return { suggestions: calendarMock.suggestions, connected: false, live: false }
    }
  },

  async sync() {
    try {
      const response = await api.POST('/delivery/wp/calendar/sync', {
        calendarIds: ['primary'],
      })
      return assertSuccessBody(response, response.msg || 'fail')
    } catch {
      return { eventCount: calendarMock.suggestions.length, suggestionCount: calendarMock.suggestions.length, live: false }
    }
  },
}
