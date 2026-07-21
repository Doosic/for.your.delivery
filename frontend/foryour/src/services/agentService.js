import api from '@/shared/libs/api.js'
import { productService } from '@/services/productService.js'
import { assertSuccessBody } from '@/shared/libs/api-result.js'

const SESSION_KEY = 'fub-agent-session-sq'

const createSession = async () => {
  const response = await api.POST('/delivery/wb/agent/sessions', {
    title: 'AI 브리핑 쇼핑',
    context: { source: 'AI_BRIEFING' },
  })
  const session = assertSuccessBody(response, response.msg || 'fail')
  sessionStorage.setItem(SESSION_KEY, String(session.sessionSq))
  return session.sessionSq
}

const currentSessionSq = async () => {
  const stored = Number(sessionStorage.getItem(SESSION_KEY))
  return Number.isSafeInteger(stored) && stored > 0 ? stored : createSession()
}

const sendToAgent = async (sessionSq, message) => {
  const response = await api.POST(`/delivery/wb/agent/sessions/${sessionSq}/messages`, {
    text: message,
    context: { source: 'AI_BRIEFING' },
  })
  return assertSuccessBody(response, response.msg || 'fail')
}

export const agentService = {
  async getStatus() {
    try {
      const response = await api.GET('/delivery/wp/agent/status')
      return { ...assertSuccessBody(response, response.msg || 'fail'), live: true }
    } catch {
      return { monitoring: [], decisions: [], live: false }
    }
  },

  async sendMessage(message) {
    try {
      let sessionSq = await currentSessionSq()
      let result
      try {
        result = await sendToAgent(sessionSq, message)
      } catch {
        sessionStorage.removeItem(SESSION_KEY)
        sessionSq = await createSession()
        result = await sendToAgent(sessionSq, message)
      }

      const body = {
        role: 'assistant',
        text: result.assistantMessage?.text ?? '요청을 확인했어요.',
        sourceAgent: result.assistantMessage?.sourceAgent,
      }
      try {
        const products = await productService.search({ query: message, size: 4 })
        const items = (products.items ?? []).map((product) => ({
          ...product,
          decision: 'PLAN',
          label: '추천',
        }))
        return {
          ...body,
          card: items.length ? { title: '실제 판매 상품', items } : undefined,
        }
      } catch {
        return { ...body, card: undefined }
      }
    } catch {
      return {
        role: 'assistant',
        text: '상품 정보를 불러오지 못했어요. 잠시 후 다시 요청해 주세요.',
      }
    }
  },

  getInitialMessages() {
    return []
  },
}
