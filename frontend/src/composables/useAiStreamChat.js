import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { parseSseEvents } from '../utils/sseParser'
import { useUserStore } from '../stores/userStore'

const STREAM_URL = '/api/ai/chat/messages/stream'

export function useAiStreamChat({
  messages,
  isAtBottom,
  scrollToBottom,
  onConversationUpdated,
  router
}) {
  const userStore = useUserStore()
  const isStreaming = ref(false)
  const activeMessageId = ref(null)

  let streamSequence = 0
  let localIdSeq = 0
  let controller = null
  let scrollFrame = null

  const nextLocalId = () => `local-${Date.now()}-${++localIdSeq}`

  const scheduleScroll = () => {
    if (scrollFrame) return
    scrollFrame = requestAnimationFrame(() => {
      scrollFrame = null
      scrollToBottom()
    })
  }

  const updateActiveMessage = (patch) => {
    const index = messages.value.findIndex((message) => message.id === activeMessageId.value)
    if (index === -1) return
    messages.value[index] = { ...messages.value[index], ...patch }
  }

  const abort = () => {
    streamSequence++
    if (controller) {
      controller.abort()
      controller = null
    }
    if (activeMessageId.value) {
      updateActiveMessage({ status: 'done' })
      activeMessageId.value = null
    }
    isStreaming.value = false
  }

  const send = async (conversationId, content) => {
    const text = String(content || '').trim()
    if (!text || isStreaming.value || !conversationId) return

    const sequence = ++streamSequence
    const abortController = new AbortController()
    controller = abortController
    isStreaming.value = true

    const assistantId = nextLocalId()
    activeMessageId.value = assistantId

    messages.value.push(
      {
        id: nextLocalId(),
        conversationId,
        role: 'user',
        content: text,
        createTime: new Date().toISOString(),
        status: 'done'
      },
      {
        id: assistantId,
        conversationId,
        role: 'assistant',
        content: '',
        createTime: new Date().toISOString(),
        status: 'thinking'
      }
    )
    scheduleScroll()

    const isCurrent = () => sequence === streamSequence && !abortController.signal.aborted
    let received = ''

    try {
      const response = await fetch(STREAM_URL, {
        method: 'POST',
        headers: {
          Accept: 'text/event-stream',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${userStore.token}`
        },
        signal: abortController.signal,
        body: JSON.stringify({ conversationId, content: text })
      })

      if (response.status === 401) {
        userStore.logout()
        router.push('/')
        ElMessage.warning('登录已过期，请重新登录')
        return
      }

      if (!response.ok) {
        let detail = `请求失败: ${response.status}`
        try {
          const body = await response.json()
          detail = body.message || detail
        } catch (error) {
          // 非 JSON 错误响应，保留状态码提示
        }
        throw new Error(detail)
      }

      const reader = response.body?.getReader()
      if (!reader) throw new Error('浏览器未返回可读流')

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done || !isCurrent()) break

        buffer += decoder.decode(value, { stream: true })
        const { events, rest } = parseSseEvents(buffer)
        buffer = rest

        for (const event of events) {
          if (!isCurrent()) break
          if (event.event === 'error') {
            throw new Error(event.data || '回答生成失败')
          }
          received += event.data
          updateActiveMessage({ content: received, status: 'streaming' })
          if (isAtBottom.value) scheduleScroll()
        }
      }

      if (buffer.trim() && isCurrent()) {
        const { events } = parseSseEvents(`${buffer}\n\n`)
        for (const event of events) {
          if (event.event === 'error') throw new Error(event.data || '回答生成失败')
          received += event.data
        }
      }

      if (isCurrent()) {
        updateActiveMessage({ content: received, status: 'done' })
        onConversationUpdated?.()
      }
    } catch (error) {
      if (error.name === 'AbortError') {
        updateActiveMessage({ status: 'done' })
        return
      }
      console.error('AI 流式请求失败:', error)
      updateActiveMessage({
        status: 'error',
        content: received || '回答生成失败，请稍后重试。'
      })
      ElMessage.error(error.message || '回答生成失败，请稍后重试')
    } finally {
      if (sequence === streamSequence) {
        isStreaming.value = false
        controller = null
        activeMessageId.value = null
      }
      if (scrollFrame) {
        cancelAnimationFrame(scrollFrame)
        scrollFrame = null
      }
    }
  }

  return { isStreaming, activeMessageId, abort, send }
}
