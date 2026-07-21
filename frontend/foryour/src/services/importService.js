import api from '@/shared/libs/api.js'
import { importMock } from '@/services/mockData.js'

export const importService = {
  async getItems() {
    try {
      const response = await api.GET('/delivery/wp/imports/items', { status: 'NEW', page: 0, size: 20 })
      const body = response.body ?? response.data
      return { items: body.items ?? [], live: true }
    } catch {
      return { items: importMock.items, live: false }
    }
  },

  async sync() {
    try {
      const response = await api.POST('/delivery/wp/imports/sync', {
        sources: ['NAVER', 'COUPANG', 'GMAIL'],
        from: '2026-06-21',
        to: '2026-07-21',
      })
      return response.body ?? response.data
    } catch {
      return { importedCount: 0, newCount: 0, live: false }
    }
  },

  async commit(importedItemSqs) {
    try {
      const response = await api.POST('/delivery/wp/imports/items/commit', { importedItemSqs })
      return response.body ?? response.data
    } catch {
      return { committedCount: importedItemSqs.length, live: false }
    }
  },

  async getConnections() {
    try {
      const response = await api.GET('/delivery/wp/import-connections')
      const body = response.body ?? response.data
      return { connections: body.connections ?? [], live: true }
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
      return response.body ?? response.data
    } catch {
      return { source, connected: true, live: false }
    }
  },
}
