import api from '@/shared/libs/api.js'
import { calendarMock } from '@/services/mockData.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

const toLocalDateString = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const calendarService = {
  getDefaultRange(days = 30) {
    const fromDate = new Date()
    const toDate = new Date()
    toDate.setDate(toDate.getDate() + days)
    return {
      from: toLocalDateString(fromDate),
      to: toLocalDateString(toDate),
      days,
    }
  },

  async getSuggestions({ from, to } = {}) {
    const range = from && to ? { from, to } : calendarService.getDefaultRange()
    try {
      const response = await api.GET('/delivery/wp/calendar/purchase-suggestions', {
        from: range.from,
        to: range.to,
      })
      const body = assertSuccessBody(response, response.msg || 'fail')
      return {
        ...body,
        suggestions: body.suggestions ?? [],
        from: range.from,
        to: range.to,
      }
    } catch {
      return {
        suggestions: calendarMock.suggestions,
        connected: false,
        live: false,
        warning: null,
        from: range.from,
        to: range.to,
      }
    }
  },

  async sync({ from, to, calendarIds = ['primary'] } = {}) {
    const range = from && to ? { from, to } : calendarService.getDefaultRange()
    try {
      const response = await api.POST('/delivery/wp/calendar/sync', {
        calendarIds,
        from: range.from,
        to: range.to,
      })
      return assertSuccessBody(response, response.msg || 'fail')
    } catch {
      return {
        eventCount: calendarMock.suggestions.length,
        suggestionCount: calendarMock.suggestions.length,
        live: false,
      }
    }
  },
}
