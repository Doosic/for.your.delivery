import api from '@/shared/libs/api.js'
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
      const response = await api.GET('/delivery/wb/calendar/purchase-suggestions', {
        from: range.from,
        to: range.to,
      }, {
        skipUnauthorizedRedirect: true,
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
        suggestions: [],
        connected: false,
        live: false,
        warning: '로그인하면 내 일정과 준비물 추천을 확인할 수 있어요.',
        from: range.from,
        to: range.to,
      }
    }
  },

}
