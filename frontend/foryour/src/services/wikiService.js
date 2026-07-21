import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const wikiService = {
  async getEntries({ category, query } = {}) {
    const response = await api.GET('/delivery/wb/agent/wiki', {
      status: 'ACTIVE',
      category: category || undefined,
      query: query || undefined,
    })
    return assertSuccessBody(response, response.msg || '개인 위키를 불러오지 못했습니다.')
  },

  async createEntry({ category, summary }) {
    const response = await api.POST('/delivery/wb/agent/wiki/entries', {
      category,
      entryKey: `CUSTOM_${Date.now().toString(36).toUpperCase()}`,
      summary: summary.trim(),
      content: { note: summary.trim() },
    })
    return assertSuccessBody(response, response.msg || '위키 항목을 추가하지 못했습니다.')
  },

  async updateEntry(wikiEntrySq, { category, summary }) {
    const response = await api.POST(`/delivery/wb/agent/wiki/entries/${wikiEntrySq}`, {
      category,
      summary: summary.trim(),
      content: { note: summary.trim() },
    })
    return assertSuccessBody(response, response.msg || '위키 항목을 수정하지 못했습니다.')
  },

  async archiveEntry(wikiEntrySq) {
    const response = await api.POST(`/delivery/wb/agent/wiki/entries/${wikiEntrySq}/archive`)
    return assertSuccessBody(response, response.msg || '위키 항목을 보관하지 못했습니다.')
  },

  async saveOnboarding(profile) {
    const response = await api.POST('/delivery/wb/agent/wiki/onboarding', {
      householdSize: profile.householdSize,
      pets: profile.pets,
      shoppingPriorities: [],
      preferredCategories: [],
      favoriteFoods: profile.favoriteFoods,
      cookingFrequency: profile.cookingFrequency,
      hobbies: profile.hobbies,
      prompt: profile.prompt.trim(),
    })
    return assertSuccessBody(response, response.msg || '개인화 정보를 저장하지 못했습니다.')
  },
}
