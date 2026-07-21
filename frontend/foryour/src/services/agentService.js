import api from '@/shared/libs/api.js'
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

const recommendationTitle = (sourceAgent) => {
  if (sourceAgent === 'CALENDAR_PREPARATION') return '일정에 맞춘 주문 목록'
  if (sourceAgent === 'PRICE_INTELLIGENCE') return '가격 기록 기반 추천'
  return '구매 기록·위키 기반 추천'
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
        status: result.assistantMessage?.payload?.status,
      }
      const harnessItems = (result.assistantMessage?.payload?.recommendations ?? [])
        .filter((item) => item?.productSq && item?.name)
      if (harnessItems.length) {
        return {
          ...body,
          card: {
            title: recommendationTitle(result.assistantMessage?.sourceAgent),
            items: harnessItems.slice(0, 6),
          },
        }
      }
      return { ...body, card: undefined }
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
