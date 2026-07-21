import api from '@/shared/libs/api.js'
import { agentMock, chatMock } from '@/services/mockData.js'

export const agentService = {
  async getStatus() {
    try {
      const response = await api.GET('/delivery/wp/agent/status')
      return { ...(response.body ?? response.data), live: true }
    } catch {
      return { ...agentMock, live: false }
    }
  },

  async sendMessage(message) {
    try {
      const response = await api.POST('/delivery/wp/agent/chat/stream', { message })
      return response.body ?? response.data
    } catch {
      return {
        role: 'assistant',
        text: '요청을 확인했어요. 실제 Agent API가 연결되면 후보 탐색과 구매 타이밍 판단 결과가 이곳에 표시됩니다.',
      }
    }
  },

  getInitialMessages() {
    return chatMock.initialMessages
  },
}
