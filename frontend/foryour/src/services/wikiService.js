import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const wikiService = {
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
