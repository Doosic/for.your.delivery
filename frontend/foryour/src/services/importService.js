import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const importService = {
  async getItems() {
    const response = await api.GET('/delivery/wp/imports/items', { status: 'NEW', page: 0, size: 20 })
    return assertSuccessBody(response, response.msg || 'fail')
  },

  async sync() {
    const response = await api.POST('/delivery/wp/imports/sync', {
      sources: ['NAVER', 'COUPANG', 'GMAIL'],
    })
    return assertSuccessBody(response, response.msg || 'fail')
  },

  async commit(importedItemSqs) {
    const response = await api.POST('/delivery/wp/imports/items/commit', { importedItemSqs })
    return assertSuccessBody(response, response.msg || 'fail')
  },

  async getConnections() {
    const response = await api.GET('/delivery/wp/import-connections')
    return assertSuccessBody(response, response.msg || 'fail')
  },

  async connect(source) {
    const response = await api.POST('/delivery/wp/import-connections/oauth/start', {
      source,
      redirectUri: window.location.href,
    })
    const body = assertSuccessBody(response, response.msg || 'fail')
    if (body.authorizationUrl) {
      window.location.assign(body.authorizationUrl)
      return { ...body, redirecting: true }
    }
    return body
  },
}
