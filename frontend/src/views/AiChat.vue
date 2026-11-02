<template>
  <div class="eam-page ai-page">
    <PageHeader
      title="AI 知识助手"
      subtitle="基于内置固定资产制度文档回答问题，回答会标注制度依据"
      eyebrow="KNOWLEDGE ASSISTANT"
    >
      <template #actions>
        <span class="ai-ready"><i></i> 知识库已就绪</span>
      </template>
    </PageHeader>

    <div class="eam-page-body ai-body">
      <div class="ai-shell" :class="{ 'rail-open': railOpen }">
        <div v-if="railOpen" class="rail-scrim" @click="railOpen = false"></div>

        <aside class="ai-rail">
          <div class="rail-head">
            <div>
              <p class="eam-eyebrow">Conversations</p>
              <strong>对话记录</strong>
            </div>
            <button type="button" class="rail-new" @click="handleCreateConversation">
              <el-icon><Plus /></el-icon>
              新建
            </button>
          </div>

          <div class="rail-list">
            <button
              v-for="conversation in conversations"
              :key="conversation.id"
              type="button"
              class="conversation"
              :class="{ 'is-active': conversation.id === selectedConversation }"
              @click="selectConversation(conversation.id)"
            >
              <span class="conversation-title">{{ conversation.title || '新对话' }}</span>
              <span class="conversation-time">{{ formatConversationTime(conversation.updateTime) }}</span>
              <span
                class="conversation-delete"
                role="button"
                tabindex="0"
                aria-label="删除会话"
                @click.stop="handleDeleteConversation(conversation)"
                @keydown.enter.stop="handleDeleteConversation(conversation)"
              >
                <el-icon><Delete /></el-icon>
              </span>
            </button>

            <div v-if="!conversations.length" class="rail-empty">
              <span class="rail-empty__icon"><el-icon><ChatLineRound /></el-icon></span>
              <strong>还没有对话</strong>
              <span>从右侧输入问题开始</span>
            </div>
          </div>

          <div class="rail-foot">
            <span class="rail-model">qwen-plus</span>
            <span class="rail-divider"></span>
            <span>RAG 检索增强</span>
          </div>
        </aside>

        <section class="ai-main">
          <header class="chat-head">
            <div class="chat-identity">
              <button type="button" class="rail-toggle" aria-label="打开对话列表" @click="railOpen = true">
                <el-icon><Expand /></el-icon>
              </button>
              <span class="chat-mark">AI</span>
              <div class="chat-copy">
                <strong>{{ currentConversation?.title || '新对话' }}</strong>
                <span>依据资产管理制度知识库回答</span>
              </div>
            </div>
            <div class="chat-status">
              <i class="status-dot"></i>
              {{ isStreaming ? '正在生成回答' : '在线' }}
            </div>
          </header>

          <div ref="messageList" class="message-scroll" @scroll="handleScroll">
            <div v-if="!messages.length && !loading" class="chat-empty">
              <span class="empty-mark">AI</span>
              <h2>今天想了解哪项制度？</h2>
              <p>我可以回答资产领用、转移、维修、报废、盘点和折旧相关问题，并根据知识库给出流程与权限说明。</p>
              <div class="suggestion-grid">
                <button
                  v-for="suggestion in suggestions"
                  :key="suggestion.prompt"
                  type="button"
                  class="suggestion"
                  :disabled="isStreaming"
                  @click="useSuggestion(suggestion.prompt)"
                >
                  <span class="suggestion-icon"><el-icon><component :is="suggestion.icon" /></el-icon></span>
                  <span class="suggestion-copy">
                    <strong>{{ suggestion.title }}</strong>
                    <small>{{ suggestion.description }}</small>
                  </span>
                  <el-icon class="suggestion-arrow"><ArrowRight /></el-icon>
                </button>
              </div>
            </div>

            <div v-else class="message-stack">
              <button
                v-if="hasMoreMessages"
                type="button"
                class="load-earlier"
                :disabled="loadingMore"
                @click="loadEarlierMessages"
              >
                {{ loadingMore ? '正在加载…' : '加载更早消息' }}
              </button>

              <AiMessageItem
                v-for="message in messages"
                :key="message.id"
                :msg="message"
                :formatted-time="formatMessageTime(message.createTime)"
                :user-initial="userInitial"
                @delete="handleDeleteMessage"
              />
            </div>
          </div>

          <button
            v-if="showScrollButton"
            type="button"
            class="scroll-bottom"
            aria-label="回到最新消息"
            @click="jumpToBottom"
          >
            <el-icon><ArrowDown /></el-icon>
          </button>

          <footer class="composer-wrap">
            <div class="composer" :class="{ 'is-focused': composerFocused }">
              <textarea
                ref="composerRef"
                v-model="inputMessage"
                class="composer-input"
                rows="1"
                maxlength="4000"
                placeholder="输入问题，例如：资产报废需要经过哪些审批？"
                @input="resizeComposer"
                @focus="composerFocused = true"
                @blur="composerFocused = false"
                @keydown="handleComposerKeydown"
              ></textarea>
              <div class="composer-bar">
                <span class="composer-hint">Enter 发送 · Shift + Enter 换行</span>
                <button
                  v-if="isStreaming"
                  type="button"
                  class="composer-stop"
                  @click="abortStream"
                >
                  <el-icon><VideoPause /></el-icon>
                  停止
                </button>
                <button
                  v-else
                  type="button"
                  class="composer-send"
                  :disabled="!inputMessage.trim() || loading"
                  @click="sendMessage"
                >
                  发送
                  <el-icon><ArrowUp /></el-icon>
                </button>
              </div>
            </div>
            <p class="composer-note">回答由 AI 基于制度文档生成，如涉及审批与财务口径请以正式制度为准。</p>
          </footer>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  ArrowUp,
  ChatLineRound,
  Delete,
  Expand,
  Plus,
  VideoPause
} from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import AiMessageItem from '../components/ai/AiMessageItem.vue'
import { useAiStreamChat } from '../composables/useAiStreamChat'
import request from '../utils/request'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()

const conversations = ref([])
const selectedConversation = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const hasMoreMessages = ref(false)
const messageList = ref(null)
const composerRef = ref(null)
const isAtBottom = ref(true)
const composerFocused = ref(false)
const railOpen = ref(false)
// 新建会话后立即发送首条消息时，跳过空历史加载，避免覆盖本地流式消息
const skipLoadFor = ref(null)

const PAGE_SIZE = 60
const BOTTOM_THRESHOLD = 64

const suggestions = [
  {
    title: '资产盘点',
    description: '计划、执行与差异处理',
    prompt: '公司资产盘点的流程是什么？',
    icon: 'DataBoard'
  },
  {
    title: '报废审批',
    description: '二级审批节点与权限',
    prompt: '资产报废需要经过哪些审批？',
    icon: 'DocumentChecked'
  },
  {
    title: '设备报修',
    description: '维修申请与进度跟踪',
    prompt: '设备出现故障时应该怎么报修？',
    icon: 'Tools'
  },
  {
    title: '折旧计算',
    description: '三种折旧方法说明',
    prompt: '直线法折旧怎么计算？',
    icon: 'TrendCharts'
  }
]

const userInitial = computed(() => {
  const user = userStore.user || {}
  const name = user.realName || user.username || '我'
  return String(name).trim().charAt(0).toUpperCase() || '我'
})

const currentConversation = computed(() =>
  conversations.value.find((conversation) => conversation.id === selectedConversation.value)
)

const showScrollButton = computed(() => messages.value.length > 0 && !isAtBottom.value)

const { isStreaming, abort, send } = useAiStreamChat({
  messages,
  isAtBottom,
  scrollToBottom,
  onConversationUpdated: touchCurrentConversation,
  router
})

function scrollToBottom() {
  const el = messageList.value
  if (!el) return
  el.scrollTop = el.scrollHeight
  isAtBottom.value = true
}

function jumpToBottom() {
  scrollToBottom()
}

function handleScroll() {
  const el = messageList.value
  if (!el) return
  isAtBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight <= BOTTOM_THRESHOLD
}

function abortStream() {
  abort()
  ElMessage.info('已停止生成')
}

function touchCurrentConversation() {
  const index = conversations.value.findIndex(
    (conversation) => conversation.id === selectedConversation.value
  )
  if (index === -1) return
  const [conversation] = conversations.value.splice(index, 1)
  conversation.updateTime = new Date().toISOString()
  conversations.value.unshift(conversation)
}

async function loadConversations() {
  try {
    const response = await request.get('/ai/chat/conversations')
    if (response.data?.code !== 200) return
    conversations.value = response.data.data || []
    if (!selectedConversation.value && conversations.value.length) {
      selectedConversation.value = conversations.value[0].id
    }
  } catch (error) {
    ElMessage.error('加载对话列表失败')
  }
}

async function fetchMessages(conversationId, beforeId = null) {
  const params = { limit: PAGE_SIZE }
  if (beforeId !== null) params.beforeId = beforeId
  const response = await request.get(`/ai/chat/conversations/${conversationId}/messages`, { params })
  if (response.data?.code !== 200) return []
  return response.data.data || []
}

async function loadMessages(conversationId) {
  loading.value = true
  try {
    const list = await fetchMessages(conversationId)
    if (selectedConversation.value !== conversationId) return
    messages.value = list
    hasMoreMessages.value = list.length >= PAGE_SIZE
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  } finally {
    if (selectedConversation.value === conversationId) loading.value = false
  }
}

async function loadEarlierMessages() {
  const first = messages.value[0]
  if (!first || typeof first.id !== 'number' || loadingMore.value || !hasMoreMessages.value) return

  loadingMore.value = true
  const el = messageList.value
  const previousHeight = el ? el.scrollHeight : 0

  try {
    const older = await fetchMessages(selectedConversation.value, first.id)
    messages.value = [...older, ...messages.value]
    hasMoreMessages.value = older.length >= PAGE_SIZE
    await nextTick()
    if (el) el.scrollTop = el.scrollHeight - previousHeight
  } catch (error) {
    ElMessage.error('加载更早消息失败')
  } finally {
    loadingMore.value = false
  }
}

async function handleCreateConversation() {
  abort()
  try {
    const response = await request.post('/ai/chat/conversations')
    if (response.data?.code !== 200) return null
    const conversation = response.data.data
    conversations.value.unshift(conversation)
    messages.value = []
    hasMoreMessages.value = false
    skipLoadFor.value = conversation.id
    selectedConversation.value = conversation.id
    railOpen.value = false
    return conversation.id
  } catch (error) {
    ElMessage.error('创建对话失败')
    return null
  }
}

function selectConversation(conversationId) {
  if (conversationId === selectedConversation.value) {
    railOpen.value = false
    return
  }
  abort()
  messages.value = []
  hasMoreMessages.value = false
  selectedConversation.value = conversationId
  railOpen.value = false
}

async function handleDeleteConversation(conversation) {
  try {
    await ElMessageBox.confirm(`确定删除「${conversation.title || '新对话'}」吗？`, '删除对话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }

  try {
    const response = await request.delete(`/ai/chat/conversations/${conversation.id}`)
    if (response.data?.code !== 200) return
    conversations.value = conversations.value.filter((item) => item.id !== conversation.id)
    if (selectedConversation.value === conversation.id) {
      abort()
      messages.value = []
      hasMoreMessages.value = false
      selectedConversation.value = conversations.value[0]?.id || null
    }
    ElMessage.success('对话已删除')
  } catch (error) {
    ElMessage.error('删除对话失败')
  }
}

async function handleDeleteMessage(message) {
  if (typeof message.id !== 'number') {
    messages.value = messages.value.filter((item) => item.id !== message.id)
    return
  }

  try {
    await ElMessageBox.confirm('确定删除这条消息吗？', '删除消息', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }

  try {
    const response = await request.delete(`/ai/chat/messages/${message.id}`)
    if (response.data?.code !== 200) return
    messages.value = messages.value.filter((item) => item.id !== message.id)
    ElMessage.success('消息已删除')
  } catch (error) {
    ElMessage.error('删除消息失败')
  }
}

function resizeComposer() {
  const el = composerRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 160)}px`
}

function handleComposerKeydown(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  sendMessage()
}

async function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text || isStreaming.value || loading.value) return

  let conversationId = selectedConversation.value
  if (!conversationId) {
    conversationId = await handleCreateConversation()
    if (!conversationId) return
  }

  if (!messages.value.length) {
    const conversation = conversations.value.find((item) => item.id === conversationId)
    if (conversation && (!conversation.title || conversation.title === '新对话')) {
      conversation.title = text.length > 28 ? `${text.slice(0, 28)}…` : text
    }
  }

  inputMessage.value = ''
  await nextTick()
  resizeComposer()
  await send(conversationId, text)
}

function useSuggestion(prompt) {
  if (isStreaming.value) return
  inputMessage.value = prompt
  sendMessage()
}

function formatConversationTime(time) {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const startOfDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dayDiff = Math.round((startOfToday - startOfDate) / 86400000)

  if (dayDiff === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (dayDiff === 1) return '昨天'
  if (date.getFullYear() === now.getFullYear()) {
    return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatMessageTime(time) {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

watch(selectedConversation, (conversationId) => {
  if (!conversationId) return
  if (skipLoadFor.value === conversationId) {
    skipLoadFor.value = null
    return
  }
  loadMessages(conversationId)
})

onMounted(loadConversations)

onBeforeUnmount(() => {
  abort()
})
</script>

<style scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

.ai-body {
  display: flex;
  flex: 1;
  min-height: 0;
  padding-top: 18px;
  padding-bottom: 24px;
}

.ai-ready {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  font-size: 12px;
  color: var(--eam-success);
  background: var(--eam-success-soft);
  border: 1px solid rgba(63, 125, 100, 0.16);
  border-radius: var(--eam-radius-pill);
}

.ai-ready i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--eam-success);
  box-shadow: 0 0 0 4px rgba(63, 125, 100, 0.12);
}

.ai-shell {
  position: relative;
  display: grid;
  grid-template-columns: 278px minmax(0, 1fr);
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--eam-line-soft);
  border-radius: 22px;
  box-shadow: var(--eam-shadow-md);
  animation: eam-rise 0.55s var(--eam-ease) both;
}

/* ---------- Conversation rail ---------- */

.ai-rail {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: linear-gradient(180deg, #f8f9fb 0%, #f3f4f7 100%);
  border-right: 1px solid var(--eam-line-soft);
}

.rail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 18px 14px;
}

.rail-head strong {
  display: block;
  margin-top: 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--eam-ink-800);
}

.rail-new {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 11px;
  font-size: 12px;
  font-weight: 500;
  color: var(--eam-primary);
  background: var(--eam-surface);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-sm);
  box-shadow: var(--eam-shadow-xs);
  cursor: pointer;
  transition: transform var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration-fast) var(--eam-ease),
    border-color var(--eam-duration-fast) var(--eam-ease);
}

.rail-new:hover {
  border-color: rgba(61, 86, 128, 0.24);
  box-shadow: var(--eam-shadow-sm);
  transform: translateY(-1px);
}

.rail-list {
  flex: 1;
  min-height: 0;
  padding: 4px 10px 12px;
  overflow-y: auto;
}

.conversation {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 3px;
  width: 100%;
  padding: 11px 13px;
  margin-bottom: 4px;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--eam-radius-sm);
  cursor: pointer;
  transition: background-color var(--eam-duration-fast) var(--eam-ease),
    border-color var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration-fast) var(--eam-ease);
}

.conversation:hover {
  background: rgba(255, 255, 255, 0.72);
}

.conversation.is-active {
  background: var(--eam-surface);
  border-color: var(--eam-line);
  box-shadow: var(--eam-shadow-xs);
}

.conversation.is-active::before {
  content: '';
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: -1px;
  width: 2px;
  border-radius: 2px;
  background: var(--eam-primary);
}

.conversation-title {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 500;
  color: var(--eam-ink-700);
}

.conversation.is-active .conversation-title {
  color: var(--eam-ink-900);
}

.conversation-time {
  font-size: 10.5px;
  color: var(--eam-ink-400);
  font-variant-numeric: tabular-nums;
}

.conversation-delete {
  position: absolute;
  top: 50%;
  right: 9px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  color: var(--eam-ink-400);
  border-radius: 7px;
  opacity: 0;
  transform: translateY(-50%);
  transition: opacity var(--eam-duration-fast) var(--eam-ease),
    color var(--eam-duration-fast) var(--eam-ease),
    background-color var(--eam-duration-fast) var(--eam-ease);
}

.conversation:hover .conversation-delete,
.conversation.is-active .conversation-delete {
  opacity: 1;
}

.conversation-delete:hover {
  color: var(--eam-danger);
  background: var(--eam-danger-soft);
}

.rail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 40px 18px;
  text-align: center;
  color: var(--eam-ink-400);
}

.rail-empty__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin-bottom: 4px;
  font-size: 17px;
  color: var(--eam-primary);
  background: var(--eam-primary-soft);
  border-radius: 13px;
}

.rail-empty strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--eam-ink-600);
}

.rail-empty span:last-child {
  font-size: 11px;
}

.rail-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 13px 18px;
  font-size: 10.5px;
  color: var(--eam-ink-400);
  border-top: 1px solid var(--eam-line-soft);
}

.rail-model {
  font-family: var(--eam-font-mono);
  letter-spacing: 0.01em;
}

.rail-divider {
  width: 1px;
  height: 10px;
  background: var(--eam-line-strong);
}

/* ---------- Chat area ---------- */

.ai-main {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background:
    radial-gradient(900px 380px at 82% -18%, rgba(61, 86, 128, 0.05), transparent 60%),
    radial-gradient(640px 320px at 0% 108%, rgba(169, 136, 95, 0.045), transparent 62%),
    var(--eam-surface);
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex: 0 0 auto;
  padding: 15px 24px;
  border-bottom: 1px solid var(--eam-line-soft);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
}

.chat-identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.chat-mark {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #fff;
  background: linear-gradient(150deg, #4a6490 0%, #2f4466 100%);
  border-radius: 12px;
  box-shadow: 0 8px 18px rgba(47, 68, 102, 0.2), var(--eam-shadow-inset);
}

.chat-copy {
  min-width: 0;
}

.chat-copy strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: var(--eam-ink-900);
}

.chat-copy span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--eam-ink-400);
}

.chat-status {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
  font-size: 11.5px;
  color: var(--eam-ink-400);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--eam-success);
  box-shadow: 0 0 0 4px rgba(63, 125, 100, 0.12);
}

.rail-toggle {
  display: none;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--eam-ink-500);
  background: var(--eam-surface);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-sm);
  cursor: pointer;
}

.message-scroll {
  flex: 1;
  min-height: 0;
  padding: 26px 26px 8px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 920px;
  margin: 0 auto;
}

.load-earlier {
  align-self: center;
  padding: 6px 14px;
  margin-bottom: 4px;
  font-size: 11.5px;
  color: var(--eam-ink-400);
  background: var(--eam-surface-muted);
  border: 1px solid var(--eam-line-soft);
  border-radius: var(--eam-radius-pill);
  cursor: pointer;
}

.load-earlier:hover {
  color: var(--eam-primary);
  border-color: rgba(61, 86, 128, 0.2);
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 660px;
  margin: 7vh auto 0;
  text-align: center;
}

.empty-mark {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  margin-bottom: 18px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #fff;
  background: linear-gradient(150deg, #4a6490 0%, #2f4466 100%);
  border-radius: 18px;
  box-shadow: 0 16px 34px rgba(47, 68, 102, 0.22), var(--eam-shadow-inset);
}

.chat-empty h2 {
  margin: 0;
  font-size: 23px;
  font-weight: 600;
  letter-spacing: -0.03em;
  color: var(--eam-ink-900);
}

.chat-empty p {
  max-width: 520px;
  margin: 9px 0 24px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--eam-ink-400);
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.suggestion {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 14px;
  text-align: left;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--eam-line-soft);
  border-radius: var(--eam-radius-md);
  box-shadow: var(--eam-shadow-xs);
  cursor: pointer;
  transition: transform var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration-fast) var(--eam-ease),
    border-color var(--eam-duration-fast) var(--eam-ease);
}

.suggestion:hover:not(:disabled) {
  border-color: rgba(61, 86, 128, 0.2);
  box-shadow: var(--eam-shadow-sm);
  transform: translateY(-2px);
}

.suggestion:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.suggestion-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  font-size: 15px;
  color: var(--eam-primary);
  background: var(--eam-primary-soft);
  border-radius: 10px;
}

.suggestion-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.suggestion-copy strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--eam-ink-800);
}

.suggestion-copy small {
  font-size: 11px;
  color: var(--eam-ink-400);
}

.suggestion-arrow {
  margin-left: auto;
  color: var(--eam-ink-300);
  transition: color var(--eam-duration-fast) var(--eam-ease),
    transform var(--eam-duration-fast) var(--eam-ease);
}

.suggestion:hover .suggestion-arrow {
  color: var(--eam-primary);
  transform: translateX(2px);
}

.scroll-bottom {
  position: absolute;
  right: 28px;
  bottom: 128px;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--eam-ink-500);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--eam-line);
  border-radius: 50%;
  box-shadow: var(--eam-shadow-sm);
  cursor: pointer;
  transition: transform var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration-fast) var(--eam-ease),
    color var(--eam-duration-fast) var(--eam-ease);
}

.scroll-bottom:hover {
  color: var(--eam-primary);
  box-shadow: var(--eam-shadow-md);
  transform: translateY(-1px);
}

/* ---------- Composer ---------- */

.composer-wrap {
  flex: 0 0 auto;
  padding: 8px 26px 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, rgba(250, 251, 252, 0.92) 34%);
}

.composer {
  width: 100%;
  max-width: 920px;
  margin: 0 auto;
  padding: 12px 14px 10px;
  background: var(--eam-surface);
  border: 1px solid var(--eam-line);
  border-radius: 18px;
  box-shadow: var(--eam-shadow-sm);
  transition: border-color var(--eam-duration) var(--eam-ease),
    box-shadow var(--eam-duration) var(--eam-ease);
}

.composer.is-focused {
  border-color: rgba(61, 86, 128, 0.32);
  box-shadow: 0 0 0 4px var(--eam-primary-ghost), var(--eam-shadow-sm);
}

.composer-input {
  display: block;
  width: 100%;
  min-height: 24px;
  max-height: 160px;
  padding: 2px 4px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.65;
  color: var(--eam-ink-800);
  background: transparent;
  border: 0;
  outline: none;
  resize: none;
}

.composer-input::placeholder {
  color: var(--eam-ink-300);
}

.composer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.composer-hint {
  padding-left: 4px;
  font-size: 10.5px;
  color: var(--eam-ink-300);
}

.composer-send,
.composer-stop {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 12.5px;
  font-weight: 500;
  border-radius: var(--eam-radius-pill);
  cursor: pointer;
  transition: transform var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration-fast) var(--eam-ease),
    background-color var(--eam-duration-fast) var(--eam-ease);
}

.composer-send {
  color: #fff;
  background: linear-gradient(180deg, #46608c 0%, #3a5177 100%);
  border: 0;
  box-shadow: 0 8px 18px rgba(47, 68, 102, 0.18), var(--eam-shadow-inset);
}

.composer-send:hover:not(:disabled) {
  box-shadow: 0 12px 24px rgba(47, 68, 102, 0.24), var(--eam-shadow-inset);
  transform: translateY(-1px);
}

.composer-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}

.composer-stop {
  color: var(--eam-danger);
  background: var(--eam-danger-soft);
  border: 1px solid rgba(164, 87, 87, 0.16);
}

.composer-stop:hover {
  background: #f5e3e3;
}

.composer-note {
  margin: 9px 0 0;
  font-size: 10.5px;
  text-align: center;
  color: var(--eam-ink-300);
}

/* ---------- Responsive ---------- */

@media (max-width: 1080px) {
  .ai-shell {
    grid-template-columns: 240px minmax(0, 1fr);
  }
}

@media (max-width: 820px) {
  .ai-body {
    padding: 12px 12px 16px;
  }

  .ai-shell {
    grid-template-columns: minmax(0, 1fr);
    border-radius: 18px;
  }

  .ai-rail {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: 20;
    width: 280px;
    border-right: 1px solid var(--eam-line);
    box-shadow: var(--eam-shadow-lg);
    transform: translateX(-102%);
    transition: transform var(--eam-duration) var(--eam-ease);
  }

  .ai-shell.rail-open .ai-rail {
    transform: translateX(0);
  }

  .rail-scrim {
    position: absolute;
    inset: 0;
    z-index: 19;
    background: rgba(20, 24, 33, 0.24);
    backdrop-filter: blur(2px);
    animation: eam-fade 0.2s var(--eam-ease) both;
  }

  .rail-toggle {
    display: grid;
  }

  .chat-head,
  .message-scroll,
  .composer-wrap {
    padding-right: 16px;
    padding-left: 16px;
  }

  .chat-empty {
    margin-top: 4vh;
  }

  .suggestion-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .scroll-bottom {
    right: 18px;
    bottom: 122px;
  }
}

@media (max-width: 560px) {
  .chat-status,
  .composer-hint {
    display: none;
  }

  .chat-empty h2 {
    font-size: 20px;
  }

  .chat-empty p {
    font-size: 12.5px;
  }

  .composer-bar {
    justify-content: flex-end;
  }
}
</style>
