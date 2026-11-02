<template>
  <article class="ai-message" :class="[`is-${msg.role}`, `status-${msg.status}`]">
    <div class="message-avatar">
      <span v-if="msg.role === 'assistant'" class="avatar-ai">AI</span>
      <span v-else class="avatar-user">{{ userInitial }}</span>
    </div>

    <div class="message-main">
      <div class="message-meta">
        <strong>{{ msg.role === 'assistant' ? '知识助手' : '我' }}</strong>
        <span v-if="formattedTime">{{ formattedTime }}</span>
      </div>

      <div class="message-bubble">
        <div v-if="isThinking" class="thinking">
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
          <span>正在检索知识库</span>
        </div>
        <template v-else>
          <div v-if="msg.role === 'assistant'" class="markdown-body" v-html="renderedHtml"></div>
          <p v-else class="plain-text">{{ msg.content }}</p>
          <span v-if="msg.status === 'streaming'" class="stream-caret" aria-hidden="true"></span>
        </template>
      </div>

      <div v-if="showActions" class="message-actions">
        <button type="button" class="message-action" @click="copyContent">复制</button>
        <button
          v-if="canDelete"
          type="button"
          class="message-action is-danger"
          @click="$emit('delete', msg)"
        >
          删除
        </button>
        <span v-if="msg.status === 'error'" class="message-error">回答中断</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { escapeHtml, renderMarkdown } from '../../utils/aiMarkdown'

const props = defineProps({
  msg: { type: Object, required: true },
  formattedTime: { type: String, default: '' },
  userInitial: { type: String, default: '我' }
})

defineEmits(['delete'])

const isThinking = computed(() => props.msg.status === 'thinking' && !props.msg.content)
const canDelete = computed(() => typeof props.msg.id === 'number')
const showActions = computed(() => Boolean(props.msg.content) && props.msg.status !== 'thinking')

const renderedHtml = ref('')
let flushTimer = null
let lastFlushAt = 0
const STREAM_RENDER_INTERVAL = 90

const buildHtml = () => {
  const content = props.msg.content || ''
  if (props.msg.role !== 'assistant') {
    renderedHtml.value = escapeHtml(content).replace(/\n/g, '<br>')
    return
  }
  renderedHtml.value = renderMarkdown(content)
}

const flushHtml = () => {
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }
  buildHtml()
  lastFlushAt = Date.now()
}

watch(
  () => [props.msg.role, props.msg.content, props.msg.status],
  () => {
    if (!props.msg.content) {
      renderedHtml.value = ''
      return
    }
    if (props.msg.status !== 'streaming') {
      flushHtml()
      return
    }
    const elapsed = Date.now() - lastFlushAt
    if (elapsed >= STREAM_RENDER_INTERVAL) {
      flushHtml()
      return
    }
    if (!flushTimer) {
      flushTimer = setTimeout(flushHtml, STREAM_RENDER_INTERVAL - elapsed)
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (flushTimer) clearTimeout(flushTimer)
})

const copyContent = async () => {
  try {
    await navigator.clipboard.writeText(props.msg.content || '')
    ElMessage.success('已复制')
  } catch (error) {
    ElMessage.error('复制失败，请手动选择文本')
  }
}
</script>

<style scoped>
.ai-message {
  display: flex;
  gap: 14px;
  padding: 6px 0;
  animation: message-in 0.36s var(--eam-ease) both;
}

.ai-message.is-user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex: 0 0 auto;
}

.avatar-ai,
.avatar-user {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.avatar-ai {
  color: #fff;
  background: linear-gradient(150deg, #4a6490 0%, #2f4466 100%);
  box-shadow: 0 8px 18px rgba(47, 68, 102, 0.2), var(--eam-shadow-inset);
}

.avatar-user {
  color: var(--eam-ink-600);
  background: var(--eam-surface-muted);
  border: 1px solid var(--eam-line);
}

.message-main {
  max-width: min(720px, 78%);
  min-width: 0;
}

.is-user .message-main {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
  padding: 0 2px;
}

.is-user .message-meta {
  flex-direction: row-reverse;
}

.message-meta strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--eam-ink-700);
}

.message-meta span {
  font-size: 11px;
  color: var(--eam-ink-400);
  font-variant-numeric: tabular-nums;
}

.message-bubble {
  position: relative;
  padding: 14px 18px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.72;
  color: var(--eam-ink-700);
}

.is-assistant .message-bubble {
  background: var(--eam-surface);
  border: 1px solid var(--eam-line-soft);
  border-top-left-radius: 6px;
  box-shadow: var(--eam-shadow-xs);
}

.is-user .message-bubble {
  color: #f8f9fb;
  background: linear-gradient(160deg, #46608c 0%, #3a5177 100%);
  border-top-right-radius: 6px;
  box-shadow: 0 10px 22px rgba(47, 68, 102, 0.18);
}

.status-error.is-assistant .message-bubble {
  background: var(--eam-danger-soft);
  border-color: rgba(164, 87, 87, 0.2);
  color: #7f4444;
}

.plain-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  color: var(--eam-ink-400);
  font-size: 12.5px;
}

.thinking-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--eam-primary);
  opacity: 0.55;
  animation: thinking 1.2s ease-in-out infinite;
}

.thinking-dot:nth-child(2) { animation-delay: 0.15s; }
.thinking-dot:nth-child(3) { animation-delay: 0.3s; }

.stream-caret {
  display: inline-block;
  width: 2px;
  height: 15px;
  margin-left: 3px;
  vertical-align: -2px;
  background: var(--eam-primary);
  animation: caret 0.9s step-end infinite;
}

.message-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding: 0 2px;
  opacity: 0;
  transition: opacity var(--eam-duration-fast) var(--eam-ease);
}

.ai-message:hover .message-actions {
  opacity: 1;
}

.message-action {
  padding: 3px 8px;
  font-size: 11.5px;
  color: var(--eam-ink-400);
  background: transparent;
  border: 0;
  border-radius: var(--eam-radius-xs);
  cursor: pointer;
  transition: color var(--eam-duration-fast) var(--eam-ease),
    background-color var(--eam-duration-fast) var(--eam-ease);
}

.message-action:hover {
  color: var(--eam-ink-700);
  background: var(--eam-surface-muted);
}

.message-action.is-danger:hover {
  color: var(--eam-danger);
  background: var(--eam-danger-soft);
}

.message-error {
  margin-left: 2px;
  font-size: 11px;
  color: var(--eam-danger);
}

/* Markdown */
.markdown-body :deep(*) {
  margin-top: 0;
}

.markdown-body :deep(p) {
  margin: 0 0 0.72em;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 1.1em 0 0.5em;
  color: var(--eam-ink-900);
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.35;
}

.markdown-body :deep(h1) { font-size: 18px; }
.markdown-body :deep(h2) { font-size: 16px; }
.markdown-body :deep(h3) { font-size: 14.5px; }

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.3em 0 0.8em;
  padding-left: 1.3em;
}

.markdown-body :deep(li) {
  margin: 0.25em 0;
}

.markdown-body :deep(strong) {
  color: var(--eam-ink-900);
  font-weight: 650;
}

.markdown-body :deep(code) {
  padding: 1px 6px;
  font-family: var(--eam-font-mono);
  font-size: 12.5px;
  color: var(--eam-primary-deep);
  background: var(--eam-primary-soft);
  border-radius: 5px;
}

.markdown-body :deep(pre) {
  margin: 0.7em 0;
  padding: 12px 14px;
  overflow-x: auto;
  background: #f6f7f9;
  border: 1px solid var(--eam-line-soft);
  border-radius: var(--eam-radius-sm);
}

.markdown-body :deep(pre code) {
  padding: 0;
  color: var(--eam-ink-700);
  background: transparent;
}

.markdown-body :deep(blockquote) {
  margin: 0.7em 0;
  padding: 6px 14px;
  color: var(--eam-ink-500);
  background: var(--eam-surface-muted);
  border-left: 2px solid var(--eam-primary);
  border-radius: 0 var(--eam-radius-xs) var(--eam-radius-xs) 0;
}

.markdown-body :deep(table) {
  width: 100%;
  margin: 0.7em 0;
  border-collapse: collapse;
  font-size: 13px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 7px 10px;
  border: 1px solid var(--eam-line);
  text-align: left;
}

.markdown-body :deep(th) {
  color: var(--eam-ink-500);
  background: var(--eam-surface-muted);
  font-weight: 600;
}

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes thinking {
  0%, 100% { opacity: 0.35; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-2px); }
}

@keyframes caret {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@media (max-width: 720px) {
  .message-main {
    max-width: calc(100% - 48px);
  }

  .message-actions {
    opacity: 1;
  }
}
</style>
