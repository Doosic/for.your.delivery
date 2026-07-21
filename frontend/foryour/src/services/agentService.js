import { chatMock } from '@/services/mockData.js'
import api from '@/shared/libs/api.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

export const agentService = {
  async getStatus() {
    const response = await api.GET('/delivery/wp/agent/status')
    return assertSuccessBody(response, response.msg || 'fail')
  },

  async sendMessage(message) {
    const response = await api.POST('/delivery/wp/agent/chat/stream', { message })
    return assertSuccessBody(response, response.msg || 'fail')
  },

  getInitialMessages() {
    return chatMock.initialMessages
  },
}
