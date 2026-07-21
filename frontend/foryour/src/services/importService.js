import api from '@/shared/libs/api.js'
import { importMock } from '@/services/mockData.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const importService = {
  async getItems() {
    try {
      const response = await api.GET('/delivery/wp/imports/items', { status: 'NEW', page: 0, size: 20 })
      return assertSuccessBody(response, response.msg || 'fail')
    } catch {
      return { items: importMock.items, live: false }
    }
  },

  async sync() {
    try {
      const response = await api.POST('/delivery/wp/imports/sync', {
        sources: ['NAVER', 'COUPANG', 'GMAIL'],
      })
      return assertSuccessBody(response, response.msg || 'fail')
    } catch {
      return { importedCount: 0, newCount: 0, live: false }
    }
  },

  async commit(importedItemSqs) {
    try {
      const response = await api.POST('/delivery/wp/imports/items/commit', { importedItemSqs })
      return assertSuccessBody(response, response.msg || 'fail')
    } catch {
      return { committedCount: importedItemSqs.length, live: false }
    }
  },

  async getConnections() {
    try {
      const response = await api.GET('/delivery/wp/import-connections')
      return assertSuccessBody(response, response.msg || 'fail')
    } catch {
      return { connections: importMock.connections, live: false }
    }
  },

  async connect(source) {
    try {
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
    } catch {
      return { source, connected: true, live: false }
    }
  },
}
