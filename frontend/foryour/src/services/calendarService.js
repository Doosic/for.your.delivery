import api from '@/shared/libs/api.js'
import { calendarMock } from '@/services/mockData.js'

const normalizeSuggestion = (suggestion, index) => ({
  eventSq: suggestion.eventSq ?? suggestion.calendarEventSq ?? index + 1,
  title: suggestion.title,
  startsAt: suggestion.startsAt,
  location: suggestion.location,
  suggestion: suggestion.suggestion ?? suggestion.items?.[0]?.reason ?? '일정 기반 구매 추천을 준비 중입니다.',
})

export const calendarService = {
  async getSuggestions() {
    try {
      const response = await api.GET('/delivery/wp/calendar/purchase-suggestions', {
        from: '2026-07-21',
        to: '2026-08-20',
      })
      const body = response.body ?? response.data
      return {
        suggestions: (body.suggestions ?? []).map(normalizeSuggestion),
        connected: body.connected ?? true,
        live: body.live ?? true,
        warning: body.warning,
      }
    } catch {
      return { suggestions: calendarMock.suggestions, live: false }
    }
  },

  async sync() {
    try {
      const response = await api.POST('/delivery/wp/calendar/sync', {
        calendarIds: ['primary'],
        from: '2026-07-21',
        to: '2026-08-20',
      })
      return response.body ?? response.data
    } catch {
      return { eventCount: calendarMock.suggestions.length, suggestionCount: calendarMock.suggestions.length, live: false }
    }
  },
}
